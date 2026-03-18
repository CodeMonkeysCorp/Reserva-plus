# **Reserva+**
Sistema Web de Gestão e Agendamento de Quadras e Quiosques

**Integrantes:**
- José Henrique Brühmüller
- Matheus Büsemayer

**Disciplina:** Programação Web – Baseada em Projetos

**Professor:** Luiz Carlos Camargo


**1. Domínio do Problema**

**Contexto:**

**Condomínios, clubes e associações frequentemente enfrentam dificuldades na organização de reservas de:**
- Quadras esportivas
- Quiosques
- Espaços de lazer

**Problemas comuns:**
- Conflito de horários
- Falta de controle centralizado
- Cancelamentos desorganizados
- Ausência de histórico
- Falta de controle de regras (limite por usuário, horários bloqueados etc.)

**Solução Proposta:**
O **Reserva+** é uma aplicação Web para gerenciamento e agendamento de espaços comuns.

**O sistema permitirá:**
1. Cadastro e autenticação de usuários
2. Cadastro e gerenciamento de espaços
3. Agendamento de reservas
4. Cancelamento de reservas
5. Controle automático de conflitos
6. Histórico de reservas
7. Controle administrativo de horários

**2. Escopo do Projeto**

**Escopo Mínimo (Conforme disciplina)**

✔ 1 CRUD completo (Espaço)

✔ 1 Transação (Reserva de horário)

✔ Autenticação com login/token

✔ API REST

✔ Persistência em banco relacional

✔ Aplicação Web funcional


**3. Requisitos**

**Requisitos Funcionais (RF):**
- RF01 – O sistema deve permitir cadastro de usuários.
- RF02 – O sistema deve permitir autenticação via login.
- RF03 – O sistema deve permitir CRUD de espaços (quadras/quiosques).
- RF04 – O sistema deve permitir criar reserva.
- RF05 – O sistema deve impedir reservas em horários já ocupados.
- RF06 – O sistema deve permitir cancelamento de reservas.
- RF07 – O sistema deve manter histórico de reservas.
- RF08 – O administrador pode bloquear horários.

**Requisitos Não Funcionais (RNF)**:
- RNF01 – Arquitetura baseada em MVC.
- RNF02 – Comunicação via API REST.
- RNF03 – Persistência relacional com integridade referencial.
- RNF04 – Senhas armazenadas criptografadas.
- RNF05 – Controle transacional para evitar conflitos.
- RNF06 – Versionamento via Git.
- RNF07 – Tempo de resposta inferior a 2 segundos.

**4. Arquitetura do Sistema**

**C-1:**
**Contexto de sistema**

<img width="474" height="425" alt="image" src="https://github.com/user-attachments/assets/a808f2d2-b997-4ed0-b931-ef83f077809a" />

**C-2:**
**Contêineres**

<img width="313" height="972" alt="image" src="https://github.com/user-attachments/assets/0d783a55-121b-41d6-a511-c151cc06d65c" />

**C-3:**
**Componentes**

<img width="574" height="513" alt="image" src="https://github.com/user-attachments/assets/b723632c-1373-4893-aa02-2f3294fb9ba5" />


**Arquitetura Client-Server:**

Angular (Frontend SPA) → Spring Boot (Backend REST API) → MySQL (Banco de Dados)

**Padrão Arquitetural**
MVC (Model-View-Controller)

**Backend organizado em camadas:**
- Controller → Endpoints REST
- Service → Regras de negócio
- Repository → Acesso ao banco
- Model → Entidades JPA

**Frontend:**
Angular como SPA (Single Page Application)

**5. Tecnologias Utilizadas**

**Java 17+**
- Linguagem robusta e orientada a objetos
- Forte tipagem
- Alinhada com a bibliografia da disciplina

**Spring Boot**
- Auto-configuração
- Embedded Tomcat
- Injeção de Dependência (IoC)
- Suporte nativo a REST
- Estrutura organizada em camadas

**Spring Data JPA**
- Abstração de acesso ao banco
- Redução de código boilerplate
- Suporte a transações

**Spring Security + JWT**
- Autenticação baseada em token
- API stateless
- Controle de acesso por perfil

**MySQL**
- Banco relacional robusto
- Suporte a transações (ACID)
- Ideal para controle de concorrência em reservas

**DBeaver**
- Ferramenta para modelagem e administração do banco
- Facilita testes e visualização de dados

**Angular**
- Framework SPA
- Arquitetura baseada em componentes
- Injeção de dependência
- Integração simples com APIs REST

**6. Modelo de Dados (Inicial)**

**Usuario**
1. id
2. nome
3. email
4. senha
5. role (ADMIN / USER)

**Espaco**
1. id
2. nome
3. tipo (QUADRA / QUIOSQUE)
4. descricao
5. ativo

**Reserva**
1. id
2. usuario_id
3. espaco_id
4. data
5. horarioInicio
6. horarioFim
7. status

**Relacionamentos:**
- Usuário 1:N Reserva
- Espaço 1:N Reserva

**7. Transação Principal**
Criação de Reserva

**Processo:**
1. Verificar se o espaço está ativo.
2. Verificar conflito de horário.
3. Criar reserva.
4. Confirmar transação.
5. Caso exista conflito → rollback automático.

A operação será controlada via **@Transactional** para garantir integridade.

**8. Padrões de Projeto Aplicados**
- MVC
- Repository Pattern
- Dependency Injection
- Singleton (gerenciado pelo container Spring)
- Strategy (possível aplicação para regras diferentes entre quadra e quiosque)

**9. Organização da Dupla**  

**Backend:**
- Modelagem do banco
- Implementação da API REST
- Regras de negócio
- Autenticação JWT
- Controle transacional

**Frontend:**
- Desenvolvimento SPA em Angular
- Telas de cadastro/login
- Tela de agendamento
- Consumo da API
- Guards de autenticação

**Ambos:**
- Testes
- Documentação
- Deploy
- Apresentação

**10. Planejamento por Entrega**

**N1**
- Estrutura do projeto
- Modelagem ER
- CRUD de Espaço
- Cadastro/Login

**N2**
- Implementação da reserva
- Validação de conflito
- Histórico de reservas

**N3**
- Sistema funcional (≥ 80% do escopo)
- Melhorias de UX
- Segurança refinada
- Deploy
