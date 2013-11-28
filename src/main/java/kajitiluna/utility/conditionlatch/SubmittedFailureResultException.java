package kajitiluna.utility.conditionlatch;

/**
 * @author kajitiluna
 *
 */
public class SubmittedFailureResultException extends Exception {

    /** serialVersionUID. */
    private static final long serialVersionUID = 929015634594759876L;

    protected SubmittedFailureResultException() {
        super();
    }

    protected SubmittedFailureResultException(String message) {
        super(message);
    }

    protected SubmittedFailureResultException(Throwable causes) {
        super(causes);
    }

    protected SubmittedFailureResultException(String message, Throwable causes) {
        super(message, causes);
    }
}
