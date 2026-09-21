CREATE TABLE tenants (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug            VARCHAR(60) NOT NULL UNIQUE,
    nome_municipio  VARCHAR(150) NOT NULL,
    db_host         VARCHAR(255) NOT NULL,
    db_port         INTEGER NOT NULL DEFAULT 3306,
    db_name         VARCHAR(100) NOT NULL,
    db_user         VARCHAR(100) NOT NULL,
    db_password     VARCHAR(255) NOT NULL,
    cor_primaria    VARCHAR(7),
    cor_secundaria  VARCHAR(7),
    logo_url        VARCHAR(500),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT = 'Cadastro central de prefeituras (tenants). Cada uma aponta para um banco MySQL proprio e isolado.';
