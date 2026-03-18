-- ============================================================
-- V1__init.sql — Criação das tabelas iniciais
-- Projeto: Equivalência Alimentar
-- ============================================================

CREATE TABLE IF NOT EXISTS alimentos (
    id                   INT             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo_substituicao  VARCHAR(20)     NOT NULL UNIQUE,
    grupo                ENUM('Carboidratos','Frutas','Gordura Vegetal','Laticíneos','Proteína') NOT NULL,
    descricao            VARCHAR(255)    NOT NULL,
    energia_kcal         DECIMAL(10, 2)  NOT NULL,
    created_at           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_grupo (grupo),
    INDEX idx_descricao (descricao)
);

CREATE TABLE IF NOT EXISTS usuarios (
    id                   VARCHAR(36)     NOT NULL PRIMARY KEY,
    nome                 VARCHAR(255)    NOT NULL,
    email                VARCHAR(255)    NOT NULL UNIQUE,
    senha                VARCHAR(255)    NOT NULL,
    tipo                 VARCHAR(50)     NOT NULL,
    ativo                TINYINT(1)      NOT NULL DEFAULT 1,
    stripe_customer_id   VARCHAR(255),
    plano                VARCHAR(50)     NOT NULL DEFAULT 'FREE',
    plano_expira_em      DATETIME,
    created_at           DATETIME        NOT NULL,
    updated_at           DATETIME
);

CREATE TABLE IF NOT EXISTS equivalencias (
    id                   VARCHAR(36)     NOT NULL PRIMARY KEY,
    alimento_origem_id   INT             NOT NULL,
    alimento_destino_id  INT             NOT NULL,
    fator_equivalencia   DECIMAL(10, 4)  NOT NULL,
    observacao           TEXT,
    created_at           DATETIME        NOT NULL,
    updated_at           DATETIME,
    CONSTRAINT fk_equiv_origem  FOREIGN KEY (alimento_origem_id)  REFERENCES alimentos(id),
    CONSTRAINT fk_equiv_destino FOREIGN KEY (alimento_destino_id) REFERENCES alimentos(id)
);
