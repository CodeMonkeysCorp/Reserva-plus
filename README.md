# Reserva+

Sistema web para cadastro, gerenciamento e reserva de espaços compartilhados, como quadras e quiosques.

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
- Banco: MySQL / MariaDB
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
- `scripts/setup-xampp-db.ps1`: configura o banco local no XAMPP
- `run-local.ps1`: abre backend e frontend para desenvolvimento local
- `compose.yaml`: sobe um MySQL local via Docker

## Requisitos

### Para rodar com XAMPP

- Java 17 ou superior
- Maven 3.9+
- Node.js e npm
- XAMPP com MySQL/MariaDB
- PowerShell

### Para rodar com Docker

- Java 17 ou superior
- Maven 3.9+
- Node.js e npm
- Docker Desktop

## Portas Padrão

- frontend: `4200`
- backend: `8080`
- banco: `3306`

## Rodando Local com XAMPP

Esse é o caminho mais simples para o projeto hoje.

### 1. Ligue o MySQL no XAMPP

Abra o XAMPP Control Panel e inicie o serviço `MySQL`.

### 2. Configure o banco local

Na raiz do projeto:

```powershell
.\scripts\setup-xampp-db.ps1
```

Esse script:

- cria o banco `reserva_plus`
- cria o usuário `reserva_app`
- usa a senha `reserva123`

Observação:

- em alguns ambientes XAMPP/MariaDB, grants por banco podem ficar inconsistentes por causa do mecanismo `Aria`
- por isso, o script concede acesso global ao usuário `reserva_app`
- isso é apenas para desenvolvimento local

### 3. Suba o projeto

Na raiz do projeto:

```powershell
.\run-local.ps1
```

Esse comando:

- abre o backend em uma janela
- abre o frontend em outra janela
- usa o profile Spring `local`

### 4. Acesse

- frontend: `http://localhost:4200`
- backend: `http://localhost:8080`
- healthcheck: `http://localhost:8080/actuator/health`

### Comandos úteis

Subir só o backend:

```powershell
.\run-local.ps1 -BackendOnly
```

Subir só o frontend:

```powershell
.\run-local.ps1 -FrontendOnly
```

## Backend Manual com Profile Local

Se quiser subir o backend manualmente:

```powershell
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

## Frontend Manual

Para subir apenas o frontend:

```powershell
cd .\frontend
npm install
npm start
```

## Rodando o Banco com Docker

Se preferir padronizar o banco em vez de usar XAMPP, existe um `compose.yaml` na raiz.

### 1. Crie o arquivo `.env`

```powershell
Copy-Item .env.example .env
```

### 2. Suba o MySQL

```powershell
docker compose up -d
```

### 3. Suba o backend

```powershell
cd .\backend
mvn spring-boot:run
```

### 4. Suba o frontend

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
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `APP_ADMIN_NAME`
- `APP_ADMIN_EMAIL`
- `APP_ADMIN_PASSWORD`

Sem sobrescrever nada:

- o setup padrão do Docker usa `reserva / reserva123`
- o setup local com XAMPP usa o profile `local`

## Credenciais Iniciais

No primeiro boot, se ainda não existir admin no banco, a aplicação cria:

- email: `admin@reserva.local`
- senha: `admin123`

## Rotas da Aplicação

- `/login`: entrar
- `/register`: criar conta
- `/home`: painel do usuário autenticado
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

### Porta 8080 em uso

Se o backend reclamar que a `8080` já está ocupada, provavelmente já existe uma instância rodando.

Para verificar:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
```

### Porta 4200 em uso

Se o frontend já estiver aberto, o Angular pode sugerir outra porta.

### Banco negando acesso no XAMPP

Se aparecer erro de acesso ao banco, rode novamente:

```powershell
.\scripts\setup-xampp-db.ps1
```

### Healthcheck

Se quiser validar rapidamente se o backend está no ar:

```text
http://localhost:8080/actuator/health
```

Resposta esperada:

```json
{"status":"UP"}
```

## Observações

- o backend usa `ddl-auto: update`, então as tabelas são criadas e ajustadas automaticamente
- o projeto foi preparado para desenvolvimento local em Windows com PowerShell
- o Docker continua disponível como opção para padronizar o banco entre máquinas

## Documentação do Projeto

### 1. Domínio do Problema

Contexto:

Condomínios, clubes e associações frequentemente enfrentam dificuldades na organização de reservas de:

- quadras esportivas
- quiosques
- espaços de lazer

Problemas comuns:

- conflito de horários
- falta de controle centralizado
- cancelamentos desorganizados
- ausência de histórico
- falta de controle de regras, como limite por usuário e horários bloqueados

Solução proposta:

O `Reserva+` é uma aplicação web para gerenciamento e agendamento de espaços comuns.

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

`Angular (Frontend SPA) -> Spring Boot (Backend REST API) -> MySQL/MariaDB (Banco de Dados)`

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
  
  <img width="474" height="425" alt="image" src="https://github.com/user-attachments/assets/e03b33aa-f01d-4825-9d85-965523b0d9c2" />

- Contêineres

  <img width="313" height="972" alt="image" src="https://github.com/user-attachments/assets/a13bedf6-e01e-4d07-9e88-5994ba4aa81e" />

- Componentes

  <img width="2732" height="511" alt="image" src="https://github.com/user-attachments/assets/7031f810-efb8-420d-8658-f8a7bf3f0f4e" />


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

MySQL / MariaDB:

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
