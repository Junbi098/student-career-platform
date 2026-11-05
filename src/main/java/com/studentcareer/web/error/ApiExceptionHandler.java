package com.studentcareer.web.error;

import com.studentcareer.common.exception.InvalidRequestException;
import com.studentcareer.company.exception.CompanyNotFoundException;
import com.studentcareer.opportunity.service.OpportunityConflictException;
import com.studentcareer.opportunity.service.OpportunityNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import com.studentcareer.company.exception.CollectionRateLimitException;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({CompanyNotFoundException.class, OpportunityNotFoundException.class})
    public ProblemDetail notFound(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail invalidQuery(InvalidRequestException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(CollectionRateLimitException.class)
    public ResponseEntity<ProblemDetail> collectionRateLimit(CollectionRateLimitException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", Long.toString(exception.getRetryAfter())).body(problem);
    }

    @ExceptionHandler({OpportunityConflictException.class, OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ProblemDetail conflict(RuntimeException exception) {
        String message = exception instanceof OpportunityConflictException ? exception.getMessage()
                : "Request conflicts with stored data; reload before retrying";
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail databaseUnavailable(DataAccessException exception) {
        LOG.error("Database operation failed", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Database operation unavailable");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception exception) {
        LOG.error("Unexpected API error", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }
}
