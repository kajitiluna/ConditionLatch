package kajitiluna.utility.conditionlatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author kajitiluna
 *
 */
public class ConditionLatch<SUCCESS_RESULT, FAILED_RESULT> {

    private final ReadWriteLock successListLock_;

    private final ReadWriteLock failedListLock_;

    private final AtomicInteger successTaskCount_;

    private final AtomicInteger failedTaskCount_;

    private final List<SUCCESS_RESULT> successList_;

    private final List<FAILED_RESULT> failedList_;

    private final UnionSynchronizer synchronizer_;

    public ConditionLatch(int succseccCount) {
        this(succseccCount, 1);
    }

    public ConditionLatch(int succseccCount, int failedCount) {
        if (succseccCount < 0) {
            throw new IllegalArgumentException("succseccCount < 0");
        }

        if (failedCount < 0) {
            throw new IllegalArgumentException("failedCount < 0");
        }

        this.successListLock_ = new ReentrantReadWriteLock();
        this.successTaskCount_ = new AtomicInteger(0);
        this.successList_ = new ArrayList<SUCCESS_RESULT>(succseccCount + 1);

        this.failedListLock_ = new ReentrantReadWriteLock();
        this.failedTaskCount_ = new AtomicInteger(0);
        this.failedList_ = new ArrayList<FAILED_RESULT>(failedCount + 1);

        this.synchronizer_ = new UnionSynchronizer(succseccCount, failedCount);
    }

    public void submit(SUCCESS_RESULT result) {
        Lock lock = this.successListLock_.writeLock();
        lock.lock();
        try {
            this.successList_.add(result);
            this.successTaskCount_.addAndGet(1);
        }
        finally {
            lock.unlock();
        }

        // TODO
    }

    public void submitForFail(FAILED_RESULT resut) {
        Lock lock = this.failedListLock_.writeLock();
        lock.lock();
        try {
            this.failedList_.add(resut);
            this.failedTaskCount_.addAndGet(1);
        }
        finally {
            lock.unlock();
        }

        // TODO
    }

    public void interrupt() {
        // TODO
    }

    public List<SUCCESS_RESULT> await() throws SubmittedFailureResultException, InterruptedException {

        // TODO

        return null;
    }

    public List<SUCCESS_RESULT> await(long timeout, TimeUnit timeUnit) throws SubmittedFailureResultException,
            TimeoutException, InterruptedException {
        // TODO

        return null;
    }

}
