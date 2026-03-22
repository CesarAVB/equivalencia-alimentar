package br.com.sistema.alimentos.service;

import br.com.sistema.alimentos.dtos.request.LoginRequest;
import br.com.sistema.alimentos.dtos.response.LoginResponse;
import br.com.sistema.alimentos.entity.Usuario;
import br.com.sistema.alimentos.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ====================================================
    // autenticar - Valida as credenciais e retorna o token JWT com dados do usuário
    // ====================================================
    public LoginResponse autenticar(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

                String token = jwtService.gerarToken(usuario);

                // Calcula string de exibição do plano: "trial" ou "padrão"
                boolean isTrial = false;
                if (usuario.getPlanoExpiraEm() != null) {
                        isTrial = java.time.LocalDateTime.now().isBefore(usuario.getPlanoExpiraEm());
                } else if (usuario.getCreatedAt() != null) {
                        isTrial = java.time.LocalDateTime.now().isBefore(usuario.getCreatedAt().plusDays(30));
                }

                String planoExibicao = isTrial ? "trial" : "padrão";

                return new LoginResponse(
                                token,
                                "Bearer",
                                usuario.getId(),
                                usuario.getNome(),
                                usuario.getEmail(),
                                usuario.getTipo(),
                                planoExibicao
                );
    }
}
