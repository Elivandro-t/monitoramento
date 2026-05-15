-- Migration: Adiciona campos de tipo de equipamento
-- Data: 2026-05-15

-- Adiciona coluna tipo_equipamento
ALTER TABLE agente ADD COLUMN tipo_equipamento VARCHAR(50);

-- Adiciona coluna caminho_cups para impressoras
ALTER TABLE agente ADD COLUMN caminho_cups VARCHAR(255);

-- Adiciona coluna caminho_monitor para balanças
ALTER TABLE agente ADD COLUMN caminho_monitor VARCHAR(255);

-- Create index para tipo_equipamento
CREATE INDEX idx_agente_tipo_equipamento ON agente(tipo_equipamento);
