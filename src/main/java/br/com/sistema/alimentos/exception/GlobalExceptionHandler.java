package br.com.sistema.alimentos.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ====================================================
    // handleValidation - Trata erros de validação dos DTOs (@Valid)
    // ====================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errosCampos = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> errosCampos.put(erro.getField(), erro.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(construirErro(HttpStatus.UNPROCESSABLE_ENTITY, "Erro de validação", errosCampos.toString(), request.getRequestURI()));
    }

    // ====================================================
    // handleEntityNotFound - Trata entidade não encontrada no banco
    // ====================================================
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(construirErro(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request.getRequestURI()));
    }

    // ====================================================
    // handleBadCredentials - Trata falha de autenticação (credenciais inválidas)
    // ====================================================
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(construirErro(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", "E-mail ou senha incorretos.", request.getRequestURI()));
    }

    // ====================================================
    // handleAccessDenied - Trata acesso negado por falta de permissão
    // ====================================================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(construirErro(HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para acessar este recurso.", request.getRequestURI()));
    }

    // ====================================================
    // handleGeneric - Captura qualquer exceção não tratada
    // ====================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(construirErro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", ex.getMessage(), request.getRequestURI()));
    }

    private Map<String, Object> construirErro(HttpStatus status, String erro, String mensagem, String path) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("timestamp", LocalDateTime.now().toString());
        corpo.put("status", status.value());
        corpo.put("error", erro);
        corpo.put("message", mensagem);
        corpo.put("path", path);
        return corpo;
    }
}
