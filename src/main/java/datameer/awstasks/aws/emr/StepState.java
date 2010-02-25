package datameer.awstasks.aws.emr;


public enum StepState {

    PENDING(false), RUNNING(false), COMPLETED(true), CANCELLED(true), FAILED(true), INTERRUPTED(true);

    private final boolean _finished;

    private StepState(boolean finished) {
        _finished = finished;
    }

    public boolean isFinished() {
        return _finished;
    }

    public boolean isSuccessful() {
        return this == StepState.COMPLETED;
    }

}
