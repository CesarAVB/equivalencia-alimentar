package br.com.sistema.alimentos.config;

import br.com.sistema.alimentos.entity.Usuario;
import br.com.sistema.alimentos.enums.UsuarioTipo;
import br.com.sistema.alimentos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bootstrap.admin.enabled", havingValue = "true")
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    @Value("${app.bootstrap.admin.nome:}")
    private String nome;

    @Value("${app.bootstrap.admin.email:}")
    private String email;

    @Value("${app.bootstrap.admin.cpf:}")
    private String cpf;

    @Value("${app.bootstrap.admin.senha:}")
    private String senha;

    @Value("${app.bootstrap.admin.tipo:ADMIN}")
    private String tipo;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(nome) || !StringUtils.hasText(email) || !StringUtils.hasText(senha)) {
            logger.warn("Bootstrap admin ignorado: propriedades obrigatorias ausentes");
            return;
        }

        if (usuarioRepository.existsByEmail(email)) {
            logger.info("Bootstrap admin ignorado: usuario ja existe para {}", email);
            return;
        }

        UsuarioTipo usuarioTipo;
        try {
            usuarioTipo = UsuarioTipo.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            logger.warn("Bootstrap admin ignorado: tipo invalido {}", tipo);
            return;
        }

        Usuario usuario = Usuario.builder()
                .nome(nome)
                .email(email)
                .cpf(StringUtils.hasText(cpf) ? cpf : null)
                .senha(passwordEncoder.encode(senha))
                .tipo(usuarioTipo)
                .build();

        usuarioRepository.save(usuario);
        logger.info("Bootstrap admin criado para {}", email);
    }
}
