# Roteiro do Vídeo e Justificativas

O que os outros três documentos não cobrem: como a entrega é avaliada, o roteiro
da gravação e as respostas prontas para as perguntas de arquitetura e segurança.

---

## 1. Como a nota é distribuída

| Peso | Item | Onde isso é conquistado |
|---|---|---|
| **40%** | Implementação | A aplicação **rodando na AWS**, respondendo de verdade |
| **30%** | Explicação da arquitetura | O que você **fala** sobre as decisões |
| **30%** | Apresentação em vídeo | Clareza, ritmo, organização |

**A leitura que muda a prioridade:** 60% da nota é você **explicando**, não
código existindo. Uma API mais simples, bem explicada, vale mais do que uma API
enorme demonstrada às pressas.

Requisitos mínimos do desafio, e onde cada um está atendido:

| Requisito | Status |
|---|---|
| Serviço de computação AWS (preferencialmente EC2) | EC2 t3.micro, Amazon Linux 2023 |
| API REST pública, mínimo 1 `GET` e 1 `POST` | 11 endpoints (CRUD completo dos dois recursos) |
| Troca de mensagens em JSON | Request e response em JSON |
| Security Groups configurados + justificativa | Dois SGs isolados por camada |
| Demonstração via Postman/Insomnia contra a AWS | Collection pronta |
| Esquema da arquitetura | Diagrama em [[Infraestrutura na AWS]] |
| Análise das decisões de arquitetura e segurança | Este documento |
| *Opcional:* banco integrado | RDS PostgreSQL — POST grava, GET recupera |

### Forma de entrega (texto do PDF)

**Só o vídeo.** YouTube não listado, ou link público direto num serviço de
armazenamento em nuvem.

> "Somente serão consideradas as entregas realizadas pelo AVA contendo o **link
> direto para o vídeo** (não enviar links para pastas). **Não é necessário
> entregar relatório ou apresentação em slides.**"

Três consequências práticas:

- **O repositório não é entregável.** Ele é ferramenta de trabalho. Não vale
  gastar tempo organizando commits ou escrevendo README para "apresentar o
  código" — o que é avaliado é o que aparece no vídeo.
- **Link do arquivo, não da pasta.** Se subir no Drive, gere o link do vídeo em
  si. Link de pasta é motivo declarado de não consideração da entrega.
- **Grupo de até 3.** É preciso entrar em um grupo no AVA **mesmo fazendo
  sozinha**, senão não há como submeter.

### O que o PDF exige que apareça no vídeo

Checklist literal da seção *Forma de entrega* — cada item precisa estar visível
ou dito em voz alta:

- [ ] O **cenário de aplicação** escolhido (catálogo de produtos)
- [ ] A **infraestrutura implantada na AWS**, identificando os serviços usados
- [ ] A configuração do **serviço de computação e dos grupos de segurança**
- [ ] O **console da AWS** evidenciando os recursos (EC2, Security Groups, RDS)
- [ ] A **demonstração da API** com Postman/Insomnia, deixando claro que as
      requisições vão **para a aplicação na AWS** — e não para `localhost`
- [ ] O uso de **JSON** na troca de mensagens
- [ ] O funcionamento da **integração com o banco de dados**
- [ ] As **principais dificuldades** encontradas no desenvolvimento e na
      implantação
- [ ] A **justificativa técnica** das decisões de arquitetura, escolha de
      serviços AWS e configurações de segurança

> ⚠️ **O item da URL é uma armadilha fácil.** O PDF pede explicitamente
> demonstrar "que as requisições são realizadas diretamente na aplicação
> implantada na AWS". Deixe a barra de URL do Postman visível com o IP público
> aparecendo — se a demonstração for gravada contra `localhost`, ela não prova o
> que precisa provar, por mais que a API funcione.

---

## 2. Roteiro sugerido (8 a 10 minutos)

### Bloco 1 — Abertura (30s)

Cenário: catálogo de produtos com categorias. Stack: Java 21 + Spring Boot 3.4.5,
JPA/Hibernate, PostgreSQL no RDS, aplicação na EC2.

### Bloco 2 — Arquitetura (2 min) · *aqui moram os 30%*

Mostre o diagrama e percorra o caminho de uma requisição:

> "O cliente bate na porta 8080 da EC2, que é a única porta aberta para a
> internet. A aplicação Spring Boot recebe, valida, e abre uma conexão JDBC com o
> RDS na porta 5432. O banco **não** tem endereço público: ele só aceita conexão
> de quem está no Security Group da aplicação."

Depois as camadas do código (30 segundos, sem entrar em arquivo):

> "Controller só sabe de HTTP. Service tem as regras de negócio e controla a
> transação. Repository só lê e grava. E o que sai para o cliente nunca é a
> entidade do banco, é um DTO — o contrato da API é independente do formato das
> tabelas."

### Bloco 3 — Segurança (2 min) · *o resto dos 30%*

Abra o console AWS nos dois Security Groups e diga, apontando:

> "Princípio do menor privilégio. A porta 8080 está aberta para todos porque a
> API **precisa** ser pública — é requisito. Mas o SSH, na 22, só aceita o IP da
> minha máquina. E o banco: repare que a origem da regra não é um bloco de IP, é
> **o Security Group da aplicação**. Isso significa que só quem está naquele
> grupo consegue falar com o banco — se a EC2 for recriada e mudar de IP, a regra
> continua valendo, porque a identidade é o grupo, não o endereço."

> "Além disso o RDS está com *Publicly accessible* em **No**. São duas camadas
> independentes: mesmo que eu errasse o Security Group, o banco continuaria sem
> rota vinda da internet."

> "As credenciais não estão no código. O perfil `prod` lê host, usuário e senha
> de variáveis de ambiente, num arquivo com permissão 600 na instância. E a EC2
> **não tem nenhuma IAM role anexada** — a aplicação não chama serviço nenhum da
> AWS, então credencial que não existe não vaza."

### Bloco 4 — Demonstração (3-4 min)

Sequência enxuta (o detalhe de cada requisição está em
[[Como Testar a API na AWS]]):

1. `GET /actuator/health` → prova que está no ar **na AWS**
2. `POST /api/categories` → **201** + header `Location`
3. `POST /api/products` → **201**, categoria aninhada na resposta
4. `GET /api/products` → o dado voltando do RDS
5. `POST` com nome vazio, estoque -3 e preço -10 → **400 com os três erros**
6. `POST` sem `stockQuantity` → **400** (a diferença entre zero e não informado)
7. `GET` de id inexistente → **404**
8. `POST` de categoria repetida → **409**
9. `DELETE` de categoria com produtos → **409** ⭐

Enquanto mostra o **⑤**:

> "Repare que voltaram os três erros de uma vez, não só o primeiro. O validador
> percorre todas as restrições e devolve o conjunto completo — quem consome a API
> corrige o formulário inteiro numa tentativa só."

Enquanto mostra o **⑨** (a melhor cena):

> "Tentei apagar uma categoria que tem produtos. O banco recusou por causa da
> chave estrangeira, e a aplicação traduziu isso em 409 com uma mensagem
> genérica. Duas coisas aqui: a integridade é garantida pelo **banco**, não só
> pela aplicação; e o cliente não recebe detalhe interno do Hibernate, porque
> mensagem de erro também é superfície de ataque."

### Bloco 5 — Dificuldades encontradas (1 min) · **obrigatório**

⚠️ **Este bloco é exigido explicitamente pelo PDF**, na lista do que o vídeo
deve apresentar: *"as principais dificuldades encontradas durante o
desenvolvimento e a implantação da solução"*. Não é opcional e não é o mesmo que
"limitações do código" — é o relato do que deu errado no caminho.

Material real desta implementação, é só escolher duas ou três:

| Dificuldade | O que ensinou |
|---|---|
| `release version 25 not supported` no build | `JAVA_HOME` é apontador, não instalação. O Maven obedece `JAVA_HOME`, o terminal obedece o `PATH` — dava para o `java -version` mostrar 25 e o build usar outra JDK. Resolvido baixando o alvo para Java 21, que é o Corretto padrão do Amazon Linux |
| `findBycategory` derrubando a aplicação na partida | O Spring Data traduz o **nome do método** em consulta. Faltou o `Id`: `findByCategoryId` navega `category` → `id`. Falhou na partida, não em produção — vantagem real dos query methods derivados |
| Escolher entre `int` e `Integer` no DTO | Com `int`, um JSON sem o campo virava `0` e passava pela validação. O primitivo apaga a diferença entre "estoque zero" e "não informado" |
| Onde converter entidade em DTO | Converter fora do método transacional estoura `LazyInitializationException`, porque o proxy da categoria precisa da sessão aberta |
| Editar categoria acusando conflito com ela mesma | A checagem de duplicata precisava perguntar "existe **outra** com esse nome", não "existe esse nome" |

E se algo der errado na própria AWS — Security Group, endpoint do RDS, sessão do
lab expirando — **anote na hora**. Dificuldade de implantação vale tanto quanto
dificuldade de código, e o PDF pede as duas.

### Bloco 6 — Fechamento (30s)

Uma limitação conhecida e o próximo passo — isso demonstra maturidade, não
fraqueza:

> "Sei que a listagem de produtos tem um problema N+1 em escala, resolvido com
> `join fetch`. Com o volume deste catálogo o cache da sessão absorve, então
> preferi manter o código simples. E o próximo passo natural seria colocar a
> aplicação atrás de um Load Balancer com HTTPS, tirando a porta 8080 da
> internet."

---

## 3. Perguntas prováveis — respostas prontas

**"Por que EC2 e não Lambda/Elastic Beanstalk?"**
A EC2 dá controle total sobre o ambiente e deixa a configuração de rede
explícita — que é justamente o que o desafio pede para justificar. Lambda exigiria
adaptar a aplicação ao modelo de função e traria *cold start* numa aplicação
Spring Boot.

**"Por que RDS e não um banco na própria EC2?"**
Separar as camadas permite aplicar regras de rede diferentes a cada uma. Com o
banco na mesma máquina, qualquer comprometimento da aplicação seria também
comprometimento do banco. Além disso o RDS cuida de backup e patch.

**"A porta 8080 aberta para todos não é inseguro?"**
É exposição consciente e mínima. A API precisa ser pública por requisito, e essa
é a **única** porta aberta — a 22 é restrita ao meu IP e a 5432 não aceita nada
vindo de fora do grupo da aplicação. Em produção real eu poria um Load Balancer
com HTTPS na frente e tiraria a 8080 da internet.

**"Por que UUID em vez de id sequencial?"**
Evita colisão e não expõe contagem de registros — com id sequencial, `/products/1`
já revela que o catálogo é pequeno. O custo é URL mais longa.

**"Por que `BigDecimal` e não `double` para preço?"**
`double` é binário e não representa `0,1` exatamente: `100 - 79.99` dá
`20.010000000000005`. Para dinheiro isso é inaceitável.

**"Onde fica a validação e por que ali?"**
No DTO, na borda da aplicação — falha rápido e barato, e devolve 400 com mensagem
legível. Mas a restrição continua no banco também, porque a aplicação não é o
único caminho até os dados.

**"O que acontece se duas pessoas cadastrarem a mesma categoria ao mesmo tempo?"**
A checagem prévia tem uma janela de corrida — as duas podem passar. Se mandarem
o nome **com a mesma grafia**, quem barra é o `unique` da coluna, aplicado de
forma atômica pelo banco: a segunda falha e vira 409.

Mas tem um caso que **não** está coberto, e eu conheço: minha regra é
*case-insensitive* e a restrição do banco é *case-sensitive*. Se as duas
requisições concorrentes mandarem "Bebidas" e "bebidas", as duas passam pelo
`exists` e as duas passam pelo `unique`, porque para o banco são strings
diferentes. A correção seria um índice único funcional sobre `lower(name)`, que
exigiria um script de schema versionado — fora do escopo deste desafio.

> **Não fuja dessa pergunta se ela vier.** Reconhecer o buraco, saber *por que*
> ele existe e saber qual seria a correção vale mais do que uma resposta redonda
> e errada. E dá para amarrar no princípio: defesa em profundidade só soma se as
> camadas aplicarem **a mesma regra** — duas camadas com regras ligeiramente
> diferentes abrem uma fresta exatamente na diferença entre elas.

**"Como as credenciais chegam na aplicação?"**
Variáveis de ambiente lidas pelo perfil `prod`, a partir de um arquivo com
permissão 600 na instância. Nada no repositório. O passo seguinte seria o AWS
Secrets Manager.

---

## 4. Checklist da entrega

**Antes de gravar** — o detalhado está em [[Como Testar a API na AWS]]:

- [ ] Sessão do Learner Lab recém-iniciada (expira em ~4h)
- [ ] `GET /actuator/health` respondendo **de fora** da EC2
- [ ] Postman com o IP atual e dados de exemplo já criados
- [ ] Uma categoria com produtos guardada para a cena do 409
- [ ] Console AWS aberto: EC2, RDS, os dois Security Groups
- [ ] Diagrama da arquitetura aberto em outra aba
- [ ] Notificações do sistema silenciadas; nada de senha visível na tela

**Depois de gravar:**

- [ ] Vídeo no YouTube como **não listado**, ou link direto em nuvem
- [ ] Link testado numa janela anônima (o erro clássico é subir privado)
- [ ] Postado no AVA, dentro do grupo
- [ ] Recursos AWS parados ou destruídos

---

## 5. Riscos e planos B

| Risco | Prevenção | Plano B |
|---|---|---|
| Sessão do lab expira no meio | Começar sessão nova antes de gravar | Gravar em blocos e editar |
| IP da EC2 muda | Conferir antes de gravar | Atualizar o environment do Postman |
| Aplicação cai durante a gravação | `Restart=on-failure` no systemd | `sudo systemctl restart catalogo` |
| RDS demora a ficar disponível | Criar o banco **antes** da EC2 | Gravar os blocos 1-3 enquanto provisiona |
| Internet cai na demonstração | — | Ter uma gravação local de reserva, deixando claro que é reserva |
| Crédito do lab acaba | Desligar recursos ao terminar | — |

---

## 6. Registro de decisões técnicas

Decisões tomadas ao longo do desenvolvimento, com o motivo — útil se alguém do
grupo perguntar "por que assim?".

| Decisão | Motivo |
|---|---|
| Duas entidades apenas (Produto + Categoria) | Entidade extra não soma na rubrica e soma risco |
| Sem Lombok | *Annotation processor* quebra no editor sem plugin; `@Data` + JPA gera recursão no `toString` |
| Entity = `class`, DTO = `record` | Hibernate exige construtor vazio, mutabilidade e herança; record nega os três |
| `fetch = LAZY` no `@ManyToOne` | O padrão `EAGER` causa N+1 |
| Sem `@OneToMany` na Category | Não cria nada no banco, quebra o JSON com recursão e não pagina |
| Quantidade é campo, não entidade `Estoque` | Estoque vira tabela só quando se precisa de histórico de movimentação |
| `Integer` em vez de `int` no request | Preserva a diferença entre "zero" e "não informado" |
| Service devolve DTO, nunca entity | Único ponto em que a sessão do Hibernate ainda está aberta |
| `PUT` completo, sem `PATCH` | `PATCH` exigiria DTO com todos os campos opcionais; fora de escopo |
| Compilar para Java 21, não 25 | Corretto 21 é o padrão do Amazon Linux 2023; nada no código usa recurso acima do 17 |
| Sem `equals`/`hashCode` nas entities | Nenhuma entity é usada em `Set`/`Map`. Implementar pelo id geraria bug: o id é `null` antes do `save()`, então o objeto "sumiria" de um `HashSet` depois de persistido |
| Swagger via springdoc | Documentação gerada dos próprios controllers e das constraints — não há arquivo a manter desatualizado. Ligado também em `prod` por ser trabalho acadêmico |
| `ddl-auto=update` | Suficiente para o desafio; em produção real seria Flyway/Liquibase |

---

## Documentos relacionados

- [[Como o Código Funciona]]
- [[Infraestrutura na AWS]]
- [[Como Testar a API na AWS]]
