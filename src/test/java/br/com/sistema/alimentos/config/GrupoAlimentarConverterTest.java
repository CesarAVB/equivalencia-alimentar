package br.com.sistema.alimentos.config;

import br.com.sistema.alimentos.enums.GrupoAlimentar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrupoAlimentarConverterTest {

    private final GrupoAlimentarConverter converter = new GrupoAlimentarConverter();

    @Test
    @DisplayName("Deve converter enum para valor de banco")
    void deveConverterEnumParaValorDeBanco() {
        String valor = converter.convertToDatabaseColumn(GrupoAlimentar.FRUTAS);

        assertEquals("Frutas", valor);
    }

    @Test
    @DisplayName("Deve retornar null ao converter enum nulo")
    void deveRetornarNullAoConverterEnumNulo() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    @DisplayName("Deve converter valor de banco para enum")
    void deveConverterValorDeBancoParaEnum() {
        GrupoAlimentar grupo = converter.convertToEntityAttribute("Proteína");

        assertEquals(GrupoAlimentar.PROTEINA, grupo);
    }

    @Test
    @DisplayName("Deve retornar null quando valor de banco for nulo")
    void deveRetornarNullQuandoValorDeBancoForNulo() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    @DisplayName("Deve lançar exceção para valor de banco inválido")
    void deveLancarExcecaoParaValorDeBancoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("Inexistente"));
    }
}
