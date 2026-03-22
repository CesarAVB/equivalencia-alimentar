-- ============================================================
-- V4__migrate_plano_padrao.sql — Normaliza planos legados
-- Objetivo: compatibilizar dados antigos com enum PlanoTipo atual
-- ============================================================

-- Converte valores legados para o plano único atual
UPDATE usuarios
SET plano = 'PADRAO'
WHERE plano IN ('FREE', 'BASIC', 'PRO') OR plano IS NULL;

-- Garante valor default do novo padrão
ALTER TABLE usuarios
    MODIFY COLUMN plano VARCHAR(50) NOT NULL DEFAULT 'PADRAO';
