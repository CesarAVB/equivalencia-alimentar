package br.com.sistema.alimentos.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum GrupoAlimentar {

    FRUTAS("Frutas"),
    CARBOIDRATOS("Carboidratos"),
    GORDURA_VEGETAL("Gordura Vegetal"),
    LATICINEOS("Laticíneos"),
    PROTEINA("Proteína");

    private final String valor;

    GrupoAlimentar(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static GrupoAlimentar fromValor(String valor) {
        return Arrays.stream(values())
                .filter(g -> g.getValor().equals(valor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Grupo alimentar desconhecido: " + valor));
    }
}
