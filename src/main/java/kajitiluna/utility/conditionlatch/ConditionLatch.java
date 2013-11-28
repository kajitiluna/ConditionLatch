package kajitiluna.utility.conditionlatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

    private final ReadWriteLock failureListLock_;

    private final List<SUCCESS_RESULT> successList_;

    private final List<FAILED_RESULT> failureList_;

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
        this.successList_ = new ArrayList<SUCCESS_RESULT>(succseccCount + 1);

        this.failureListLock_ = new ReentrantReadWriteLock();
        this.failureList_ = new ArrayList<FAILED_RESULT>(failedCount + 1);

        this.synchronizer_ = new UnionSynchronizer(succseccCount, failedCount);
    }

    public void submit(SUCCESS_RESULT result) {
        Lock lock = this.successListLock_.writeLock();
        lock.lock();
        try {
            this.successList_.add(result);
        } finally {
            lock.unlock();
        }

        this.synchronizer_.releaseShared(1);
    }

    public void submitForFail(FAILED_RESULT resut) {
        Lock lock = this.failureListLock_.writeLock();
        lock.lock();
        try {
            this.failureList_.add(resut);
        } finally {
            lock.unlock();
        }

        this.synchronizer_.releaseShared(-1);
    }

    public List<SUCCESS_RESULT> await() throws SubmittedFailureResultException, InterruptedException {
        this.synchronizer_.acquireSharedInterruptibly(1);

        return this.returnResult();
    }

    public List<SUCCESS_RESULT> await(long timeout, TimeUnit timeUnit) throws SubmittedFailureResultException,
            TimeoutException, InterruptedException {
        this.synchronizer_.tryAcquireSharedNanos(1, timeUnit.toNanos(timeout));

        return this.returnResult();
    }

    private List<SUCCESS_RESULT> returnResult() throws SubmittedFailureResultException {
        int successCount = this.synchronizer_.getSuccessCount();

        if (successCount > 0) {
            throw new SubmittedFailureResultException("Failed procedure.");
        }

        List<SUCCESS_RESULT> successList = this.copyList(this.successList_, this.successListLock_);

        return successList;
    }

    private <TYPE> List<TYPE> copyList(List<TYPE> srcList, ReadWriteLock baseLock) {
        List<TYPE> copyList;

        Lock lock = baseLock.readLock();
        try {
            copyList = Collections.unmodifiableList(new ArrayList<TYPE>(srcList));
        } finally {
            lock.unlock();
        }

        return copyList;
    }

    public List<SUCCESS_RESULT> getSuccessList() {
        return this.copyList(this.successList_, this.successListLock_);
    }

    public List<FAILED_RESULT> getFailureList() {
        return this.copyList(this.failureList_, this.failureListLock_);
    }
}
