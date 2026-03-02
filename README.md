

# SysSentinel — Server (Host)

O SysSentinel Server é a aplicação central responsável por receber, validar e persistir as informações enviadas pelos agentes monitorados.
Ele expõe endpoints REST protegidos por JWT para autenticação, gerenciamento de usuários e consulta de métricas, atuando como núcleo de controle e agregação do sistema.
___
## Índice

* [Função](#função)
* [Estrutura (alto nível)](#estrutura-alto-nível)
* [Requisitos](#requisitos)
* [Configuração de chaves (JWT + registro do agente)](#configuração-de-chaves-jwt--registro-do-agente)

  * [Como o servidor usa essas chaves](#como-o-servidor-usa-essas-chaves)
* [Executando o servidor](#executando-o-servidor)
* [Autenticação (como está no código)](#autenticação-como-está-no-código)

  * [Tokens emitidos](#tokens-emitidos)

    * [Token do usuário (login)](#token-do-usuário-login)
    * [Token do agente (cliente)](#token-do-agente-cliente)
* [Persistência (H2 / JPA)](#persistência-h2--jpa)
* [Endpoints implementados](#endpoints-implementados)

  * [1) Endpoints do Agente (Cliente → Servidor)](#1-endpoints-do-agente-cliente--servidor)

    * `POST /api/systems/sysinfo`
    * `POST /api/systems/sysinfovolatile`
    * `GET /api/systems/updateAuth`
  * [2) Endpoints do Frontend (Frontend → Servidor)](#2-endpoints-do-frontend-frontend--servidor)

    * `GET /api/metrics/systems`
    * `GET /api/metrics/systemVolatileInfo`
    * `POST /api/metrics/systemRegister`
  * [3) Endpoints de Usuário (Frontend → Servidor)](#3-endpoints-de-usuário-frontend--servidor)
    * `POST /api/user/login`
    * `POST /api/user/register`

* [Tráfego JSON “fim a fim” (como o projeto implementa)](#tráfego-json-fim-a-fim-como-o-projeto-implementa)

  * [Fluxo do Agente (cliente) → Host](#fluxo-do-agente-cliente--host)
  * [Fluxo do Usuário (frontend) → Host](#fluxo-do-usuário-frontend--host)

* [Frontend servido pelo servidor](#frontend-servido-pelo-servidor)
* [Swagger / OpenAPI](#swagger--openapi)
___
## Função

* recebe **cadastro/atualização** de informações do agente (cliente) via JSON
* expõe endpoints para o **frontend** consultar **lista de sistemas** e **métricas voláteis** (incluindo processos)
* gerencia **usuários** (register/login) com **senha em BCrypt**
* autentica via **JWT HS256** (Resource Server)

---

## Estrutura (alto nível)

Pasta principal: `SysSentinel-Server/`

Principais pacotes:

* `com.bolota.syssentinel.Controllers`

    * `HostToClientController` — endpoints usados pelo **cliente/agente**
    * `HostToMetricsFrontendController` — endpoints usados pelo **frontend**
    * `HostToUserFrontendController` — login/register e remoção de vínculo user↔system
    * `WebController` — serve `GET /` retornando template `index`
* `com.bolota.syssentinel.Entities`

    * `SystemEntities` — entidades JPA persistidas (SystemEntityPersistent / SystemVolatileEntityPersistent)
    * `DTOs` — modelos usados no tráfego JSON 
    * `UserEntities` — entidade JPA de usuário
* `com.bolota.syssentinel.Security`

    * `SecurityConfig`, `JwtConfig`, `SystemSecurity`
* `com.bolota.syssentinel.Resource`

    * repositories JPA (SystemEntityResources, SystemVolatileEntityResources, UserEntityResources)
* `src/main/resources`
    * `static/` e `templates/` (build do front servido pelo Spring)

---

## Requisitos

* Java compatível com Spring Boot 3.5.x
* Maven

---

## Configuração de chaves (JWT + registro do agente)

O servidor lê **duas chaves** de:

`src/main/java/com/bolota/syssentinel/Security/authKey.config`

Formato (exatamente assim):

```txt
AuthKey=<string>
RegisterKey=<string>
```

### Como o servidor usa essas chaves

> Existe um método`SystemSecurity.registerAuthKey()`que pede essas chaves via `stdin` e grava no arquivo, e ele **está ligado automaticamente** ao fluxo de inicialização .

* `AuthKey` é usada para assinar/verificar JWT (HS256) — via `JwtConfig` + `SystemSecurity.getAuthKey()`.
* `RegisterKey` é usada como “segredo” para permitir **registro**/provisionamento do agente (via header `RegisterToken` em endpoints específicos) — via `SystemSecurity.getRegisterKey()`.

> [!WARNING]
> A `RegisterKey` definida no Servidor deve ser idêntica à que for definida no Cliente, caso contrário, o cliente não conseguirá fazer uma requisição para receber um JWT próprio. .

---

## Executando o servidor

Dentro de `SysSentinel-Server/`:

```bash
mvn spring-boot:run
```

Config atual (em `application.yaml`):

* Porta: `8080`
* Bind: `0.0.0.0`
* Banco: `H2` em memória (`jdbc:h2:mem:test`)
* Console H2:

    * habilitado em `/h2-console`
    * `web-allow-others: true`

---

## Autenticação (como está no código)

O servidor funciona como **OAuth2 Resource Server JWT** (Spring Security), então endpoints marcados como `authenticated()` exigem:

* Header padrão: `Authorization: Bearer <token>`

Além disso, alguns endpoints usam headers próprios (`JwtToken`, `RegisterToken`, `login`, etc.), porque o código explicitamente lê esses valores.

### Tokens emitidos

#### Token do usuário (login)

Emitido por `HostToUserFrontendController.issueLoginToken()`:

* `subject`: login do usuário
* claim `roles`: `["USER"]`
* expiração: `now + 60*60*3` (3 horas)

#### Token do agente (cliente)

Emitido por `HostToClientController.issueAgentToken()`:

* `subject`: UUID do sistema
* claim `roles`: `["AGENT"]`
* expiração: `now + 60` segundos (**1 minuto**)

> Importante: o endpoint `/api/systems/sysinfo` **não usa Authorization Bearer**. Ele recebe o token pelo header **`JwtToken`** e valida manualmente com `jwtDecoder.decode()`.

---

## Persistência (H2 / JPA)

Entidades persistidas:

* `SystemEntityPersistent` (`@Entity`)

    * `@Id` = `UUID` (String)
    * campos: `UUID, name, os, host, cpu, gpu (List<String>), memRamMax`
* `SystemVolatileEntityPersistent` (`@Entity`)

    * `@Id` = `UUID` (String)
    * campos stringificados em JSON:

        * `basicComputerInfo`
        * `internetCurrentUsage`
        * `internetAdapters`
        * `systemProcessEntities` (`@Lob`)
* `UserEntity` (`@Entity`)

    * `id` autoincrement
    * `login` único
    * `passwordHash` (BCrypt)
    * `systemsInPossession` (ArrayList<String>)

Serialização JSON usada internamente:

* `SysSentinelService.genJSON(Object)` usa Jackson `ObjectMapper.writeValueAsString`.

---

## Endpoints implementados

### 1) Endpoints do Agente (Cliente → Servidor)

Base: `/api/systems`

#### `POST /api/systems/sysinfo`  (consome `application/json`) — **permitAll**

Controller: `HostToClientController.SysEntityHandler`

Headers exigidos pelo código:

* `JwtToken` (string; pode ser `"null"`)
* `RegisterToken` (string; pode ser `"null"`)

Body: JSON do tipo `SystemEntityPersistent`:

Campos (conforme `SystemEntityPersistent.java`):

```json
{
  "UUID": "string",
  "name": "string",
  "os": "string",
  "host": "string",
  "cpu": "string",
  "gpu": ["string", "..."],
  "memRamMax": 0.0
}
```

Fluxos **exatos** implementados:

1. **Primeiro registro (gerar UUID)**

* Condição:

    * `seNew.UUID == "null"`
    * `JwtToken == "null"`
    * `RegisterToken == getRegisterKey()`
* Ação:

    * servidor gera UUID aleatório (15 chars)
    * emite token AGENT (exp 60s)
    * salva `SystemEntityPersistent(seNew)` já com UUID setado
* Resposta: `200` com JSON:

```json
{ "UUID": "<novoUUID>", "token": "<jwtAgente>" }
```

2. **Re-registro com UUID existente (provisionar token)**

* Condição:

    * `seNew.UUID != "null"`
    * `JwtToken == "null"`
    * `RegisterToken == getRegisterKey()`
* Se UUID existe no banco:

    * salva `SystemEntityPersistent(seNew)`
    * responde `200` com `{ "UUID": "<uuid>", "token": "<jwtAgente>" }`
* Se UUID **não** existe:

    * responde `401` com `{ "UUID": null, "token": null }`

3. **UUID null mas mandando JwtToken**

* Condição:

    * `seNew.UUID == "null"`
    * `JwtToken != "null"`
    * `RegisterToken == "null"`
* Resposta: `404` com `{ "UUID": null, "token": null }`

4. **Atualização normal (com JwtToken)**

* O código tenta decodificar `JwtToken` e exige:

    * `seNew.UUID == jwt.subject`
* Se bater:

    * salva `SystemEntityPersistent(seNew)`
    * retorna `200` com `{ "UUID": "<uuid>", "token": "<mesmo JwtToken>" }`
* Se não bater / token inválido:

    * retorna `401` com `{ "UUID": null, "token": null }`

---

#### `POST /api/systems/sysinfovolatile` (consome `application/json`) — **authenticated**

Controller: `HostToClientController.SysVolatileHandler`

Auth:

* precisa `Authorization: Bearer <jwtAgente>` (porque o endpoint está `authenticated()` no `SecurityConfig`)
* usa `@AuthenticationPrincipal Jwt jwt`

Body: JSON do tipo `SystemVolatileEntityDTO`:

Campos (conforme `SystemVolatileEntityDTO.java`):

```json
{
  "UUID": "string",
  "basicComputerInfo": { "k": "v" },
  "internetCurrentUsage": { "k": 0.0 },
  "internetAdapters": { "k": "v" },
  "systemProcessEntities": [
    { "name": "string", "pid": 0, "residentMem": 0.0, "virtualMem": 0.0, "cpuLoad": 0.0 }
  ]
}
```

Regras do handler (como está no código):

* Se `jwt == null` ⇒ `401`
* Se `sveNew.UUID == null` ⇒ `403`
* Se `sveNew.UUID != jwt.subject` ⇒ `403`
* Se `SystemEntity` com esse UUID **não existe** ⇒ `404`
* Persistência:

    * se já existe volatile com UUID:

        * `deleteByUUID(uuid)` e depois `save(new SystemVolatileEntityPersistent(sveNew))`
    * senão:

        * `save(new SystemVolatileEntityPersistent(sveNew))`
* Resposta final: `200`

> Observação importante: aqui o projeto faz “update” do volatile via **delete+save** quando já existe.

---

#### `GET /api/systems/updateAuth` — **permitAll**

Controller: `HostToClientController.updateAuth`

Headers:

* `JwtToken`
* `RegisterToken`
* `sysUUID`

Comportamento:

* Se `JwtToken == "null"` e `RegisterToken == getRegisterKey()`:

    * retorna `200` com `{ "UUID": "<sysUUID>", "token": "<novo jwtAgente>" }`
* Caso contrário:

    * retorna `200` com `{ "UUID": "<sysUUID>", "token": null }`

---

### 2) Endpoints do Frontend (Frontend → Servidor)

Base: `/api/metrics`

#### `GET /api/metrics/systems` — **authenticated**

Controller: `HostToMetricsFrontendController.sendSystems`

Auth:

* `Authorization: Bearer <jwtUser>`

Header:

* `login: <string>` (obrigatório no código)

Query (paginação):

* usa `Pageable` com default `size = 10`
* aceita parâmetros padrão do Spring Data (`page`, `size`, etc.)

Regras:

* se `jwt == null` ou `login == null` ⇒ `401`
* se `jwt.subject != login` ⇒ `401`
* se usuário não existe (`!uer.existsByLogin(login)`) ⇒ `401`

Resposta:

* `200` com `Page<SystemEntityDTO>` composto pelos UUIDs que o usuário tem em `systemsInPossession`.

> Implementação: pega os UUIDs do usuário, faz `ser.getByUUID(uuid)` para cada um, monta uma lista e aplica paginação com `toPage(list, pageable)`.

---

#### `GET /api/metrics/systemVolatileInfo` — **authenticated**

Controller: `HostToMetricsFrontendController.sendSystemVolatileInfo`

Auth:

* `Authorization: Bearer <jwtUser>`

Header:

* `login: <string>`

Query params:

* `uuid=<uuid do sistema>`
* `sort=<campo,ordem>` (default: `cpuLoad,desc`)

Campos de sort implementados no `switch`:

* `name`
* `pid`
* `residentMem`
* `virtualMem`
* `cpuLoad`

Ordem:

* `desc` ou qualquer outra string (o código trata como asc no `else`)

Regras:

* se `jwt == null` ou `login == null` ⇒ `401`
* se `jwt.subject != login` ⇒ `401`
* se não existe volatile para esse uuid ⇒ `404`
* se o usuário **não** possui esse uuid em `systemsInPossession` ⇒ `401`

Resposta:

* pega `SystemVolatileEntityPersistent` do banco
* faz `readValue(sve.systemProcessEntities, SystemProcessEntity[].class)`
* ordena em memória conforme `sort`
* re-serializa a lista ordenada e seta de volta em `sve.systemProcessEntities`
* retorna `200` com `SystemVolatileEntityPersistent`

> Repare: o retorno é um SystemVolatileEntityPersistent, e o campo `systemProcessEntities` é uma **string JSON** (porque na entidade é `String @Lob`). O controller apenas garante que essa string esteja ordenada conforme o sort solicitado.

---

#### `POST /api/metrics/systemRegister` — **authenticated**

Controller: `HostToMetricsFrontendController.registerHandler`

Auth:

* `Authorization: Bearer <jwtUser>`

Header:

* `login: <string>`

Query param:

* `uuid=<uuid do sistema>`

Regras:

* se `login == null` ou vazio ⇒ `400`
* se usuário não existe ⇒ `409`
* se `jwt.subject != login` ⇒ `401`
* se sistema não existe (`!ser.existsByUUID(uuid)`) ⇒ `404`
* se usuário já possui uuid ⇒ `200`

Ação:

* adiciona uuid em `systemsInPossession` e salva usuário
* retorna `200`

---

### 3) Endpoints de Usuário (Frontend → Servidor)

Base: `/api/user`

#### `POST /api/user/login` — **permitAll**

Controller: `HostToUserFrontendController.loginHandler`

Entrada:

* `@RequestParam HashMap<String,String> loginInfo`

Ou seja: **form/query params**, não JSON body.

Chaves exigidas:

* `login`
* `password`

Regras:

* params ausentes ⇒ `400`
* usuário não existe ⇒ `404`
* senha não confere (BCrypt) ⇒ `401`
* sucesso ⇒ `200` com **token JWT (string)**

---

#### `POST /api/user/register` — **permitAll**

Controller: `HostToUserFrontendController.registerHandler`

Entrada:

* `@RequestParam HashMap<String,String> loginInfo`
* chaves: `login`, `password`

Regras:

* ausente ⇒ `400`
* vazio/whitespace ⇒ `400`
* se já existe ⇒ `409`
* salva `UserEntity(login, BCrypt(password))`
* retorna `200` com **token JWT (string)**

---

#### `GET /api/user/remove

Controller: `HostToUserFrontendController.removeUUIDFromUser`

Header:
* `Authorization: Bearer <tokenUSER>`

Query params:
* `user=<login>`
* `systemUUID=<uuid>`

Comportamento:

* se sistema existe e usuário possui uuid:

    * remove da lista e salva
    * retorna `200` com `{ "DeleteResult": true/false }` (resultado do `remove`)
* caso contrário:
  * params ausentes ⇒ `400`
  * usuário não existe ⇒ `404`
  * JWT não autenticado ⇒ `401`
  * retorna `404` com `{ "DeleteResult": false }`


---

## Tráfego JSON “fim a fim” (como o projeto implementa)

### Fluxo do Agente (cliente) → Host

1. **Provisionamento inicial**

* Agente manda `POST /api/systems/sysinfo`

    * headers: `JwtToken: "null"`, `RegisterToken: <RegisterKey>`
    * body: `SystemEntityDTO` com `"UUID": "null"`
* Host responde com:

    * UUID gerado
    * token AGENT (60s)

2. **Atualização periódica do inventário “fixo”**

* Agente manda `POST /api/systems/sysinfo`

    * headers: `JwtToken: <tokenAGENT>`, `RegisterToken: "null"`
    * body: `SystemEntityDTO` com `"UUID": "<uuid>"`
* Host valida `uuid == subject(token)` e salva `SystemEntityPersistent`.

3. **Atualização periódica de métricas voláteis**

* Agente manda `POST /api/systems/sysinfovolatile`

    * `Authorization: Bearer <tokenAGENT>`
    * body: `SystemVolatileEntityDTO`
* Host valida `uuid == subject(token)` e salva `SystemVolatileEntityPersistent` (stringificando os mapas/lista em JSON).

4. **Renovar token AGENT**

* Se token expirou, existe `GET /api/systems/updateAuth`

    * headers: `JwtToken: "null"`, `RegisterToken: <RegisterKey>`, `sysUUID: <uuid>`
* Host retorna token novo.

---

### Fluxo do Usuário (frontend) → Host

1. `POST /api/user/register` ou `POST /api/user/login`

* envia params `login` e `password`
* recebe token USER (3h)

2. Vincular um sistema ao usuário (posse)

* `POST /api/metrics/systemRegister?uuid=<uuid>`

    * `Authorization: Bearer <tokenUSER>`
    * header `login: <login>`
* adiciona uuid em `systemsInPossession`

3. Listar sistemas do usuário

* `GET /api/metrics/systems?page=0&size=10`

    * `Authorization: Bearer <tokenUSER>`
    * header `login: <login>`
* retorna Page de `SystemEntityPersistent`

4. Consultar métricas voláteis (com ordenação de processos)

* `GET /api/metrics/systemVolatileInfo?uuid=<uuid>&sort=cpuLoad,desc`

    * `Authorization: Bearer <tokenUSER>`
    * header `login: <login>`
* retorna `SystemVolatileEntityPersistent` com `systemProcessEntities` ordenado (string JSON).

---

## Frontend servido pelo servidor

Existe:

* `resources/static/index.html` + assets

* `resources/templates/index.html` + assets
  e `WebController` serve:

* `GET /` ⇒ retorna `"index"` (template)

Então o Spring está configurado para servir uma página inicial.

---

## Swagger / OpenAPI

O projeto inclui a dependência:

* `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.15`

E o `SecurityConfig` libera:

* `/swagger-ui.html`
* `/swagger-ui/**`
* `/v3/**`

---
