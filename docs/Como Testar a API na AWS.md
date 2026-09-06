# Como Testar a API na AWS

Roteiro de testes contra a aplicação rodando na EC2 — é o que aparece na
demonstração do vídeo. Postman como ferramenta principal, com o equivalente em
`curl` para conferência rápida.

---

## 0. Instalar o Postman

### É gratuito

O Postman tem uma versão paga, mas **nada neste projeto encosta nela**. Tudo o
que este documento pede — criar requisições, environments com variáveis, scripts
de pós-resposta, importar OpenAPI, salvar collections — está no plano gratuito.

O que a versão paga acrescenta é voltado a **time e automação**: mais pessoas
colaborando na mesma collection, execução automática de testes em volume,
monitoramento agendado da API, catálogo interno de APIs, login corporativo e
controle de permissões. Nada disso tem a ver com demonstrar uma API num vídeo.

> Você precisa criar uma conta gratuita para usar a maior parte dos recursos (é
> ela que sincroniza suas collections entre dispositivos). Existe também um modo
> mais enxuto sem login, mas ele limita o que dá para salvar — para este trabalho,
> criar a conta é o caminho mais tranquilo.
>
> Os limites exatos do plano gratuito mudam de tempos em tempos; se aparecer
> algum aviso de limite, confira em [postman.com/pricing](https://www.postman.com/pricing).

### Nesta máquina (Linux Mint) já está instalado

Instalado via snap, versão 11.71.7. Nada a fazer — pule para a seção 1.

### Instalação no Windows (para o resto do grupo)

**Opção 1 — instalador (mais direto)**

1. Abrir [postman.com/downloads](https://www.postman.com/downloads/)
2. Clicar em **Windows 64-bit** — o site já detecta o sistema
3. Executar o `.exe` baixado

O instalador **não pede senha de administrador**: o Postman se instala na pasta
do próprio usuário. Isso resolve o caso de computador do trabalho ou da
faculdade, onde normalmente não se tem permissão de administrador.

**Opção 2 — pelo terminal, com o `winget`**

O `winget` já vem no Windows 10 e 11. No PowerShell ou Prompt de Comando:

```powershell
winget install Postman.Postman
```

Depois é só abrir pelo menu Iniciar.

### Detalhes que valem para o grupo

**Todo mundo precisa de conta própria.** As collections ficam vinculadas à conta
de quem as criou. Se você montar a collection e quiser passar adiante, exporte:
**Collection → ⋯ → Export → Collection v2.1**, gera um `.json` que os outros
importam. O mesmo vale para o environment (o das variáveis com o IP).

> ⚠️ **Cuidado ao exportar o environment.** Se em algum momento você guardar
> senha ou credencial numa variável, ela vai junto no arquivo exportado. Para
> este projeto não há segredo nenhum nas variáveis (só o IP e uns UUIDs), mas
> vale saber antes de mandar arquivo no grupo do WhatsApp.

**Se a API não responder na máquina de alguém e responder na sua**, quase sempre
é a rede daquela pessoa bloqueando a porta 8080 (comum em rede corporativa ou de
faculdade) — não é problema da aplicação. Testar pelo celular na rede móvel
confirma na hora.

**macOS**, se alguém usar: mesmo link de download, ou `brew install --cask postman`.

### Alternativas aceitas pelo desafio

O PDF pede "Postman, Insomnia **ou recurso equivalente**". Então valem também:

| Ferramenta | Observação |
|---|---|
| **Insomnia** | Mesma proposta, mais leve. Instalador em [insomnia.rest/download](https://insomnia.rest/download) |
| **Swagger UI** | Já embutido na sua aplicação (seção 1.4) — mas use como apoio, não como demonstração principal |
| **`curl`** | Funciona (seção 4), porém no vídeo é menos legível que uma interface |

---

## 1. Preparar o Postman

### 1.1 Criar o Environment

Um *environment* guarda variáveis, para você não repetir o IP em cada requisição
— e, principalmente, para **trocar de local para AWS mudando uma linha só**.

#### Onde achar o IP

Console → **EC2 → Instances** → clicar na `catalogo-api` → campo
**Public IPv4 address**. É um número no formato `54.86.132.19`.

#### Como montar a `baseUrl` a partir dele

```text
IP público da instância:  54.86.132.19
                          └──────┬─────┘
                                 │
   http://  54.86.132.19  :8080  /api
   └─┬──┘   └─────┬────┘  └─┬─┘  └─┬─┘
  protocolo     o IP      porta   prefixo das rotas
```

Ou seja: **se o IP da sua instância for `54.86.132.19`, a `baseUrl` é
`http://54.86.132.19:8080/api`** — e as requisições ficam assim:

| Para chamar | A URL completa fica |
|---|---|
| Health check | `http://54.86.132.19:8080/actuator/health` |
| Listar categorias | `http://54.86.132.19:8080/api/categories` |
| Criar produto | `http://54.86.132.19:8080/api/products` |
| Swagger UI | `http://54.86.132.19:8080/swagger-ui.html` |

> Repare que o **health check e o Swagger não levam o `/api`** — esse prefixo só
> existe nos endpoints do catálogo, porque foi o que os controllers declararam no
> `@RequestMapping("/api/...")`. O `/actuator/health` é do Spring Boot e mora na
> raiz. Por isso a variável do Postman guarda a `baseUrl` **com** `/api`, e no
> health check a gente sai dela.

**Environments → +** → nome `catalogo-aws`:

| Variable | Initial value | Current value |
|---|---|---|
| `baseUrl` | `http://54.86.132.19:8080/api` ← *troque pelo IP real da sua instância* | idem |
| `categoryId` | *(vazio)* | *(vazio)* |
| `productId` | *(vazio)* | *(vazio)* |

Crie também um `catalogo-local` com `baseUrl = http://localhost:8080/api`, para
testar antes de subir.

> **Sobre a máscara de rede:** um IP sozinho, como `54.86.132.19`, **não tem
> máscara** — ele é só um endereço, e é assim que ele entra numa URL. A máscara
> aparece na notação CIDR, que descreve uma *faixa* de endereços: o `/32` de
> `189.45.12.7/32` significa "exatamente este endereço, mais nenhum", e o `/0` de
> `0.0.0.0/0` significa "qualquer endereço da internet". Por isso ela aparece nas
> regras de Security Group (que falam de faixas de origem permitida) e **nunca**
> numa URL do Postman. Se você digitar `http://54.86.132.19/32:8080`, não
> funciona.

> ⚠️ **`http`, não `https`.** Não há certificado TLS na instância. O Postman não
> reclama, mas o navegador pode tentar forçar https e falhar.

> ⚠️ O IP público da EC2 **muda** quando a instância é parada e religada (e a
> sessão do Learner Lab faz exatamente isso). Se tudo der timeout de repente,
> confira o IP antes de procurar bug.

### 1.2 Criar a Collection

**Collections → +** → nome `Catalogo de Produtos - Desafio 2`.
Selecione o environment `catalogo-aws` no canto superior direito.

### 1.3 O truque que salva a demonstração

Como as chaves são UUID de 36 caracteres, copiar e colar id na frente da câmera é
o jeito mais fácil de errar. Faça o Postman guardar sozinho.

Na requisição **POST Criar categoria**, aba **Scripts → Post-response**:

```javascript
const body = pm.response.json();
pm.environment.set("categoryId", body.id);

pm.test("Status 201", () => pm.response.to.have.status(201));
pm.test("Header Location presente", () =>
    pm.expect(pm.response.headers.has("Location")).to.be.true);
```

Na **POST Criar produto**:

```javascript
const body = pm.response.json();
pm.environment.set("productId", body.id);

pm.test("Status 201", () => pm.response.to.have.status(201));
pm.test("Categoria veio aninhada", () =>
    pm.expect(body.category).to.be.an("object"));
```

A partir daí você usa `{{categoryId}}` e `{{productId}}` nas outras requisições e
nunca mais digita UUID.

### 1.4 Atalho: importar tudo pronto pelo OpenAPI

Em vez de criar as requisições uma a uma, dá para importar o contrato que a
própria aplicação publica:

**Postman → Import → Link →** `http://54.86.132.19:8080/v3/api-docs`
(trocando `54.86.132.19` pelo IP da sua instância)

Ele cria a collection inteira, com os 11 endpoints, os campos de cada corpo e os
tipos. Você só preenche os valores e ajusta o `{{baseUrl}}`.

E se quiser testar **sem Postman nenhum**, direto do navegador:

**http://54.86.132.19:8080/swagger-ui.html** → escolher o endpoint →
*Try it out* → preencher → *Execute*.

> Para o vídeo, use os dois: o Swagger dá a visão geral da API em uma tela, e o
> Postman faz a demonstração caso a caso — que é o que a rubrica pede. Detalhes
> em [[Como o Código Funciona]], seção 0.5.

---

## 2. Health check — sempre o primeiro

```
GET {{baseUrl}}/../actuator/health
```

Ou direto no navegador: `http://54.86.132.19:8080/actuator/health`
(de novo: `54.86.132.19` é só o exemplo — use o IP da sua instância)

```json
{"status":"UP"}
```

Se isso falhar, **pare**: não é problema da API, é infraestrutura. Volte para a
seção "Quando der errado" de [[Infraestrutura na AWS]].

---

## 3. Sequência de testes

A ordem importa: produto precisa de uma categoria existente.

### 3.1 Categorias

**① POST criar categoria** — o endpoint de cadastro exigido pelo desafio

```
POST {{baseUrl}}/categories
Content-Type: application/json

{
  "name": "  Ferramentas  ",
  "description": "  Itens de oficina  "
}
```

Esperado: **201 Created**, header `Location: /api/categories/<uuid>`

```json
{
  "id": "5d5eafb4-9f09-40ef-99b9-e805cc78247c",
  "name": "Ferramentas",
  "description": "Itens de oficina"
}
```

> **Mostre isto no vídeo:** os espaços do `name` sumiram. É o construtor compacto
> do record normalizando a entrada **antes** da validação.

**② POST outra categoria** (para o teste de PUT com conflito, depois)

```json
{ "name": "Eletronicos", "description": "Eletroportateis" }
```

**③ GET listar** — o endpoint de consulta exigido pelo desafio

```
GET {{baseUrl}}/categories
```
Esperado: **200** com o array.

**④ GET por id**

```
GET {{baseUrl}}/categories/{{categoryId}}
```
Esperado: **200**.

**⑤ GET id inexistente**

```
GET {{baseUrl}}/categories/00000000-0000-0000-0000-000000000000
```
Esperado: **404**

```json
{
  "timestamp": "2026-09-06T14:27:06Z",
  "status": 404,
  "message": "Categoria não encontrada: 00000000-0000-0000-0000-000000000000",
  "campos": {}
}
```

**⑥ POST nome duplicado** — repita a requisição ① com `"name": "FERRAMENTAS"`

Esperado: **409 Conflict**. Repare que **maiúsculas não escapam** — a checagem é
*case-insensitive*.

**⑦ POST nome em branco**

```json
{ "name": "     " }
```
Esperado: **400** — o `trim()` transformou em vazio e o `@NotBlank` pegou.

### 3.2 Produtos

**⑧ POST criar produto**

```
POST {{baseUrl}}/products

{
  "name": "Martelo",
  "stockQuantity": 10,
  "price": 49.90,
  "categoryId": "{{categoryId}}"
}
```

Esperado: **201** com a categoria **aninhada** na resposta:

```json
{
  "id": "db936668-...",
  "name": "Martelo",
  "stockQuantity": 10,
  "price": 49.90,
  "category": {
    "id": "5d5eafb4-...",
    "name": "Ferramentas",
    "description": "Itens de oficina"
  }
}
```

> **Mostre isto no vídeo:** a requisição manda `categoryId` (uma referência) e a
> resposta devolve `category` (o objeto inteiro). Entity modela o banco, DTO
> modela o JSON — são contratos diferentes de propósito.

**⑨ POST estoque zero — deve PASSAR**

```json
{ "name": "Chave de fenda", "stockQuantity": 0, "price": 19.90, "categoryId": "{{categoryId}}" }
```
Esperado: **201**. Produto esgotado é um produto válido.

**⑩ POST com três erros de uma vez** — a requisição mais importante da demo

```json
{ "name": "", "stockQuantity": -3, "price": -10.00, "categoryId": "{{categoryId}}" }
```

Esperado: **400** com os **três** campos:

```json
{
  "status": 400,
  "message": "Dados inválidos",
  "campos": {
    "name": "O nome do produto é obrigatório",
    "stockQuantity": "O estoque não pode ser negativo",
    "price": "O preço deve ser maior que zero"
  }
}
```

> **Mostre isto no vídeo:** o validador **não para no primeiro erro** — ele
> coleta o conjunto completo e devolve tudo de uma vez. Quem consome a API
> corrige o formulário inteiro numa tentativa só.

**⑪ POST sem o campo `stockQuantity`**

```json
{ "name": "Serrote", "price": 30.00, "categoryId": "{{categoryId}}" }
```
Esperado: **400**.

> **Mostre isto no vídeo:** se o DTO usasse `int` em vez de `Integer`, o campo
> ausente viraria `0` e entraria no banco em silêncio. `Integer` preserva a
> diferença entre "estoque zero" e "não informado".

**⑫ POST com preço zero** → **400** (`@Positive` exige maior que zero)

**⑬ POST com preço `0.1234`** → **400** (`@Digits` — a coluna é `numeric(10,2)`)

**⑭ POST com categoria inexistente**

```json
{ "name": "Fantasma", "stockQuantity": 1, "price": 1.00,
  "categoryId": "00000000-0000-0000-0000-000000000000" }
```
Esperado: **404** (não 400 — o formato está certo, o recurso é que não existe)

**⑮ GET listar produtos** → **200**, todos com a categoria aninhada

**⑯ GET filtrando por categoria**

```
GET {{baseUrl}}/products?categoryId={{categoryId}}
```
Esperado: **200**

**⑰ GET filtrando por categoria inexistente** → **404**, e não uma lista vazia.
Lista vazia mentiria dizendo "a categoria existe e não tem produtos".

### 3.3 Atualização e remoção

**⑱ PUT alterando só a descrição**, mantendo o mesmo nome

```
PUT {{baseUrl}}/categories/{{categoryId}}

{ "name": "Ferramentas", "description": "Oficina e jardim" }
```
Esperado: **200**.

> Sutil e importante: se a checagem de duplicata fosse ingênua, isso daria 409 —
> a categoria conflitando **com ela mesma**. Por isso o repository pergunta
> "existe **outra** com esse nome" (`existsByNameIgnoreCaseAndIdNot`).

**⑲ PUT usando o nome da outra categoria** (`"Eletronicos"`) → **409**

**⑳ PUT em produto, trocando de categoria**

```
PUT {{baseUrl}}/products/{{productId}}

{ "name": "Martelo de borracha", "stockQuantity": 7, "price": 59.90,
  "categoryId": "<id da categoria Eletronicos>" }
```
Esperado: **200**, agora com a outra categoria aninhada.

**㉑ PUT com preço negativo** → **400**. A mesma validação do POST, porque o
`PUT` reusa o mesmo DTO e o mesmo `@Valid`.

**㉒ DELETE de categoria QUE TEM produtos** — ⭐ a melhor cena do vídeo

```
DELETE {{baseUrl}}/categories/<id de uma categoria com produtos>
```

Esperado: **409 Conflict**

```json
{
  "status": 409,
  "message": "Operação viola uma restrição do banco de dados",
  "campos": {}
}
```

> **Mostre isto no vídeo.** Uma tela só provando três coisas: a chave estrangeira
> do banco funcionando, a exceção sendo traduzida em status HTTP na borda da
> aplicação, e o cliente **não** recebendo detalhe interno do Hibernate. É a
> narrativa de defesa em profundidade fechando.

**㉓ DELETE de produto** → **204 No Content** (sem corpo)

**㉔ DELETE do mesmo produto de novo** → **404**

> O `deleteById` do Spring Data é silencioso com id inexistente. Sem a checagem
> explícita, isto responderia 204 dizendo "apaguei" algo que nunca existiu.

**㉕ DELETE da categoria agora vazia** → **204**

---

## 4. Os mesmos testes em `curl`

Útil para conferir rápido pelo terminal, inclusive de dentro da EC2.

```bash
API=http://54.86.132.19:8080/api      # troque pelo IP da sua instancia

# health
curl -s $API/../actuator/health; echo

# criar categoria e guardar o id
CAT=$(curl -s -X POST $API/categories \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ferramentas","description":"Itens de oficina"}')
echo "$CAT"
CID=$(echo "$CAT" | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])")

# criar produto
curl -s -X POST $API/products -H 'Content-Type: application/json' \
  -d "{\"name\":\"Martelo\",\"stockQuantity\":10,\"price\":49.90,\"categoryId\":\"$CID\"}"; echo

# listar
curl -s $API/products; echo

# ver o status code junto com o corpo
curl -s -w '\nHTTP %{http_code}\n' -X POST $API/products \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"\",\"stockQuantity\":-3,\"price\":-10,\"categoryId\":\"$CID\"}"

# ver os headers (para mostrar o Location do 201)
curl -s -D - -o /dev/null -X POST $API/categories \
  -H 'Content-Type: application/json' -d '{"name":"Jardinagem"}'
```

---

## 5. Erros que parecem bug da API e não são

| Sintoma | Causa real |
|---|---|
| Tudo dá timeout | IP da EC2 mudou depois de parar/religar a instância |
| `Could not get response` no Postman | Porta 8080 não liberada no Security Group |
| **415** Unsupported Media Type | Faltou o header `Content-Type: application/json` |
| **400** com mensagem de parse | JSON malformado — vírgula sobrando, aspas faltando |
| **400** em campo que você mandou | Nome do campo errado (`qttStock` em vez de `stockQuantity`) |
| **500** ao criar produto | `categoryId` num formato que não é UUID válido |
| Acentos aparecem como `Ã§` | Problema de exibição do cliente; a API responde em UTF-8 |
| Funciona local e não na AWS | Perfil `prod` não ativado, ou variáveis do banco erradas |

---

## 6. Checklist antes de gravar

- [ ] `GET /actuator/health` responde `{"status":"UP"}` **de fora** da EC2
- [ ] Environment do Postman apontando para o **IP atual** da instância
- [ ] Banco com dados de exemplo já criados (não comece do zero na gravação)
- [ ] Uma categoria **com produtos**, guardada para a cena do 409
- [ ] Requisições da collection na ordem da demonstração, já salvas
- [ ] Aba de **Headers** da resposta aberta ao menos uma vez (para o `Location`)
- [ ] Console AWS aberto em outra aba: EC2, RDS e os dois Security Groups
- [ ] Sessão do Learner Lab **recém-iniciada** (ela expira em ~4h)

---

## Documentos relacionados

- [[Como o Código Funciona]]
- [[Infraestrutura na AWS]]
- [[Roteiro do Vídeo e Justificativas]]
