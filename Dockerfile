# ===============================
# STAGE 1 — BUILD
# ===============================
# Usamos Eclipse Temurin 21 JDK porque:
# - O projeto compila com Java 21
# - O Coolify NÃO tem JDK 21 instalado
# - A imagem já vem com javac, Maven wrapper funciona sem gambiarra
FROM eclipse-temurin:21-jdk AS build

# Define o diretório de trabalho dentro do container
# Evita espalhar arquivos pela raiz do sistema
WORKDIR /app

# Copiamos todo o código-fonte para dentro da imagem
# Isso inclui pom.xml, src/, mvnw etc.
COPY . .

# Executa o build dentro da imagem
# Motivo:
# - O ambiente fica 100% controlado
# - Não depende do Java do host (Coolify)
# - Garante que o jar final é Java 21
RUN chmod +x ./mvnw \
 && ./mvnw -B -DskipTests clean package


# ===============================
# STAGE 2 — RUNTIME
# ===============================
# Usamos apenas o JRE (sem compilador) para rodar a aplicação
# Motivo:
# - Imagem menor
# - Menos superfície de ataque
# - Melhor prática para produção
FROM eclipse-temurin:21-jre

# Diretório da aplicação no container final
WORKDIR /app

# Copiamos APENAS o jar gerado no stage de build
# Nada de código-fonte, Maven ou cache vai para produção
COPY --from=build /app/target/*.jar app.jar

# Porta padrão do Spring Boot
EXPOSE 8080

# Comando de inicialização do container
# O Java 21 já está dentro da imagem, independente do Coolify
ENTRYPOINT ["java", "-jar", "app.jar"]