package br.com.sistema.alimentos.controller;

import br.com.sistema.alimentos.dtos.request.AtualizarUsuarioRequest;
import br.com.sistema.alimentos.dtos.request.CriarUsuarioRequest;
import br.com.sistema.alimentos.dtos.response.UsuarioResponse;
import br.com.sistema.alimentos.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Gestão de usuários do sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // ====================================================
    // listar - Retorna todos os usuários (acesso restrito a ADMIN)
    // ====================================================
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos os usuários")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    // ====================================================
    // buscarPorId - Retorna um usuário pelo ID
    // ====================================================
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Buscar usuário por ID")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    // ====================================================
    // criar - Cria um novo usuário (acesso restrito a ADMIN)
    // ====================================================
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar novo usuário")
    public ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid CriarUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(request));
    }

    // ====================================================
    // atualizar - Atualiza dados de um usuário (acesso restrito a ADMIN)
    // ====================================================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar dados do usuário")
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid AtualizarUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    // ====================================================
    // ativar - Ativa o acesso de um usuário
    // ====================================================
    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ativar usuário")
    public ResponseEntity<UsuarioResponse> ativar(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.alterarStatus(id, true));
    }

    // ====================================================
    // desativar - Desativa o acesso de um usuário
    // ====================================================
    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativar usuário")
    public ResponseEntity<UsuarioResponse> desativar(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.alterarStatus(id, false));
    }

    // ====================================================
    // remover - Remove um usuário permanentemente
    // ====================================================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover usuário")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        usuarioService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
