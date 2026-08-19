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

def deployFromRegistry() {
    def imageName = "${config.dockerHubUsername}/${config.imageName}:${env.BUILD_NUMBER}"
    def containerName = config.containerName ?: "${config.imageName}-container"
    def appPort = config.appPort ?: 5000
    // The container is started through the mounted docker socket, so -p publishes
    // the port on the HOST, not inside the Jenkins container. Reaching it from here
    // needs the host gateway name, the same one the SonarQube server URL uses.
    def appHost = config.appHost ?: 'host.docker.internal'

    echo "Image pulling from docker registry: ${imageName}"
    sh "docker pull ${imageName}"

    echo "Old container removal if exists: ${containerName}"
    sh "docker rm -f ${containerName} || true"

    echo "Deploying new container from image: ${imageName}"
    sh "docker run -d -p ${appPort}:${appPort} --name ${containerName} ${imageName}"

    verifyDeployment(containerName, appHost, appPort)
}

def verifyDeployment(String containerName, String appHost, def appPort) {
    def baseUrl = "http://${appHost}:${appPort}"
    echo "[DockerTool] Waiting for the application to answer on ${baseUrl}"

    def status = sh(
        script: """
            for i in \$(seq 1 30); do
                if curl -fsS ${baseUrl}/health > /dev/null 2>&1; then
                    exit 0
                fi
                sleep 2
            done
            exit 1
        """,
        returnStatus: true
    )

    if (status != 0) {
        echo "[DockerTool] Application never answered - last container logs:"
        sh "docker logs --tail 50 ${containerName} || true"
        error("[DockerTool] Application is not reachable on ${baseUrl} after 60 seconds")
    }

    sh "curl -fsS ${baseUrl}/"
    println "[DockerTool] Deployment verified, application reachable on ${baseUrl}"
}