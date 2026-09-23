-- Upload direto de logo (PNG) por tenant, além da opção de URL externa já existente
-- em logo_url (item 3.6 do levantamento de requisitos: identidade visual por prefeitura).
ALTER TABLE tenants
    ADD COLUMN logo_dados LONGBLOB NULL,
    ADD COLUMN logo_content_type VARCHAR(100) NULL;
