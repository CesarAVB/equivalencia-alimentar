package br.com.sistema.alimentos.filter;

import br.com.sistema.alimentos.service.JwtService;
import br.com.sistema.alimentos.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve seguir cadeia sem autenticar quando header Authorization estiver ausente")
    void deveSeguirCadeiaSemAutenticarQuandoHeaderAuthorizationAusente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthFilter.doFilterInternal(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extrairEmail(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("Deve autenticar no contexto quando token for válido")
    void deveAutenticarNoContextoQuandoTokenForValido() throws Exception {
        String token = "token-valido";
        String email = "usuario@email.com";
        UserDetails userDetails = User.withUsername(email).password("x").authorities("ROLE_ADMIN").build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.extrairEmail(token)).thenReturn(email);
        when(usuarioService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.validarToken(token, userDetails)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(usuarioService).loadUserByUsername(email);
    }
}
