def call(Map config) {

    pipeline {
        agent any

        stages {
            stage('Init') {
                steps {
                    script {
                        dockerTool = new DockerTool()
                        dockerTool.init(config)
                    }
                }
            }

            stage('Build and Push Image') {
                steps {
                    script {
                        dockerTool.buildAndPush()
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