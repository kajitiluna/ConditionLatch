package kajitiluna.utility.conditionlatch;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Test;

/**
 *
 * @author kajitiluna
 *
 */
public class ConditionLatchTest {

    private ExecutorService executorService_;

    @After
    public void tearDown() {
        if (this.executorService_ != null) {
            this.executorService_.shutdownNow();
        }
    }

    @Test
    public void testSubmit_1Task() {
        final ConditionLatch<Object, Object> target = new ConditionLatch<Object, Object>(1);

        this.executorService_ = Executors.newFixedThreadPool(1);
        Runnable task = TestUtil.createSuccessTask(target, 1000);

        long startTime = System.currentTimeMillis();
        this.executorService_.submit(task);
        try {
            target.await();
        } catch (SubmittedFailureResultException | InterruptedException exc) {
            fail(exc.getMessage());
        }

        long endTime = System.currentTimeMillis();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);
        assertTrue(actualtime >= 1000);
    }

    @Test
    public void testSubmit_1TaskWithFailure1Task() {
        final ConditionLatch<Object, Object> target = new ConditionLatch<Object, Object>(1);

        this.executorService_ = Executors.newFixedThreadPool(4);
        Runnable successTask = TestUtil.createSuccessTask(target, 1000);
        Runnable failureTask = TestUtil.createFailureTask(target, 3000);

        long startTime = System.currentTimeMillis();
        this.executorService_.submit(successTask);
        this.executorService_.submit(failureTask);

        try {
            target.await();
        } catch (SubmittedFailureResultException | InterruptedException exc) {
            fail(exc.getMessage());
        }

        long endTime = System.currentTimeMillis();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);
        assertTrue((actualtime >= 1000) && (actualtime <= 3000));
    }

    @Test
    public void testSubmit_3Tasks_sequentially() {
        final ConditionLatch<Object, Object> target = new ConditionLatch<Object, Object>(3);

        this.executorService_ = Executors.newFixedThreadPool(1);
        Runnable[] tasks = TestUtil.createSuccessTasks(target, new long[] { 1000, 500, 500 });

        long startTime = System.currentTimeMillis();
        for (Runnable task : tasks) {
            this.executorService_.submit(task);
        }

        try {
            target.await();
        } catch (SubmittedFailureResultException | InterruptedException exc) {
            fail(exc.getMessage());
        }

        long endTime = System.currentTimeMillis();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);
        assertTrue(actualtime >= 2000);
    }

    @Test
    public void testSubmit_3Tasks_parallel() {
        final ConditionLatch<Object, Object> target = new ConditionLatch<Object, Object>(3);

        this.executorService_ = Executors.newFixedThreadPool(4);
        Runnable[] successTasks = TestUtil.createSuccessTasks(target, new long[] { 1000, 500, 1000 });
        Runnable failureTask = TestUtil.createFailureTask(target, 3000);

        long startTime = System.currentTimeMillis();
        this.executorService_.submit(failureTask);
        for (Runnable task : successTasks) {
            this.executorService_.submit(task);
        }

        try {
            target.await();
        } catch (SubmittedFailureResultException | InterruptedException exc) {
            fail(exc.getMessage());
        }

        long endTime = System.currentTimeMillis();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);
        assertTrue((actualtime >= 1000) && (actualtime <= 2000));
    }

    @Test
    public void testSubmit_withFailure() {
        final ConditionLatch<String, String> target = new ConditionLatch<String, String>(3, 4);

        this.executorService_ = Executors.newFixedThreadPool(4);
        String[] successResults = { "Success Result 1", "Success Result 2", "Success Result 3" };
        String[] failureResults = { "Failure Result 1", "Failure Result 2", "Failure Result 3" };
        long[] waitTimes = { 1000, 500, 1000 };
        Runnable[] successTasks = TestUtil.createSuccessTasks(target, successResults, waitTimes);
        Runnable[] failureTasks = TestUtil.createFailureTasks(target, failureResults, waitTimes);

        long startTime = System.currentTimeMillis();
        for (int index = 0; index < successTasks.length; index++) {
            this.executorService_.submit(successTasks[index]);
            this.executorService_.submit(failureTasks[index]);
        }

        List<String> successList = null;
        try {
            successList = target.await();
        } catch (SubmittedFailureResultException | InterruptedException exc) {
            fail(exc.getMessage());
        }

        long endTime = System.currentTimeMillis();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);
        assertTrue(actualtime >= 1000);

        assertTrue(successList.size() == 3);
        assertThat(successList, hasItems(successResults));

        List<String> failureList = target.getFailureList();
        assertTrue(failureList.size() <= 3);
    }

    @Test
    public void testSubmitForFail_1task() {
        final ConditionLatch<Object, Object> target = new ConditionLatch<Object, Object>(1);

        this.executorService_ = Executors.newFixedThreadPool(1);
        Runnable failureTask = TestUtil.createFailureTask(target, 1000);

        long startTime = System.currentTimeMillis();
        this.executorService_.submit(failureTask);

        try {
            target.await();
            fail("Unexpected success.");
        } catch (SubmittedFailureResultException sfrExc) {
            assertTrue(true);
        } catch (InterruptedException iExc) {
            fail("Unexpected exception : " + iExc.getMessage());
        }

        long endTime = System.currentTimeMillis();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);
        assertTrue(actualtime >= 1000);
    }

    @Test
    public void testSubmitForFail_1taskWithSuccess1Task() {
        final ConditionLatch<Object, Object> target = new ConditionLatch<Object, Object>(1);

        this.executorService_ = Executors.newFixedThreadPool(4);
        Runnable successTask = TestUtil.createSuccessTask(target, 3000);
        Runnable failureTask = TestUtil.createFailureTask(target, 1000);

        long startTime = System.currentTimeMillis();
        this.executorService_.submit(successTask);
        this.executorService_.submit(failureTask);

        try {
            target.await();
            fail("Unexpected success.");
        } catch (SubmittedFailureResultException sfrExc) {
            assertTrue(true);
        } catch (InterruptedException iExc) {
            fail("Unexpected exception : " + iExc.getMessage());
        }

        long endTime = System.currentTimeMillis();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);
        assertTrue((actualtime >= 1000) && (actualtime <= 3000));
    }

    @Test
    public void testSubmitForFail_3Tasks_sequentially() {
        final ConditionLatch<Object, Object> target = new ConditionLatch<Object, Object>(1, 3);

        this.executorService_ = Executors.newFixedThreadPool(1);
        Runnable[] failureTasks = TestUtil.createFailureTasks(target, new long[] { 1000, 500, 500 });

        long startTime = System.currentTimeMillis();
        for (Runnable task : failureTasks) {
            this.executorService_.submit(task);
        }

        try {
            target.await();
            fail("Unexpected success.");
        } catch (SubmittedFailureResultException sfrExc) {
            assertTrue(true);
        } catch (InterruptedException iExc) {
            fail("Unexpected exception : " + iExc.getMessage());
        }

        long endTime = System.currentTimeMillis();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);
        assertTrue(actualtime >= 2000);
    }

    @Test
    public void testSubmitForFail_3Tasks_parallel() {
        final ConditionLatch<Object, Object> target = new ConditionLatch<Object, Object>(1, 3);

        this.executorService_ = Executors.newFixedThreadPool(4);
        Runnable successTask = TestUtil.createSuccessTask(target, 3000);
        Runnable[] failureTasks = TestUtil.createFailureTasks(target, new long[] { 1000, 500, 1000 });

        long startTime = System.currentTimeMillis();
        this.executorService_.submit(successTask);
        for (Runnable task : failureTasks) {
            this.executorService_.submit(task);
        }

        try {
            target.await();
            fail("Unexpected success.");
        } catch (SubmittedFailureResultException sfrExc) {
            assertTrue(true);
        } catch (InterruptedException iExc) {
            fail("Unexpected exception : " + iExc.getMessage());
        }

        long endTime = System.currentTimeMillis();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);
        assertTrue((actualtime >= 1000) && (actualtime <= 2000));
    }

    @Test
    public void testSubmitForFail_withSuccess() {
        final ConditionLatch<String, String> target = new ConditionLatch<String, String>(4, 3);

        this.executorService_ = Executors.newFixedThreadPool(4);
        String[] successResults = { "Success Result 1", "Success Result 2", "Success Result 3" };
        String[] failureResults = { "Failure Result 1", "Failure Result 2", "Failure Result 3" };
        long[] waitTimes = { 1000, 500, 1000 };
        Runnable[] successTasks = TestUtil.createSuccessTasks(target, successResults, waitTimes);
        Runnable[] failureTasks = TestUtil.createFailureTasks(target, failureResults, waitTimes);

        long startTime = System.currentTimeMillis();
        for (int index = 0; index < successTasks.length; index++) {
            this.executorService_.submit(successTasks[index]);
            this.executorService_.submit(failureTasks[index]);
        }

        try {
            target.await();
            fail("Unexpected success.");
        } catch (SubmittedFailureResultException sfrExc) {
            assertTrue(true);
        } catch (InterruptedException iExc) {
            fail("Unexpected exception : " + iExc.getMessage());
        }

        long endTime = System.currentTimeMillis();

        List<String> failureList = target.getFailureList();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);
        assertTrue(actualtime >= 1000);

        assertTrue(failureList.size() == 3);
        assertThat(failureList, hasItems(failureResults));

        List<String> successList = target.getSuccessList();
        assertTrue(successList.size() <= 3);
    }

    @Test
    public void testInterrupt() {
        final ConditionLatch<Object, Object> target = new ConditionLatch<Object, Object>(1);

        this.executorService_ = Executors.newFixedThreadPool(4);
        Runnable task = TestUtil.createSuccessTask(target, 3000);

        final Thread testThread = Thread.currentThread();
        Runnable interruptTask = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException exc) {
                    System.out.println("Interrupted : " + exc.getMessage());
                }

                testThread.interrupt();
            }
        };

        long startTime = System.currentTimeMillis();
        this.executorService_.submit(task);
        this.executorService_.submit(interruptTask);

        try {
            target.await();
            fail("Unexpected success.");
        } catch (SubmittedFailureResultException sfrExc) {
            fail("Unexpected exception : " + sfrExc.getMessage());
        } catch (InterruptedException iExc) {
            System.out.println("Occured InterruptedException : " + iExc.getMessage());
            assertTrue(true);
        }

        long endTime = System.currentTimeMillis();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);
        assertTrue(actualtime >= 1000);
    }
}
