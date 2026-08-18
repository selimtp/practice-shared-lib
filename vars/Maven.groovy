def config

def init(Map cfg) {
    this.config = cfg
    println "[Maven] initialized with config: ${config}"
}

def build() {
    def mavenBuildCommand = config.buildCommand ?: "mvn clean package -U"
    println "[Maven] Running: ${mavenBuildCommand}"
    sh "${mavenBuildCommand}"
}