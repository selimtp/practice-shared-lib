def config

def init(Map cfg) {
    this.config = cfg
    println "[SonarQube] initialized with config: ${config}"
}
def mvn() {
    withSonarQubeEnv('sonarqube-lab') {
        sh "mvn clean verify sonar:sonar -Dsonar.projectKey=${config.sonarProjectKey}"
    }
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
def qualityGate() {
    timeout(time: 3, unit: 'MINUTES') {
        def qGate = waitForQualityGate()
        if (qGate.status != 'OK') {
            error "Quality gate başarısız: ${qGate.status}"
        }
        println "[SonarQubeTool] Quality gate PASSED"
    }
}

