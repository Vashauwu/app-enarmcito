-- DATOS DEMO: una cuenta por rol administrativo y contenido en distintos estados del flujo editorial.
-- Contraseña de todas las cuentas: Demo1234 (sólo desarrollo; no usar fuera de local).
WITH nuevos AS (
    INSERT INTO usuarios (id, nombre, apellido, correo, created_at, updated_at) VALUES
        ('00000000-0000-4000-8000-000000000010', 'Sofía', 'Superadmin', 'superadmin@demo.local', now(), now()),
        ('00000000-0000-4000-8000-000000000011', 'Pablo', 'Plataforma', 'plataforma@demo.local', now(), now()),
        ('00000000-0000-4000-8000-000000000012', 'Úrsula', 'Usuarios', 'usuarios@demo.local', now(), now()),
        ('00000000-0000-4000-8000-000000000013', 'Saúl', 'Soporte', 'soporte@demo.local', now(), now()),
        ('00000000-0000-4000-8000-000000000014', 'Elena', 'Editora', 'editor@demo.local', now(), now()),
        ('00000000-0000-4000-8000-000000000015', 'Ramón', 'Revisor', 'revisor@demo.local', now(), now()),
        ('00000000-0000-4000-8000-000000000016', 'Rita', 'Revisora', 'revisor2@demo.local', now(), now()),
        ('00000000-0000-4000-8000-000000000017', 'Andrés', 'Autor', 'autor@demo.local', now(), now())
    RETURNING id, correo
)
INSERT INTO usuarios_sistema (usuario_id, password_hash, rol, tier_id, estado, created_at, updated_at)
SELECT n.id, '{bcrypt}$2a$10$rOZxkvcs5FpcLxmhHwYN4.E/.QZHHEPuf7Jx2l1r461tGi8cVI0qC', d.rol,
       (SELECT id FROM tier WHERE codigo = 'PREMIUM'), 'ACTIVO', now(), now()
FROM nuevos n
JOIN (VALUES
        ('superadmin@demo.local', 'SUPER_ADMIN'),
        ('plataforma@demo.local', 'ADMIN_PLATAFORMA'),
        ('usuarios@demo.local', 'ADMIN_USUARIOS'),
        ('soporte@demo.local', 'SOPORTE'),
        ('editor@demo.local', 'EDITOR_CONTENIDO'),
        ('revisor@demo.local', 'REVISOR_MEDICO'),
        ('revisor2@demo.local', 'REVISOR_MEDICO'),
        ('autor@demo.local', 'AUTOR_CONTENIDO')
     ) AS d (correo, rol) ON d.correo = n.correo;

-- El borrador de asma pasa a revisión: lo escribió el autor demo y lo revisa el revisor demo.
UPDATE contenido SET
    titulo = 'Asma: tratamiento escalonado según GINA',
    objetivo_aprendizaje = 'Reconocer por qué no se recomienda el tratamiento sólo con SABA y cuál es el rescate preferido.',
    resumen = 'La estrategia GINA organiza el tratamiento del asma por pasos y recomienda que adolescentes y adultos no reciban tratamiento sólo con agonistas beta 2 de acción corta (SABA), por el riesgo de exacerbaciones graves. En la vía preferida, el rescate se hace con dosis bajas de corticosteroide inhalado-formoterol (ICS-formoterol).',
    puntos_clave = 'No se recomienda tratar a adolescentes ni adultos sólo con SABA, sin corticosteroide inhalado.
Vía preferida: ICS-formoterol a dosis baja como rescate según necesidad.
El tratamiento se ajusta por pasos según el control de síntomas y el riesgo de exacerbaciones.',
    fuente_titulo = 'Global Strategy for Asthma Management and Prevention, 2024 update.',
    estado = 'EN_REVISION',
    autor_id = '00000000-0000-4000-8000-000000000017',
    revisor_id = '00000000-0000-4000-8000-000000000015',
    enviado_revision_at = now()
WHERE slug = 'asma-tratamiento-escalonado';

-- Borrador incompleto del autor demo: se puede guardar, pero no enviar a revisión hasta completarlo.
INSERT INTO contenido (id, titulo, slug, area_medica, metodo_aprendizaje, tier_minimo_id, estado, autor_id)
VALUES (gen_random_uuid(), 'Anafilaxia: reconocimiento inicial', 'anafilaxia-reconocimiento-inicial', 'Urgencias',
        'LECTURA_GUIADA', (SELECT id FROM tier WHERE codigo = 'BASICO'), 'BORRADOR',
        '00000000-0000-4000-8000-000000000017');

-- Los artículos publicados de ejemplo quedan a cargo de la editora demo.
UPDATE contenido SET autor_id = '00000000-0000-4000-8000-000000000014'
WHERE estado = 'PUBLICADO' AND autor_id IS NULL;
