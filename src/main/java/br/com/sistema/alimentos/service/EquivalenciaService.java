package br.com.sistema.alimentos.service;

import br.com.sistema.alimentos.dtos.request.AtualizarEquivalenciaRequest;
import br.com.sistema.alimentos.dtos.request.CriarEquivalenciaRequest;
import br.com.sistema.alimentos.dtos.response.EquivalenciaResponse;
import br.com.sistema.alimentos.entity.Alimento;
import br.com.sistema.alimentos.entity.Equivalencia;
import br.com.sistema.alimentos.repository.AlimentoRepository;
import br.com.sistema.alimentos.repository.EquivalenciaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EquivalenciaService {

    private final EquivalenciaRepository equivalenciaRepository;
    private final AlimentoRepository alimentoRepository;

    // ====================================================
    // listar - Lista todas as equivalências com paginação
    // ====================================================
    public Page<EquivalenciaResponse> listar(Pageable pageable) {
        return equivalenciaRepository.findAll(pageable).map(e -> toResponse(e, BigDecimal.valueOf(100)));
    }

    // ====================================================
    // buscarPorAlimentoOrigem - Lista equivalências com cálculo dinâmico de gramas
    // ====================================================
    public List<EquivalenciaResponse> buscarPorAlimentoOrigem(Integer alimentoOrigemId, BigDecimal quantidadeGramas) {
        return equivalenciaRepository.findByAlimentoOrigemId(alimentoOrigemId).stream()
                .map(e -> toResponse(e, quantidadeGramas))
                .toList();
    }

    // ====================================================
    // buscarPorId - Retorna uma equivalência pelo ID
    // ====================================================
    public EquivalenciaResponse buscarPorId(UUID id) {
        return toResponse(encontrarPorId(id), BigDecimal.valueOf(100));
    }

    // ====================================================
    // criar - Cadastra uma nova equivalência entre dois alimentos
    // ====================================================
    @Transactional
    public EquivalenciaResponse criar(CriarEquivalenciaRequest request) {
        Alimento origem = alimentoRepository.findById(request.alimentoOrigemId())
                .orElseThrow(() -> new EntityNotFoundException("Alimento de origem não encontrado"));

        Alimento destino = alimentoRepository.findById(request.alimentoDestinoId())
                .orElseThrow(() -> new EntityNotFoundException("Alimento de destino não encontrado"));

        Equivalencia equivalencia = Equivalencia.builder()
                .alimentoOrigem(origem)
                .alimentoDestino(destino)
                .fatorEquivalencia(request.fatorEquivalencia())
                .observacao(request.observacao())
                .build();

        return toResponse(equivalenciaRepository.save(equivalencia), BigDecimal.valueOf(100));
    }

    // ====================================================
    // atualizar - Atualiza uma equivalência existente
    // ====================================================
    @Transactional
    public EquivalenciaResponse atualizar(UUID id, AtualizarEquivalenciaRequest request) {
        Equivalencia equivalencia = encontrarPorId(id);

        Alimento origem = alimentoRepository.findById(request.alimentoOrigemId())
                .orElseThrow(() -> new EntityNotFoundException("Alimento de origem não encontrado"));

        Alimento destino = alimentoRepository.findById(request.alimentoDestinoId())
                .orElseThrow(() -> new EntityNotFoundException("Alimento de destino não encontrado"));

        equivalencia.setAlimentoOrigem(origem);
        equivalencia.setAlimentoDestino(destino);
        equivalencia.setFatorEquivalencia(request.fatorEquivalencia());
        equivalencia.setObservacao(request.observacao());

        return toResponse(equivalenciaRepository.save(equivalencia), BigDecimal.valueOf(100));
    }

    // ====================================================
    // remover - Remove uma equivalência pelo ID
    // ====================================================
    @Transactional
    public void remover(UUID id) {
        if (!equivalenciaRepository.existsById(id)) {
            throw new EntityNotFoundException("Equivalência não encontrada: " + id);
        }
        equivalenciaRepository.deleteById(id);
    }

    private Equivalencia encontrarPorId(UUID id) {
        return equivalenciaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equivalência não encontrada: " + id));
    }

    private EquivalenciaResponse toResponse(Equivalencia e, BigDecimal quantidadeGramas) {
        BigDecimal quantidadeDestino = quantidadeGramas
                .multiply(e.getFatorEquivalencia())
                .setScale(2, RoundingMode.HALF_UP);

        return new EquivalenciaResponse(
                e.getId(),
                e.getAlimentoOrigem().getId(),
                e.getAlimentoOrigem().getDescricao(),
                e.getAlimentoDestino().getId(),
                e.getAlimentoDestino().getDescricao(),
                e.getFatorEquivalencia(),
                quantidadeGramas,
                quantidadeDestino,
                e.getObservacao(),
                e.getCreatedAt()
        );
    }
}
