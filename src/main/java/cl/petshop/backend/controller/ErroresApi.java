package cl.petshop.backend.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

@RestControllerAdvice
public class ErroresApi extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException error, HttpHeaders headers, HttpStatusCode estado, WebRequest solicitud) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, "Revisa los campos indicados");
        problema.setProperty("campos", error.getBindingResult().getFieldErrors().stream()
                .map(campo -> Map.of("campo", campo.getField(), "mensaje", campo.getDefaultMessage()))
                .toList());
        return handleExceptionInternal(error, problema, headers, estado, solicitud);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException error, HttpHeaders headers, HttpStatusCode estado, WebRequest solicitud) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado,
                "El cuerpo debe ser JSON y sus campos deben tener el tipo correcto");
        return handleExceptionInternal(error, problema, headers, estado, solicitud);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflictoDeDatos() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "No se pudo guardar o eliminar el registro porque ya existe o está vinculado a otros datos");
    }
}
