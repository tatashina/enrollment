package megamarket.exception;

public class ResponseExceptionBody {

    public final String message;
    public final int code;

    public ResponseExceptionBody(String message, int code) {
        super();
        this.message = message;
        this.code = code;
    }

}