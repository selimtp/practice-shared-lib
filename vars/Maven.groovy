def config

def init(Map cfg) {
    this.config = cfg
    println "[Maven] initialized with config: ${config}"
}

def build() {
    def mavenBuildCommand = config.buildCommand ?: "mvn clean package -U"
    println "[Maven] Pretending to run: ${mavenBuildCommand}"
    sh "echo 'FAKE BUILD RUNNING: ${mavenBuildCommand}'"
    sh "echo 'BUILD SUCCESSFUL (simulated)'"
}
