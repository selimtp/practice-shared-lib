def config

def init(Map cfg) {
    this.config = cfg ?: [:]
    println "[Execute] initialized with config: ${config}"
}

def runIntegrationTestCommands() {
    if (config == null) {
        error("[Execute] init(config) must be called before runIntegrationTestCommands()")
    }

    def commandGroups = config.integrationTestCommands

    if (commandGroups == null) {
        println "[Execute] no integrationTestCommands configured - skipping integration tests"
        return
    }

    if (!(commandGroups instanceof Map)) {
        error("[Execute] integrationTestCommands must be a Map<String, List>, got ${commandGroups.getClass().name}")
    }

    // Plain for-loops instead of .each {} : closures over pipeline steps are the
    // usual source of CPS / NotSerializableException problems in shared libraries.
    for (String groupName : commandGroups.keySet()) {
        def commands = commandGroups[groupName]

        if (!(commands instanceof List)) {
            error("[Execute] integrationTestCommands['${groupName}'] must be a List, got ${commands?.getClass()?.name}")
        }

        println "[Execute] running ${commands.size()} command(s) for '${groupName}'"

        for (int i = 0; i < commands.size(); i++) {
            String command = commands[i]?.toString()?.trim()
            if (!command) {
                println "[Execute] [${groupName}] skipping empty command at index ${i}"
                continue
            }
            println "[Execute] [${groupName}] > ${command}"
            sh command
        }
    }

    println "[Execute] integration tests finished"
}
