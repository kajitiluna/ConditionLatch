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
 * When the either count reaches to zero, all waiting threads are released and any subsequent invocations of
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
 * @param <SUCCESS_RESULT> Object type submitted in success procedure
 * @param <FAILURE_RESULT> Object type submitted in failed procedure
 */
public class ConditionLatch<SUCCESS_RESULT, FAILURE_RESULT> {

    /** Lock object of handling success list. */
    private final ReadWriteLock successListLock_;

    /** Lock object of handling failure list. */
    private final ReadWriteLock failureListLock_;

    /** List in submitting success procedure. */
    private final List<SUCCESS_RESULT> successList_;

    /** List in submitting failure procedure. */
    private final List<FAILURE_RESULT> failureList_;

    /** Synchronizer of count down status. */
    private final UnionSynchronizer synchronizer_;

    /**
     * Constructs a {@code ConditionLatch} initialized with one given success count.
     * The other parameter is initialized as 1.
     *
     * @param succseccCount the number of times {@link #submit} must be invoked
     *   before threads can pass through {@link #await}
     * @throws IllegalArgumentException if {@code succseccCount} is negative or over <code>0x0000FFFF</code>.
     */
    public ConditionLatch(int succseccCount) throws IllegalArgumentException {
        this(succseccCount, 1);
    }

    /**
     * Constructs a {@code ConditionLatch} initialized with two given success count.
     *
     * @param succseccCount the number of times {@link #submit} must be invoked
     *   before threads can pass through {@link #await}
     * @param failureCount the number of times {@link #submitForFail} must be invoked
     *   before threads can pass through {@link #await}
     * @throws IllegalArgumentException if either {@code succseccCount} or {@code failureCount}
     *   are negative or over <code>0x0000FFFF</code>.
     */
    public ConditionLatch(int succseccCount, int failureCount) throws IllegalArgumentException {
        this.synchronizer_ = new UnionSynchronizer(succseccCount, failureCount);

        this.successListLock_ = new ReentrantReadWriteLock();
        this.successList_ = this.createList(succseccCount + 1);

        this.failureListLock_ = new ReentrantReadWriteLock();
        this.failureList_ = this.createList(failureCount + 1);
    }

    /**
     * Creates a {@code List} in use of initializing of success and failure lists.
     * By over-riding this method, customize the type of success and failure lists.
     *
     * @param capacity the initial capacity of the list
     * @return {@code List}
     */
    protected <TYPE> List<TYPE> createList(int capacity) {
        return new ArrayList<TYPE>(capacity);
    }

    /**
     * Decrements the success count with submitting success result.
     * If the success count reaches zero, all waiting threads are released.
     * The submitted results are available at the thread invocating {@link #await}.
     *
     * @param result successful procedure's object available at the thread invocating {@link #await}
     */
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

    /**
     * Decrements the success count, releasing all waiting threads if the success count reaches zero.
     *
     */
    public final void submit() {
        this.synchronizer_.releaseShared(1);
    }

    /**
     * Decrements the failure count with submitting failure result.
     * If the failure count reaches zero, all waiting threads are released.
     * The submitted results are available at the thread invocating {@link #await}.
     *
     * @param resut ailed procedure's object available at the thread invocating {@link #await}
     */
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

    /**
     * Decrements the failure count, releasing all waiting threads if the failure count reaches zero.
     *
     */
    public final void submitForFail() {
        this.synchronizer_.releaseShared(-1);
    }

    /**
     * Causes the current thread to wait until the latch has counted down to zero
     * unless the thread is {@linkplain Thread#interrupt interrupted}.
     * <p>
     * If the current success count is zero, then this method returns immediately.
     * Others, if the current failure count is zero, then this method throws
     *  {@linkplain SubmittedFailureResultException SubmittedFailureResultException} immediately.
     * </p>
     * <p>
     * If the current success or failure counts are greater than zero,
     * then the current thread becomes disabled for thread scheduling purposes
     * and lies dormant until one of two things happen:
     * <li>The either counts reach zero due to invocations of the
     * {@link #submit} or {@link #submitForFail} methods
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the current thread.
     * </ul>
     * </p>
     * <p>
     * <b>Warning:</b> The contents of the returned list are not guaranteed to be same as when latch is released,
     * but are guaranteed to contains all contents at the time of released.
     * </p>
     *
     * @return list of succeed procedure's result
     * @throws SubmittedFailureResultException if the failure count reaches zero before success count does.
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public List<SUCCESS_RESULT> await() throws SubmittedFailureResultException, InterruptedException {
        this.synchronizer_.acquireSharedInterruptibly(1);

        return this.returnResult();
    }

    /**
     * Causes the current thread to wait until the latch has counted down to zero
     * unless the thread is {@linkplain Thread#interrupt interrupted}, or the specified waiting time elapses.
     * <p>
     * If the current success count is zero, then this method returns immediately.
     * Others, if the current failure count is zero, then this method throws
     *  {@linkplain SubmittedFailureResultException SubmittedFailureResultException} immediately.
     * </p>
     * <p>
     * If the current success or failure counts are greater than zero,
     * then the current thread becomes disabled for thread scheduling purposes
     * and lies dormant until one of three things happen:
     * <li>The either counts reach zero due to invocations of the
     * {@link #submit} or {@link #submitForFail} methods
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the current thread.
     * <li>The specified waiting time elapses.
     * </ul>
     * </p>
     * <p>
     * <b>Warning:</b> The contents of the returned list are not guaranteed to be same as when latch is released,
     * but are guaranteed to contains all contents at the time of released.
     * </p>
     *
     * @param timeout the maximum time to wait
     * @param timeUnit the time unit of the {@code timeout} argument
     * @return list of succeed procedure's result
     * @throws SubmittedFailureResultException if the failure count reaches zero before success count does
     * @throws TimeoutException if the waiting time elapsed before the either counts reached zero
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public List<SUCCESS_RESULT> await(long timeout, TimeUnit timeUnit) throws SubmittedFailureResultException,
            TimeoutException, InterruptedException {
        boolean result = this.synchronizer_.tryAcquireSharedNanos(1, timeUnit.toNanos(timeout));

        if (result == false) {
            throw new TimeoutException("Time over for waiting in ConditionLatch.");
        }

        return this.returnResult();
    }

    /**
     * Returns list of succeed procedure's result.
     *
     * @return list of succeed procedure's result
     * @throws SubmittedFailureResultException if the success count doesn't reach zero
     */
    private List<SUCCESS_RESULT> returnResult() throws SubmittedFailureResultException {
        int successCount = this.synchronizer_.getSuccessCount();

        if (successCount > 0) {
            throw new SubmittedFailureResultException("Failed procedure.");
        }

        List<SUCCESS_RESULT> successList = this.copyList(this.successList_, this.successListLock_);

        return successList;
    }

    /**
     * Copies list.
     *
     * @param srcList
     * @param baseLock
     * @return copyed list
     */
    private <TYPE> List<TYPE> copyList(List<TYPE> srcList, ReadWriteLock baseLock) {
        List<TYPE> copyList;

        Lock lock = baseLock.readLock();
        lock.lock();
        try {
            List<TYPE> tempList = this.createList(1);
            tempList.addAll(srcList);
            copyList = Collections.unmodifiableList(tempList);
        } finally {
            lock.unlock();
        }

        return copyList;
    }

    /**
     * Returns list of succeed procedure's result.
     *
     * @return list of succeed procedure's result
     */
    public final List<SUCCESS_RESULT> getSuccessList() {
        return this.copyList(this.successList_, this.successListLock_);
    }

    /**
     * Returns list of failed procedure's result.
     *
     * @return list of failed procedure's result
     */
    public final List<FAILURE_RESULT> getFailureList() {
        return this.copyList(this.failureList_, this.failureListLock_);
    }
}
