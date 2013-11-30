package kajitiluna.utility.conditionlatch;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

/**
 *
 * @author kajitiluna
 *
 */
public class ConditionLatchHugeTaskTest {

    @Test
    public void testSubmit_MaxTasks() {
        final int maxCount = 0x0000FFFF;
        final ConditionLatch<String, String> target = new ConditionLatch<String, String>(maxCount);

        ExecutorService executor = Executors.newFixedThreadPool(16);

        String[] results = new String[maxCount - 1];
        long[] waitTimes = new long[maxCount - 1];
        for (int index = 0; index < maxCount - 1; index++) {
            results[index] = "Result :" + index;
            waitTimes[index] = -1;
        }

        Runnable[] tasks = TestUtil.createSuccessTasks(target, results, waitTimes);

        long startTime = System.currentTimeMillis();
        for (Runnable task : tasks) {
            executor.submit(task);
        }

        Runnable lastTask = new Runnable() {
            @Override
            public void run() {
                System.out.println("==End of Task==============================");
                target.submit("Result :" + (maxCount - 1));
            }
        };
        executor.submit(lastTask);

        List<String> resultList = null;
        try {
            resultList = target.await();
        } catch (SubmittedFailureResultException | InterruptedException exc) {
            fail(exc.getMessage());
        }

        long endTime = System.currentTimeMillis();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);

        assertTrue(resultList.size() >= maxCount);

        for (int index = 0; index < maxCount; index += 16) {
            assertThat(resultList, hasItem(results[index]));
        }
    }

    @Test
    public void testSubmit_OverMaxTasks() {
        int maxCount = 0x0000FFFF;
        try {
            new ConditionLatch<Object, Object>(maxCount + 1);
            fail("Unexpected success.");
        } catch (IllegalArgumentException iaExc) {
            assertTrue(true);
        } catch (Exception exc) {
            fail("Unexpected exception : " + exc.getMessage());
        }
    }

    @Test
    public void testSubmit_underMin() {
        try {
            new ConditionLatch<Object, Object>(-1);
            fail("Unexpected success.");
        } catch (IllegalArgumentException iaExc) {
            assertTrue(true);
        } catch (Exception exc) {
            fail("Unexpected exception : " + exc.getMessage());
        }
    }

    @Test
    public void testSubmitForFail_MaxTasks() {
        final int maxCount = 0x0000FFFF;
        final ConditionLatch<String, String> target = new ConditionLatch<String, String>(1, maxCount);

        ExecutorService executor = Executors.newFixedThreadPool(16);

        String[] results = new String[maxCount - 1];
        long[] waitTimes = new long[maxCount - 1];
        for (int index = 0; index < maxCount - 1; index++) {
            results[index] = "Result :" + index;
            waitTimes[index] = -1;
        }

        Runnable[] tasks = TestUtil.createFailureTasks(target, results, waitTimes);

        long startTime = System.currentTimeMillis();
        for (Runnable task : tasks) {
            executor.submit(task);
        }

        Runnable lastTask = new Runnable() {
            @Override
            public void run() {
                System.out.println("==End of Task==============================");
                target.submitForFail("Result :" + (maxCount - 1));
            }
        };
        executor.submit(lastTask);

        try {
            target.await();
            fail("Unexpected success.");
        } catch (InterruptedException exc) {
            fail(exc.getMessage());
        } catch (SubmittedFailureResultException exc) {
            assertTrue(true);
        }

        long endTime = System.currentTimeMillis();

        long actualtime = endTime - startTime;
        System.out.println("Wait time : " + actualtime);

        List<String> resultList = target.getFailureList();
        assertTrue(resultList.size() >= maxCount);

        for (int index = 0; index < maxCount; index += 16) {
            assertThat(resultList, hasItem(results[index]));
        }
    }

    @Test
    public void testSubmitForFail_OverMaxTasks() {
        int maxCount = 0x0000FFFF;
        try {
            new ConditionLatch<Object, Object>(1, maxCount + 1);
            fail("Unexpected success.");
        } catch (IllegalArgumentException iaExc) {
            assertTrue(true);
        } catch (Exception exc) {
            fail("Unexpected exception : " + exc.getMessage());
        }
    }

    @Test
    public void testSubmitForFail_underMin() {
        try {
            new ConditionLatch<Object, Object>(1, -1);
            fail("Unexpected success.");
        } catch (IllegalArgumentException iaExc) {
            assertTrue(true);
        } catch (Exception exc) {
            fail("Unexpected exception : " + exc.getMessage());
        }
    }
}
