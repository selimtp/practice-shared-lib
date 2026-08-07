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
                        Execute execute = new Execute()
                        execute.init(config)
                        // NOTE: execute is deliberately NOT initialized here,
                        // just like in the real MPL_Maven_Weblogic.groovy.
                        // Try running this, see it fail/go UNSTABLE, then
                        // add: execute = new Execute(); execute.init(config)
                        // and re-run to see the difference.
                    }
                }
            }

            stage('Build') {
                when {
                    beforeAgent true
                    expression { GlobalConstants.BUILD_BRANCH_LIST.any { env.BRANCH_NAME.contains(it) } }
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
                echo "Pipeline SUCCESS"
            }
            unstable {
                echo "Pipeline UNSTABLE — check which stage went yellow"
            }
            failure {
                echo "Pipeline FAILED"
            }
            always {
                echo "Pipeline finished with result: ${currentBuild.result}"
            }
        }
    }
}
