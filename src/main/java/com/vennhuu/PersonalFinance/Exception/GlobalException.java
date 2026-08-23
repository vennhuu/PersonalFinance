package com.vennhuu.PersonalFinance.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vennhuu.PersonalFinance.Entity.Response.RestResponse;

@RestControllerAdvice
public class GlobalException {

    // all exc
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Object>> handleAllException(Exception ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        res.setError("Internal Server Error");
        res.setMessage(ex.getMessage());
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
    public ResponseEntity<RestResponse<Object>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatus(HttpStatus.UNAUTHORIZED.value());
        res.setError("Unauthorized");
        res.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    // validate
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        RestResponse<Object> res = new RestResponse<>();

        res.setStatus(HttpStatus.BAD_REQUEST.value());
        res.setError("Bad Request");

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Dữ liệu không hợp lệ");

        res.setMessage(message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(res);
    }
}
