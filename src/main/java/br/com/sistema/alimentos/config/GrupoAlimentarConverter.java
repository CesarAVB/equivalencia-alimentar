package br.com.sistema.alimentos.config;

import br.com.sistema.alimentos.enums.GrupoAlimentar;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter(autoApply = true)
public class GrupoAlimentarConverter implements AttributeConverter<GrupoAlimentar, String> {

    @Override
    public String convertToDatabaseColumn(GrupoAlimentar grupo) {
        if (grupo == null) return null;
        return grupo.getValor();
    }

    @Override
    public GrupoAlimentar convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return Arrays.stream(GrupoAlimentar.values())
                .filter(g -> g.getValor().equals(dbData))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Grupo alimentar desconhecido: " + dbData));
    }
}
