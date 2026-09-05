package com.vennhuu.PersonalFinance.Exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vennhuu.PersonalFinance.Entity.Response.RestResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalException {

    // all exc
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Object>> handleAllException(Exception ex) {

        log.error("Unhandled exception occurred", ex); // in full stack trace ra terminal

        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        RestResponse<Object> res = new RestResponse<>();
        res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        res.setError("Internal Server Error");
        res.setMessage(root.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }

    // 400
    @ExceptionHandler(IdInvalidException.class)
    public ResponseEntity<RestResponse<Object>> handleIdInvalidException(IdInvalidException ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatus(HttpStatus.BAD_REQUEST.value());
        res.setError("Bad Request");
        res.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    // 409
    @ExceptionHandler(value = {
        ExistsEmailException.class,
        ExistsPhoneNumberException.class
    })
    public ResponseEntity<RestResponse<Object>> handleExistsEmailException(Exception ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatus(HttpStatus.CONFLICT.value());
        res.setError("CONFLICT");
        res.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(res);
    }

    // 401
    @ExceptionHandler(value = {
        BadCredentialsException.class,
        UsernameNotFoundException.class
    })  
    public ResponseEntity<RestResponse<Object>> handleUsernameNotFoundException(Exception ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatus(HttpStatus.UNAUTHORIZED.value());
        res.setError("Unauthorized");
        res.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
    }

    // validate
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Object>> handleValidationError(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        List<FieldError> fieldErrors = result.getFieldErrors();

        List<String> errors = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        RestResponse<Object> res = new RestResponse<>();
        res.setStatus(HttpStatus.BAD_REQUEST.value());
        res.setError(ex.getBody().getDetail());
        res.setMessage(errors.size() == 1 ? errors.get(0) : errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<RestResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatus(HttpStatus.NOT_FOUND.value());
        res.setError(HttpStatus.NOT_FOUND.getReasonPhrase());
        res.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<RestResponse<Object>> handleForbiddenException(ForbiddenException ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatus(HttpStatus.FORBIDDEN.value());
        res.setError(HttpStatus.FORBIDDEN.getReasonPhrase());
        res.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
    }

    @ExceptionHandler(value = {
            StorageException.class,
    })
    public ResponseEntity<RestResponse<Object>> handleFileUploadException(Exception ex) {
        RestResponse<Object> res = new RestResponse<Object>();
        res.setStatus(HttpStatus.BAD_REQUEST.value());
        res.setMessage(ex.getMessage());
        res.setError("Exception upload file...");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

}
