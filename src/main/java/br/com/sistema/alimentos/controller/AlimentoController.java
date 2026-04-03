package br.com.sistema.alimentos.controller;

import br.com.sistema.alimentos.dtos.request.AtualizarAlimentoRequest;
import br.com.sistema.alimentos.dtos.request.CriarAlimentoRequest;
import br.com.sistema.alimentos.dtos.response.AlimentoResponse;
import br.com.sistema.alimentos.dtos.response.CatalogoAlimentosResponse;
import br.com.sistema.alimentos.dtos.response.EquivalenciaDinamicaResponse;

import java.math.BigDecimal;
import br.com.sistema.alimentos.enums.GrupoAlimentar;
import br.com.sistema.alimentos.service.AlimentoService;
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

@RestController
@RequestMapping("/api/v1/alimentos")
@RequiredArgsConstructor
@Tag(name = "Alimentos", description = "Catálogo de alimentos e informações nutricionais")
public class AlimentoController {

    private final AlimentoService alimentoService;

    @GetMapping("/catalogo")
    @Operation(summary = "Retorna todos os alimentos agrupados por grupo alimentar")
    public ResponseEntity<CatalogoAlimentosResponse> catalogar() {
        return ResponseEntity.ok(alimentoService.catalogar());
    }

    @GetMapping
    @Operation(summary = "Listar alimentos com filtros opcionais de descrição e grupo")
    public ResponseEntity<Page<AlimentoResponse>> listar(
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) GrupoAlimentar grupo,
            @PageableDefault(size = 20, sort = "descricao") Pageable pageable) {
        return ResponseEntity.ok(alimentoService.listar(descricao, grupo, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar alimento por ID")
    public ResponseEntity<AlimentoResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(alimentoService.buscarPorId(id));
    }

    @GetMapping("/{id}/equivalencias")
    @Operation(summary = "Calcular equivalências calóricas para uma quantidade informada")
    public ResponseEntity<EquivalenciaDinamicaResponse> calcularEquivalencias(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "100") BigDecimal quantidadeGramas) {
        return ResponseEntity.ok(alimentoService.calcularEquivalencias(id, quantidadeGramas));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NUTRICIONISTA')")
    @Operation(summary = "Cadastrar novo alimento")
    public ResponseEntity<AlimentoResponse> criar(@RequestBody @Valid CriarAlimentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alimentoService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NUTRICIONISTA')")
    @Operation(summary = "Atualizar dados do alimento")
    public ResponseEntity<AlimentoResponse> atualizar(
            @PathVariable Integer id,
            @RequestBody @Valid AtualizarAlimentoRequest request) {
        return ResponseEntity.ok(alimentoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover alimento permanentemente")
    public ResponseEntity<Void> remover(@PathVariable Integer id) {
        alimentoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
