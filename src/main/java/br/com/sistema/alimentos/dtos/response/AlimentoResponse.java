package br.com.sistema.alimentos.dtos.response;

import br.com.sistema.alimentos.enums.GrupoAlimentar;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlimentoResponse(
        Integer id,
        String codigoSubstituicao,
        GrupoAlimentar grupo,
        String descricao,
        BigDecimal energiaKcal,
        LocalDateTime createdAt
) {}
