-- DATOS DEMO del módulo de guías y contenido enriquecido. Contraseña de la cuenta nueva: Demo1234.

-- ---------------------------------------------------------------------------------------------------
-- Gestor multimedia
-- ---------------------------------------------------------------------------------------------------
INSERT INTO usuarios (id, nombre, apellido, correo, created_at, updated_at)
VALUES ('00000000-0000-4000-8000-000000000018', 'Mónica', 'Medios', 'medios@demo.local', now(), now());
INSERT INTO usuarios_sistema (usuario_id, password_hash, rol, tier_id, estado, created_at, updated_at)
VALUES ('00000000-0000-4000-8000-000000000018',
        '{bcrypt}$2a$10$rOZxkvcs5FpcLxmhHwYN4.E/.QZHHEPuf7Jx2l1r461tGi8cVI0qC', 'GESTOR_MULTIMEDIA',
        (SELECT id FROM tier WHERE codigo = 'BASICO'), 'ACTIVO', now(), now());

-- ---------------------------------------------------------------------------------------------------
-- GPC publicada (Premium). La versión pública se genera al arrancar la aplicación.
-- Recomendaciones con la fuerza y calidad de evidencia que declara la guía.
-- ---------------------------------------------------------------------------------------------------
INSERT INTO contenido (id, tipo, politica_acceso, tier_minimo_id, titulo, slug, area_medica, objetivo_aprendizaje,
                       resumen, metodo_aprendizaje, fuente_titulo, fuente_autores, fuente_url, fuente_doi,
                       fecha_publicacion_fuente, fecha_revision, tiempo_lectura_minutos, revision_clinica_requerida,
                       institucion_emisora, fuente_edicion, poblacion_objetivo, contexto_atencion, alcance_clinico,
                       vigente_hasta, proxima_revision, recomendaciones, estado, autor_id, revisor_id, editor_id,
                       aprobado_at, publicado_at, publicacion_activa)
VALUES ('00000000-0000-4000-8000-0000000000a1', 'GUIA_PRACTICA_CLINICA', 'POR_TIER',
        (SELECT id FROM tier WHERE codigo = 'PREMIUM'),
        'Sepsis y choque séptico en adultos: reanimación inicial (SSC 2021)', 'gpc-sepsis-choque-septico-adultos',
        'Medicina crítica',
        'Aplicar las recomendaciones iniciales de tamizaje, antimicrobianos, líquidos y vasopresores en adultos con sepsis o choque séptico.',
        'Resumen estructurado de las recomendaciones de reanimación inicial de la Campaña Sobreviviendo a la Sepsis 2021 para adultos. Prioriza el reconocimiento temprano, la administración inmediata de antimicrobianos cuando hay choque séptico o alta probabilidad de sepsis, la reanimación con cristaloides en la hipoperfusión y el uso de norepinefrina con una meta inicial de presión arterial media de 65 mmHg.',
        'LECTURA_GUIADA',
        'Surviving Sepsis Campaign: International Guidelines for Management of Sepsis and Septic Shock 2021. Crit Care Med. 2021;49(11):e1063-e1143.',
        'Evans L, Rhodes A, Alhazzani W, et al.', 'https://doi.org/10.1097/CCM.0000000000005337',
        '10.1097/CCM.0000000000005337', DATE '2021-11-01', DATE '2026-10-29', 9, TRUE,
        'Society of Critical Care Medicine y European Society of Intensive Care Medicine', '2021',
        'Adultos con sepsis o choque séptico.',
        'Urgencias, hospitalización y unidades de cuidados intensivos.',
        'Reconocimiento y reanimación inicial: tamizaje, antimicrobianos, líquidos intravenosos y vasopresores. No aborda población pediátrica.',
        DATE '2027-10-29', DATE '2027-04-29',
        '[{"texto":"En adultos con posible choque séptico o alta probabilidad de sepsis, administrar antimicrobianos de inmediato, idealmente dentro de la primera hora del reconocimiento.","fuerza":"Fuerte","nivelEvidencia":"Baja (choque séptico); muy baja (sepsis sin choque)","referencia":"SSC 2021"},{"texto":"No usar qSOFA como herramienta única de tamizaje de sepsis o choque séptico, en comparación con SIRS, NEWS o MEWS.","fuerza":"Fuerte","nivelEvidencia":"Moderada","referencia":"SSC 2021"},{"texto":"En hipoperfusión inducida por sepsis o choque séptico, administrar al menos 30 mL/kg de cristaloide intravenoso en las primeras 3 horas de la reanimación.","fuerza":"Débil","nivelEvidencia":"Baja","referencia":"SSC 2021"},{"texto":"Usar cristaloides balanceados en lugar de solución salina para la reanimación.","fuerza":"Débil","nivelEvidencia":"Baja","referencia":"SSC 2021"},{"texto":"En choque séptico con vasopresores, usar una meta inicial de presión arterial media de 65 mmHg sobre metas más altas.","fuerza":"Fuerte","nivelEvidencia":"Moderada","referencia":"SSC 2021"},{"texto":"Usar norepinefrina como vasopresor de primera línea frente a otros vasopresores.","fuerza":"Fuerte","nivelEvidencia":"Alta (frente a dopamina)","referencia":"SSC 2021"}]',
        'PUBLICADO', '00000000-0000-4000-8000-000000000014', '00000000-0000-4000-8000-000000000015',
        '00000000-0000-4000-8000-000000000014', now(), now(), TRUE);

INSERT INTO contenido_bloque (id, contenido_id, posicion, tipo, datos, autor_id) VALUES
('00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000a1', 0, 'CALLOUT',
 '{"variante":"ALERTA","titulo":"Uso de esta guía","texto":"Resumen educativo de una guía internacional. No sustituye el juicio clínico ni los protocolos de cada institución; consulta siempre la fuente original."}',
 '00000000-0000-4000-8000-000000000014'),
('00000000-0000-4000-8000-0000000000b2', '00000000-0000-4000-8000-0000000000a1', 1, 'ALGORITMO',
 '{"titulo":"Primeras acciones ante sospecha de sepsis en adultos","version":"1.0","fuente":"Surviving Sepsis Campaign 2021","descripcionTextual":"Ante sospecha de choque séptico o alta probabilidad de sepsis se administran antimicrobianos de inmediato, idealmente en la primera hora. Si hay hipoperfusión inducida por sepsis o choque séptico, se administran al menos 30 mL/kg de cristaloide intravenoso en las primeras 3 horas. Si la hipotensión persiste tras la reanimación hídrica, se inicia norepinefrina como vasopresor de primera línea con una meta inicial de presión arterial media de 65 mmHg. En todos los casos se reevalúa de forma continua.","nodos":[{"id":"inicio","tipo":"INICIO","texto":"Sospecha de choque séptico o alta probabilidad de sepsis"},{"id":"atb","tipo":"ACCION","texto":"Antimicrobianos de inmediato, idealmente en la primera hora"},{"id":"hipoperfusion","tipo":"DECISION","texto":"¿Hipoperfusión inducida por sepsis o choque séptico?"},{"id":"liquidos","tipo":"ACCION","texto":"Cristaloide IV ≥ 30 mL/kg en las primeras 3 horas"},{"id":"hipotension","tipo":"DECISION","texto":"¿Persiste la hipotensión tras la reanimación hídrica?"},{"id":"norepinefrina","tipo":"ACCION","texto":"Norepinefrina de primera línea; meta inicial de PAM 65 mmHg"},{"id":"reevaluar","tipo":"FIN","texto":"Reevaluar de forma continua"}],"conexiones":[{"desde":"inicio","hacia":"atb","etiqueta":null},{"desde":"atb","hacia":"hipoperfusion","etiqueta":null},{"desde":"hipoperfusion","hacia":"liquidos","etiqueta":"Sí"},{"desde":"hipoperfusion","hacia":"reevaluar","etiqueta":"No"},{"desde":"liquidos","hacia":"hipotension","etiqueta":null},{"desde":"hipotension","hacia":"norepinefrina","etiqueta":"Sí"},{"desde":"hipotension","hacia":"reevaluar","etiqueta":"No"},{"desde":"norepinefrina","hacia":"reevaluar","etiqueta":null}]}',
 '00000000-0000-4000-8000-000000000014'),
('00000000-0000-4000-8000-0000000000b3', '00000000-0000-4000-8000-0000000000a1', 2, 'REFERENCIAS',
 '{"referencias":[{"titulo":"Surviving Sepsis Campaign: International Guidelines for Management of Sepsis and Septic Shock 2021","autores":"Evans L, Rhodes A, Alhazzani W, et al.","emisor":"Crit Care Med","url":"https://doi.org/10.1097/CCM.0000000000005337","doi":"10.1097/CCM.0000000000005337","fecha":"2021","etiquetaEvidencia":"Guía de práctica clínica"},{"titulo":"The Third International Consensus Definitions for Sepsis and Septic Shock (Sepsis-3)","autores":"Singer M, Deutschman CS, Seymour CW, et al.","emisor":"JAMA","url":"https://doi.org/10.1001/jama.2016.0287","doi":"10.1001/jama.2016.0287","fecha":"2016","etiquetaEvidencia":"Consenso internacional"}]}',
 '00000000-0000-4000-8000-000000000014');

-- La aprobación clínica de la versión 1, hecha por el revisor demo (distinto del autor).
INSERT INTO contenido_revision (id, contenido_id, revisor_id, decision, comentario, etapa, version_contenido, created_at)
VALUES (gen_random_uuid(), '00000000-0000-4000-8000-0000000000a1', '00000000-0000-4000-8000-000000000015',
        'APROBADO', 'Recomendaciones cotejadas contra la fuente original.', 'CLINICA', 1, now());

-- ---------------------------------------------------------------------------------------------------
-- Recurso de estudio publicado (gratuito con registro), de elaboración propia del equipo demo.
-- ---------------------------------------------------------------------------------------------------
INSERT INTO contenido (id, tipo, politica_acceso, titulo, slug, area_medica, resumen, metodo_aprendizaje,
                       fuente_titulo, fuente_autores, fuente_url, fuente_doi, fecha_revision, tiempo_lectura_minutos,
                       revision_clinica_requerida, procedencia, titular_derechos, licencia_uso,
                       autorizacion_publicacion, autorizacion_referencia, estado, autor_id, editor_id, publicado_at,
                       publicacion_activa)
VALUES ('00000000-0000-4000-8000-0000000000a2', 'RECURSO_ESTUDIO', 'REGISTRADO_GRATUITO',
        'Ficha de repaso: criterios qSOFA y definición de choque séptico', 'ficha-repaso-qsofa-choque-septico',
        'Urgencias',
        'Ficha breve para repasar los criterios de qSOFA y la definición operativa de choque séptico del consenso Sepsis-3.',
        'LECTURA_GUIADA',
        'The Third International Consensus Definitions for Sepsis and Septic Shock (Sepsis-3). JAMA. 2016;315(8):801-810.',
        'Singer M, Deutschman CS, Seymour CW, et al.', 'https://doi.org/10.1001/jama.2016.0287',
        '10.1001/jama.2016.0287', DATE '2026-10-29', 3, FALSE,
        'Elaborada por el equipo editorial demo a partir del consenso Sepsis-3.', 'Plataforma (contenido propio)',
        'Uso educativo dentro de la plataforma', TRUE, 'Material de elaboración propia; no requiere autorización externa.',
        'PUBLICADO', '00000000-0000-4000-8000-000000000014', '00000000-0000-4000-8000-000000000014', now(), TRUE);

INSERT INTO contenido_bloque (id, contenido_id, posicion, tipo, datos, autor_id) VALUES
('00000000-0000-4000-8000-0000000000c1', '00000000-0000-4000-8000-0000000000a2', 0, 'TABLA',
 '{"titulo":"Criterios de qSOFA (1 punto cada uno)","encabezados":["Criterio","Umbral"],"filas":[["Frecuencia respiratoria","≥ 22/min"],["Estado mental","Alterado"],["Presión arterial sistólica","≤ 100 mmHg"]],"fuente":"Sepsis-3 (JAMA 2016)","nota":"qSOFA ≥ 2 fuera de la UCI identifica mayor riesgo de mala evolución; no es un criterio diagnóstico de sepsis."}',
 '00000000-0000-4000-8000-000000000014'),
('00000000-0000-4000-8000-0000000000c2', '00000000-0000-4000-8000-0000000000a2', 1, 'CALLOUT',
 '{"variante":"PUNTO_CLAVE","titulo":"Choque séptico","texto":"Vasopresores para mantener PAM ≥ 65 mmHg y lactato > 2 mmol/L a pesar de una reanimación adecuada con volumen."}',
 '00000000-0000-4000-8000-000000000014');

-- ---------------------------------------------------------------------------------------------------
-- Cirugía General ENARM (PDF recibido el 29/10/2026). Transcripción fiel por secciones.
-- BORRADORES NO PUBLICABLES: falta la autorización escrita de uso y la revisión clínica.
-- ---------------------------------------------------------------------------------------------------
INSERT INTO contenido (id, tipo, politica_acceso, titulo, slug, area_medica, resumen, metodo_aprendizaje,
                       fuente_titulo, fecha_revision, tiempo_lectura_minutos, revision_clinica_requerida, procedencia,
                       titular_derechos, licencia_uso, autorizacion_publicacion, autorizacion_referencia, estado,
                       autor_id, revisor_id)
VALUES
('00000000-0000-4000-8000-0000000000d1', 'RECURSO_ESTUDIO', 'REGISTRADO_GRATUITO',
 'Cirugía general ENARM: heridas quirúrgicas e infección de sitio quirúrgico', 'cirugia-enarm-heridas-isq',
 'Cirugía general',
 'Clasificación de la herida quirúrgica según el grado de contaminación, riesgo de infección de sitio quirúrgico y medidas de prevención.',
 'LECTURA_GUIADA', 'Cirugía General ENARM (documento PDF de estudio).', NULL, 6, TRUE,
 'Documento «Cirugía General ENARM» (PDF, 37 páginas) recibido por el equipo del proyecto el 29/10/2026. Sección: Heridas quirúrgicas e infección de sitio quirúrgico.',
 'Pendiente de confirmar', 'Pendiente de confirmar: uso interno hasta contar con autorización escrita', FALSE, NULL,
 'EN_EDICION', '00000000-0000-4000-8000-000000000017', '00000000-0000-4000-8000-000000000015'),
('00000000-0000-4000-8000-0000000000d2', 'RECURSO_ESTUDIO', 'REGISTRADO_GRATUITO',
 'Cirugía general ENARM: abdomen agudo', 'cirugia-enarm-abdomen-agudo', 'Cirugía general',
 'Definición de abdomen agudo y causas principales según la localización del dolor.',
 'LECTURA_GUIADA', 'Cirugía General ENARM (documento PDF de estudio).', NULL, 3, TRUE,
 'Documento «Cirugía General ENARM» (PDF, 37 páginas) recibido por el equipo del proyecto el 29/10/2026. Sección: Abdomen agudo.',
 'Pendiente de confirmar', 'Pendiente de confirmar: uso interno hasta contar con autorización escrita', FALSE, NULL,
 'EN_EDICION', '00000000-0000-4000-8000-000000000017', '00000000-0000-4000-8000-000000000015'),
('00000000-0000-4000-8000-0000000000d3', 'RECURSO_ESTUDIO', 'REGISTRADO_GRATUITO',
 'Cirugía general ENARM: apendicitis aguda', 'cirugia-enarm-apendicitis-aguda', 'Cirugía general',
 'Definición, epidemiología, etiología, cuadro clínico, diagnóstico (escala de Alvarado e imagen) y tratamiento de la apendicitis aguda.',
 'LECTURA_GUIADA', 'Cirugía General ENARM (documento PDF de estudio).', NULL, 12, TRUE,
 'Documento «Cirugía General ENARM» (PDF, 37 páginas) recibido por el equipo del proyecto el 29/10/2026. Sección: Apendicitis aguda.',
 'Pendiente de confirmar', 'Pendiente de confirmar: uso interno hasta contar con autorización escrita', FALSE, NULL,
 'EN_EDICION', '00000000-0000-4000-8000-000000000017', '00000000-0000-4000-8000-000000000015');

-- Heridas quirúrgicas e ISQ
INSERT INTO contenido_bloque (id, contenido_id, posicion, tipo, datos, autor_id) VALUES
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d1', 0, 'TEXTO_RICO',
 '{"html":"<p><strong>Definición:</strong> clasificación de la herida según el <strong>grado de contaminación bacteriana</strong> al momento de la cirugía; predice el riesgo de <strong>infección de sitio quirúrgico (ISQ)</strong> y define la profilaxis.</p>"}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d1', 1, 'TABLA',
 '{"titulo":"Clasificación y riesgo de ISQ","encabezados":["Clase","Definición","Ejemplo","ISQ"],"filas":[["I · Limpia","No infectada, sin apertura de mucosas; programada, cierre primario","Hernioplastia, cirugía de mama","1–2%"],["II · Limpia-contaminada","Apertura controlada de tubo digestivo, respiratorio o genitourinario","Colecistectomía","<5–10%"],["III · Contaminada","Trauma reciente, falla de técnica estéril, derrame de contenido gastrointestinal","Derrame intestinal, inflamación aguda no purulenta","8–15%"],["IV · Sucia","Trauma antiguo con tejido desvitalizado, víscera perforada, pus","Apéndice perforado, Hinchey III–IV, pie diabético","30–40%"]],"fuente":"Cirugía General ENARM (PDF)","nota":null}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d1', 2, 'TABLA',
 '{"titulo":"Prevención (GPC)","encabezados":["Medida","Recomendación"],"filas":[["Profilaxis antibiótica","Cefazolina 2 g IV 30–60 min antes de la incisión; 3 g si peso >120 kg; redosificar si cirugía >4 h o sangrado >1,500 mL"],["Alergia a betalactámicos","Clindamicina o vancomicina ± aminoglucósido/fluoroquinolona"],["Glucosa perioperatoria","<200 mg/dL en las primeras 18–24 h; HbA1c <7% en cirugía programada"],["Temperatura","Normotermia: ≥35.5 °C"],["Tabaquismo","Suspender 30 días antes"],["Tricotomía","Solo si es necesaria, con rasuradora eléctrica, inmediatamente antes"]],"fuente":"Cirugía General ENARM (PDF)","nota":null}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d1', 3, 'CALLOUT',
 '{"variante":"PUNTO_CLAVE","titulo":"Punto clave ENARM","texto":"Colecistectomía = limpia-contaminada; apéndice perforado = sucia (requiere tratamiento antibiótico, no solo profilaxis). Hernioplastia sin malla, en paciente sin riesgo, puede no requerir profilaxis."}',
 '00000000-0000-4000-8000-000000000017');

-- Abdomen agudo
INSERT INTO contenido_bloque (id, contenido_id, posicion, tipo, datos, autor_id) VALUES
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d2', 0, 'TEXTO_RICO',
 '{"html":"<p><strong>Definición:</strong> síndrome de <strong>dolor abdominal de inicio reciente (&lt;48 h a 6 días)</strong>, con repercusión general, que exige diagnóstico rápido por posibilidad de <strong>tratamiento quirúrgico urgente</strong>; puede acompañarse de rigidez, rebote y resistencia muscular involuntaria (peritonismo).</p>"}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d2', 1, 'TABLA',
 '{"titulo":"Localización del dolor","encabezados":["Cuadrante","Causas principales"],"filas":[["Superior derecho","Colecistitis, colangitis, hepatitis, absceso hepático, úlcera perforada, neumonía basal"],["Superior izquierdo","Pancreatitis, infarto/rotura esplénica, úlcera gástrica, IAM inferior"],["Periumbilical","Apendicitis temprana, oclusión intestinal, isquemia mesentérica, aneurisma aórtico"],["Inferior derecho","Apendicitis, embarazo ectópico, quiste ovárico, EPI, adenitis mesentérica, Meckel, litiasis"],["Inferior izquierdo","Diverticulitis, ectópico, torsión ovárica, EPI, vólvulo de sigmoides"]],"fuente":"Cirugía General ENARM (PDF)","nota":null}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d2', 2, 'CALLOUT',
 '{"variante":"PUNTO_CLAVE","titulo":"Punto clave ENARM","texto":"Causas de perforación intestinal: apendicitis complicada, úlcera péptica y diverticulitis. Causa clásica de abdomen agudo NO quirúrgico: cetoacidosis diabética (también pancreatitis, porfiria, saturnismo, IAM inferior)."}',
 '00000000-0000-4000-8000-000000000017');

-- Apendicitis aguda
INSERT INTO contenido_bloque (id, contenido_id, posicion, tipo, datos, autor_id) VALUES
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 0, 'TEXTO_RICO',
 '{"html":"<p><strong>Definición:</strong> inflamación aguda del apéndice cecal por <strong>obstrucción de su luz</strong>, con sobrecrecimiento bacteriano, isquemia de la pared y riesgo de perforación.</p><h3>Epidemiología</h3><ul><li><strong>Urgencia quirúrgica abdominal más frecuente</strong>; riesgo de por vida ~7%; pico <strong>10–30 años</strong>; ligero predominio masculino.</li><li><strong>Urgencia quirúrgica no obstétrica más frecuente en el embarazo</strong>.</li><li>En &lt;5 años la perforación al diagnóstico es muy frecuente por presentación atípica.</li></ul>"}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 1, 'TABLA',
 '{"titulo":"Etiología","encabezados":["Grupo","Causa de obstrucción"],"filas":[["Niños","Hiperplasia de folículos linfoides"],["Adultos","Fecalito (apendicolito)"],["Tumor","Tumor neuroendocrino (carcinoide): neoplasia apendicular más frecuente"]],"fuente":"Cirugía General ENARM (PDF)","nota":null}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 2, 'TABLA',
 '{"titulo":"Fases","encabezados":["Fase","Hallazgo"],"filas":[["I · Congestiva","Edema, hiperemia"],["II · Supurativa","Exudado fibrinopurulento"],["III · Gangrenosa","Necrosis de la pared"],["IV · Perforada","Peritonitis localizada o generalizada"]],"fuente":"Cirugía General ENARM (PDF)","nota":null}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 3, 'TEXTO_RICO',
 '{"html":"<h3>Cuadro clínico</h3><p><strong>Cronología de Murphy:</strong> dolor periumbilical → anorexia → náusea/vómito → <strong>migración a FID</strong> → fiebre (dolor antes que vómito). Cuadro clásico solo en ~50–65%.</p>"}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 4, 'TABLA',
 '{"titulo":"Signos","encabezados":["Signo","Descripción","Rendimiento aprox."],"filas":[["McBurney","Dolor en unión tercio externo–medio de línea ombligo–espina iliaca anterosuperior","S 50–94%, E 75–86%"],["Rovsing","Dolor en FID al comprimir FII","S 22–68%, E 58–96%"],["Blumberg","Dolor a la descompresión (rebote)","S 63%, E 69%"],["Psoas","Dolor a la extensión de cadera derecha (retrocecal)","S 16%, E 95%"],["Obturador","Dolor a la rotación interna de cadera flexionada (pélvico)","S 8%, E 94%"],["Dunphy / Lanz / Summer","Dolor con la tos / punto de Lanz / hiperestesia y defensa en FID","—"]],"fuente":"Cirugía General ENARM (PDF)","nota":null}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 5, 'TABLA',
 '{"titulo":"Grupos especiales","encabezados":["Grupo","Particularidad"],"filas":[["Lactantes y preescolares","Fiebre, vómito, irritabilidad y diarrea; diagnóstico tardío"],["Embarazo","Dolor en FID es el dato más frecuente (~57%); el apéndice asciende con el útero (3–4 cm por encima de McBurney en el 3er trimestre)"],["Adulto mayor","Síntomas atenuados, fiebre y leucocitosis ausentes; mayor perforación"]],"fuente":"Cirugía General ENARM (PDF)","nota":null}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 6, 'TABLA',
 '{"titulo":"Escala de Alvarado (MANTRELS) — elección inicial","encabezados":["Criterio","Puntos"],"filas":[["Dolor en FID","2"],["Leucocitosis >10,000","2"],["Migración del dolor","1"],["Anorexia","1"],["Náusea/vómito","1"],["Rebote","1"],["Temperatura ≥37.3 °C","1"],["Desviación a la izquierda (neutrofilia)","1"]],"fuente":"Cirugía General ENARM (PDF)","nota":null}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 7, 'TABLA',
 '{"titulo":"Interpretación del puntaje de Alvarado","encabezados":["Puntaje","Conducta"],"filas":[["≤4","Descarta (S ~99% como regla de exclusión)"],["5–6","Posible → estudio de imagen"],["7–8","Probable"],["9–10","Cirugía"]],"fuente":"Cirugía General ENARM (PDF)","nota":null}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 8, 'TABLA',
 '{"titulo":"Estudios","encabezados":["Estudio","Papel"],"filas":[["Biometría hemática, PCR, EGO","Leucocitosis con neutrofilia; PCR >55 mg/L sugiere perforación; EGO descarta IVU"],["PIE (prueba de embarazo)","Obligada en toda mujer en edad fértil"],["USG abdominal","Inicial en niños, embarazadas y mujeres jóvenes: apéndice no compresible >6 mm (sospechoso) y >9 mm (confirmatorio), pared >2 mm (S 76–86%, E 81–94%)"],["TC abdominal contrastada","Estándar de oro de imagen (S 94–98%, E 95–97%) en adultos con duda diagnóstica"],["RM sin gadolinio","Embarazada con USG no concluyente (S 91–96%)"],["Histopatología","Diagnóstico definitivo: infiltrado neutrofílico de la muscular propia"]],"fuente":"Cirugía General ENARM (PDF)","nota":null}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 9, 'TEXTO_RICO',
 '{"html":"<p><strong>Diagnóstico diferencial:</strong> embarazo ectópico, EPI, torsión ovárica, <strong>síndrome de Mittelschmerz</strong> (ovulación dolorosa a mitad del ciclo, dolor unilateral en fosa iliaca tipo calambre, cede en ≤48 h), adenitis mesentérica, divertículo de Meckel, litiasis ureteral.</p>"}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 10, 'TABLA',
 '{"titulo":"Tratamiento","encabezados":["Escenario","Manejo"],"filas":[["No complicada","Apendicectomía laparoscópica (de elección: menos ISQ y estancia). Profilaxis: cefazolina 2 g IV (o cefoxitina 2 g) + metronidazol 500 mg IV dosis única"],["Paciente inestable o sin recurso","Apendicectomía abierta"],["Complicada (perforada, gangrenosa)","Cirugía + antibiótico terapéutico: ceftriaxona 2 g IV c/24 h + metronidazol 500 mg IV c/8 h o piperacilina-tazobactam 4.5 g IV c/6 h; 4 días tras control del foco"],["Plastrón/absceso estable","Absceso ≥3 cm → drenaje percutáneo + antibiótico; apendicectomía de intervalo opcional; colonoscopia en >40 años"],["Tumor neuroendocrino","<2 cm en punta → apendicectomía; >2 cm, base o invasión del mesoapéndice → hemicolectomía derecha"],["Embarazo","GPC: laparoscopia <28 SDG, abierta >28 SDG"],["Niños","GPC: abierta <5 años, laparoscopia >5 años"]],"fuente":"Cirugía General ENARM (PDF)","nota":null}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 11, 'CALLOUT',
 '{"variante":"INFORMACION","titulo":"Discrepancias","texto":"SAGES y la WSES 2020 consideran la laparoscopia segura en cualquier trimestre y a cualquier edad pediátrica. El manejo no operatorio con antibiótico en apendicitis no complicada (ensayo CODA 2020) es alternativa internacional, con recurrencia de ~25–40%; no es el criterio de la GPC."}',
 '00000000-0000-4000-8000-000000000017'),
(gen_random_uuid(), '00000000-0000-4000-8000-0000000000d3', 12, 'CALLOUT',
 '{"variante":"PUNTO_CLAVE","titulo":"Punto clave ENARM","texto":"La perforación ocurre con mayor frecuencia entre las 24 y 72 h del inicio del dolor. Alvarado es la escala más usada; el primer estudio de imagen en niños y embarazadas es el USG y el estándar de oro de imagen es la TC. Apendicitis con tumor de 3 cm en la punta → hemicolectomía derecha."}',
 '00000000-0000-4000-8000-000000000017');
