def call(Map config) {

    pipeline {
        agent any

        stages {
            stage('Init') {
                steps {
                    script {
                        def dockerToolInstance = new DockerTool()
                        dockerToolInstance.init(config)
                    }
                }
            }

            stage('Build and Push Image') {
                steps {
                    script {
                        dockerToolInstance.buildAndPush()
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