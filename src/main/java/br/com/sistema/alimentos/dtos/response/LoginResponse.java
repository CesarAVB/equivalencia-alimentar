package br.com.sistema.alimentos.dtos.response;

import br.com.sistema.alimentos.enums.PlanoTipo;
import br.com.sistema.alimentos.enums.UsuarioTipo;

import java.util.UUID;

public record LoginResponse(
        String token,
        String tipo,
        UUID id,
        String nome,
        String email,
        UsuarioTipo perfil,
        String plano
) {}
