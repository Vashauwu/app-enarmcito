-- Todo el catálogo exige cuenta y el acceso depende del tipo de contenido:
--   ARTICULO              -> REGISTRADO_GRATUITO (cualquier cuenta, sin tier)
--   GUIA_PRACTICA_CLINICA -> POR_TIER (tier de pago; el editor elige cuál, por defecto el más bajo)
--   RECURSO_ESTUDIO       -> POR_TIER con el tier más alto (Premium)
-- Ya no existe el acceso anónimo: PUBLICO_GRATUITO desaparece.

ALTER TABLE contenido DROP CONSTRAINT ck_contenido_politica;
ALTER TABLE contenido DROP CONSTRAINT ck_contenido_articulo_gratuito;
ALTER TABLE contenido DROP CONSTRAINT ck_contenido_tier;

ALTER TABLE contenido ALTER COLUMN politica_acceso SET DEFAULT 'REGISTRADO_GRATUITO';

UPDATE contenido
   SET politica_acceso = 'REGISTRADO_GRATUITO', tier_minimo_id = NULL
 WHERE tipo = 'ARTICULO';

-- Guías: conservan el tier que ya tuvieran; si no tenían, el tier activo más bajo.
UPDATE contenido
   SET politica_acceso = 'POR_TIER',
       tier_minimo_id = COALESCE(tier_minimo_id,
                                 (SELECT id FROM tier WHERE activo ORDER BY orden_acceso ASC LIMIT 1))
 WHERE tipo = 'GUIA_PRACTICA_CLINICA';

-- Recursos de estudio: siempre el tier activo más alto.
UPDATE contenido
   SET politica_acceso = 'POR_TIER',
       tier_minimo_id = (SELECT id FROM tier WHERE activo ORDER BY orden_acceso DESC LIMIT 1)
 WHERE tipo = 'RECURSO_ESTUDIO';

ALTER TABLE contenido ADD CONSTRAINT ck_contenido_politica
    CHECK (politica_acceso IN ('REGISTRADO_GRATUITO', 'POR_TIER'));
ALTER TABLE contenido ADD CONSTRAINT ck_contenido_tier
    CHECK ((politica_acceso = 'POR_TIER') = (tier_minimo_id IS NOT NULL));
-- Artículos: gratis con cuenta. Guías y recursos: siempre por tier.
ALTER TABLE contenido ADD CONSTRAINT ck_contenido_acceso_por_tipo
    CHECK ((tipo = 'ARTICULO' AND politica_acceso = 'REGISTRADO_GRATUITO')
        OR (tipo <> 'ARTICULO' AND politica_acceso = 'POR_TIER'));
