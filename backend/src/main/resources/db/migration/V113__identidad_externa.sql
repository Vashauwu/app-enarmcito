-- Inicio de sesión con proveedores externos (Google). La identidad externa se guarda aparte de la cuenta local:
-- el proveedor sólo autentica; rol, tier y estado siguen siendo decisión de la plataforma.
-- Nunca se guardan contraseña, token de acceso, refresh token ni el código OAuth del proveedor.

CREATE TABLE usuario_identidad_externa (
    id               UUID         PRIMARY KEY,
    usuario_id       UUID         NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    proveedor        VARCHAR(20)  NOT NULL,
    -- Claim `sub` del proveedor: inmutable. El correo nunca identifica una identidad externa.
    subject          VARCHAR(255) NOT NULL,
    correo_verificado VARCHAR(254),
    email_verified   BOOLEAN      NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_login_at    TIMESTAMPTZ,
    CONSTRAINT ck_identidad_proveedor CHECK (proveedor IN ('GOOGLE')),
    CONSTRAINT uk_identidad_proveedor_subject UNIQUE (proveedor, subject),
    -- Una cuenta vincula como máximo una identidad por proveedor.
    CONSTRAINT uk_identidad_usuario_proveedor UNIQUE (usuario_id, proveedor)
);

-- Una cuenta creada con Google no tiene contraseña: queda ACTIVA con password_hash nulo. Esa regla ya no
-- puede expresarse en la tabla; la aplicación la garantiza (ver GoogleLoginService y desvinculación).
ALTER TABLE usuarios_sistema DROP CONSTRAINT ck_usuarios_sistema_password;
