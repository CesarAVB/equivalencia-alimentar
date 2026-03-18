package br.com.sistema.alimentos.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class EquivalenciaTest {

    @Test
    @DisplayName("Deve preencher createdAt no prePersist")
    void devePreencherCreatedAtNoPrePersist() {
        Equivalencia equivalencia = Equivalencia.builder().build();

        equivalencia.onCreate();

        assertNotNull(equivalencia.getCreatedAt());
    }

    @Test
    @DisplayName("Deve preencher updatedAt no preUpdate")
    void devePreencherUpdatedAtNoPreUpdate() {
        Equivalencia equivalencia = Equivalencia.builder().build();

        equivalencia.onUpdate();

        assertNotNull(equivalencia.getUpdatedAt());
    }
}
