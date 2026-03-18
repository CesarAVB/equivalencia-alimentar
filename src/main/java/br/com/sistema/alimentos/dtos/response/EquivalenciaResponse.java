package br.com.sistema.alimentos.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EquivalenciaResponse(
        UUID id,
        Integer alimentoOrigemId,
        String alimentoOrigemDescricao,
        Integer alimentoDestinoId,
        String alimentoDestinoDescricao,
        BigDecimal fatorEquivalencia,
        String observacao,
        LocalDateTime createdAt
) {}
