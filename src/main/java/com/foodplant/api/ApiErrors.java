package com.foodplant.api;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.*;
class ConflictException extends RuntimeException { ConflictException(String message) {super(message);} }
class InvalidException extends RuntimeException { InvalidException(String message) {super(message);} }
class MissingException extends RuntimeException { MissingException(String message) {super(message);} }
@RestControllerAdvice class ApiErrors {
 private static final Logger log=LoggerFactory.getLogger(ApiErrors.class);
 record ErrorBody(String code,String message) {}
 private ResponseEntity<ErrorBody> error(HttpStatus status,String code,String message) { return ResponseEntity.status(status).body(new ErrorBody(code,message)); }
 @ExceptionHandler(MissingException.class) ResponseEntity<ErrorBody> missing(MissingException e) {return error(HttpStatus.NOT_FOUND,"NOT_FOUND",e.getMessage());}
 @ExceptionHandler(ConflictException.class) ResponseEntity<ErrorBody> businessConflict(ConflictException e) {return error(HttpStatus.CONFLICT,"CONFLICT",e.getMessage());}
 @ExceptionHandler(InvalidException.class) ResponseEntity<ErrorBody> businessInvalid(InvalidException e) {return error(HttpStatus.BAD_REQUEST,"INVALID_REQUEST",e.getMessage());}
 @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class) ResponseEntity<ErrorBody> noResource(Exception e) {return error(HttpStatus.NOT_FOUND,"NOT_FOUND","Resource not found");}
 @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class) ResponseEntity<ErrorBody> method(Exception e) {return error(HttpStatus.METHOD_NOT_ALLOWED,"METHOD_NOT_ALLOWED","Method not allowed");}
 @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class) ResponseEntity<ErrorBody> media(Exception e) {return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"UNSUPPORTED_MEDIA_TYPE","Unsupported media type");}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ErrorBody> conflict(Exception e) {return error(HttpStatus.CONFLICT,"CONFLICT","A uniqueness or reference constraint was violated");}
 @ExceptionHandler({MethodArgumentNotValidException.class,ConstraintViolationException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class}) ResponseEntity<ErrorBody> invalid(Exception e) {return error(HttpStatus.BAD_REQUEST,"INVALID_REQUEST","Invalid request parameters or body");}
 @ExceptionHandler(Exception.class) ResponseEntity<ErrorBody> unexpected(Exception e) {log.error("Unexpected API failure",e); return error(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR","Request could not be completed");}
}
