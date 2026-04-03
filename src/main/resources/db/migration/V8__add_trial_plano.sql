-- ============================================================
-- V8__add_trial_plano.sql
-- Adiciona TRIAL como valor padrão para novos usuários
-- ============================================================

ALTER TABLE usuarios
    MODIFY COLUMN plano VARCHAR(50) NOT NULL DEFAULT 'TRIAL';
