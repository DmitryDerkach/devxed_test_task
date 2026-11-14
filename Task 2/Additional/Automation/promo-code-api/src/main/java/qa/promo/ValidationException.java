package qa.promo;

/** A business-rule rejection; the message is returned to the client with HTTP 400. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
