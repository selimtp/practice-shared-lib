def call(Map config) {

    pipeline {
        agent any

        options {
            buildDiscarder(logRotator(numToKeepStr: "${GlobalConstants.PIPELINE_LOG_ROTATOR_NUMBER_TO_KEEP}"))
            timestamps()
            timeout(time: GlobalConstants.PIPELINE_TIMEOUT_IN_MINUTES, unit: 'MINUTES')
        }

        stages {

            stage('Init') {
                steps {
                    script {
                        maven = new Maven()
                        maven.init(config)
                        sonarQube = new SonarQube()
                        sonarQube.init(config)
                        execute = new Execute()
                        execute.init(config)
                        notification = new Notification()
                        notification.init(config)
                        dockerToolInstance = new DockerTool()
                        dockerToolInstance.init(config)

                        // Docker stages only make sense when the job actually provides
                        // docker settings, so Jenkinsfiles without them keep working.
                        env.DOCKER_ENABLED = (config.dockerHubUsername && config.imageName && config.dockerCredentialsId) ? 'true' : 'false'
                        if (env.DOCKER_ENABLED != 'true') {
                            echo "No docker configuration found (dockerHubUsername / imageName / dockerCredentialsId). Docker stages will be skipped."
                        }

                        echo " Looking for changes in files"
                        def changedFilesRaw = sh(script: "git diff-tree --no-commit-id --name-status -r HEAD || true", returnStdout: true).trim()
                        println "Changes:\n${changedFilesRaw}"
                        if (changedFilesRaw && !changedFilesRaw.contains('.java') && changedFilesRaw.contains('.md')) {
                            env.SKIP_BUILD_AND_TEST = 'true'
                            echo "No Java code changes detected. Skipping build and test stages. Only documentation changes detected."
                        } else {
                            env.SKIP_BUILD_AND_TEST = 'false'
                        }
                    }
                }
            }

            stage('Build') {
                when {
                    beforeAgent true
                    expression { branchMatches(GlobalConstants.BUILD_BRANCH_LIST) && env.SKIP_BUILD_AND_TEST != 'true' }
                }
                steps {
                    script {
                        echo "Build started"
                        maven.build()
                        echo "Build finished"
                    }
                }
            }

            stage('SonarQube Scan') {
                when {
                    beforeAgent true
                    expression { GlobalConstants.ENABLE_SONAR && branchMatches(GlobalConstants.SONAR_SCAN_BRANCH_LIST) && env.SKIP_BUILD_AND_TEST != 'true' }
                }
                steps {
                    script {
                        echo "SonarQube scan started"
                        sonarQube.mvn()
                        echo "SonarQube scan finished"
                    }
                }
            }

            stage('SonarQube Quality Gate') {
                when {
                    beforeAgent true
                    expression { GlobalConstants.ENABLE_SONAR && branchMatches(GlobalConstants.SONAR_SCAN_BRANCH_LIST) && env.SKIP_BUILD_AND_TEST != 'true' }
                }
                steps {
                    script {
                        echo "Waiting for SonarQube Quality Gate result"
                        sonarQube.qualityGate()
                        echo "SonarQube Quality Gate passed"
                    }
                }
            }

            stage('Integration Test') {
                when {
                    expression { env.SKIP_BUILD_AND_TEST != 'true' }
                }
                steps {
                    script {
                        catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                            execute.runIntegrationTestCommands()
                        }
                    }
                }
            }

            stage('Build and Push Image') {
                when {
                    beforeAgent true
                    // Needs target/*.jar from the Build stage, so it must not run when that was skipped.
                    expression { env.DOCKER_ENABLED == 'true' && branchMatches(GlobalConstants.DOCKER_BRANCH_LIST) && env.SKIP_BUILD_AND_TEST != 'true' }
                }
                steps {
                    script {
                        echo "Docker image build and push started"
                        dockerToolInstance.buildAndPush()
                        echo "Docker image build and push finished"
                    }
                }
            }

            stage('Deploy from Registry') {
                when {
                    beforeAgent true
                    expression { env.DOCKER_ENABLED == 'true' && branchMatches(GlobalConstants.DOCKER_BRANCH_LIST) && env.SKIP_BUILD_AND_TEST != 'true' }
                }
                steps {
                    script {
                        echo "Deploy started"
                        dockerToolInstance.deployFromRegistry()
                        echo "Deploy finished"
                    }
                }
            }
        }

        post {
            success {
                script {
                    if (env.DOCKER_ENABLED == 'true') {
                        echo "Image pushed and deployed successfully"
                    }
                    notification.sendSuccessEmailToTriggerUserOrCommitAuthor()
                }
            }
            unstable {
                script {
                    notification.sendFailEmailToTriggerUserOrCommitAuthor()
                }
            }
            failure {
                script {
                    if (env.DOCKER_ENABLED == 'true') {
                        echo "Pipeline failed - check build, sonar or docker build/push stages"
                    }
                    notification.sendFailEmailToTriggerUserOrCommitAuthor()
                }
            }
            always {
                script {
                    notification.updateBranchAndParentJobDescription('Practice lab full pipeline ran')
                }
            }
        }
    }
}

private boolean branchMatches(List branchList) {
    String branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: ''
    return branchList.any { branch.contains(it) }
}
