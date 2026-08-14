def config

def init(Map cfg) {
    this.config = cfg
    println "[DockerTool] initialized with config: ${config}"
}

def buildAndPush() {
    def imageName = "${config.dockerHubUsername}/${config.imageName}:${env.BUILD_NUMBER}"

    withCredentials([usernamePassword(
        credentialsId: config.dockerCredentialsId,
        usernameVariable: 'DOCKER_USER',
        passwordVariable: 'DOCKER_PASS'
    )]) {
        sh "echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin"
        sh "docker build -t ${imageName} ."
        sh "docker push ${imageName}"
        sh "docker logout"
    }
    
    println "[DockerTool] Pushed image: ${imageName}"
}