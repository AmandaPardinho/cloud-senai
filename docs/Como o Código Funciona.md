# Como o Código Funciona

API REST de catálogo de produtos — Spring Boot 3.4.5 + JPA/Hibernate.
Documento de referência para explicar a implementação no vídeo do Desafio 2.

---

## 0. Rodando o projeto localmente

### 0.1 O que precisa estar instalado

| Item | Versão | Como conferir |
|---|---|---|
| JDK | **21 ou superior** | `java -version` |
| Maven | 3.9+ | `mvn -version` |

Não precisa instalar banco: o perfil padrão usa **H2 em memória**.

> ⚠️ **A pegadinha desta máquina.** O Maven obedece a variável `JAVA_HOME`, **não**
> o `PATH`. Dá para o `java -version` mostrar 25 e o Maven usar outra JDK ao mesmo
> tempo — o sintoma é `release version XX not supported`. Confira as duas coisas:
>
> ```bash
> echo $JAVA_HOME
> java -version
> ```
>
> Se `JAVA_HOME` estiver apontando para o JBR do Android Studio, corrija na sessão:
>
> ```bash
> export JAVA_HOME="/usr/lib/jvm/jdk-25.0.4.1-oracle-x64"
> export PATH="$JAVA_HOME/bin:$PATH"
> ```

### 0.2 Subir a aplicação

```bash
cd ~/github/cloud-senai
mvn spring-boot:run
```

Alternativa (empacota e roda o jar, que é o mesmo modo usado na EC2):

```bash
mvn clean package -DskipTests
java -jar target/catalogo-1.0.0.jar
```

### 0.3 O que deve aparecer no console

```text
Starting CatalogoApplication v1.0.0 using Java 21 with PID 157151
The following 1 profile is active: "dev"
Tomcat initialized with port 8080 (http)
HHH000026: Second-level cache disabled
Initialized JPA EntityManagerFactory for persistence unit 'default'
H2 console available at '/h2-console'. Database available at 'jdbc:h2:mem:catalogo'
Tomcat started on port 8080 (http) with context path '/'
Started CatalogoApplication in 5.534 seconds (process running for 5.923)
```

Linha a linha, o que cada uma confirma:

| Linha | O que ela prova |
|---|---|
| `The following 1 profile is active: "dev"` | Está no perfil de desenvolvimento (H2). Se aparecer `prod` aqui, ele vai tentar conectar no RDS e falhar |
| `Tomcat initialized with port 8080` | O servidor web embutido subiu |
| `Initialized JPA EntityManagerFactory` | O Hibernate leu as entities e montou o mapeamento — **erro de `@Entity` aparece aqui** |
| `H2 console available at '/h2-console'` | O banco em memória está de pé |
| `Started CatalogoApplication in X seconds` | ✅ **pronto para receber requisição** |

Como o perfil `dev` tem `spring.jpa.show-sql=true`, você também vai ver o SQL que
o Hibernate gera, formatado — inclusive o `create table` da inicialização e cada
`select` das suas requisições. É ótimo para *ver* o problema N+1 acontecendo.

**Se aparecer `APPLICATION FAILED TO START`,** a causa vem logo abaixo, na seção
`Description:`. Os dois casos comuns estão na tabela do 0.7.

### 0.4 Confirmar que está no ar

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### 0.5 Swagger UI — testar pelo navegador, sem Postman

O projeto inclui o **springdoc-openapi**, que lê os controllers em tempo de
execução e gera a documentação sozinho:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.4</version>
</dependency>
```

Com a aplicação rodando:

| Endereço | O que é |
|---|---|
| http://localhost:8080/swagger-ui.html | **Interface visual** — lista os endpoints e permite executar cada um |
| http://localhost:8080/v3/api-docs | O contrato em JSON (OpenAPI 3.1), importável no Postman e no Insomnia |

Os 11 endpoints aparecem agrupados por controller. Para testar um:
**Try it out** → preencher o corpo → **Execute**. Ele mostra a URL chamada, o
corpo enviado, o status, o corpo da resposta e os headers.

**Como isso funciona:** não existe arquivo de documentação no projeto. O
springdoc varre os beans anotados com `@RestController` na inicialização, lê
`@RequestMapping`, `@PathVariable`, `@RequestBody`, o tipo dos DTOs e **as
próprias constraints do Bean Validation**, montando o esquema. Por isso o
`price` aparece com `exclusiveMinimum: 0` e o `name` como obrigatório — é o mesmo
`@Positive` e `@NotBlank` sendo lido por outro consumidor.

> **No vídeo:** o Swagger é um bônus, não substitui a demonstração pedida pela
> rubrica — o desafio pede Postman/Insomnia. Use o Swagger para dar a visão geral
> ("olha a API inteira aqui") e o Postman para a demonstração dos casos.

> **Nota de segurança para a AWS:** o Swagger UI fica público junto com a API na
> porta 8080. Para um trabalho de faculdade tudo bem, e até ajuda o professor a
> explorar. Em produção real você desligaria no perfil `prod`:
>
> ```properties
> # application-prod.properties
> springdoc.api-docs.enabled=false
> springdoc.swagger-ui.enabled=false
> ```
>
> Vale citar essa escolha no vídeo — mostra que você sabe que documentação
> exposta é superfície de reconhecimento.

### 0.6 Console do H2 — ver o banco por dentro

Só existe no perfil `dev`.

http://localhost:8080/h2-console

| Campo | Valor |
|---|---|
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:mem:catalogo` |
| User Name | `sa` |
| Password | *(vazio)* |

A **JDBC URL precisa ser digitada exatamente assim** — o valor que vem
preenchido por padrão é outro, e é o erro nº 1 de quem abre esse console pela
primeira vez.

Lá dentro dá para rodar SQL direto (`SELECT * FROM PRODUCTS;`) e conferir que o
que a API gravou chegou mesmo no banco. Útil para provar, no vídeo, que o POST
persistiu.

> Os dados vivem **em memória**: ao parar a aplicação, tudo some. É de propósito
> — cada execução começa limpa.

### 0.7 Parar e resolver problemas

Parar: `Ctrl+C` no terminal onde está rodando.

| Erro | Causa | Solução |
|---|---|---|
| `Web server failed to start. Port 8080 was already in use` | Outra execução ficou viva | `lsof -i :8080` e matar o PID, ou `--server.port=8081` |
| `release version 25 not supported` | `JAVA_HOME` apontando para JDK mais velha que o alvo do build | Ver o aviso do 0.1 |
| `Failed to configure a DataSource` | Perfil `prod` ativo sem as variáveis do banco | Rodar sem `--spring.profiles.active=prod` |
| `Reason: Failed to determine a suitable driver class` | Idem | Idem |
| `Property 'category' not found` / erro em query method | Nome de método do repository não casa com o campo | Conferir a seção 4 |
| Aplicação sobe mas `/api/...` dá 404 | Rota escrita errada | Ver a lista em `/v3/api-docs` |

### 0.8 Rodar apontando para um Postgres local (opcional)

Se quiser testar o perfil `prod` sem AWS, com Docker:

```bash
docker run --name catalogo-pg -e POSTGRES_PASSWORD=senha123 \
  -e POSTGRES_DB=catalogo -p 5432:5432 -d postgres:16

export DB_HOST=localhost DB_PORT=5432 DB_NAME=catalogo \
       DB_USER=postgres DB_PASSWORD=senha123

java -jar target/catalogo-1.0.0.jar --spring.profiles.active=prod
```

Isso valida o perfil `prod` inteiro — driver, dialeto, variáveis de ambiente —
antes de você depender da EC2 para descobrir um erro de configuração.

---

## 1. O desenho em uma frase

Requisição HTTP entra pelo **controller**, que só sabe de HTTP. Ele chama o
**service**, que é quem conhece as regras de negócio e controla a transação. O
service usa o **repository**, que só sabe ler e gravar linha no banco. O que
volta pro cliente nunca é a entidade do banco — é um **DTO**.

```
Cliente HTTP
    │  JSON
    ▼
Controller ──── @Valid barra dado inválido aqui (400)
    │  DTO de request
    ▼
Service ─────── @Transactional abre a sessão; regras de negócio moram aqui
    │  Entity
    ▼
Repository ──── proxy gerado pelo Spring Data; traduz método em SQL
    │
    ▼
Banco (H2 local / PostgreSQL RDS)
```

### Por que cada camada existe

| Camada | Responsabilidade | O que NÃO faz |
|---|---|---|
| `controller` | Traduzir HTTP ↔ objeto Java. Definir rota, verbo, status code | Não tem regra de negócio nem acesso a banco |
| `service` | Regras de negócio, controle transacional, conversão para DTO | Não conhece HTTP (nunca vê `ResponseEntity`) |
| `repository` | Persistência | Não valida, não decide nada |
| `dto` | Contrato da API (o formato do JSON) | Não é mapeado pelo JPA |
| `entity` | Mapeamento do banco | Nunca sai da aplicação |
| `exception` | Traduzir exceção de domínio em status HTTP | — |

O ganho concreto: o service diz `throw new ResourceNotFoundException(...)` sem
saber que existe um número 404 no mundo. Quem faz essa tradução é o
`GlobalExceptionHandler`, num lugar só, para todos os endpoints.

---

## 2. Entities — o mapeamento do banco

`Product` e `Category` são `class`, não `record`. Isso é obrigatório: o
Hibernate precisa de **construtor vazio**, de **mutabilidade** (para escrever os
campos ao carregar do banco) e de **herança** (para gerar proxies do
carregamento LAZY). Record nega os três.

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private int stockQuantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
```

### Decisões que valem explicar

**`BigDecimal` para dinheiro, nunca `double`.** `double` é binário: não consegue
representar `0,1` exatamente, então `100 - 79.99` dá `20.010000000000005` e dez
parcelas de R$ 0,10 não somam R$ 1,00. `BigDecimal` guarda os dígitos decimais
de verdade.

**`precision = 10, scale = 2`** vira `numeric(10,2)` no banco: até 8 dígitos
inteiros e 2 casas decimais.

**`fetch = LAZY` no `@ManyToOne`.** O padrão do JPA é `EAGER`, que carrega a
categoria junto de todo produto — 200 produtos viram 201 consultas (**problema
N+1**). Com `LAZY`, a categoria só é buscada se alguém pedir.

**Sem `@OneToMany` na Category.** A chave estrangeira já existe do lado do
produto; a lista inversa não cria nada no banco, gera recursão infinita na
serialização JSON e não pagina. O método `findByCategoryId` resolve melhor.

**UUID como chave primária.** Evita colisão e não expõe contagem de registros
(com `id` sequencial, `/products/1` revela que existem poucos produtos). Custo:
a URL fica com 36 caracteres, chata de digitar ao vivo.

---

## 3. DTOs — o contrato da API

São `record`, que o compilador expande em campos `final`, construtor canônico,
acessadores (`dto.name()`, sem `get`), `equals`, `hashCode` e `toString`.

### Por que não devolver a entity direto

Quatro motivos, todos concretos:

1. **`LazyInitializationException`.** Se o Jackson tentasse serializar um
   `Product` depois da sessão fechada, o proxy LAZY da categoria explodiria.
2. ***Mass assignment*.** Aceitando a entity na entrada, o cliente poderia mandar
   `"id": "..."` e escolher a chave primária.
3. **Hospedar a validação.** As constraints ficam no DTO, longe do mapeamento.
4. **Estabilidade do contrato.** Renomear uma coluna do banco não quebra o JSON
   dos clientes.

### O construtor compacto normaliza antes de validar

```java
public record ProductRequest(

        @NotBlank(message = "O nome do produto é obrigatório")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String name,

        @NotNull(message = "A quantidade em estoque é obrigatória")
        @PositiveOrZero(message = "O estoque não pode ser negativo")
        Integer stockQuantity,

        @NotNull(message = "O preço é obrigatório")
        @Positive(message = "O preço deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "Preço fora do formato 10,2")
        BigDecimal price,

        @NotNull(message = "A categoria é obrigatória")
        UUID categoryId

) {
    public ProductRequest {
        if (name != null) {
            name = name.trim();
        }
    }
}
```

O bloco `public ProductRequest { ... }` é o **construtor compacto**: ele roda
sobre os *parâmetros*, e o compilador insere as atribuições `this.name = name`
logo depois dele. Ou seja, o `trim()` acontece **antes** de qualquer validação —
é impossível validar um record antes de construí-lo, porque construir é o único
jeito de ele existir.

Consequência prática: `"   "` vira `""` e o `@NotBlank` pega. E `"  Caneta  "` é
gravado como `"Caneta"`, o que faz o `@Size` contar os caracteres reais e a
checagem de duplicata funcionar contra o `"Caneta"` que já está no banco.

### `Integer` em vez de `int` — a decisão menos óbvia do projeto

Com `int`, um JSON **sem** o campo `stockQuantity` faz o Jackson gravar `0`,
porque um primitivo não consegue representar "ausente". Esse `0` passa pelo
`@PositiveOrZero` (é de fato zero-ou-positivo) e o `@NotNull` não tem o que
checar (um `int` nunca é nulo). Resultado: produto entra no banco com estoque
zero que ninguém informou.

Com `Integer`, campo ausente vira `null`, o `@NotNull` acorda e devolve 400.

> `int` apaga a diferença entre "informou zero" e "não informou". Essas duas
> coisas não são a mesma coisa.

No `ProductResponse` o campo continua `int` — e está certo, porque na saída o
valor sempre existe (veio do banco).

### Como as anotações chegam onde precisam

Um record não tem corpo com campos declarados. O compilador expande cada
componente em campo, parâmetro de construtor e accessor — e **propaga a
anotação** para todos os lugares onde o `@Target` dela permite. As anotações do
Jakarta Validation permitem `FIELD`, `PARAMETER` e `METHOD`, então elas caem nos
três. Dá para conferir no bytecode:

```
javap -v -p target/classes/br/com/senai/catalogo/dto/ProductRequest.class

private final java.lang.String  name;   → RuntimeVisibleAnnotations
private final java.lang.Integer stockQuantity;  → RuntimeVisibleAnnotations
                                        → RuntimeVisibleParameterAnnotations
```

---

## 4. Repositories

```java
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByCategoryId(UUID categoryId);
    List<Product> findByNameContainingIgnoreCase(String name);
}

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
```

**Não existe implementação dessas interfaces.** Na inicialização o Spring cria um
**proxy dinâmico**: os métodos herdados vão para `SimpleJpaRepository`, e os
métodos que você declara têm o **nome traduzido em JPQL**.

`findByCategoryId` navega `category` → `id`. Se fosse `findByCategory(UUID)`, o
Spring casaria com o campo `category` (do tipo `Category`) e recusaria comparar
com `UUID` — e falharia **na partida da aplicação**, não em produção. Essa falha
antecipada é a vantagem real dos *query methods* derivados.

`existsByNameIgnoreCase` vira `upper(name) = upper(?)`, que não usa índice comum:
é **O(n)** em vez de O(log n). Irrelevante com 15 categorias.

---

## 5. Services — transação e o momento certo de converter

```java
@Transactional(readOnly = true)
public List<ProductResponse> findAll() {
    return productRepository.findAll()
            .stream()
            .map(ProductService::toResponse)
            .toList();
}
```

### O que `@Transactional` faz por baixo

O Spring não executa o seu método direto. Ele cria um **proxy** que se passa pelo
service:

```
proxy → abre transação e sessão do Hibernate
      → chama o SEU método
      → commit e fecha a sessão
```

### A regra que decide onde converter para DTO

Quando o Hibernate carrega um `Product`, ele **não** carrega a `Category`: põe
no lugar um **proxy vazio**, com o `id` e uma linha aberta com a sessão. Na
primeira chamada a `product.getCategory().getName()`, esse proxy busca o resto.

Se essa chamada acontecer **depois** da sessão fechar →
`LazyInitializationException` → erro 500.

Por isso **o service devolve DTO, nunca entity**: a conversão precisa acontecer
dentro do método transacional, enquanto a sessão está viva. Não é preferência de
estilo, é o único ponto do código onde isso funciona.

### `readOnly = true` nas leituras

Desliga o *dirty checking* — a varredura que compara cada objeto carregado com
seu estado original para descobrir o que mudou. Em leitura não há o que salvar,
então é trabalho jogado fora.

### Injeção por construtor, sem `@Autowired`

Desde o Spring 4.3, classe com um único construtor tem injeção automática. O
ganho é o `final`: a dependência vira obrigatória e imutável, e um objeto sem ela
**não compila**. Com `@Autowired` em campo, compila e falha em execução com
`NullPointerException`.

### O `update` não chama `save()` — e isso não é bug

```java
@Transactional
public CategoryResponse update(UUID id, CategoryRequest request) {
    Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + id));

    if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
        throw new DuplicateResourceException("Já existe uma categoria com o nome: " + request.name());
    }

    category.setName(request.name());
    category.setDescription(request.description());

    return toResponse(category);
}
```

Uma entity carregada dentro de um método `@Transactional` volta **gerenciada**: o
Hibernate guarda uma cópia do estado original. No commit ele compara campo a
campo e emite o `UPDATE` sozinho. É o **dirty checking** — o mesmo que
`readOnly = true` desliga. Chamar `save()` aqui seria redundante.

### `AndIdNot` — a pegadinha do update

Editar só a descrição da categoria "Ferramentas", mantendo o nome, faria o
`existsByNameIgnoreCase` acusar duplicata **da categoria contra ela mesma**. A
pergunta certa é "existe **outra** com esse nome": `existsByNameIgnoreCaseAndIdNot`.

### `deleteById` é silencioso

O Spring Data não lança nada quando o id não existe — ele simplesmente não faz
nada. Sem uma checagem explícita de existência, o `DELETE` responderia 204
dizendo "apaguei" sobre algo que nunca existiu. Daí o:

```java
if (!productRepository.existsById(id)) {
    throw new ResourceNotFoundException("Produto não encontrado: " + id);
}
```

### `findById` da categoria no `create` de produto

Não é só validação: o JPA precisa da entity `Category` de verdade para montar o
`Product` — não aceita um `UUID` solto. Validar e obter são a mesma operação.

---

## 6. Controllers

```java
@PostMapping
public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
    ProductResponse created = productService.create(request);
    URI location = URI.create("/api/products/" + created.id());
    return ResponseEntity.created(location).body(created);
}
```

**`@Valid` é a linha que ativa tudo.** Sem ela, o Spring desserializa o JSON,
ignora todas as constraints do record e segue a vida — preço negativo entra no
banco em silêncio. É o esquecimento nº 1 em API Spring.

**`201 Created` com header `Location`**, não 200. O `Location` aponta para o
recurso recém-criado, permitindo ao cliente buscá-lo sem adivinhar a URL.
Cadastro que responde 200 é erro comum.

**`204 No Content` no DELETE** — sucesso sem corpo de resposta.

**Os GETs devolvem o objeto puro, sem `ResponseEntity`.** Quando o status é
sempre 200 e não há header customizado, embrulhar só adiciona ruído.
`ResponseEntity` entra quando é preciso controlar status ou header.

**`PUT` é substituição, não remendo.** Manda o recurso inteiro; campo omitido
vira nulo, e é assim que deve ser. `PATCH` (alteração parcial) exigiria um DTO
com todos os campos opcionais e a lógica de "só altera o que veio" — ficou fora
de propósito.

**Filtro opcional por query param:**

```java
@GetMapping
public List<ProductResponse> findAll(@RequestParam(required = false) UUID categoryId) {
    if (categoryId == null) {
        return productService.findAll();
    }
    return productService.findByCategory(categoryId);
}
```

---

## 7. Tratamento de erro centralizado

Quando o método do controller lança uma exceção, ela sobe até o
**DispatcherServlet**, que consulta uma cadeia de *resolvers*. Um deles procura
um `@ExceptionHandler` que case com o **tipo** da exceção — do mais específico
para o mais genérico.

O `@RestControllerAdvice` vale para **todos** os controllers de uma vez.

| Exceção | Status | Quando acontece |
|---|---|---|
| `MethodArgumentNotValidException` | **400** | `@Valid` reprovou o corpo da requisição |
| `ResourceNotFoundException` | **404** | id não existe |
| `DuplicateResourceException` | **409** | nome de categoria repetido |
| `DataIntegrityViolationException` | **409** | violação de restrição do banco (FK, unique) |

Formato uniforme de erro:

```json
{
  "timestamp": "2026-09-06T14:27:06Z",
  "status": 400,
  "message": "Dados inválidos",
  "campos": {
    "name": "O nome do produto é obrigatório",
    "stockQuantity": "O estoque não pode ser negativo",
    "price": "O preço deve ser maior que zero"
  }
}
```

Repare que vêm **três erros de uma vez**: o Hibernate Validator coleta um
`Set<ConstraintViolation>` completo, não para no primeiro. E `campos` é `Map.of()`
(vazio) em vez de `null` nos erros que não são de validação, para o cliente não
precisar checar nulo antes de iterar.

### O handler de integridade e a condição de corrida

Duas requisições simultâneas com o nome "Ferramentas": as duas rodam o
`existsByNameIgnoreCase`, as duas recebem `false`, as duas salvam. Isso é
**TOCTOU** — *time-of-check to time-of-use*: entre checar e usar, o mundo mudou.

O `exists` **não é** a garantia — ele só dá uma mensagem decente no caso normal,
em vez de stacktrace. Quem fecha a janela é a restrição do banco, que é aplicada
de forma atômica.

#### ⚠️ Mas aqui a rede de segurança tem um buraco — e é conhecido

**A regra da aplicação é *case-insensitive*; a restrição do banco é
*case-sensitive*.** `existsByNameIgnoreCase` diz que "Bebidas" e "bebidas" são a
mesma categoria. O `unique = true` da coluna diz que são duas strings
diferentes, e aceita as duas.

Consequência: para colisões que envolvem **caixa diferente**, o banco não cobre
nada. As duas requisições passam pelo `exists`, as duas passam pelo `unique`, e
o resultado não é um 409 — são **duas categorias logicamente duplicadas gravadas
em silêncio**.

Isso foi verificado, não deduzido. Disparando 12 pares concorrentes com caixa
alternada (`Corrida1` / `CORRIDA1`, …), 2 pares gravaram os dois registros:

```text
POST "Bebidas" depois POST "bebidas"  (sequencial)   → 201 e 409   ✅ o exists pega
12 pares concorrentes com caixa alternada            → 2 pares vazaram
   corrida4 -> ['CORRIDA4', 'Corrida4']
   corrida7 -> ['CORRIDA7', 'Corrida7']
```

**Cobertura real de cada camada:**

| Cenário | `exists` pega? | `unique` do banco pega? |
|---|---|---|
| Sequencial, mesma caixa | ✅ | (nem chega lá) |
| Sequencial, caixa diferente | ✅ | (nem chega lá) |
| **Concorrente, mesma caixa** | ❌ | ✅ vira 409 |
| **Concorrente, caixa diferente** | ❌ | ❌ **duplicata entra** |

**A correção correta** seria um **índice único funcional** sobre `lower(name)`:

```sql
CREATE UNIQUE INDEX ux_categories_name_lower ON categories (lower(name));
```

Isso faz o banco aplicar exatamente a mesma regra que a aplicação. Não foi feito
aqui porque o `ddl-auto=update` do Hibernate não gera índice sobre expressão —
exigiria um script de schema versionado (Flyway/Liquibase), o que está fora do
escopo do desafio.

> **A lição que vale além deste projeto:** defesa em profundidade só funciona se
> as camadas aplicarem **a mesma regra**. Duas camadas com regras ligeiramente
> diferentes não somam proteção — elas criam uma fresta exatamente na diferença
> entre as duas. Dizer "o banco garante" sem conferir *o que exatamente* o banco
> garante é onde mora o erro.

A mensagem desse handler é **genérica de propósito** — "Operação viola uma
restrição do banco de dados". Nunca devolver detalhe interno de banco para o
cliente é decisão de segurança, não de estilo.

---

## 8. Perfis: dev e prod

| | `dev` (padrão) | `prod` |
|---|---|---|
| Banco | H2 em memória | PostgreSQL (RDS) |
| Console H2 | ligado em `/h2-console` | desligado |
| `show-sql` | `true` | `false` |
| Credenciais | `sa` sem senha | variáveis de ambiente |

```properties
# application-prod.properties
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME:catalogo}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
```

**Nenhuma credencial no código.** O perfil `prod` lê tudo do ambiente. Rodar em
produção:

```bash
java -jar catalogo.jar --spring.profiles.active=prod
```

---

## 9. Defesa em profundidade — a mesma regra em três camadas

| Camada | Onde | Para que serve |
|---|---|---|
| DTO (`@Positive`) | borda da aplicação | rejeita cedo, devolve **400** legível |
| Entity (`@Column(nullable=false)`) | mapeamento JPA | gera o DDL correto |
| Banco (`NOT NULL`, `UNIQUE`, FK) | PostgreSQL | última linha; protege até de escrita fora da API |

Por que a terceira camada não é redundante: script de migração SQL, alguém no
DBeaver, um job de carga, ou um service futuro que grave direto pelo repository —
em nenhum desses o `@Positive` do record está no caminho.

É exatamente o mesmo raciocínio dos **Security Groups** na AWS: cada camada
protege, nenhuma confia sozinha.

---

## 10. Complexidade e limites conhecidos

| Operação | Complexidade | Observação |
|---|---|---|
| `findById` | O(log n) | índice da chave primária |
| `findAll` | O(n) | varredura completa |
| `findByNameContainingIgnoreCase` | O(n) | `LIKE %x%` não usa índice comum |
| Bean Validation | O(n) constraints | metamodelo em cache; custo irrelevante |

**N+1 conhecido e aceito:** o `findAll` de produtos faz 1 consulta para os
produtos e mais 1 por categoria não-cacheada. Com poucas categorias, o cache de
primeiro nível da sessão absorve quase tudo. A solução em escala seria `@Query`
com `join fetch` — fora de escopo neste desafio, mas saber nomear o problema
vale mais do que resolvê-lo aqui.

---

## 11. Endpoints

| Método | Rota | Sucesso | Erros possíveis |
|---|---|---|---|
| `GET` | `/api/categories` | 200 | — |
| `GET` | `/api/categories/{id}` | 200 | 404 |
| `POST` | `/api/categories` | 201 + `Location` | 400, 409 |
| `PUT` | `/api/categories/{id}` | 200 | 400, 404, 409 |
| `DELETE` | `/api/categories/{id}` | 204 | 404, **409 se tiver produtos** |
| `GET` | `/api/products` | 200 | — |
| `GET` | `/api/products?categoryId={id}` | 200 | 404 |
| `GET` | `/api/products/{id}` | 200 | 404 |
| `POST` | `/api/products` | 201 + `Location` | 400, 404 |
| `PUT` | `/api/products/{id}` | 200 | 400, 404 |
| `DELETE` | `/api/products/{id}` | 204 | 404 |
| `GET` | `/actuator/health` | 200 | — |

---

## Documentos relacionados

- [[Infraestrutura na AWS]]
- [[Como Testar a API na AWS]]
- [[Roteiro do Vídeo e Justificativas]]
