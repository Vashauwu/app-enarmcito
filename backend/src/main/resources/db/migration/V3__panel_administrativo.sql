-- Panel administrativo: roles, estado de cuenta, invitaciones, flujo editorial, revisiones y auditoría.
-- Compatible con bases existentes: conserva cuentas, contenidos y progreso.

-- ---------------------------------------------------------------------------------------------------
-- Roles y estado de cuenta
-- ---------------------------------------------------------------------------------------------------
ALTER TABLE usuarios_sistema DROP CONSTRAINT ck_usuarios_sistema_rol;
ALTER TABLE usuarios_sistema ALTER COLUMN rol TYPE VARCHAR(30);
-- El rol ADMIN del MVP se migra a ADMIN_PLATAFORMA.
UPDATE usuarios_sistema SET rol = 'ADMIN_PLATAFORMA' WHERE rol = 'ADMIN';
ALTER TABLE usuarios_sistema ADD CONSTRAINT ck_usuarios_sistema_rol CHECK (rol IN (
    'ALUMNO', 'AUTOR_CONTENIDO', 'REVISOR_MEDICO', 'EDITOR_CONTENIDO', 'SOPORTE',
    'ADMIN_USUARIOS', 'ADMIN_PLATAFORMA', 'SUPER_ADMIN'));

-- PENDIENTE: cuenta creada por invitación que aún no define su contraseña.
ALTER TABLE usuarios_sistema DROP CONSTRAINT ck_usuarios_sistema_estado;
ALTER TABLE usuarios_sistema ADD CONSTRAINT ck_usuarios_sistema_estado
    CHECK (estado IN ('ACTIVO', 'BLOQUEADO', 'PENDIENTE'));
ALTER TABLE usuarios_sistema ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE usuarios_sistema ADD CONSTRAINT ck_usuarios_sistema_password
    CHECK (estado = 'PENDIENTE' OR password_hash IS NOT NULL);

ALTER TABLE usuarios_sistema
    ADD COLUMN bloqueo_motivo TEXT,
    ADD COLUMN bloqueado_at   TIMESTAMPTZ;

CREATE INDEX ix_usuarios_sistema_rol ON usuarios_sistema (rol);
CREATE INDEX ix_usuarios_sistema_estado ON usuarios_sistema (estado);
CREATE INDEX ix_usuarios_sistema_last_login ON usuarios_sistema (last_login_at);
CREATE INDEX ix_usuarios_created_at ON usuarios (created_at);

-- ---------------------------------------------------------------------------------------------------
-- Enlaces de un solo uso para activar cuentas invitadas o restablecer el acceso.
-- Sólo se guarda el SHA-256 del token; el enlace se muestra una única vez a quien lo genera.
-- ---------------------------------------------------------------------------------------------------
CREATE TABLE token_acceso (
    id          UUID        PRIMARY KEY,
    usuario_id  UUID        NOT NULL REFERENCES usuarios (id),
    tipo        VARCHAR(20) NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expira_at   TIMESTAMPTZ NOT NULL,
    usado_at    TIMESTAMPTZ,
    creado_por  UUID        REFERENCES usuarios (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_token_acceso_hash UNIQUE (token_hash),
    CONSTRAINT ck_token_acceso_tipo CHECK (tipo IN ('INVITACION', 'RESTABLECIMIENTO'))
);

CREATE INDEX ix_token_acceso_usuario ON token_acceso (usuario_id);

-- ---------------------------------------------------------------------------------------------------
-- Flujo editorial del contenido
-- ---------------------------------------------------------------------------------------------------
ALTER TABLE contenido DROP CONSTRAINT ck_contenido_estado;
ALTER TABLE contenido ADD CONSTRAINT ck_contenido_estado CHECK (estado IN (
    'BORRADOR', 'EN_REVISION', 'CAMBIOS_SOLICITADOS', 'APROBADO', 'PUBLICADO', 'ARCHIVADO'));

-- Un borrador puede guardarse incompleto; desde la revisión en adelante los campos son obligatorios.
ALTER TABLE contenido
    ALTER COLUMN area_medica DROP NOT NULL,
    ALTER COLUMN resumen DROP NOT NULL,
    ALTER COLUMN puntos_clave DROP NOT NULL,
    ALTER COLUMN fuente_titulo DROP NOT NULL,
    ALTER COLUMN fecha_revision DROP NOT NULL,
    ALTER COLUMN tiempo_lectura_minutos DROP NOT NULL;

ALTER TABLE contenido ADD CONSTRAINT ck_contenido_completo CHECK (
    estado IN ('BORRADOR', 'CAMBIOS_SOLICITADOS', 'ARCHIVADO')
    OR (area_medica IS NOT NULL AND resumen IS NOT NULL AND puntos_clave IS NOT NULL
        AND fuente_titulo IS NOT NULL AND fecha_revision IS NOT NULL AND tiempo_lectura_minutos IS NOT NULL));

ALTER TABLE contenido
    ADD COLUMN autor_id            UUID REFERENCES usuarios (id),
    ADD COLUMN revisor_id          UUID REFERENCES usuarios (id),
    ADD COLUMN enviado_revision_at TIMESTAMPTZ,
    ADD COLUMN aprobado_at         TIMESTAMPTZ,
    ADD COLUMN publicado_at        TIMESTAMPTZ,
    ADD COLUMN archivado_at        TIMESTAMPTZ,
    -- Bloqueo optimista: dos ediciones simultáneas no se sobrescriben en silencio.
    ADD COLUMN version             BIGINT NOT NULL DEFAULT 0;

-- Separación obligatoria: nadie revisa su propio contenido.
ALTER TABLE contenido ADD CONSTRAINT ck_contenido_autor_revisor
    CHECK (autor_id IS NULL OR revisor_id IS NULL OR autor_id <> revisor_id);

CREATE INDEX ix_contenido_autor ON contenido (autor_id);
CREATE INDEX ix_contenido_revisor ON contenido (revisor_id);
CREATE INDEX ix_contenido_updated_at ON contenido (updated_at);

UPDATE contenido SET aprobado_at = created_at, publicado_at = created_at WHERE estado = 'PUBLICADO';

-- Historial de revisiones: cada decisión queda registrada en lugar de sobrescribir un campo.
CREATE TABLE contenido_revision (
    id            UUID        PRIMARY KEY,
    contenido_id  UUID        NOT NULL REFERENCES contenido (id),
    revisor_id    UUID        REFERENCES usuarios (id),
    decision      VARCHAR(30) NOT NULL,
    comentario    TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_contenido_revision_decision CHECK (decision IN ('APROBADO', 'CAMBIOS_SOLICITADOS'))
);

CREATE INDEX ix_contenido_revision_contenido ON contenido_revision (contenido_id, created_at DESC);

-- El contenido publicado antes de este flujo conserva una revisión aprobada.
INSERT INTO contenido_revision (id, contenido_id, revisor_id, decision, comentario, created_at)
SELECT gen_random_uuid(), id, NULL, 'APROBADO',
       'Contenido publicado antes del flujo editorial (migración V3).', created_at
FROM contenido
WHERE estado = 'PUBLICADO';

-- ---------------------------------------------------------------------------------------------------
-- Bitácora de auditoría (sólo inserción)
-- ---------------------------------------------------------------------------------------------------
CREATE TABLE auditoria_evento (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ocurrido_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actor_id      UUID         REFERENCES usuarios (id),
    actor_correo  VARCHAR(254),
    actor_rol     VARCHAR(30),
    accion        VARCHAR(60)  NOT NULL,
    recurso_tipo  VARCHAR(30),
    recurso_id    VARCHAR(64),
    resumen       TEXT         NOT NULL,
    motivo        TEXT,
    -- JSON con las diferencias {campo: {antes, despues}}; nunca contraseñas, cookies ni tokens.
    cambios       TEXT
);

CREATE INDEX ix_auditoria_ocurrido ON auditoria_evento (ocurrido_at DESC);
CREATE INDEX ix_auditoria_actor ON auditoria_evento (actor_id, ocurrido_at DESC);
CREATE INDEX ix_auditoria_recurso ON auditoria_evento (recurso_tipo, recurso_id);
CREATE INDEX ix_auditoria_accion ON auditoria_evento (accion);

CREATE FUNCTION auditoria_evento_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La bitácora de auditoría no admite modificaciones ni eliminaciones';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_auditoria_evento_inmutable
    BEFORE UPDATE OR DELETE ON auditoria_evento
    FOR EACH ROW EXECUTE FUNCTION auditoria_evento_inmutable();
