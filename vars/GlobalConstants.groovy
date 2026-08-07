class GlobalConstants {
    static final boolean ENABLE_SONAR = true
    static final List BUILD_BRANCH_LIST = ['develop', 'integration', 'releasable', 'PR-']
    static final List SONAR_SCAN_BRANCH_LIST = ['develop', 'releasable']
    static final int PIPELINE_TIMEOUT_IN_MINUTES = 30
    static final int PIPELINE_LOG_ROTATOR_NUMBER_TO_KEEP = 10
}
