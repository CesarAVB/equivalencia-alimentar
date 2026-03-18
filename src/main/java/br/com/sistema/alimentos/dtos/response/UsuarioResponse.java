package br.com.sistema.alimentos.dtos.response;

import br.com.sistema.alimentos.enums.PlanoTipo;
import br.com.sistema.alimentos.enums.UsuarioTipo;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        String cpf,
        UsuarioTipo tipo,
        boolean ativo,
        PlanoTipo plano,
        LocalDateTime planoExpiraEm,
        LocalDateTime createdAt
) {}
