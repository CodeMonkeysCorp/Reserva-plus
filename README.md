# Reserva+

Sistema web para cadastro, gerenciamento e reserva de espaços compartilhados, como salas, quadras, estúdios e ambientes multiuso.

## Informações do Projeto

- Integrantes: José Henrique Brühmüller, Matheus Büsemayer
- Disciplina: Programação Web - Baseada em Projetos
- Professor: Luiz Carlos Camargo

## Visão Geral

O projeto foi dividido em duas aplicações:

- `frontend/`: SPA em Angular
- `backend/`: API REST em Spring Boot

O fluxo principal hoje é:

1. usuário cria conta ou faz login
2. usuário acessa a área de reservas
3. usuário escolhe espaço, data e horário
4. sistema valida conflito e bloqueios
5. reserva é registrada no banco

Usuários comuns veem e gerenciam as próprias reservas.
Administradores também podem cadastrar espaços e controlar bloqueios de horário.

## Stack

- Frontend: Angular 17
- Backend: Spring Boot 3.3
- Java: 17+
- Banco: MySQL 8
- Autenticação: Spring Security + JWT

## Funcionalidades

- cadastro de usuários
- login com token JWT
- criação e cancelamento de reservas
- histórico de reservas
- cadastro e edição de espaços
- bloqueio de horários por administrador
- criação automática de usuário admin no primeiro boot

## Estrutura do Projeto

- `frontend/`: interface web
- `backend/`: API e regras de negócio
- `run-local.ps1`: sobe a stack local via Docker
- `stop-local.ps1`: para a stack local via Docker
- `compose.yaml`: sobe frontend, backend e MySQL via Docker

## Requisitos

### Para rodar com Docker

- Docker Desktop

### Para rodar backend/frontend manualmente

- Java 17 ou superior
- Maven 3.9+
- Node.js e npm
- PowerShell

## Portas Padrão

- frontend: `4300`
- backend: `8180`
- banco: `3310`

## Rodando com Docker

Esse é o caminho padrão do projeto agora.

### 1. Crie o arquivo `.env`

Na raiz do projeto:

```powershell
Copy-Item .env.example .env
```

### 2. Suba a stack

```powershell
.\run-local.ps1 -Build
```

Esse comando sobe:

- frontend Angular servido por Nginx
- backend Spring Boot
- MySQL 8.4

Você também pode usar o Compose diretamente:

```powershell
docker compose up --build -d
```

Se a stack já estiver rodando, o `run-local.ps1` derruba e sobe novamente os serviços alvo automaticamente.
Quando houver mudança de código no backend ou frontend, prefira usar `-Build` para reconstruir as imagens locais antes de subir.

### 3. Acesse

- frontend: `http://localhost:4300`
- backend: `http://localhost:8180`
- healthcheck: `http://localhost:8180/actuator/health`

### 4. Comandos úteis

Parar toda a stack:

```powershell
.\stop-local.ps1
```

Subir apenas o banco:

```powershell
.\run-local.ps1 -DatabaseOnly
```

Subir apenas backend e banco:

```powershell
.\run-local.ps1 -BackendOnly
```

Ver logs:

```powershell
docker compose logs -f
```

## Backend Manual com Profile Local

Se quiser subir o backend manualmente:

```powershell
.\run-local.ps1 -DatabaseOnly
cd .\backend
$env:SPRING_PROFILES_ACTIVE="local"
mvn spring-boot:run
```

O profile `local` fica em:

- `backend/src/main/resources/application-local.yml`

Esse profile usa:

- host: `127.0.0.1`
- banco: `reserva_plus`
- usuário: `reserva_app`
- senha: `reserva123`

Esse fluxo usa o MySQL publicado pelo Docker na porta `3310`.

## Frontend Manual

Para subir apenas o frontend:

```powershell
cd .\frontend
npm install
npm start
```

## Variáveis de Ambiente do Backend

O backend suporta estas variáveis:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `SERVER_PORT`
- `BACKEND_PORT`
- `FRONTEND_PORT`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `APP_ADMIN_NAME`
- `APP_ADMIN_EMAIL`
- `APP_ADMIN_PASSWORD`
- `RESERVA_CONCLUSAO_CRON`
- `RESERVA_CONCLUSAO_ZONE`
- `APP_STORAGE_R2_ENABLED`
- `APP_STORAGE_R2_ACCOUNT_ID`
- `APP_STORAGE_R2_ENDPOINT`
- `APP_STORAGE_R2_BUCKET`
- `APP_STORAGE_R2_ACCESS_KEY_ID`
- `APP_STORAGE_R2_SECRET_ACCESS_KEY`
- `APP_STORAGE_R2_PUBLIC_BASE_URL`
- `APP_STORAGE_R2_OBJECT_KEY_PREFIX`
- `APP_STORAGE_R2_MAX_FILE_SIZE`

Sem sobrescrever nada:

- o setup padrão do Docker usa `reserva_app / reserva123`
- o frontend Docker consome a API por proxy interno em `/api`
- o profile `local` continua disponivel para rodar o backend manualmente contra o MySQL do Docker

## Cloudflare R2 para Imagens de Espacos

O backend agora possui uma camada inicial de comunicacao com o Cloudflare R2 para upload e remocao de imagens.

Variáveis principais:

- `APP_STORAGE_R2_ENABLED=true` para ativar a integracao
- `APP_STORAGE_R2_ACCOUNT_ID` ou `APP_STORAGE_R2_ENDPOINT` para o endpoint S3 do R2
- `APP_STORAGE_R2_BUCKET` para o bucket de imagens
- `APP_STORAGE_R2_ACCESS_KEY_ID` e `APP_STORAGE_R2_SECRET_ACCESS_KEY` para as credenciais
- `APP_STORAGE_R2_PUBLIC_BASE_URL` para montar a URL publica da imagem quando o bucket/CDN estiver exposto
- `APP_STORAGE_R2_OBJECT_KEY_PREFIX` para o prefixo raiz dos objetos
- `APP_STORAGE_R2_MAX_FILE_SIZE` para limitar o tamanho do upload

Observacoes:

- quando `APP_STORAGE_R2_ENABLED=false`, os endpoints de imagem respondem com `503 Service Unavailable`
- os uploads aceitam `JPG`, `PNG` e `WEBP`
- o limite de upload e o multipart do Spring usam `APP_STORAGE_R2_MAX_FILE_SIZE`

Endpoints iniciais:

- `POST /api/espacos/imagens` com `multipart/form-data` no campo `arquivo`
- `DELETE /api/espacos/imagens?chaveObjeto=<chave>` para remocao administrativa

Exemplo de upload:

```powershell
curl -X POST http://localhost:8180/api/espacos/imagens `
  -H "Authorization: Bearer <token-admin>" `
  -F "arquivo=@C:\caminho\imagem.png"
```

## Credenciais Iniciais

No primeiro boot, se ainda não existir admin no banco, a aplicação cria:

- email: `admin@reserva.local`
- senha: `admin123`

## Rotas da Aplicação

- `/login`: entrar
- `/register`: criar conta
- `/home`: tela inicial do usuário autenticado
- `/reservas`: criar e visualizar reservas
- `/espacos`: administração de espaços
- `/bloqueios`: administração de bloqueios

## Fluxo do Usuário

### Usuário comum

- faz cadastro ou login
- acessa `/reservas`
- cria reserva
- visualiza o próprio histórico
- cancela as próprias reservas

### Administrador

- faz login
- acessa todas as reservas
- cadastra espaços
- edita espaços
- controla bloqueios de horário

## Solução de Problemas

### PowerShell bloqueando scripts

Se o PowerShell bloquear `ps1`:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

### Porta 8180 em uso

Se o backend reclamar que a `8180` já está ocupada, provavelmente já existe uma instância rodando.

Para verificar:

```powershell
Get-NetTCPConnection -LocalPort 8180 -State Listen
```

### Porta 4300 em uso

Se o frontend já estiver aberto, o Angular pode sugerir outra porta.

### Porta 3310 em uso

Se o MySQL do Docker não subir, provavelmente já existe outro banco ocupando a porta `3310`.

Para verificar:

```powershell
Get-NetTCPConnection -LocalPort 3310 -State Listen
```

Para encerrar a stack atual:

```powershell
.\stop-local.ps1
```

Se o processo não for desta stack, libere a porta manualmente ou altere `MYSQL_PORT` no `.env`.

### Status dos containers

Para verificar rapidamente o estado da stack:

```powershell
docker compose ps
```

### Healthcheck

Se quiser validar rapidamente se o backend está no ar:

```text
http://localhost:8180/actuator/health
```

Resposta esperada:

```json
{"status":"UP"}
```

## Observações

- o backend usa `ddl-auto: update`, então as tabelas são criadas e ajustadas automaticamente
- o projeto foi preparado para desenvolvimento local em Windows com PowerShell
- o Docker é o ambiente padrão de desenvolvimento local

## Documentação do Projeto

## CI/CD

O projeto agora possui esteira de CI/CD baseada em GitHub Actions e Docker:

- `.github/workflows/ci.yml`: valida backend, frontend e build das imagens em pull requests e pushes
- `.github/workflows/cd.yml`: testa, publica imagens no GHCR e pode acionar deploy via SSH
- `compose.prod.yaml`: stack de produção usando imagens publicadas
- `scripts/deploy-prod.sh` e `scripts/deploy-prod.ps1`: deploy manual por Docker Compose
- `scripts/init-prod-env.ps1` e `scripts/init-prod-env.sh`: geram `.env.prod` com segredos fortes

Detalhes de variaveis, secrets e exemplo de `.env.prod` estao em:

- `docs/ci-cd.md`

### 1. Domínio do Problema

Contexto:

Empresas, clubes, instituições de ensino, coworkings e outras operações com espaços compartilhados frequentemente enfrentam dificuldades na organização de reservas de:

- salas
- quadras esportivas
- estúdios
- áreas multiuso

Problemas comuns:

- conflito de horários
- falta de controle centralizado
- cancelamentos desorganizados
- ausência de histórico
- falta de controle de regras, como limite por usuário e horários bloqueados

Solução proposta:

O `Reserva+` é uma aplicação web para gerenciamento e agendamento de espaços e recursos compartilhados.

O sistema permite:

1. cadastro e autenticação de usuários
2. cadastro e gerenciamento de espaços
3. agendamento de reservas
4. cancelamento de reservas
5. controle automático de conflitos
6. histórico de reservas
7. controle administrativo de horários

### 2. Escopo do Projeto

Escopo mínimo:

- CRUD completo de espaço
- transação de reserva de horário
- autenticação com login e token
- API REST
- persistência em banco relacional
- aplicação web funcional

### 3. Requisitos

Requisitos funcionais:

- RF01: permitir cadastro de usuários
- RF02: permitir autenticação via login
- RF03: permitir CRUD de espaços
- RF04: permitir criar reserva
- RF05: impedir reservas em horários ocupados
- RF06: permitir cancelamento de reservas
- RF07: manter histórico de reservas
- RF08: permitir bloqueio de horários pelo administrador

Requisitos não funcionais:

- RNF01: arquitetura baseada em MVC
- RNF02: comunicação via API REST
- RNF03: persistência relacional com integridade referencial
- RNF04: senhas armazenadas com criptografia
- RNF05: controle transacional para evitar conflitos
- RNF06: versionamento via Git
- RNF07: tempo de resposta inferior a 2 segundos

### 4. Arquitetura do Sistema

Arquitetura client-server:

`Angular (Frontend SPA) -> Spring Boot (Backend REST API) -> MySQL (Banco de Dados)`

Padrão arquitetural:

- MVC

Backend organizado em camadas:

- Controller: endpoints REST
- Service: regras de negócio
- Repository: acesso ao banco
- Model: entidades JPA

Frontend:

- Angular como SPA

Diagramas C4 originalmente referenciados no projeto:

- Contexto do sistema
  
  <img width="589" height="903" alt="imagem (10)" src="https://github.com/user-attachments/assets/879a59b4-5f69-4b63-8113-829fbe252fd9" />


- Contêineres

  <img width="759" height="1283" alt="imagem (11)" src="https://github.com/user-attachments/assets/d6d84ade-8ee7-4b6b-803f-caf003a239c3" />


- Componentes

  <img width="574" height="442" alt="imagem (12)" src="https://github.com/user-attachments/assets/834bac41-19b2-48e6-b0b3-982847ee4483" />



### 5. Tecnologias Utilizadas

Java 17+:

- linguagem orientada a objetos
- forte tipagem
- compatibilidade com Spring Boot 3

Spring Boot:

- auto-configuração
- servidor embutido
- injeção de dependência
- suporte nativo a REST

Spring Data JPA:

- abstração de acesso ao banco
- menos boilerplate
- suporte a transações

Spring Security + JWT:

- autenticação stateless
- controle de acesso por perfil

MySQL:

- banco relacional
- suporte a transações
- adequado para controle de concorrência em reservas

Angular:

- SPA baseada em componentes
- integração com API REST

### 6. Modelo de Dados

Usuário:

1. id
2. nome
3. email
4. senha
5. role

Espaço:

1. id
2. nome
3. tipo
4. descrição
5. ativo

Reserva:

1. id
2. usuario_id
3. espaco_id
4. data
5. horarioInicio
6. horarioFim
7. status

Relacionamentos:

- usuário 1:N reserva
- espaço 1:N reserva

### 7. Transação Principal

Criação de reserva:

1. verificar se o espaço está ativo
2. verificar bloqueios de horário
3. verificar conflito de horário
4. criar reserva
5. confirmar transação
6. em caso de conflito, realizar rollback automático

A operação é controlada via `@Transactional`.

### 8. Padrões de Projeto Aplicados

- MVC
- Repository Pattern
- Dependency Injection
- Singleton gerenciado pelo container Spring

### 9. Organização da Dupla

Backend:

- modelagem do banco
- implementação da API REST
- regras de negócio
- autenticação JWT
- controle transacional

Frontend:

- desenvolvimento SPA em Angular
- telas de cadastro e login
- tela de agendamento
- consumo da API
- guards de autenticação

Ambos:

- testes
- documentação
- deploy
- apresentação

### 10. Planejamento por Entrega

N1:

- estrutura do projeto
- modelagem ER
- CRUD de espaço
- cadastro e login

N2:

- implementação da reserva
- validação de conflito
- histórico de reservas

N3:

- sistema funcional com maior parte do escopo
- melhorias de UX
- segurança refinada
- deploy
