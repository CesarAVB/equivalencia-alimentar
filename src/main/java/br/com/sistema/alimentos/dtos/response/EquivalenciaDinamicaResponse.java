package br.com.sistema.alimentos.dtos.response;

import java.math.BigDecimal;
import java.util.List;

public record EquivalenciaDinamicaResponse(
        Integer alimentoOrigemId,
        String alimentoOrigemDescricao,
        String grupo,
        BigDecimal quantidadeGramas,
        List<ItemEquivalencia> equivalencias
) {
    public record ItemEquivalencia(
            Integer alimentoId,
            String alimentoDescricao,
            BigDecimal quantidadeEquivalente
    ) {}
}
