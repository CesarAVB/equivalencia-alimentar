package br.com.sistema.alimentos.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import br.com.sistema.alimentos.enums.GrupoAlimentar;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	// ====================================
	// addCorsMappings - Configura CORS para permitir requisições do frontend
	// ====================================
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// ====================================
		// Converter String para Array (com trim para remover espaços extras)
		// ====================================
		String[] origins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.toArray(String[]::new);

		log.info("🔐 CORS configurado para origens: {}", allowedOrigins);
		log.info("📊 Total de origens permitidas: {}", origins.length);

		for (String origin : origins) {
			log.info("✅ Origem permitida: {}", origin.trim());
		}

		registry.addMapping("/**")
				.allowedOrigins(origins)
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.exposedHeaders("Authorization", "Content-Disposition", "X-Total-Count")
				.allowCredentials(true)
				.maxAge(3600);

		log.info("✅ CORS inicializado com sucesso!");
	}

	@Override
	public void addFormatters(@NonNull FormatterRegistry registry) {
		registry.addConverter(String.class, GrupoAlimentar.class, GrupoAlimentar::fromValor);
	}
}