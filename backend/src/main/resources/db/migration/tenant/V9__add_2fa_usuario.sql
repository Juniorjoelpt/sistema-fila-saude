-- Autenticacao em dois fatores (2FA) opcional por usuario, via TOTP (app
-- autenticador tipo Google Authenticator/Authy). O segredo fica em texto
-- puro (base32) porque precisa ser lido de volta para validar o codigo a
-- cada login -- nao e um hash de uma via como senha. Continua protegido
-- pelas mesmas garantias de acesso ao banco que ja protegem senha_hash.
ALTER TABLE usuarios ADD COLUMN two_factor_secret VARCHAR(64) NULL;
ALTER TABLE usuarios ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;
