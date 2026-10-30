-- Catálogo inicial de tiers. Nombres y beneficios pendientes de confirmar con el responsable funcional.
INSERT INTO tier (codigo, nombre, descripcion, orden_acceso, activo) VALUES
    ('BASICO', 'Básico', 'Artículos esenciales de medicina general y urgencias.', 10, TRUE),
    ('INTERMEDIO', 'Intermedio', 'Incluye Básico y temas de manejo de enfermedades crónicas.', 20, TRUE),
    ('PREMIUM', 'Premium', 'Acceso a todo el catálogo, incluidos temas de cuidados críticos y cardiología avanzada.', 30, TRUE);
