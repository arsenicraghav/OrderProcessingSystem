package com.example.orders.api;

import com.example.orders.domain.InvalidOrderStateException;
import com.example.orders.domain.OrderNotFoundException;
import com.example.orders.domain.OrderValidationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Maps errors to RFC 7807 problem responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Order not found", e.getMessage());
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    ProblemDetail invalidState(InvalidOrderStateException e) {
        return problem(HttpStatus.CONFLICT, "Invalid order state", e.getMessage());
    }

    @ExceptionHandler(OrderValidationException.class)
    ProblemDetail rejected(OrderValidationException e) {
        return problem(HttpStatus.BAD_REQUEST, "Order rejected", e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrentUpdate(ObjectOptimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "Concurrent modification",
                "The order was modified by another request; reload and retry");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request body is invalid");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ProblemDetail badRequest(Exception e) {
        String detail = e instanceof HttpMessageNotReadableException
                ? "Malformed JSON or invalid value in request body"
                : e.getMessage();
        return problem(HttpStatus.BAD_REQUEST, "Bad request", detail);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
