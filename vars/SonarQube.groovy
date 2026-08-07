def config

def init(Map cfg) {
    this.config = cfg
    println "[SonarQube] initialized with config: ${config}"
}

private boolean isSonarScanEnabled(Map config) {
    if (!GlobalConstants.ENABLE_SONAR) {
        println "[SonarQube] disabled globally"
        return false
    }
    if (!GlobalConstants.SONAR_SCAN_BRANCH_LIST.any { env.BRANCH_NAME.contains(it) }) {
        println "[SonarQube] not enabled for branch ${env.BRANCH_NAME}"
        return false
    }
    return true
}

def scan() {
    println "[SonarQube] Pretending to run sonar:sonar scan..."
    sh "echo 'FAKE SONAR SCAN for project ${config.appServiceName}'"
}
