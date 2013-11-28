package kajitiluna.utility.conditionlatch;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @author kajitiluna
 *
 */
class UnionSynchronizer extends AbstractQueuedSynchronizer {

    /** serialVersionUID. */
    private static final long serialVersionUID = -1925006638712213997L;

    /**
     * Constructor.
     *
     * @param successCount
     */
    public UnionSynchronizer(int successCount) {
        this(successCount, 1);
    }

    /**
     * Constructor.
     *
     */
    public UnionSynchronizer(int successCount, int failureCount) {
        this.checkParameter(successCount, "successCount");
        this.checkParameter(failureCount, "failureCount");

        int status = this.convertToState(successCount, failureCount);
        this.setState(status);
    }

    private void checkParameter(int param, String name) {
        if (param < 0) {
            throw new IllegalArgumentException(name + " < 0");
        }

        if (param > 0x0000FFFF) {
            throw new IllegalArgumentException(name + " > " + 0x0000FFFF);
        }
    }

    private int convertToState(int successCount, int failureCount) {
        int status = successCount | (failureCount << 16);
        return status;
    }

    public int getSuccessCount() {
        int success = this.getState() & 0x0000FFFF;
        return success;
    }

    public int getFailureCount() {
        int failure = (this.getState() & 0xFFFF0000) >>> 16;
        return failure;
    }

    public boolean releaseSharedInSuccess() {
        return this.releaseShared(1);
    }

    public boolean releaseSharedInFailure() {
        return this.releaseShared(-1);
    }

    @Override
    protected boolean tryReleaseShared(int releases) {
        while (true) {
            int successCount = this.getSuccessCount();
            int failureCount = this.getFailureCount();

            if ((successCount <= 0) || (failureCount <= 0)) {
                return false;
            }

            int nowState = this.getState();

            int nextCount;
            int nextState;
            if (releases >= 0) {
                nextCount = successCount - 1;
                nextState = this.convertToState(nextCount, failureCount);
            } else {
                nextCount = failureCount - 1;
                nextState = this.convertToState(successCount, nextCount);
            }

            if (this.compareAndSetState(nowState, nextState) == false) {
                continue;
            }

            return (nextCount == 0);
        }
    }

    @Override
    protected int tryAcquireShared(int acquires) {
        int successCount = this.getSuccessCount();
        int failureCount = this.getFailureCount();

        int result = -1;
        if ((successCount == 0) || (failureCount == 0)) {
            result = 1;
        }

        return result;
    }
}
