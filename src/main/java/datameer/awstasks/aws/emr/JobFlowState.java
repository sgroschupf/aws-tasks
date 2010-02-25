package datameer.awstasks.aws.emr;

public enum JobFlowState {

    COMPLETED(true), FAILED(false), TERMINATED(false), RUNNING(true), SHUTTING_DOWN(false), STARTING(false), WAITING(true);

    private final boolean _operational;

    private JobFlowState(boolean operational) {
        _operational = operational;
    }

    public boolean isOperational() {
        return _operational;
    }
}
