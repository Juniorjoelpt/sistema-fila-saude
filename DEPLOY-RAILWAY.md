# Deploy no Railway (demonstração em produção)

Três serviços no mesmo projeto: **MySQL**, **backend** (`/backend`) e **frontend** (`/frontend`).
Ambos usam o `Dockerfile` da própria pasta.

## 1. Projeto e banco
1. railway.com → **New Project → Deploy from GitHub repo** → `Juniorjoelpt/sistema-fila-saude` (não precisa escolher serviço ainda).
2. **+ New → Database → Add MySQL**. Nomeie o serviço exatamente `MySQL` (as referências abaixo usam esse nome).

## 2. Backend
**+ New → GitHub Repo** (mesmo repo) → Settings → **Root Directory = `backend`**. Em Variables:

| Variável | Valor |
|---|---|
| `MASTER_DB_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/filasaude_master?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Fortaleza` |
| `MASTER_DB_USER` | `${{MySQL.MYSQLUSER}}` |
| `MASTER_DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `JWT_SECRET` | texto aleatório com 48+ caracteres (**não** use o padrão) |
| `SUPERADMIN_EMAIL` | e-mail do superadmin |
| `SUPERADMIN_PASSWORD_HASH` | hash bcrypt da senha que você escolher (veja abaixo) |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | preencher no passo 4 |
| `FRONTEND_URL_PATTERN` | preencher no passo 4 |

Gerar o hash (a senha fica só com você):
```
docker run --rm httpd:2.4-alpine htpasswd -nbBC 10 x "SUA_SENHA"
```
Use o trecho depois de `x:` (começa com `$2y$10$`).

Settings → Networking → **Generate Domain** (anote a URL).

## 3. Frontend
**+ New → GitHub Repo** → **Root Directory = `frontend`**. Variables (valem no build):

| Variável | Valor |
|---|---|
| `VITE_API_URL` | URL pública do backend (`https://....up.railway.app`, sem barra final) |
| `VITE_TENANT_FIXO` | `demo` |

**Generate Domain** e anote a URL.

## 4. Fechar o ciclo
No backend, defina:
- `CORS_ALLOWED_ORIGIN_PATTERNS` = URL do frontend
- `FRONTEND_URL_PATTERN` = `<URL do frontend>/?tenant={tenant}`

O Railway reimplanta sozinho.

## 5. Criar a prefeitura demo
1. Abra `<URL do frontend>/superadmin/login` e entre com o e-mail/senha do passo 2.
2. Provisione a prefeitura com identificador **`demo`** (cria o banco, migra o schema e gera o admin; a senha provisória aparece uma única vez na tela).
3. Entre em `<URL do frontend>/admin/login` com esse admin e cadastre unidades, procedimentos e usuários.

## Observações
- Use sempre dados fictícios na demo (nada de pacientes reais).
- O banco do Railway guarda os dados; reimplantar não apaga.
- Domínio próprio com subdomínio por prefeitura: remova `VITE_TENANT_FIXO` e aponte `*.seudominio`.
