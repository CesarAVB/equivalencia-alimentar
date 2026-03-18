package br.com.sistema.alimentos.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CriarEquivalenciaRequest(
        @NotNull(message = "O alimento de origem é obrigatório")
        Integer alimentoOrigemId,

        @NotNull(message = "O alimento de destino é obrigatório")
        Integer alimentoDestinoId,

        @NotNull(message = "O fator de equivalência é obrigatório")
        @Positive(message = "O fator de equivalência deve ser positivo")
        BigDecimal fatorEquivalencia,

        String observacao
) {}
