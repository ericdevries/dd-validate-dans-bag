package nl.knaw.dans.validatedansbag.core.exception;

public class BagDoesNotBelongToAuthenticatedUserException extends Exception {

    public BagDoesNotBelongToAuthenticatedUserException(String msg) {
        super(msg);
    }
}
