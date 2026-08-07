def config

def init(Map cfg) {
    this.config = cfg
    println "[Execute] initialized with config: ${config}"
}

def runIntegrationTestCommands() {
    // Just like the real Execute.groovy: this reads config directly.
    // If init(config) was never called, config is null here -> NPE.
    if (!(config.integrationTestCommands instanceof Map)) {
        error("integrationTestCommands must be a Map<String, List>")
    }
    println "[Execute] Running fake integration tests..."
    sh "echo 'FAKE INTEGRATION TEST RUN'"
}
