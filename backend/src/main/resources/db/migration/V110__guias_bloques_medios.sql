-- Módulo de guías de práctica clínica y contenido enriquecido (docs/modulo-guias-practica-clinica.md).
-- Compatible con bases existentes: los contenidos actuales pasan a ARTICULO / PUBLICO_GRATUITO.

-- ---------------------------------------------------------------------------------------------------
-- Rol nuevo
-- ---------------------------------------------------------------------------------------------------
ALTER TABLE usuarios_sistema DROP CONSTRAINT ck_usuarios_sistema_rol;
ALTER TABLE usuarios_sistema ADD CONSTRAINT ck_usuarios_sistema_rol CHECK (rol IN (
    'ALUMNO', 'AUTOR_CONTENIDO', 'REVISOR_MEDICO', 'EDITOR_CONTENIDO', 'GESTOR_MULTIMEDIA', 'SOPORTE',
    'ADMIN_USUARIOS', 'ADMIN_PLATAFORMA', 'SUPER_ADMIN'));

-- ---------------------------------------------------------------------------------------------------
-- Contenido: tipo, política de acceso, flujo editorial y clínico, campos de GPC y de recursos
-- ---------------------------------------------------------------------------------------------------
ALTER TABLE contenido DROP CONSTRAINT ck_contenido_estado;
ALTER TABLE contenido DROP CONSTRAINT ck_contenido_completo;
ALTER TABLE contenido ALTER COLUMN estado TYPE VARCHAR(30);
-- La revisión única del flujo anterior equivale a la revisión clínica.
UPDATE contenido SET estado = 'EN_REVISION_CLINICA' WHERE estado = 'EN_REVISION';
ALTER TABLE contenido ADD CONSTRAINT ck_contenido_estado CHECK (estado IN (
    'BORRADOR', 'EN_EDICION', 'EN_REVISION_EDITORIAL', 'CAMBIOS_SOLICITADOS', 'EN_REVISION_CLINICA',
    'APROBADO', 'PUBLICADO', 'REVISION_REQUERIDA', 'ARCHIVADO'));

ALTER TABLE contenido
    ADD COLUMN tipo                       VARCHAR(30)  NOT NULL DEFAULT 'ARTICULO',
    ADD COLUMN politica_acceso            VARCHAR(30)  NOT NULL DEFAULT 'PUBLICO_GRATUITO',
    ADD COLUMN editor_id                  UUID REFERENCES usuarios (id),
    ADD COLUMN revision_clinica_requerida BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Guía de práctica clínica
    ADD COLUMN institucion_emisora        VARCHAR(200),
    ADD COLUMN fuente_edicion             VARCHAR(100),
    ADD COLUMN poblacion_objetivo         TEXT,
    ADD COLUMN contexto_atencion          TEXT,
    ADD COLUMN alcance_clinico            TEXT,
    ADD COLUMN vigente_hasta              DATE,
    ADD COLUMN proxima_revision           DATE,
    -- Lista JSON [{texto, fuerza, nivelEvidencia, referencia}]
    ADD COLUMN recomendaciones            TEXT,
    -- Recurso de estudio: procedencia y derechos de uso
    ADD COLUMN procedencia                TEXT,
    ADD COLUMN titular_derechos           VARCHAR(200),
    ADD COLUMN licencia_uso               VARCHAR(200),
    ADD COLUMN autorizacion_publicacion   BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN autorizacion_referencia    TEXT,
    -- Publicación: versión pública vigente (instantánea) y si sigue en circulación
    ADD COLUMN version_publicada          INTEGER,
    ADD COLUMN publicacion_activa         BOOLEAN      NOT NULL DEFAULT FALSE;

ALTER TABLE contenido ADD CONSTRAINT ck_contenido_tipo
    CHECK (tipo IN ('ARTICULO', 'GUIA_PRACTICA_CLINICA', 'RECURSO_ESTUDIO'));
ALTER TABLE contenido ADD CONSTRAINT ck_contenido_politica
    CHECK (politica_acceso IN ('PUBLICO_GRATUITO', 'REGISTRADO_GRATUITO', 'POR_TIER'));
-- Los artículos son siempre gratuitos y públicos.
ALTER TABLE contenido ADD CONSTRAINT ck_contenido_articulo_gratuito
    CHECK (tipo <> 'ARTICULO' OR politica_acceso = 'PUBLICO_GRATUITO');
-- Una GPC siempre exige revisión clínica.
ALTER TABLE contenido ADD CONSTRAINT ck_contenido_gpc_revision
    CHECK (tipo <> 'GUIA_PRACTICA_CLINICA' OR revision_clinica_requerida);

-- El tier sólo aplica a la política POR_TIER.
ALTER TABLE contenido ALTER COLUMN tier_minimo_id DROP NOT NULL;
UPDATE contenido SET tier_minimo_id = NULL;
ALTER TABLE contenido ADD CONSTRAINT ck_contenido_tier
    CHECK ((politica_acceso = 'POR_TIER') = (tier_minimo_id IS NOT NULL));

UPDATE contenido SET publicacion_activa = TRUE WHERE estado = 'PUBLICADO';

CREATE INDEX ix_contenido_tipo ON contenido (tipo);
CREATE INDEX ix_contenido_publicacion ON contenido (publicacion_activa, version_publicada);

-- ---------------------------------------------------------------------------------------------------
-- Revisiones: etapa (editorial o clínica) y versión preparada
-- ---------------------------------------------------------------------------------------------------
ALTER TABLE contenido_revision
    ADD COLUMN etapa             VARCHAR(20) NOT NULL DEFAULT 'CLINICA',
    ADD COLUMN version_contenido INTEGER     NOT NULL DEFAULT 1;
ALTER TABLE contenido_revision ADD CONSTRAINT ck_contenido_revision_etapa
    CHECK (etapa IN ('EDITORIAL', 'CLINICA'));

-- ---------------------------------------------------------------------------------------------------
-- Bloques de contenido enriquecido
-- ---------------------------------------------------------------------------------------------------
CREATE TABLE contenido_bloque (
    id            UUID        PRIMARY KEY,
    contenido_id  UUID        NOT NULL REFERENCES contenido (id),
    posicion      INTEGER     NOT NULL,
    tipo          VARCHAR(20) NOT NULL,
    -- JSON validado y saneado por la API según el tipo.
    datos         TEXT        NOT NULL,
    estado        VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    autor_id      UUID        REFERENCES usuarios (id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_bloque_tipo CHECK (tipo IN ('TEXTO_RICO', 'IMAGEN', 'DIAGRAMA', 'TABLA', 'CALLOUT', 'ALGORITMO',
                                              'ACORDEON', 'REFERENCIAS', 'ADJUNTO')),
    CONSTRAINT ck_bloque_estado CHECK (estado IN ('ACTIVO', 'RETIRADO'))
);

CREATE INDEX ix_bloque_contenido ON contenido_bloque (contenido_id, posicion);

-- ---------------------------------------------------------------------------------------------------
-- Activos multimedia (los archivos viven fuera de la base de datos)
-- ---------------------------------------------------------------------------------------------------
CREATE TABLE activo_multimedia (
    id                   UUID         PRIMARY KEY,
    nombre_original      VARCHAR(255) NOT NULL,
    tipo_mime            VARCHAR(50)  NOT NULL,
    tamano_bytes         BIGINT       NOT NULL,
    sha256               VARCHAR(64)  NOT NULL,
    ruta_original        VARCHAR(300) NOT NULL,
    ruta_escritorio      VARCHAR(300),
    ruta_movil           VARCHAR(300),
    ancho                INTEGER,
    alto                 INTEGER,
    texto_alternativo    VARCHAR(500),
    pie                  VARCHAR(500),
    credito              VARCHAR(300),
    licencia             VARCHAR(200),
    derechos_confirmados BOOLEAN      NOT NULL,
    estado               VARCHAR(20)  NOT NULL,
    motivo_rechazo       TEXT,
    subido_por           UUID         NOT NULL REFERENCES usuarios (id),
    revisado_por         UUID         REFERENCES usuarios (id),
    revisado_at          TIMESTAMPTZ,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_activo_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'ARCHIVADO')),
    CONSTRAINT ck_activo_mime CHECK (tipo_mime IN ('image/jpeg', 'image/png', 'image/webp', 'image/svg+xml',
                                                   'application/pdf')),
    -- Ninguna imagen se aprueba sin texto alternativo.
    CONSTRAINT ck_activo_alt CHECK (estado <> 'APROBADO' OR tipo_mime = 'application/pdf'
                                    OR texto_alternativo IS NOT NULL)
);

CREATE INDEX ix_activo_estado ON activo_multimedia (estado, created_at DESC);

-- Relación vigente (copia de trabajo) entre contenido, bloque y medio.
CREATE TABLE contenido_activo (
    contenido_id  UUID        NOT NULL REFERENCES contenido (id),
    bloque_id     UUID        NOT NULL REFERENCES contenido_bloque (id),
    activo_id     UUID        NOT NULL REFERENCES activo_multimedia (id),
    proposito     VARCHAR(20) NOT NULL,
    PRIMARY KEY (bloque_id, activo_id)
);

CREATE INDEX ix_contenido_activo_activo ON contenido_activo (activo_id);

-- ---------------------------------------------------------------------------------------------------
-- Versiones publicadas: instantánea inmutable de lo que ve el público
-- ---------------------------------------------------------------------------------------------------
CREATE TABLE contenido_version (
    id                      UUID         PRIMARY KEY,
    contenido_id            UUID         NOT NULL REFERENCES contenido (id),
    version                 INTEGER      NOT NULL,
    tipo                    VARCHAR(30)  NOT NULL,
    titulo                  VARCHAR(200) NOT NULL,
    slug                    VARCHAR(220) NOT NULL,
    area_medica             VARCHAR(100),
    extracto                TEXT,
    tiempo_lectura_minutos  INTEGER,
    proxima_revision        DATE,
    -- JSON con cabecera, bloques saneados, medios, fuentes y responsables.
    datos                   TEXT         NOT NULL,
    publicado_por           UUID         REFERENCES usuarios (id),
    publicado_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_contenido_version UNIQUE (contenido_id, version)
);

CREATE TABLE contenido_version_activo (
    contenido_id  UUID    NOT NULL REFERENCES contenido (id),
    version       INTEGER NOT NULL,
    activo_id     UUID    NOT NULL REFERENCES activo_multimedia (id),
    PRIMARY KEY (contenido_id, version, activo_id)
);

CREATE INDEX ix_version_activo_activo ON contenido_version_activo (activo_id);

-- Las versiones publicadas tampoco se modifican.
CREATE FUNCTION contenido_version_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Una versión publicada no admite modificaciones ni eliminaciones';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_contenido_version_inmutable
    BEFORE UPDATE OR DELETE ON contenido_version
    FOR EACH ROW EXECUTE FUNCTION contenido_version_inmutable();
