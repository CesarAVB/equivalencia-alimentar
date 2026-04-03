package br.com.sistema.alimentos.service;

import br.com.sistema.alimentos.dtos.request.AtualizarAlimentoRequest;
import br.com.sistema.alimentos.dtos.request.CriarAlimentoRequest;
import br.com.sistema.alimentos.dtos.response.AlimentoResponse;
import br.com.sistema.alimentos.dtos.response.CatalogoAlimentosResponse;
import br.com.sistema.alimentos.dtos.response.CatalogoAlimentosResponse.AlimentoCatalogoItem;
import br.com.sistema.alimentos.dtos.response.EquivalenciaDinamicaResponse;
import br.com.sistema.alimentos.dtos.response.EquivalenciaDinamicaResponse.ItemEquivalencia;
import br.com.sistema.alimentos.entity.Alimento;
import br.com.sistema.alimentos.enums.GrupoAlimentar;
import br.com.sistema.alimentos.repository.AlimentoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlimentoService {

    private final AlimentoRepository alimentoRepository;

    // ====================================================
    // catalogar - Retorna todos os alimentos agrupados por grupo alimentar
    // ====================================================
    public CatalogoAlimentosResponse catalogar() {
        Map<String, List<AlimentoCatalogoItem>> foods = new LinkedHashMap<>();

        Arrays.stream(GrupoAlimentar.values()).forEach(grupo -> {
            List<AlimentoCatalogoItem> itens = alimentoRepository
                    .findByGrupoOrderByDescricaoAsc(grupo)
                    .stream()
                    .map(a -> new AlimentoCatalogoItem(a.getDescricao(), a.getEnergiaKcal()))
                    .toList();
            foods.put(grupo.getValor(), itens);
        });

        return new CatalogoAlimentosResponse("success", foods.keySet().stream().toList(), foods);
    }

    // ====================================================
    // calcularEquivalencias - Calcula equivalências calóricas dinamicamente
    // fator = kcal_origem / kcal_destino
    // quantidadeEquivalente = quantidadeGramas * fator
    // ====================================================
    public EquivalenciaDinamicaResponse calcularEquivalencias(Integer alimentoOrigemId, BigDecimal quantidadeGramas) {
        Alimento origem = encontrarPorId(alimentoOrigemId);

        List<ItemEquivalencia> equivalencias = alimentoRepository
                .findByGrupoOrderByDescricaoAsc(origem.getGrupo())
                .stream()
                .filter(a -> !a.getId().equals(alimentoOrigemId))
                .map(destino -> {
                    BigDecimal fator = origem.getEnergiaKcal()
                            .divide(destino.getEnergiaKcal(), 10, RoundingMode.HALF_UP);
                    BigDecimal quantidadeEquivalente = quantidadeGramas
                            .multiply(fator)
                            .setScale(2, RoundingMode.HALF_UP);
                    return new ItemEquivalencia(destino.getId(), destino.getDescricao(), quantidadeEquivalente);
                })
                .toList();

        return new EquivalenciaDinamicaResponse(
                origem.getId(),
                origem.getDescricao(),
                origem.getGrupo().getValor(),
                quantidadeGramas,
                equivalencias
        );
    }

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
