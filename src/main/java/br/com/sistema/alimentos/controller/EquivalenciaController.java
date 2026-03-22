package br.com.sistema.alimentos.controller;

import br.com.sistema.alimentos.dtos.request.AtualizarEquivalenciaRequest;
import br.com.sistema.alimentos.dtos.request.CriarEquivalenciaRequest;
import br.com.sistema.alimentos.dtos.response.EquivalenciaResponse;
import br.com.sistema.alimentos.service.EquivalenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/equivalencias")
@RequiredArgsConstructor
@Tag(name = "Equivalências", description = "Equivalências nutricionais entre alimentos")
public class EquivalenciaController {

    private final EquivalenciaService equivalenciaService;

    @GetMapping
    @Operation(summary = "Listar equivalências com paginação")
    public ResponseEntity<Page<EquivalenciaResponse>> listar(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(equivalenciaService.listar(pageable));
    }

    @GetMapping("/alimento/{alimentoOrigemId}")
    @Operation(summary = "Buscar equivalências pelo alimento de origem")
    public ResponseEntity<List<EquivalenciaResponse>> buscarPorAlimentoOrigem(
            @PathVariable Integer alimentoOrigemId) {
        return ResponseEntity.ok(equivalenciaService.buscarPorAlimentoOrigem(alimentoOrigemId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar equivalência por ID")
    public ResponseEntity<EquivalenciaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(equivalenciaService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NUTRICIONISTA')")
    @Operation(summary = "Cadastrar nova equivalência alimentar")
    public ResponseEntity<EquivalenciaResponse> criar(@RequestBody @Valid CriarEquivalenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equivalenciaService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NUTRICIONISTA')")
    @Operation(summary = "Atualizar equivalência alimentar")
    public ResponseEntity<EquivalenciaResponse> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid AtualizarEquivalenciaRequest request) {
        return ResponseEntity.ok(equivalenciaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NUTRICIONISTA')")
    @Operation(summary = "Remover equivalência alimentar")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        equivalenciaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
