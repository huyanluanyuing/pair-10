package ca.en.solution.common;

import ca.en.solution.crm.CrmUnavailableException;
import ca.en.solution.portfolio.PortfolioNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(PortfolioNotFoundException.class)
    public ResponseEntity<ApiError> notFound(PortfolioNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("not_found", exception.getMessage()));
    }

    @ExceptionHandler(CrmUnavailableException.class)
    public ResponseEntity<ApiError> crmUnavailable(CrmUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("crm_unavailable", exception.getMessage()));
    }

    public record ApiError(String error, String message) {
    }
}
