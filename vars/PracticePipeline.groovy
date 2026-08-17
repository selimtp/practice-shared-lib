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
                    expression { GlobalConstants.BUILD_BRANCH_LIST.any { env.BRANCH_NAME.contains(it) } && env.SKIP_BUILD_AND_TEST != 'true' }
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
                    expression { sonarQube.isSonarScanEnabled(config) }
                }
                steps {
                    script {
                        sonarQube.scan()
                    }
                }
            }

            stage('Integration Test') {
                steps {
                    script {
                        catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                            execute.runIntegrationTestCommands()
                        }
                    }
                }
            }
        }

        post {
    success {
        script {
            notification.sendSuccessEmailToTriggerUserOrCommitAuthor()
        }
    }
    failure {
        script {
            notification.sendFailEmailToTriggerUserOrCommitAuthor()
        }
    }
    always {
        script {
            notification.updateBranchAndParentJobDescription('Practice lab pipeline ran')
        }
    }
        }
    }
}
