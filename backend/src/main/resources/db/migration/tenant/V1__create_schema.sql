-- Schema de negocio, aplicado em cada banco de tenant (uma prefeitura).

CREATE TABLE usuarios (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    senha_hash      VARCHAR(255) NOT NULL,
    papel           VARCHAR(30) NOT NULL, -- ACS, REGULADOR, ADMIN
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE unidades_saude (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    endereco        VARCHAR(255),
    latitude        DOUBLE,
    longitude       DOUBLE,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE procedimentos (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    tipo            VARCHAR(20) NOT NULL,  -- CONSULTA, EXAME, CIRURGIA
    especialidade   VARCHAR(100),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE pacientes (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome                VARCHAR(150) NOT NULL,
    cpf                 VARCHAR(11) UNIQUE,
    cns                 VARCHAR(15) UNIQUE,
    data_nascimento     DATE,
    telefone            VARCHAR(20),
    email               VARCHAR(150),
    acs_responsavel_id  BIGINT,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_paciente_acs FOREIGN KEY (acs_responsavel_id) REFERENCES usuarios(id),
    CONSTRAINT chk_paciente_documento CHECK (cpf IS NOT NULL OR cns IS NOT NULL)
);

CREATE TABLE protocolos (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_protocolo     VARCHAR(30) NOT NULL UNIQUE,
    paciente_id          BIGINT NOT NULL,
    procedimento_id      BIGINT NOT NULL,
    unidade_saude_id     BIGINT,
    categoria_prioridade VARCHAR(20) NOT NULL DEFAULT 'NORMAL', -- URGENCIA, JUDICIAL, ESPECIAL, LEGAL, NORMAL
    status               VARCHAR(20) NOT NULL DEFAULT 'AGUARDANDO', -- AGUARDANDO, AGENDADO, EM_ANDAMENTO, CONCLUIDO, CANCELADO
    processo_judicial    VARCHAR(60),
    data_solicitacao     DATE NOT NULL,
    data_inclusao        DATE NOT NULL DEFAULT (CURRENT_DATE),
    data_prevista        DATE,
    criado_em            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_protocolo_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id),
    CONSTRAINT fk_protocolo_procedimento FOREIGN KEY (procedimento_id) REFERENCES procedimentos(id),
    CONSTRAINT fk_protocolo_unidade FOREIGN KEY (unidade_saude_id) REFERENCES unidades_saude(id)
);

CREATE INDEX idx_protocolos_status ON protocolos(status);
CREATE INDEX idx_protocolos_categoria ON protocolos(categoria_prioridade);
CREATE INDEX idx_protocolos_paciente ON protocolos(paciente_id);

CREATE TABLE etapas_protocolo (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    protocolo_id    BIGINT NOT NULL,
    nome_etapa      VARCHAR(100) NOT NULL,
    ordem           INTEGER NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'AGUARDANDO', -- REALIZADO, EM_ANDAMENTO, AGUARDANDO
    data_realizacao DATE,
    CONSTRAINT fk_etapa_protocolo FOREIGN KEY (protocolo_id) REFERENCES protocolos(id) ON DELETE CASCADE
);

CREATE INDEX idx_etapas_protocolo_protocolo ON etapas_protocolo(protocolo_id);

CREATE TABLE historico_status (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    protocolo_id    BIGINT NOT NULL,
    status_anterior VARCHAR(20),
    status_novo     VARCHAR(20) NOT NULL,
    usuario_id      BIGINT,
    observacao      VARCHAR(500),
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historico_protocolo FOREIGN KEY (protocolo_id) REFERENCES protocolos(id) ON DELETE CASCADE,
    CONSTRAINT fk_historico_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_historico_status_protocolo ON historico_status(protocolo_id);
