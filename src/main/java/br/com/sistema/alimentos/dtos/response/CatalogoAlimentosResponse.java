package br.com.sistema.alimentos.dtos.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CatalogoAlimentosResponse(
        String type,
        List<String> groups,
        Map<String, List<AlimentoCatalogoItem>> foods
) {
    public record AlimentoCatalogoItem(String text, BigDecimal quantity) {}
}
