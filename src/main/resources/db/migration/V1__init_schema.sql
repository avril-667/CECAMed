-- ============================================================================
-- CECAMed - Sistema de Control y Gestión Médica
-- V1__init_schema.sql: Esquema inicial DDL completo con restricciones relacionales
-- ============================================================================

-- 1. Tabla: usuario
-- Manejo de usuarios del sistema (Administradores, Médicos, Recepcionistas)
CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,
    nombre_completo VARCHAR(150) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(30) NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'MEDICO', 'RECEPCIONISTA')),
    cedula_profesional VARCHAR(50),
    especialidad VARCHAR(100),
    email VARCHAR(120) UNIQUE,
    telefono VARCHAR(20),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_usuario_username ON usuario(username);
CREATE INDEX idx_usuario_rol ON usuario(rol);

-- 2. Tabla: horario_atencion
-- Configuración de horarios habituales de atención por médico
CREATE TABLE horario_atencion (
    id BIGSERIAL PRIMARY KEY,
    medico_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    dia_semana SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7), -- 1 = Lunes, 7 = Domingo
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    duracion_cita_minutos INTEGER NOT NULL DEFAULT 30 CHECK (duracion_cita_minutos > 0),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_horario_valido CHECK (hora_inicio < hora_fin)
);

CREATE INDEX idx_horario_medico_dia ON horario_atencion(medico_id, dia_semana);

-- 3. Tabla: bloqueo_agenda
-- Bloqueos de agenda (vacaciones, congresos, incapacidades, descansos extraordinarios)
CREATE TABLE bloqueo_agenda (
    id BIGSERIAL PRIMARY KEY,
    medico_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_hora_inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_hora_fin TIMESTAMP WITH TIME ZONE NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bloqueo_rango CHECK (fecha_hora_inicio < fecha_hora_fin)
);

CREATE INDEX idx_bloqueo_medico_rango ON bloqueo_agenda(medico_id, fecha_hora_inicio, fecha_hora_fin);

-- 4. Tabla: paciente
-- Datos demográficos y antecedentes del paciente
CREATE TABLE paciente (
    id BIGSERIAL PRIMARY KEY,
    expediente_numero VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL,
    primer_apellido VARCHAR(80) NOT NULL,
    segundo_apellido VARCHAR(80),
    fecha_nacimiento DATE NOT NULL,
    genero VARCHAR(20) NOT NULL CHECK (genero IN ('MASCULINO', 'FEMENINO', 'OTRO')),
    curp VARCHAR(25) UNIQUE,
    telefono VARCHAR(20) NOT NULL,
    email VARCHAR(120),
    direccion TEXT,
    alergias TEXT,
    antecedentes_patologicos TEXT,
    antecedentes_no_patologicos TEXT,
    antecedentes_heredofamiliares TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_paciente_nombre ON paciente(primer_apellido, segundo_apellido, nombre);
CREATE INDEX idx_paciente_telefono ON paciente(telefono);

-- 5. Tabla: cita
-- Citas médicas con control de estado y sincronización con Google Calendar
CREATE TABLE cita (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES paciente(id) ON DELETE RESTRICT,
    medico_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    fecha_hora_inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_hora_fin TIMESTAMP WITH TIME ZONE NOT NULL,
    motivo_consulta VARCHAR(255) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADA' 
        CHECK (estado IN ('PROGRAMADA', 'CONFIRMADA', 'EN_SALA', 'EN_CONSULTA', 'ATENDIDA', 'CANCELADA', 'NO_ASISTIO')),
    google_calendar_event_id VARCHAR(255),
    notas TEXT,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cita_rango CHECK (fecha_hora_inicio < fecha_hora_fin)
);

CREATE INDEX idx_cita_medico_fechas ON cita(medico_id, fecha_hora_inicio, fecha_hora_fin);
CREATE INDEX idx_cita_paciente ON cita(paciente_id);
CREATE INDEX idx_cita_estado ON cita(estado);

-- 6. Tabla: expediente_clinico
-- Consultas médicas, somatometría (con cálculo de IMC), signos vitales y diagnóstico
CREATE TABLE expediente_clinico (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES paciente(id) ON DELETE RESTRICT,
    medico_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    cita_id BIGINT UNIQUE REFERENCES cita(id) ON DELETE SET NULL,
    fecha_consulta TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo_consulta TEXT NOT NULL,
    subjetivo TEXT,
    objetivo TEXT,
    -- Somatometría y Signos Vitales
    peso_kg NUMERIC(5,2) CHECK (peso_kg > 0 AND peso_kg < 500),
    talla_cm NUMERIC(5,2) CHECK (talla_cm > 0 AND talla_cm < 300),
    imc NUMERIC(4,2) GENERATED ALWAYS AS (
        CASE 
            WHEN peso_kg IS NOT NULL AND talla_cm IS NOT NULL AND talla_cm > 0 
            THEN ROUND((peso_kg / ((talla_cm / 100.0) * (talla_cm / 100.0))), 2)
            ELSE NULL 
        END
    ) STORED,
    presion_arterial VARCHAR(15), -- Formato ej: 120/80
    frecuencia_cardiaca INTEGER CHECK (frecuencia_cardiaca > 0),
    frecuencia_respiratoria INTEGER CHECK (frecuencia_respiratoria > 0),
    temperatura_c NUMERIC(4,2) CHECK (temperatura_c > 20 AND temperatura_c < 50),
    saturacion_oxigeno NUMERIC(4,1) CHECK (saturacion_oxigeno >= 0 AND saturacion_oxigeno <= 100),
    glucosa_mg_dl NUMERIC(5,2),
    -- Diagnóstico y Prescripción
    diagnostico TEXT NOT NULL,
    plan_tratamiento TEXT NOT NULL,
    receta_medica TEXT,
    notas_adicionales TEXT,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_expediente_paciente ON expediente_clinico(paciente_id);
CREATE INDEX idx_expediente_medico ON expediente_clinico(medico_id);
CREATE INDEX idx_expediente_fecha ON expediente_clinico(fecha_consulta);

-- 7. Tabla: atencion_sala
-- Monitor de check-in y sala de espera en tiempo real
CREATE TABLE atencion_sala (
    id BIGSERIAL PRIMARY KEY,
    cita_id BIGINT NOT NULL UNIQUE REFERENCES cita(id) ON DELETE CASCADE,
    paciente_id BIGINT NOT NULL REFERENCES paciente(id) ON DELETE RESTRICT,
    turno_codigo VARCHAR(20) NOT NULL,
    estado_sala VARCHAR(30) NOT NULL DEFAULT 'ESPERANDO'
        CHECK (estado_sala IN ('ESPERANDO', 'LLAMADO', 'EN_CONSULTA', 'FINALIZADO', 'CANCELADO')),
    hora_llegada TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    hora_llamado TIMESTAMP WITH TIME ZONE,
    hora_inicio_atencion TIMESTAMP WITH TIME ZONE,
    hora_fin_atencion TIMESTAMP WITH TIME ZONE,
    observaciones VARCHAR(255)
);

CREATE INDEX idx_atencion_sala_estado ON atencion_sala(estado_sala);
CREATE INDEX idx_atencion_sala_llegada ON atencion_sala(hora_llegada);

-- 8. Tabla: registro_auditoria
-- Trazabilidad de seguridad de acciones críticas en el sistema
CREATE TABLE registro_auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    tabla_afectada VARCHAR(60) NOT NULL,
    registro_id BIGINT,
    accion VARCHAR(20) NOT NULL CHECK (accion IN ('INSERT', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'ACCESO_DENEGADO')),
    datos_anteriores JSONB,
    datos_nuevos JSONB,
    direccion_ip VARCHAR(45),
    fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auditoria_usuario ON registro_auditoria(usuario_id);
CREATE INDEX idx_auditoria_tabla ON registro_auditoria(tabla_afectada, registro_id);
CREATE INDEX idx_auditoria_fecha ON registro_auditoria(fecha_registro);
