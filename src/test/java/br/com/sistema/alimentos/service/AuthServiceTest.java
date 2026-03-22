package br.com.sistema.alimentos.service;

import br.com.sistema.alimentos.dtos.request.LoginRequest;
import br.com.sistema.alimentos.dtos.response.LoginResponse;
import br.com.sistema.alimentos.entity.Usuario;
import br.com.sistema.alimentos.enums.PlanoTipo;
import br.com.sistema.alimentos.enums.UsuarioTipo;
import br.com.sistema.alimentos.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve autenticar e retornar token quando credenciais forem válidas")
    void deveAutenticarERetornarTokenQuandoCredenciaisForemValidas() {
        LoginRequest request = new LoginRequest("maria@email.com", "123456");
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Maria")
                .email("maria@email.com")
                .senha("senha")
                .tipo(UsuarioTipo.ADMIN)
                .plano(PlanoTipo.PADRAO)
                .ativo(true)
                .build();

        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario));
        when(jwtService.gerarToken(usuario)).thenReturn("token-jwt");

        LoginResponse response = authService.autenticar(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertEquals("token-jwt", response.token());
        assertEquals("maria@email.com", response.email());
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não for encontrado após autenticação")
    void deveLancarExcecaoQuandoUsuarioNaoForEncontrado() {
        LoginRequest request = new LoginRequest("naoexiste@email.com", "123456");
        when(usuarioRepository.findByEmail("naoexiste@email.com")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> authService.autenticar(request));
    }
}
