
package cl.petshop.ms_productos.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Manejar errores de negocio: 400, 404, 409, etc.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> manejarErrorNegocio(
            ResponseStatusException ex,
            HttpServletRequest request) {

        int estado = ex.getStatusCode().value();

        String mensaje = ex.getReason() != null
                ? ex.getReason()
                : "No se pudo completar la solicitud";

        return ResponseEntity.status(estado).body(
                crearRespuesta(
                        estado,
                        mensaje,
                        request.getRequestURI()
                )
        );
    }

    // Manejar errores de validacion de los DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidaciones(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<String> detalles = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " +
                        error.getDefaultMessage()
                )
                .distinct()
                .toList();

        Map<String, Object> respuesta = crearRespuesta(
                400,
                "Los datos ingresados no son validos",
                request.getRequestURI()
        );

        respuesta.put("detalles", detalles);

        return ResponseEntity.badRequest().body(respuesta);
    }

    // Manejar JSON incorrecto o mal formado
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarJsonInvalido(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        return ResponseEntity.badRequest().body(
                crearRespuesta(
                        400,
                        "El cuerpo JSON de la solicitud no es valido",
                        request.getRequestURI()
                )
        );
    }

    // Manejar conflictos de integridad en MySQL
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarConflictoDatos(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                crearRespuesta(
                        409,
                        "La operacion incumple una restriccion de la base de datos",
                        request.getRequestURI()
                )
        );
    }

    // Crear una respuesta JSON con formato uniforme
    private Map<String, Object> crearRespuesta(
            int estado,
            String mensaje,
            String ruta) {

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("fecha", Instant.now().toString());
        respuesta.put("estado", estado);
        respuesta.put("mensaje", mensaje);
        respuesta.put("ruta", ruta);

        return respuesta;
    }
}
