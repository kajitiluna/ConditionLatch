package kajitiluna.utility.conditionlatch;

import java.util.List;

/**
 * 
 * @author kajitiluna
 *
 */
public class ConditionLatch<SUCCESS_RESULT, FAILED_RESULT> {

    private List<SUCCESS_RESULT> successList_;

    private List<FAILED_RESULT>  failedList_;

    public void submit(SUCCESS_RESULT result) {
        // TODO
    }

    public void submitForFail(FAILED_RESULT resut) {
        // TODO
    }

    public void interrupt() {
        // TODO
    }

    public List<SUCCESS_RESULT> await(int count) throws InterruptedException {
        // TODO

        return null;
    }

}
