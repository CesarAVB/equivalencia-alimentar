package br.com.sistema.alimentos.controller;

import br.com.sistema.alimentos.dtos.request.LoginRequest;
import br.com.sistema.alimentos.dtos.response.LoginResponse;
import br.com.sistema.alimentos.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de login e controle de sessão")
public class AuthController {

    private final AuthService authService;

    // ====================================================
    // login - Autentica o usuário e retorna o token JWT
    // ====================================================
    @PostMapping("/login")
    @Operation(summary = "Realizar login e obter token JWT")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.autenticar(request));
    }
}
