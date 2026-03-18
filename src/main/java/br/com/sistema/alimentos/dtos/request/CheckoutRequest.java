package br.com.sistema.alimentos.dtos.request;

import br.com.sistema.alimentos.enums.PlanoTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull(message = "O plano é obrigatório")
        PlanoTipo plano,

        @NotBlank(message = "A URL de sucesso é obrigatória")
        String successUrl,

        @NotBlank(message = "A URL de cancelamento é obrigatória")
        String cancelUrl
) {}
