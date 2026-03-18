package br.com.sistema.alimentos.service;

import br.com.sistema.alimentos.dtos.request.AtualizarUsuarioRequest;
import br.com.sistema.alimentos.dtos.request.CriarUsuarioRequest;
import br.com.sistema.alimentos.dtos.response.UsuarioResponse;
import br.com.sistema.alimentos.entity.Usuario;
import br.com.sistema.alimentos.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // ====================================================
    // loadUserByUsername - Implementação do UserDetailsService para o Spring Security
    // ====================================================
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }

    // ====================================================
    // listar - Retorna todos os usuários cadastrados
    // ====================================================
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // ====================================================
    // buscarPorId - Retorna um usuário pelo ID
    // ====================================================
    public UsuarioResponse buscarPorId(UUID id) {
        return toResponse(encontrarPorId(id));
    }

    // ====================================================
    // criar - Cria um novo usuário com senha criptografada
    // ====================================================
    @Transactional
    public UsuarioResponse criar(CriarUsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Já existe um usuário com o e-mail: " + request.email());
        }

        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .email(request.email())
                .cpf(request.cpf())
                .senha(passwordEncoder.encode(request.senha()))
                .tipo(request.tipo())
                .build();

        return toResponse(usuarioRepository.save(usuario));
    }

    // ====================================================
    // atualizar - Atualiza os dados de um usuário existente
    // ====================================================
    @Transactional
    public UsuarioResponse atualizar(UUID id, AtualizarUsuarioRequest request) {
        Usuario usuario = encontrarPorId(id);
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setCpf(request.cpf());
        usuario.setTipo(request.tipo());
        return toResponse(usuarioRepository.save(usuario));
    }

    // ====================================================
    // alterarStatus - Ativa ou desativa o acesso de um usuário
    // ====================================================
    @Transactional
    public UsuarioResponse alterarStatus(UUID id, boolean ativo) {
        Usuario usuario = encontrarPorId(id);
        usuario.setAtivo(ativo);
        return toResponse(usuarioRepository.save(usuario));
    }

    // ====================================================
    // remover - Remove permanentemente um usuário pelo ID
    // ====================================================
    @Transactional
    public void remover(UUID id) {
        if (!usuarioRepository.existsById(id)) {
            throw new EntityNotFoundException("Usuário não encontrado: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    private Usuario encontrarPorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + id));
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getCpf(),
                u.getTipo(),
                u.isAtivo(),
                u.getPlano(),
                u.getPlanoExpiraEm(),
                u.getCreatedAt()
        );
    }
}
