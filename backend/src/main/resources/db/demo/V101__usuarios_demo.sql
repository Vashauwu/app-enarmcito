-- DATOS DEMO: una cuenta por tier para probar el control de acceso en local.
-- Contraseña de las tres cuentas: Demo1234 (sólo desarrollo; no usar fuera de local).
WITH nuevos AS (
    INSERT INTO usuarios (id, nombre, apellido, correo, created_at, updated_at) VALUES
        ('00000000-0000-4000-8000-000000000001', 'Demo', 'Básico', 'basico@demo.local', now(), now()),
        ('00000000-0000-4000-8000-000000000002', 'Demo', 'Intermedio', 'intermedio@demo.local', now(), now()),
        ('00000000-0000-4000-8000-000000000003', 'Demo', 'Premium', 'premium@demo.local', now(), now())
    RETURNING id, correo
)
INSERT INTO usuarios_sistema (usuario_id, password_hash, rol, tier_id, estado, created_at, updated_at)
SELECT n.id, d.password_hash, 'ALUMNO', t.id, 'ACTIVO', now(), now()
FROM nuevos n
JOIN (VALUES
        ('basico@demo.local', 'BASICO', '{bcrypt}$2a$10$B.7qLdq6KS17YgkR0kvSUeMd3u7nHnCAjoEgjOFrfyQ2TBSRCwtH2'),
        ('intermedio@demo.local', 'INTERMEDIO', '{bcrypt}$2a$10$O0HUn.q6.TJPxw4U7fJ.1uhNqY8SCVLEIzEihoQX7R24Plt29uAcm'),
        ('premium@demo.local', 'PREMIUM', '{bcrypt}$2a$10$rOZxkvcs5FpcLxmhHwYN4.E/.QZHHEPuf7Jx2l1r461tGi8cVI0qC')
     ) AS d (correo, tier_codigo, password_hash) ON d.correo = n.correo
JOIN tier t ON t.codigo = d.tier_codigo;
