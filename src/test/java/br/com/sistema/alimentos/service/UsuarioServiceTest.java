package br.com.sistema.alimentos.service;

import br.com.sistema.alimentos.dtos.request.AtualizarUsuarioRequest;
import br.com.sistema.alimentos.dtos.request.CriarUsuarioRequest;
import br.com.sistema.alimentos.dtos.response.UsuarioResponse;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Deve carregar usuário por e-mail quando existir")
    void deveCarregarUsuarioPorEmailQuandoExistir() {
        Usuario usuario = usuario(UUID.randomUUID(), "joao@email.com", "12345678");
        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuario));

        var userDetails = usuarioService.loadUserByUsername("joao@email.com");

        assertEquals("joao@email.com", userDetails.getUsername());
    }

    @Test
    @DisplayName("Deve lançar exceção ao carregar usuário inexistente")
    void deveLancarExcecaoAoCarregarUsuarioInexistente() {
        when(usuarioRepository.findByEmail("x@email.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> usuarioService.loadUserByUsername("x@email.com"));
    }

    @Test
    @DisplayName("Deve listar usuários convertendo para response")
    void deveListarUsuariosConvertendoParaResponse() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario(UUID.randomUUID(), "maria@email.com", "12345678")));

        List<UsuarioResponse> resultado = usuarioService.listar();

        assertEquals(1, resultado.size());
        assertEquals("maria@email.com", resultado.getFirst().email());
    }

    @Test
    @DisplayName("Deve buscar usuário por id quando existir")
    void deveBuscarUsuarioPorIdQuandoExistir() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario(id, "ana@email.com", "12345678")));

        UsuarioResponse response = usuarioService.buscarPorId(id);

        assertEquals(id, response.id());
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar usuário por id inexistente")
    void deveLancarExcecaoAoBuscarUsuarioPorIdInexistente() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> usuarioService.buscarPorId(id));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar usuário com e-mail duplicado")
    void deveLancarExcecaoAoCriarUsuarioComEmailDuplicado() {
        CriarUsuarioRequest request = new CriarUsuarioRequest("João", "joao@email.com", "123.456.789-00", "12345678", UsuarioTipo.ADMIN);
        when(usuarioRepository.existsByEmail("joao@email.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> usuarioService.criar(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar usuário com senha criptografada")
    void deveCriarUsuarioComSenhaCriptografada() {
        CriarUsuarioRequest request = new CriarUsuarioRequest("João", "joao@email.com", "123.456.789-00", "12345678", UsuarioTipo.ADMIN);
        Usuario salvo = usuario(UUID.randomUUID(), "joao@email.com", "senha-criptografada");

        when(usuarioRepository.existsByEmail("joao@email.com")).thenReturn(false);
        when(passwordEncoder.encode("12345678")).thenReturn("senha-criptografada");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(salvo);

        UsuarioResponse response = usuarioService.criar(request);

        assertEquals("joao@email.com", response.email());
    }

    @Test
    @DisplayName("Deve atualizar usuário quando id existir")
    void deveAtualizarUsuarioQuandoIdExistir() {
        UUID id = UUID.randomUUID();
        Usuario existente = usuario(id, "old@email.com", "12345678");
        AtualizarUsuarioRequest request = new AtualizarUsuarioRequest("Novo", "novo@email.com", "123.456.789-00", UsuarioTipo.NUTRICIONISTA);

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(eq(existente))).thenReturn(existente);

        UsuarioResponse response = usuarioService.atualizar(id, request);

        assertEquals("novo@email.com", response.email());
        assertEquals(UsuarioTipo.NUTRICIONISTA, response.tipo());
    }

    @Test
    @DisplayName("Deve alterar status do usuário")
    void deveAlterarStatusDoUsuario() {
        UUID id = UUID.randomUUID();
        Usuario existente = usuario(id, "status@email.com", "12345678");

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(eq(existente))).thenReturn(existente);

        UsuarioResponse response = usuarioService.alterarStatus(id, false);

        assertEquals(false, response.ativo());
    }

    @Test
    @DisplayName("Deve remover usuário quando id existir")
    void deveRemoverUsuarioQuandoIdExistir() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.existsById(id)).thenReturn(true);

        usuarioService.remover(id);

        verify(usuarioRepository).deleteById(id);
    }

    @Test
    @DisplayName("Deve lançar exceção ao remover usuário inexistente")
    void deveLancarExcecaoAoRemoverUsuarioInexistente() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.existsById(id)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> usuarioService.remover(id));
    }

    private static Usuario usuario(UUID id, String email, String senha) {
        return Usuario.builder()
                .id(id)
                .nome("Nome")
                .email(email)
                .senha(senha)
                .cpf("123.456.789-00")
                .tipo(UsuarioTipo.ADMIN)
                .ativo(true)
                .plano(PlanoTipo.PADRAO)
                .build();
    }
}
