def config

def init(Map cfg) {
    this.config = cfg
    println "[Notification] initialized with config: ${config}"
}

private String resolveRecipient() {
    def manualCause = currentBuild.getBuildCauses().find { it._class?.contains('UserIdCause') }
    if (manualCause) {
        return "trigger-user (${manualCause.userId ?: 'unknown'})"
    }
    return "commit-author (simulated)"
}

def sendSuccessEmailToTriggerUserOrCommitAuthor() {
    def recipient = resolveRecipient()
    println "[Notification] Would send SUCCESS email to: ${recipient}"
    sh "echo 'FAKE EMAIL: SUCCESS notification sent to ${recipient}'"
}

def sendFailEmailToTriggerUserOrCommitAuthor() {
    def recipient = resolveRecipient()
    println "[Notification] Would send FAILURE email to: ${recipient}"
    sh "echo 'FAKE EMAIL: FAILURE notification sent to ${recipient}'"
}

def updateBranchAndParentJobDescription(String message) {
    println "[Notification] Updating job description to: '${message}'"
    currentBuild.description = message
}