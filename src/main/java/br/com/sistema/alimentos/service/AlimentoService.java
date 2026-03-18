package br.com.sistema.alimentos.service;

import br.com.sistema.alimentos.dtos.request.AtualizarAlimentoRequest;
import br.com.sistema.alimentos.dtos.request.CriarAlimentoRequest;
import br.com.sistema.alimentos.dtos.response.AlimentoResponse;
import br.com.sistema.alimentos.entity.Alimento;
import br.com.sistema.alimentos.enums.GrupoAlimentar;
import br.com.sistema.alimentos.repository.AlimentoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlimentoService {

    private final AlimentoRepository alimentoRepository;

    // ====================================================
    // listar - Lista alimentos com paginação e filtros opcionais
    // ====================================================
    public Page<AlimentoResponse> listar(String descricao, GrupoAlimentar grupo, Pageable pageable) {
        if (descricao != null && !descricao.isBlank()) {
            return alimentoRepository.buscarPorDescricao(descricao, pageable).map(this::toResponse);
        }
        if (grupo != null) {
            return alimentoRepository.findByGrupo(grupo, pageable).map(this::toResponse);
        }
        return alimentoRepository.findAll(pageable).map(this::toResponse);
    }

    // ====================================================
    // buscarPorId - Retorna um alimento pelo ID
    // ====================================================
    public AlimentoResponse buscarPorId(Integer id) {
        return toResponse(encontrarPorId(id));
    }

    // ====================================================
    // criar - Cadastra um novo alimento
    // ====================================================
    @Transactional
    public AlimentoResponse criar(CriarAlimentoRequest request) {
        if (alimentoRepository.existsByCodigoSubstituicao(request.codigoSubstituicao())) {
            throw new IllegalArgumentException("Já existe um alimento com o código: " + request.codigoSubstituicao());
        }

        Alimento alimento = Alimento.builder()
                .codigoSubstituicao(request.codigoSubstituicao())
                .grupo(request.grupo())
                .descricao(request.descricao())
                .energiaKcal(request.energiaKcal())
                .build();

        return toResponse(alimentoRepository.save(alimento));
    }

    // ====================================================
    // atualizar - Atualiza os dados de um alimento existente
    // ====================================================
    @Transactional
    public AlimentoResponse atualizar(Integer id, AtualizarAlimentoRequest request) {
        Alimento alimento = encontrarPorId(id);
        alimento.setCodigoSubstituicao(request.codigoSubstituicao());
        alimento.setGrupo(request.grupo());
        alimento.setDescricao(request.descricao());
        alimento.setEnergiaKcal(request.energiaKcal());
        return toResponse(alimentoRepository.save(alimento));
    }

    // ====================================================
    // remover - Remove permanentemente um alimento pelo ID
    // ====================================================
    @Transactional
    public void remover(Integer id) {
        if (!alimentoRepository.existsById(id)) {
            throw new EntityNotFoundException("Alimento não encontrado: " + id);
        }
        alimentoRepository.deleteById(id);
    }

    private Alimento encontrarPorId(Integer id) {
        return alimentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alimento não encontrado: " + id));
    }

    private AlimentoResponse toResponse(Alimento a) {
        return new AlimentoResponse(
                a.getId(),
                a.getCodigoSubstituicao(),
                a.getGrupo(),
                a.getDescricao(),
                a.getEnergiaKcal(),
                a.getCreatedAt()
        );
    }
}
