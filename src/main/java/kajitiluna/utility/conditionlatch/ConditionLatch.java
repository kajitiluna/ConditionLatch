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
 * A synchronization aid that allows one or more threads to wait
 * until a set of operations being performed in other threads completes successful or alternatively failed.
 *
 * <p>
 * A {@code ConditionLatch} has the feature similar to alternative {@code CountDownLatch}.
 * A {@code ConditionLatch} is initialized with two given parameters : <em>success count</em> and <em>failure count</em>.
 * The {@link #await await} methods block until either <em>success count</em> or <em>failure count</em> reaches zero.
 * These counts are decreased by the invocation of each {@link #submit} or {@link #submitForFail} methods.
 * When the either count reached to zero, all waiting threads are released and any subsequent invocations of
 * {@link #await await} method returns immediately.
 * These counts cannot be reset like a {@code CountDownLatch}.
 * </p>
 *
 * <p>
 * When <em>success count</em> reaches to zero, {@link #await} method returns a list of submitted objects
 * in {@link #submit} method's argument.
 * On the other, when <em>failure count</em> reaches to zero,
 * {@link #await} method returns a {@code SubmittedFailureResultException}.
 * Thus, these responses of {@link #await} method can control the subsequences of this method.
 * </p>
 *
 * @author kajitiluna
 *
 * @param <SUCCESS_RESULT>
 * @param <FAILURE_RESULT>
 */
public class ConditionLatch<SUCCESS_RESULT, FAILURE_RESULT> {

    private final ReadWriteLock successListLock_;

    private final ReadWriteLock failureListLock_;

    private final List<SUCCESS_RESULT> successList_;

    private final List<FAILURE_RESULT> failureList_;

    private final UnionSynchronizer synchronizer_;

    public ConditionLatch(int succseccCount) throws IllegalArgumentException {
        this(succseccCount, 1);
    }

    public ConditionLatch(int succseccCount, int failedCount) throws IllegalArgumentException {
        this.synchronizer_ = new UnionSynchronizer(succseccCount, failedCount);

        this.successListLock_ = new ReentrantReadWriteLock();
        this.successList_ = this.createList(succseccCount + 1);

        this.failureListLock_ = new ReentrantReadWriteLock();
        this.failureList_ = this.createList(failedCount + 1);
    }

    protected <TYPE> List<TYPE> createList(int capacity) {
        return new ArrayList<TYPE>(capacity);
    }

    public void submit(SUCCESS_RESULT result) {
        Lock lock = this.successListLock_.writeLock();
        lock.lock();
        try {
            this.successList_.add(result);
        } finally {
            lock.unlock();
        }

        this.submit();
    }

    public final void submit() {
        this.synchronizer_.releaseShared(1);
    }

    public void submitForFail(FAILURE_RESULT resut) {
        Lock lock = this.failureListLock_.writeLock();
        lock.lock();
        try {
            this.failureList_.add(resut);
        } finally {
            lock.unlock();
        }

        this.submitForFail();
    }

    public final void submitForFail() {
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
        lock.lock();
        try {
            copyList = Collections.unmodifiableList(new ArrayList<TYPE>(srcList));
        } finally {
            lock.unlock();
        }

        return copyList;
    }

    public final List<SUCCESS_RESULT> getSuccessList() {
        return this.copyList(this.successList_, this.successListLock_);
    }

    public final List<FAILURE_RESULT> getFailureList() {
        return this.copyList(this.failureList_, this.failureListLock_);
    }
}
