-- Registra a prefeitura de demonstração no banco MASTER.
-- Rodar depois do primeiro boot da aplicação (quando a tabela `tenants` já existe):
--   mysql -h localhost -u root -proot filasaude_master < seed-master-tenant.sql

INSERT IGNORE INTO tenants (slug, nome_municipio, db_host, db_port, db_name, db_user, db_password,
                             cor_primaria, cor_secundaria, ativo)
VALUES ('demo', 'Prefeitura Demonstração', 'localhost', 3306, 'filasaude_demo', 'root', 'root',
        '#1F3864', '#1B7A6E', true);
