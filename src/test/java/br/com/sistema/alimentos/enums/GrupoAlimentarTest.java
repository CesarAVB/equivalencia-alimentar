package br.com.sistema.alimentos.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrupoAlimentarTest {

    @Test
    @DisplayName("Deve retornar valor textual do enum")
    void deveRetornarValorTextualDoEnum() {
        assertEquals("Frutas", GrupoAlimentar.FRUTAS.getValor());
    }

    @Test
    @DisplayName("Deve converter valor textual para enum")
    void deveConverterValorTextualParaEnum() {
        assertEquals(GrupoAlimentar.PROTEINA, GrupoAlimentar.fromValor("Proteína"));
    }

    @Test
    @DisplayName("Deve lançar exceção para valor textual inválido")
    void deveLancarExcecaoParaValorTextualInvalido() {
        assertThrows(IllegalArgumentException.class, () -> GrupoAlimentar.fromValor("Inexistente"));
    }
}
