-- ============================================================
-- V3__add_cpf_usuario.sql — Adiciona coluna CPF na tabela usuarios
-- ============================================================

ALTER TABLE usuarios
    ADD COLUMN cpf VARCHAR(14) NULL UNIQUE AFTER email;
