package br.com.sistema.alimentos.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
        @NotBlank(message = "A URL de sucesso é obrigatória")
        String successUrl,

        @NotBlank(message = "A URL de cancelamento é obrigatória")
        String cancelUrl
) {}
