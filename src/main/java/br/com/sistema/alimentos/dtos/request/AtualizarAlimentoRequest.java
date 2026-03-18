package br.com.sistema.alimentos.dtos.request;

import br.com.sistema.alimentos.enums.GrupoAlimentar;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AtualizarAlimentoRequest(
        @NotBlank(message = "O código de substituição é obrigatório")
        String codigoSubstituicao,

        @NotNull(message = "O grupo alimentar é obrigatório")
        GrupoAlimentar grupo,

        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        @NotNull(message = "A energia (kcal) é obrigatória")
        @Positive(message = "A energia deve ser um valor positivo")
        BigDecimal energiaKcal
) {}
