-- Agendamento de horario real (evolucao do "AGENDADO" que ate aqui era so um
-- status + uma data solta, sem hora nem controle de vagas por horario). Um
-- horario_agenda representa uma vaga concreta: unidade + especialidade + dia
-- + hora, com uma capacidade (quantas pessoas podem ser atendidas naquele
-- horario). O regulador cria os horarios (um a um ou em lote recorrente) e,
-- na tela do protocolo, escolhe um horario com vaga livre para o paciente.
CREATE TABLE horarios_agenda (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    unidade_saude_id  BIGINT NOT NULL,
    especialidade     VARCHAR(100) NOT NULL,
    data              DATE NOT NULL,
    hora_inicio       TIME NOT NULL,
    capacidade_total  INT NOT NULL,
    criado_em         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_horario_agenda_unidade FOREIGN KEY (unidade_saude_id) REFERENCES unidades_saude(id),
    -- Nao faz sentido cadastrar duas vezes o mesmo horario para a mesma
    -- unidade/especialidade/dia -- se precisar de mais vagas ali, aumenta a
    -- capacidade_total do horario existente.
    CONSTRAINT uk_horario_agenda UNIQUE (unidade_saude_id, especialidade, data, hora_inicio)
);

-- Vinculo do protocolo ao horario especifico que ele ocupa (quando agendado
-- por um horario real, via /api/fila/{id}/agendar-horario). NULL quando o
-- protocolo nunca foi agendado por essa via (ex.: fluxo antigo de
-- "Distribuir Vagas", que so seta dataPrevista sem horario).
ALTER TABLE protocolos ADD COLUMN horario_agenda_id BIGINT NULL;
ALTER TABLE protocolos ADD CONSTRAINT fk_protocolo_horario_agenda
    FOREIGN KEY (horario_agenda_id) REFERENCES horarios_agenda(id);
