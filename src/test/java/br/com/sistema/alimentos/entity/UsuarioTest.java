package br.com.sistema.alimentos.entity;

import br.com.sistema.alimentos.enums.PlanoTipo;
import br.com.sistema.alimentos.enums.UsuarioTipo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioTest {

    @Test
    @DisplayName("Deve expor dados de autenticação e perfil corretamente")
    void deveExporDadosDeAutenticacaoEPerfilCorretamente() {
        Usuario usuario = Usuario.builder()
                .nome("Admin")
                .email("admin@email.com")
                .senha("senha")
                .tipo(UsuarioTipo.ADMIN)
                .plano(PlanoTipo.PADRAO)
                .ativo(true)
                .build();

        assertEquals("admin@email.com", usuario.getUsername());
        assertEquals("senha", usuario.getPassword());
        assertEquals(1, usuario.getAuthorities().size());
        assertTrue(usuario.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(usuario.isEnabled());
        assertTrue(usuario.isAccountNonLocked());
    }

    @Test
    @DisplayName("Deve indicar conta bloqueada quando usuário estiver inativo")
    void deveIndicarContaBloqueadaQuandoUsuarioInativo() {
        Usuario usuario = Usuario.builder()
                .email("inativo@email.com")
                .senha("senha")
                .tipo(UsuarioTipo.PACIENTE)
                .ativo(false)
                .build();

        assertFalse(usuario.isEnabled());
        assertFalse(usuario.isAccountNonLocked());
        assertTrue(usuario.isAccountNonExpired());
        assertTrue(usuario.isCredentialsNonExpired());
    }

    @Test
    @DisplayName("Deve preencher datas de criação e atualização")
    void devePreencherDatasDeCriacaoEAtualizacao() {
        Usuario usuario = Usuario.builder()
                .email("teste@email.com")
                .senha("senha")
                .tipo(UsuarioTipo.PACIENTE)
                .ativo(true)
                .build();

        usuario.onCreate();
        assertNotNull(usuario.getCreatedAt());

        usuario.onUpdate();
        assertNotNull(usuario.getUpdatedAt());
    }
}
