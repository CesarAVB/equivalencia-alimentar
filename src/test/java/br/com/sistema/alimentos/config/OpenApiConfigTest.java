package br.com.sistema.alimentos.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiConfigTest {

    @Test
    @DisplayName("Deve montar configuração OpenAPI com info e esquema bearer")
    void deveMontarConfiguracaoOpenApiComInfoEEsquemaBearer() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.customOpenAPI();

        assertEquals("Equivalência Alimentar API - Sistema de Nutrição", openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("Bearer Authentication"));
        assertEquals("bearer", openAPI.getComponents().getSecuritySchemes().get("Bearer Authentication").getScheme());
    }
}
