# Fila Saúde — Sistema de Gestão de Filas do SUS

Produto SaaS multi-prefeitura para gestão de filas de regulação do SUS (consultas,
exames e cirurgias), inspirado na referência de mercado analisada (Fila Saúde /
lp.filasaude.cloud). Este repositório contém a **Fase 1 (MVP)**, conforme o
[levantamento de requisitos](#) validado com o cliente.

## Escopo desta fase (MVP)

- Módulo do cidadão: consulta pública de protocolo por CPF/CNS, com timeline do
  ciclo de atendimento.
- Gestão de fila e regulação, com motor de priorização automático (Urgência,
  Judicial, Especial, Legal, Normal).
- Painel administrativo simples: dashboard de indicadores e listagem da fila.
- Notificação ao cidadão por e-mail em mudança de status.
- Autenticação JWT com 3 perfis internos: ACS, Regulador, Admin.

**Fora do escopo desta fase** (fase 2): gestão de cotas por unidade, importação em
lote, multi-tenant self-service (provisionamento automático de nova prefeitura) e
painel de superadmin.

## Arquitetura

- **Backend**: Java 21 + Spring Boot 3 (Web, Security, Data JPA, Mail), MySQL, JWT.
- **Frontend**: React 19 + TypeScript + Vite + Tailwind CSS v4 + React Router.
- **Multi-tenancy**: **banco de dados MySQL separado por tenant** (decisão
  registrada no levantamento de requisitos). Um banco MASTER guarda o cadastro de
  prefeituras (`tenants`) e a aplicação roteia dinamicamente para o banco de cada
  uma a cada requisição, resolvendo o tenant pelo header `X-Tenant-Id` (ou pelo
  subdomínio, em produção). Veja `backend/src/main/java/br/com/filasaude/tenancy/`.

```
sistema-fila-saude/
├── backend/     Spring Boot (API REST)
├── frontend/    React + Vite (SPA)
└── docker-compose.yml   MySQL local para desenvolvimento
```

## Rodando localmente

### 1. Banco de dados

```bash
docker compose up -d
```

Isso sobe um MySQL 8 local com dois bancos: `filasaude_master` (cadastro de
tenants) e `filasaude_demo` (dados de uma prefeitura de demonstração).

### 2. Backend

```bash
cd backend
# defina as variáveis de ambiente necessárias (ou use os defaults do application.yml,
# que já apontam para o MySQL local subido pelo docker-compose)
mvn spring-boot:run
```

No primeiro boot, o schema do banco MASTER é migrado automaticamente. Em seguida:

```bash
# 1. Registra a prefeitura de demonstração no banco master
mysql -h localhost -u root -proot filasaude_master < backend/scripts/seed-master-tenant.sql

# 2. Dispara uma requisição para aquele tenant, o que aciona a migração
#    (sob demanda) do schema de negócio no banco filasaude_demo
curl -H "X-Tenant-Id: demo" http://localhost:8080/api/procedimentos

# 3. Popula dados de exemplo (usuário admin, pacientes, protocolos)
mysql -h localhost -u root -proot filasaude_demo < backend/scripts/seed-tenant-demo.sql
```

Login de demonstração: `admin@demo.filasaude.com.br` / `admin123`

> **Nota sobre compilação**: este ambiente de desenvolvimento (sandbox) não tem
> acesso ao Maven Central, então o backend não pôde ser compilado/testado aqui.
> O código foi revisado manualmente, mas **rode `mvn compile` (ou abra no
> IntelliJ/VS Code) assim que clonar o repositório** para pegar qualquer erro de
> compilação antes de seguir adiante.

### 3. Frontend

```bash
cd frontend
cp .env.example .env    # ajuste VITE_TENANT_ID se necessário
npm install
npm run dev
```

Acesse `http://localhost:5173` (consulta pública do cidadão) e
`http://localhost:5173/admin/login` (painel administrativo).

## Motor de priorização

A ordenação da fila (item central do produto) segue, nesta ordem:

1. **Categoria de prioridade**: Urgência > Judicial > Especial (80+) > Legal
   (60+, PCD, gestante) > Normal.
2. Dentro da mesma categoria: **data de inclusão na fila** (FIFO), garantindo a
   cronologia auditável exigida no levantamento de requisitos (proteção contra
   "fura-filas").

Implementado em `FilaPriorizacaoService` e reutilizado tanto na consulta pública
do cidadão (posição na fila) quanto na listagem administrativa.

## Próximos passos (fora desta fase)

Ver seção 6 do levantamento de requisitos para o racional completo:

- Gestão de cotas por unidade de saúde
- Importação em lote de procedimentos/pacientes
- Provisionamento self-service de novo tenant + painel de superadmin
- Estrutura de valores do licenciamento por porte de município
