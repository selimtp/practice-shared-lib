def call(Map config) {

    pipeline {
        agent any

        stages {
            stage('Init') {
                steps {
                    script {
                        dockerToolInstance = new DockerTool()
                        dockerToolInstance.init(config)
                    }
                }
            }

            stage('Build and Push Image') {
                steps {
                    script {
                        dockerToolInstance.buildAndPush(config)
                    }
                }
            }
            {
                stage('Deploy from Registry') {
                    steps {
                        script {
                            dockerToolInstance.deployFromRegistry(config)
                        }
                    }
                }
            }
        }

        post {
            success {
                echo "Image pushed successfully"
            }
            failure {
                echo "Docker build/push failed"
            }
        }
    }
}