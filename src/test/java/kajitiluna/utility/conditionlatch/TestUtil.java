package kajitiluna.utility.conditionlatch;

/**
 *
 * @author kajitiluna
 *
 */
public class TestUtil {

    private TestUtil() {
        // Do nothing.
    }

    static <SUCCESS_RESULT, FAILED_RESULT> Runnable createSuccessTask(
            final ConditionLatch<SUCCESS_RESULT, FAILED_RESULT> latch, final SUCCESS_RESULT result, final long waitTime) {
        Runnable task = new Runnable() {

            @Override
            public void run() {
                sleepTask(waitTime);
                latch.submit(result);
            }
        };

        return task;
    }

    static <SUCCESS_RESULT, FAILED_RESULT> Runnable createSuccessTask(
            final ConditionLatch<SUCCESS_RESULT, FAILED_RESULT> latch, final long waitTime) {
        return createSuccessTask(latch, null, waitTime);
    }

    static <SUCCESS_RESULT, FAILED_RESULT> Runnable[] createSuccessTasks(
            final ConditionLatch<SUCCESS_RESULT, FAILED_RESULT> latch, final long[] waitTimes) {
        Runnable[] tasks = new Runnable[waitTimes.length];
        for (int index = 0; index < waitTimes.length; index++) {
            tasks[index] = createSuccessTask(latch, waitTimes[index]);
        }

        return tasks;
    }

    static <SUCCESS_RESULT, FAILED_RESULT> Runnable[] createSuccessTasks(
            final ConditionLatch<SUCCESS_RESULT, FAILED_RESULT> latch, final SUCCESS_RESULT[] results,
            final long[] waitTimes) {
        assert results.length == waitTimes.length;

        Runnable[] tasks = new Runnable[waitTimes.length];
        for (int index = 0; index < waitTimes.length; index++) {
            tasks[index] = createSuccessTask(latch, results[index], waitTimes[index]);
        }

        return tasks;
    }

    static <SUCCESS_RESULT, FAILED_RESULT> Runnable createFailureTask(
            final ConditionLatch<SUCCESS_RESULT, FAILED_RESULT> latch, final FAILED_RESULT result, final long waitTime) {
        Runnable task = new Runnable() {

            @Override
            public void run() {
                sleepTask(waitTime);
                latch.submitForFail(result);
            }
        };

        return task;
    }

    static <SUCCESS_RESULT, FAILED_RESULT> Runnable createFailureTask(
            final ConditionLatch<SUCCESS_RESULT, FAILED_RESULT> latch, final long waitTime) {
        return createFailureTask(latch, null, waitTime);
    }

    static <SUCCESS_RESULT, FAILED_RESULT> Runnable[] createFailureTasks(
            final ConditionLatch<SUCCESS_RESULT, FAILED_RESULT> latch, final long[] waitTimes) {
        Runnable[] tasks = new Runnable[waitTimes.length];
        for (int index = 0; index < waitTimes.length; index++) {
            tasks[index] = createFailureTask(latch, waitTimes[index]);
        }

        return tasks;
    }

    static <SUCCESS_RESULT, FAILED_RESULT> Runnable[] createFailureTasks(
            final ConditionLatch<SUCCESS_RESULT, FAILED_RESULT> latch, final FAILED_RESULT[] results,
            final long[] waitTimes) {
        assert results.length == waitTimes.length;

        Runnable[] tasks = new Runnable[waitTimes.length];
        for (int index = 0; index < waitTimes.length; index++) {
            tasks[index] = createFailureTask(latch, results[index], waitTimes[index]);
        }

        return tasks;
    }

    private static void sleepTask(final long waitTime) {
        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException exc) {
                System.out.println("Interrupted : " + exc.getMessage());
            }

            System.out.println("Finished waiting task for " + waitTime + " miliseconds.");
        } else if (Math.random() == 0) {
            System.out.println("random value is 0.");
        }
    }
}
