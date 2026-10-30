-- DATOS DEMO: artículos de ejemplo para desarrollo local.
-- Se cargan sólo cuando app.demo-data=true (APP_DEMO_DATA). Antes de publicar el sistema
-- deben pasar por el proceso editorial definido (redacción, revisión y aprobación).
-- Cada artículo cita una fuente real; puntos_clave usa un punto por línea.

INSERT INTO contenido (id, titulo, slug, area_medica, objetivo_aprendizaje, resumen, puntos_clave, aplicacion_clinica,
                       metodo_aprendizaje, tier_minimo_id, fuente_titulo, fuente_autores, fuente_url, fuente_doi,
                       fecha_publicacion_fuente, fecha_revision, tiempo_lectura_minutos, estado)
VALUES
(gen_random_uuid(),
 'Sepsis-3: definiciones de sepsis y choque séptico',
 'sepsis-3-definiciones',
 'Urgencias',
 'Reconocer las definiciones actuales de sepsis y choque séptico y el papel de SOFA y qSOFA.',
 'El tercer consenso internacional (Sepsis-3) redefinió la sepsis como una disfunción orgánica potencialmente mortal causada por una respuesta desregulada del huésped a la infección. La disfunción orgánica se identifica como un aumento agudo de 2 puntos o más en la escala SOFA. El choque séptico es un subgrupo de la sepsis con alteraciones circulatorias, celulares y metabólicas lo bastante profundas como para aumentar de forma sustancial la mortalidad. Los criterios de SIRS dejaron de utilizarse para definir sepsis.',
 'Sepsis = infección + disfunción orgánica (aumento agudo de SOFA ≥ 2 puntos).
Choque séptico: necesidad de vasopresores para mantener PAM ≥ 65 mmHg y lactato > 2 mmol/L pese a reanimación adecuada con volumen.
La mortalidad hospitalaria del choque séptico descrita en el consenso supera el 40 %.
qSOFA: frecuencia respiratoria ≥ 22/min, alteración del estado mental y PAS ≤ 100 mmHg.
qSOFA ≥ 2 fuera de la UCI identifica mayor riesgo de mala evolución, pero no es un criterio diagnóstico de sepsis.',
 'Ante sospecha de infección, buscar datos de disfunción orgánica y considerar qSOFA como alerta para evaluar con más detalle; el diagnóstico se apoya en la valoración de SOFA.',
 'LECTURA_GUIADA', (SELECT id FROM tier WHERE codigo = 'BASICO'),
 'The Third International Consensus Definitions for Sepsis and Septic Shock (Sepsis-3). JAMA. 2016;315(8):801-810.',
 'Singer M, Deutschman CS, Seymour CW, et al.',
 'https://doi.org/10.1001/jama.2016.0287', '10.1001/jama.2016.0287',
 DATE '2016-02-23', DATE '2026-10-29', 5, 'PUBLICADO'),

(gen_random_uuid(),
 'Criterios diagnósticos de diabetes y prediabetes',
 'diabetes-criterios-diagnosticos',
 'Endocrinología',
 'Aplicar los criterios de laboratorio para el diagnóstico de diabetes y prediabetes en adultos.',
 'Los Estándares de Atención de la Asociación Americana de Diabetes establecen que la diabetes puede diagnosticarse con hemoglobina glucosilada (A1C), glucosa plasmática en ayuno o glucosa a las 2 horas de una prueba oral de tolerancia con 75 g. En ausencia de hiperglucemia inequívoca, el diagnóstico requiere dos resultados alterados, en la misma muestra o en muestras distintas.',
 'A1C ≥ 6.5 % (48 mmol/mol) con método certificado por NGSP.
Glucosa plasmática en ayuno ≥ 126 mg/dL (7.0 mmol/L), con ayuno de al menos 8 horas.
Glucosa a las 2 horas ≥ 200 mg/dL (11.1 mmol/L) durante una curva de tolerancia oral con 75 g.
Glucosa al azar ≥ 200 mg/dL con síntomas clásicos de hiperglucemia o crisis hiperglucémica.
Prediabetes: A1C 5.7-6.4 %, glucosa en ayuno 100-125 mg/dL o glucosa a las 2 horas 140-199 mg/dL.',
 'La A1C puede no ser confiable cuando se altera el recambio eritrocitario (hemoglobinopatías, embarazo, hemodiálisis, pérdida sanguínea o transfusión reciente, tratamiento con eritropoyetina); en esos casos se usan criterios de glucosa plasmática.',
 'LECTURA_GUIADA', (SELECT id FROM tier WHERE codigo = 'BASICO'),
 '2. Diagnosis and Classification of Diabetes: Standards of Care in Diabetes—2025. Diabetes Care. 2025;48(Suppl 1):S27-S49.',
 'American Diabetes Association Professional Practice Committee',
 'https://doi.org/10.2337/dc25-S002', '10.2337/dc25-S002',
 NULL, DATE '2026-10-29', 5, 'PUBLICADO'),

(gen_random_uuid(),
 'Hipertensión arterial: medición y clasificación en adultos',
 'hipertension-clasificacion-adultos',
 'Cardiología',
 'Clasificar la presión arterial en adultos y reconocer los requisitos de una medición correcta.',
 'La guía ACC/AHA de 2017 clasifica la presión arterial de adultos en cuatro categorías y recomienda basar la clasificación en el promedio de dos o más lecturas obtenidas en dos o más ocasiones con técnica adecuada. Las mediciones fuera del consultorio ayudan a confirmar el diagnóstico y a detectar hipertensión de bata blanca o enmascarada.',
 'Normal: < 120 y < 80 mmHg.
Elevada: 120-129 y < 80 mmHg.
Hipertensión estadio 1: 130-139 o 80-89 mmHg.
Hipertensión estadio 2: ≥ 140 o ≥ 90 mmHg.
Técnica: reposo previo de 5 minutos, paciente sentado con la espalda apoyada, brazo a la altura del corazón y brazalete del tamaño adecuado.',
 'Antes de etiquetar a un paciente con hipertensión, confirmar con varias lecturas bien tomadas y, cuando sea posible, con monitoreo ambulatorio o domiciliario.',
 'LECTURA_GUIADA', (SELECT id FROM tier WHERE codigo = 'BASICO'),
 '2017 ACC/AHA/AAPA/ABC/ACPM/AGS/APhA/ASH/ASPC/NMA/PCNA Guideline for the Prevention, Detection, Evaluation, and Management of High Blood Pressure in Adults. Hypertension. 2018;71(6):e13-e115.',
 'Whelton PK, Carey RM, Aronow WS, et al.',
 'https://doi.org/10.1161/HYP.0000000000000065', '10.1161/HYP.0000000000000065',
 NULL, DATE '2026-10-29', 6, 'PUBLICADO'),

(gen_random_uuid(),
 'EPOC: confirmación espirométrica y evaluación inicial',
 'epoc-diagnostico-espirometrico',
 'Neumología',
 'Confirmar el diagnóstico de EPOC con espirometría y clasificar la gravedad de la obstrucción.',
 'El informe GOLD define la EPOC como una enfermedad pulmonar heterogénea con síntomas respiratorios crónicos y limitación persistente, a menudo progresiva, del flujo aéreo. La espirometría posbroncodilatador es necesaria para confirmar el diagnóstico. La evaluación inicial combina la gravedad de la obstrucción, la carga de síntomas y el antecedente de exacerbaciones.',
 'Diagnóstico: cociente FEV1/FVC posbroncodilatador < 0.7.
GOLD 1: FEV1 ≥ 80 % del predicho; GOLD 2: 50-79 %; GOLD 3: 30-49 %; GOLD 4: < 30 %.
Grupo E: 2 o más exacerbaciones moderadas, o 1 o más que requirió hospitalización, en el último año.
Sin exacerbaciones relevantes, el grupo A o B depende de los síntomas (mMRC 0-1 / CAT < 10 frente a mMRC ≥ 2 / CAT ≥ 10).',
 'Ante disnea, tos crónica o expectoración con exposición a tabaco u otros factores de riesgo, solicitar espirometría antes de etiquetar el cuadro como EPOC.',
 'LECTURA_GUIADA', (SELECT id FROM tier WHERE codigo = 'BASICO'),
 'Global Strategy for the Prevention, Diagnosis and Management of Chronic Obstructive Pulmonary Disease: 2025 Report.',
 'Global Initiative for Chronic Obstructive Lung Disease (GOLD)',
 'https://goldcopd.org/', NULL,
 NULL, DATE '2026-10-29', 5, 'PUBLICADO'),

(gen_random_uuid(),
 'Neumonía adquirida en la comunidad: evaluación inicial en adultos',
 'neumonia-comunidad-evaluacion-inicial',
 'Infectología',
 'Decidir el sitio de atención de la neumonía adquirida en la comunidad y reconocer los criterios de gravedad.',
 'La guía ATS/IDSA de 2019 recomienda combinar el juicio clínico con una regla de predicción validada, preferentemente el índice de gravedad de neumonía (PSI) sobre CURB-65, para decidir entre manejo ambulatorio u hospitalario. Para identificar neumonía grave que puede requerir terapia intensiva se utilizan los criterios ATS/IDSA de 2007.',
 'Neumonía grave: 1 criterio mayor o 3 o más menores.
Criterios mayores: choque séptico con necesidad de vasopresores o insuficiencia respiratoria que requiere ventilación mecánica.
Criterios menores: FR ≥ 30/min, PaO2/FiO2 ≤ 250, infiltrados multilobares, confusión, BUN ≥ 20 mg/dL, leucocitos < 4000/µL, plaquetas < 100 000/µL, temperatura < 36 °C e hipotensión que requiere reanimación hídrica intensiva.
En adultos ambulatorios sin comorbilidades, una opción es amoxicilina 1 g tres veces al día.
No se recomienda decidir el inicio de antibiótico con base en procalcitonina.',
 'Las reglas de predicción apoyan, pero no sustituyen, la valoración clínica; considerar también oxigenación, comorbilidades y contexto social.',
 'LECTURA_GUIADA', (SELECT id FROM tier WHERE codigo = 'INTERMEDIO'),
 'Diagnosis and Treatment of Adults with Community-acquired Pneumonia. An Official Clinical Practice Guideline of the American Thoracic Society and Infectious Diseases Society of America. Am J Respir Crit Care Med. 2019;200(7):e45-e67.',
 'Metlay JP, Waterer GW, Long AC, et al.',
 'https://doi.org/10.1164/rccm.201908-1581ST', '10.1164/rccm.201908-1581ST',
 NULL, DATE '2026-10-29', 7, 'PUBLICADO'),

(gen_random_uuid(),
 'Insuficiencia cardiaca: estadios y clasificación por fracción de eyección',
 'insuficiencia-cardiaca-estadios-fevi',
 'Cardiología',
 'Ubicar al paciente en el estadio de insuficiencia cardiaca correcto y clasificarlo por fracción de eyección.',
 'La guía AHA/ACC/HFSA de 2022 describe la insuficiencia cardiaca como un continuo con cuatro estadios, desde el riesgo hasta la enfermedad avanzada, y clasifica a los pacientes según la fracción de eyección del ventrículo izquierdo (FEVI). En la insuficiencia cardiaca con FEVI reducida, el tratamiento médico dirigido por guías se apoya en cuatro grupos de fármacos.',
 'Estadio A: en riesgo, sin cardiopatía estructural ni síntomas.
Estadio B (pre-IC): cardiopatía estructural, función anormal o péptidos natriuréticos elevados, sin síntomas.
Estadio C: síntomas actuales o previos de insuficiencia cardiaca. Estadio D: insuficiencia cardiaca avanzada.
FEVI reducida ≤ 40 %; levemente reducida 41-49 %; preservada ≥ 50 %; mejorada: previa ≤ 40 % y en seguimiento > 40 %.
FEVI reducida: ARNI/IECA/ARA II, betabloqueador con evidencia, antagonista mineralocorticoide e iSGLT2.',
 'Los péptidos natriuréticos son útiles para apoyar o descartar el diagnóstico cuando la causa de la disnea es incierta.',
 'LECTURA_GUIADA', (SELECT id FROM tier WHERE codigo = 'INTERMEDIO'),
 '2022 AHA/ACC/HFSA Guideline for the Management of Heart Failure. Circulation. 2022;145(18):e895-e1032.',
 'Heidenreich PA, Bozkurt B, Aguilar D, et al.',
 'https://doi.org/10.1161/CIR.0000000000001063', '10.1161/CIR.0000000000001063',
 NULL, DATE '2026-10-29', 7, 'PUBLICADO'),

(gen_random_uuid(),
 'Sepsis y choque séptico: reanimación inicial',
 'sepsis-reanimacion-inicial',
 'Medicina crítica',
 'Ordenar las primeras acciones de reanimación en sepsis e hipoperfusión inducida por sepsis.',
 'La Campaña Sobreviviendo a la Sepsis de 2021 actualizó las recomendaciones de reanimación inicial. Prioriza la administración temprana de antimicrobianos, la reanimación con cristaloides en hipoperfusión, una meta inicial de presión arterial media y el uso de norepinefrina como vasopresor de primera línea.',
 'Se recomienda no usar qSOFA como herramienta única de tamizaje frente a SIRS, NEWS o MEWS.
Hipoperfusión o choque séptico: al menos 30 mL/kg de cristaloide IV en las primeras 3 horas (recomendación débil).
Meta inicial de presión arterial media: 65 mmHg.
Choque séptico o alta probabilidad de sepsis: antimicrobianos de inmediato, idealmente en la primera hora.
Norepinefrina es el vasopresor de primera línea; añadir vasopresina si la dosis de norepinefrina está entre 0.25 y 0.5 µg/kg/min.',
 'En adultos con choque séptico y requerimiento sostenido de vasopresor, la guía sugiere hidrocortisona IV.',
 'LECTURA_GUIADA', (SELECT id FROM tier WHERE codigo = 'PREMIUM'),
 'Surviving Sepsis Campaign: International Guidelines for Management of Sepsis and Septic Shock 2021. Crit Care Med. 2021;49(11):e1063-e1143.',
 'Evans L, Rhodes A, Alhazzani W, et al.',
 'https://doi.org/10.1097/CCM.0000000000005337', '10.1097/CCM.0000000000005337',
 NULL, DATE '2026-10-29', 8, 'PUBLICADO'),

(gen_random_uuid(),
 'Fibrilación auricular: estadios y prevención de tromboembolia',
 'fibrilacion-auricular-estadios-anticoagulacion',
 'Cardiología',
 'Clasificar la fibrilación auricular por estadios y decidir cuándo está indicada la anticoagulación.',
 'La guía ACC/AHA/ACCP/HRS de 2023 propone clasificar la fibrilación auricular (FA) por estadios que reflejan su progresión e indica la anticoagulación con base en el riesgo anual estimado de tromboembolia.',
 'Estadio 1: en riesgo; estadio 2: pre-FA; estadio 3: FA (paroxística, persistente, persistente de larga duración o tras ablación exitosa); estadio 4: FA permanente.
Anticoagular si el riesgo anual de tromboembolia es ≥ 2 % (p. ej., CHA2DS2-VASc ≥ 2 en hombres o ≥ 3 en mujeres).
Se prefieren los anticoagulantes orales directos sobre warfarina, salvo estenosis mitral moderada o grave o válvula mecánica.
El ácido acetilsalicílico solo no se recomienda como alternativa para prevenir eventos vasculares cerebrales.',
 'La decisión de anticoagular se revalora periódicamente, considerando el riesgo de sangrado y las preferencias del paciente.',
 'LECTURA_GUIADA', (SELECT id FROM tier WHERE codigo = 'PREMIUM'),
 '2023 ACC/AHA/ACCP/HRS Guideline for the Diagnosis and Management of Atrial Fibrillation. Circulation. 2024;149(1):e1-e156.',
 'Joglar JA, Chung MK, Armbruster AL, et al.',
 'https://doi.org/10.1161/CIR.0000000000001193', '10.1161/CIR.0000000000001193',
 NULL, DATE '2026-10-29', 7, 'PUBLICADO'),

-- Borrador: no debe aparecer en búsquedas ni abrirse desde la API.
(gen_random_uuid(),
 'Asma: tratamiento escalonado (borrador)',
 'asma-tratamiento-escalonado',
 'Neumología',
 'Describir el uso de ICS-formoterol como tratamiento de rescate.',
 'Borrador pendiente de revisión editorial.',
 'GINA recomienda no tratar a adolescentes ni adultos sólo con agonistas beta 2 de acción corta.',
 NULL,
 'LECTURA_GUIADA', (SELECT id FROM tier WHERE codigo = 'BASICO'),
 'Global Strategy for Asthma Management and Prevention.',
 'Global Initiative for Asthma (GINA)',
 'https://ginasthma.org/', NULL,
 NULL, DATE '2026-10-29', 6, 'BORRADOR');
