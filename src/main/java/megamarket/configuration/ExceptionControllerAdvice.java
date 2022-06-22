package megamarket.configuration;

import megamarket.exception.ResponseExceptionBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler({Throwable.class})
    public ResponseEntity<?> handleThrowable(Throwable t) {
        if (t instanceof megamarket.exception.ShopUnitNotFoundException) {
            return new ResponseEntity<Object>(
                    new ResponseExceptionBody("Item not found", HttpStatus.NOT_FOUND.value()),
                    HttpStatus.NOT_FOUND
            );
        }
        return new ResponseEntity<Object>(
                new ResponseExceptionBody("Validation Failed", HttpStatus.BAD_REQUEST.value()),
                HttpStatus.BAD_REQUEST
        );
    }


}