# Copilot Session — 2026-09-04

Registro completo da sessão: port do utilitário C# `Comuns.NHibernate.QueryUtils` para um projeto Java 11 + Maven, seguido de uma série de renomeações e de uma refatoração maior da API.

> **Atualizado em 2026-09-11.** As seções 1–8 descrevem o port inicial e suas renomeações. A **refatoração da API** (remoção da dependência de Hibernate em runtime, parâmetros posicionais, configuração *fluent*/baseada em interface) e o trabalho posterior estão nas **seções 9 e 10**, que refletem o estado atual.

---

## 1. Objetivo original

Criar, na pasta `c:\git\github.com\hailtondecastro\query-util`, um projeto que é o **port em Java** do código C# em:

```
c:\alm\sb\cpvi\01\CONPROVI\01-Sistema\05-Implementacao\01-Aplicacao\Comuns\NHibernate\QueryUtils
```

### Requisitos
- Projeto **Java** com **Maven**.
- Versionado no **GitHub** com **CI do próprio GitHub** (GitHub Actions).
- `PreenchedorPrps.GetProperty` substituído por uma classe própria `PropertyUtils.getProperty` (getter por reflexão).
- A classe de demonstração (`Demonstracao.cs`) convertida em **teste unitário** usando `junit:4.12` e `hamcrest-all:1.3`.
- Descrições, nomes de classes e métodos traduzidos de **português para inglês** quando necessário.
- `pom.xml` baseado em `C:\git\github.com\hailtondecastro\json-playback-player-hibernate\pom.xml`.

### Ajustes de requisito durante a sessão
- **Java 11** (em vez de Java 8), usando expressões lambda onde possível.

---

## 2. Decisões de design

- **Package** final: `io.github.querytemplate` (evoluiu de `io.github.hailtondecastro.queryutil`).
- **groupId** `io.github.hailtondecastro`, **artifactId** `query-template`, **version** `0.1.0-SNAPSHOT`.
- `QueryTemplateException` estende `RuntimeException` (exceções em C# são unchecked).
- Uso de `org.hibernate.query.Query` + `org.hibernate.type.Type` (hibernate-core `5.4.5.Final`, escopo `provided`).
- Teste `QueryMock` = `java.lang.reflect.Proxy` dinâmico de `Query` registrando as chamadas `setParameter*` (evita implementar ~80 métodos).
- Parâmetros C# `ref`/`out` mapeados para *holders* `int[]` / `boolean[]` em Java.
- Regex com `java.util.regex.Pattern`; `split(..., -1)` para preservar strings vazias finais (equivalente ao `Regex.Split` do .NET).
- `LinkedHashSet` / `LinkedHashMap` para determinismo nos testes.
- Propriedades referenciadas em **camelCase** (`filterPrp1`, resolve para `getFilterPrp1`), diferente do PascalCase de C#.

---

## 3. Traduções e mapeamento de nomes (C# → Java, estado final)

### Classes / tipos
| C# original | Java (final) |
|---|---|
| `QueryMountParam` | `QueryTemplate` |
| `QueryUtilsException` | `QueryTemplateException` |
| `QueryUtilTokenTo` | `QueryTemplateTokenTo` |
| `QUTo` | `PropertyMapper` |
| `IFillVerifier` | `FillVerifier` |
| `FillVerifierByDelegate` | `FillVerifierByDelegate` |
| `FillVerifierDelegate` (delegate) | `FillVerifierDelegate` (`@FunctionalInterface`) |
| `FillVerifiers` | `FillVerifiers` |
| `ParteSQL` | `FragmentInclusion` (`INCLUDE` / `DO_NOT_INCLUDE`) |
| `PreenchedorPrps.GetProperty` | `PropertyUtils.getProperty` |
| `MeuFiltro` (teste) | `MyFilter` |
| `MinhaPropriedade` (teste) | `MyProperty` |
| `QueryMock` | `QueryMock` (proxy dinâmico) |
| `Demonstracao` | `QueryTemplateTest` (JUnit) |
| `CollectionUtil` / `SupportClass` | `CollectionUtil` |

### Enum de token: `TipoToken` → `QueryTemplateTokenTo.TokenType`
`FILTERS, WHERE, AND, OR, NO_OPERATOR, OPEN_PARENTHESIS, CLOSE_PARENTHESIS, EXTRA, PARAMETER, REPEAT, CRITERION, QUERY_HELPER`

### Métodos
| C# | Java |
|---|---|
| `MontaStrQuery` | `buildQueryString` |
| `SetParamQuery` | `setParamQuery` |
| `GetParametrosUsaveis` | `getUsableParameters` |
| `AddQueryHelper` | `addQueryHelper` |

### Constantes de FillVerifiers
`IF_NOTNULL`, `IF_NOTZERO`, `IF_FRAGMENT_INCLUSION` (era `IF_PARTE_SQL`/`IF_SQL_PART`), `IF_NOTEMPTY`, `IF_COLLECTION`, `IF_ARRAY`.

### Palavras-chave da DSL (gramática de substituição) traduzidas
- `[filtros]` → `[filters]`
- `[sem_operador]` → `[no_operator]`
- `[repetir]` → `[repeat]`
- `$algum$` → `$any$`
- (`[where]`, `[and]`, `[or]`, `[extra]`, `[(]`, `[)]`, `[Q:...]` já em inglês / neutros)

### Variáveis locais nos testes
- `qmp*` → `qt*` (`qt`, `qtCompact`, `qtHelper`)
- `List<PropertyMapper> tos` → `List<PropertyMapper> mappers`

---

## 4. Estrutura final do projeto

```
query-template/            (equivalente à pasta query-util aberta — ver seção 7)
├─ pom.xml                 (Java 11, hibernate-core provided, junit 4.12, hamcrest-all 1.3, slf4j)
├─ .gitignore
├─ .github/workflows/maven.yml   (GitHub Actions, JDK 11 temurin, mvn -B verify)
├─ .mvn/ , mvnw , mvnw.cmd        (Maven Wrapper)
└─ src/
   ├─ main/java/io/github/querytemplate/
   │   ├─ QueryTemplate.java
   │   ├─ QueryTemplateException.java
   │   ├─ QueryTemplateTokenTo.java
   │   ├─ PropertyMapper.java
   │   ├─ PropertyUtils.java
   │   ├─ FillVerifier.java
   │   ├─ FillVerifierByDelegate.java
   │   ├─ FillVerifierDelegate.java
   │   ├─ FillVerifiers.java
   │   ├─ FragmentInclusion.java
   │   └─ CollectionUtil.java
   └─ test/java/io/github/querytemplate/
       ├─ QueryTemplateTest.java   (4 testes)
       ├─ MyFilter.java
       ├─ MyProperty.java
       └─ QueryMock.java
```

### Testes (JUnit 4) — `QueryTemplateTest`
1. `buildQueryWithoutParenthesis` — cláusulas condicionais + token `[extra]` + escaping.
2. `buildQueryWithParenthesis` — parênteses aninhados + token `[repeat]` (desdobra `filterPrp7_0`, `filterPrp8_2`).
3. `buildQueryWithQueryHelper` — `[Q:...]` query helper inline.
4. `conditionedQueryHelper` — query helper condicionado a `filterPrp5` (com fallback).

**Resultado:** `Tests run: 4, Failures: 0, Errors: 0` — BUILD SUCCESS.

---

## 5. Sequência cronológica das mudanças

1. Port inicial completo (todas as classes + pom + CI + .gitignore).
2. Alterado de Java 8 → **Java 11** no pom (`release` 11).
3. Correção de convenção: propriedades da DSL em **camelCase** (`FilterPrpN` → `filterPrpN`).
4. `enum SqlPart` → `FragmentInclusion`.
5. `IF_SQL_PART` → `IF_FRAGMENT_INCLUSION`.
6. `QueryMountParam` → `QueryTemplate` (classe + arquivo + `QueryMountParamTest` → `QueryTemplateTest`).
7. Variáveis `qmp*` → `qt*`.
8. Package: `io.github.hailtondecastro.queryutil` → `io.github.queryUtil.queryutil` → `io.github.queryutil` → **`io.github.querytemplate`**.
9. `QueryUtilsException` → `QueryTemplateException`; `QueryUtilTokenTo` → `QueryTemplateTokenTo`.
10. `QUTo` → `PropertyMapper`.
11. `List<PropertyMapper> tos` → `List<PropertyMapper> mappers`.
12. pom: `artifactId`/`name`/`url`/`scm`/`description` → `query-template`.

---

## 6. Ambiente / build

- **JDK instalado:** 21 (compila com `release=11`).
- **Maven:** não instalado no PATH; baixado Apache Maven **3.9.9** portátil em `%TEMP%\qu-maven` e usado para os builds. (O repositório também tem Maven Wrapper `mvnw`.)
- Comando de validação: `mvn -B -ntp clean test` → sempre BUILD SUCCESS com 4 testes.
- Observação: os "erros" reportados pelo editor eram *code smells* do SonarLint (complexidade cognitiva, literais duplicados, falso-positivo de "secret" em `PARAM_TOKEN`), **não** erros de compilação.

---

## 7. Pendência: renomear a pasta raiz `query-util` → `query-template`

- Não foi possível fazer o **rename in-place** porque `query-util` é o **workspace aberto no VS Code** (bloqueado pelo SO: language server, file watcher, terminais com cwd dentro) **e** `query-template` já existe.
- **Workaround aplicado:** a pasta **`query-template`** foi criada/espelhada (`robocopy /MIR`, excluindo `target`) como cópia **idêntica e atualizada** de `query-util`, e o build + 4 testes passam nela.
- **Ação manual pendente para o usuário:**
  1. Abrir `C:\git\github.com\hailtondecastro\query-template` no VS Code.
  2. Apagar a antiga: `Remove-Item "C:\git\github.com\hailtondecastro\query-util" -Recurse -Force`.
- Enquanto as duas pastas coexistirem, editar **apenas** `query-template` para não divergirem.

---

## 8. Notas técnicas relevantes

- O provider de *rename* semântico do Java (JDT) não estava disponível nesta sessão; renomeações em massa (ex.: 71+ referências) foram feitas por substituição textual literal segura (identificadores distintos, sem colisão de substring).
- `PropertyUtils.getProperty(obj, "filterPrp1")` monta o getter `getFilterPrp1` (primeira letra maiúscula), então nomes de propriedade em camelCase resolvem corretamente para os getters do bean.

---

## 9. Refatoração da API (2026-09-11)

O projeto passou por uma refatoração significativa feita pelo usuário. Principais mudanças:

### 9.1. Remoção da dependência de Hibernate em runtime
- `hibernate-core` passou de `provided` para **`test`** no `pom.xml`. **Única dependência de runtime: `slf4j-api`.**
- A biblioteca ficou **desacoplada de qualquer API de query**: o binding de valores é delegado a *callbacks* fornecidos pelo consumidor. (Hibernate é usado apenas como alvo de exemplo nos testes.)

### 9.2. Suporte a parâmetros posicionais
- `QueryTemplateConfig.convertNamedToPositionalParameters(true)` converte `:nome` em marcadores `?` e faz binding por posição (1-based, configurável via `parameterBasePosition`).
- Dois delegates de binding:
  - `AssignNamedParameterDelegate<Q,P>` → `accept(Q query, String name, P value)`
  - `AssignPositionalParameterDelegate<Q,P>` → `accept(Q query, int index, P value)`

### 9.3. Configuração *fluent* e baseada em interface
- `QueryTemplateConfig<Q>` (interface) — criada com `QueryTemplateConfig.of(queryText, queryClass)`. `Q` é o tipo do objeto de query alvo (ex.: `org.hibernate.query.Query<T>`).
  - `addMapper(name, Class<P>)` → `PropertyMapperConfig<Q,P>` com `.fillVerifier(...)`, `.parameterCallback(named|positional)`, `.unpackListItems(bool)`, `.repeater(bool)`, `.done()`.
  - `modifyMapper(...)`, `removeMapper(...)`, `clearMappers()`.
  - `addQueryHelper(key, queryText)`.
  - `convertNamedToPositionalParameters`, `compactQueryText`, `targetReservedWord*`, `parameterUsagePrefix`, `parameterNamePattern`, `parameterBasePosition`, `targetItemListSeparatorMarker`, tokens customizáveis.
- `QueryTemplate<Q>` (interface) — criada com `QueryTemplate.of(config)`.
  - `buildQueryState(filter)` → `QueryTemplateState<Q>` (imutável), com `getQueryString()`.
  - `setParamQuery(state, query)` — faz o binding só dos parâmetros usados e preenchidos.
  - `getUsableParameters()`.
- `SimpleTypeToken<T>` — *super type token* para passar tipos genéricos, ex.: `new SimpleTypeToken<Query<MyEntity>>(){}.getRawType()`.

### 9.4. Novo inventário de classes (main)
`Action`, `AssignNamedParameterDelegate`, `AssignPositionalParameterDelegate`, `CollectionUtil`, `FillVerifier`, `FillVerifierByDelegate`, `FillVerifierDelegate`, `FillVerifiers`, `FragmentInclusion`, `OutputParam`, `PropertyMapper<Q,P>`, `PropertyUtils`, `QueryTemplate` (interface), `QueryTemplateConfig` (interface), `QueryTemplateConfigRoot`, `QueryTemplateConfigQueryHelper`, `QueryTemplateDefault`, `QueryTemplateException`, `QueryTemplateInternal`, `QueryTemplateState`, `QueryTemplateTokenPojo`, `SimpleTypeToken`.

Renomeações relevantes desde a §3:
- `QueryTemplateTokenTo` → **`QueryTemplateTokenPojo`**.
- `PropertyMapper` → **`PropertyMapper<Q,P>`** (agora genérico; a configuração é via `QueryTemplateConfig.PropertyMapperConfig`).
- Constantes de `FillVerifiers` perderam o prefixo `IF_`: **`NOTNULL`, `NUMBER_NOTZERO`, `STRING_NOTEMPTY`, `COLLECTION_NOTEMPTY`, `ARRAY_NOTEMPTY`, `FRAGMENT_INCLUSION`**.
- Holders C# `ref`/`out` → **`OutputParam<T>`** (substituindo os `int[]`/`boolean[]`).

### 9.5. Testes (agora 6) — `QueryTemplateTest` + `MyEntity`
1. `buildQueryWithoutParenthesisPrp4Unpacked` — lista "unpacked" → `:filterPrp4_0, :filterPrp4_1`.
2. `buildQueryWithoutParenthesisPrp4Packed` — lista "packed" → `:filterPrp4` (setParameterList).
3. `buildQueryWithoutParenthesisPositional` — parâmetros posicionais `?` + binding por posição.
4. `buildQueryWithParenthesis` — parênteses aninhados + `[repeat]`.
5. `buildQueryWithQueryHelper` — `[Q:...]` inline.
6. `conditionedQueryHelper` — helper condicionado a `filterPrp5` (com fallback).
- `QueryMock<T>` agora é genérico.

---

## 10. Documentação e ajustes finais

- **`README.md`** criado e atualizado para a nova API (fluent, sem dependência de Hibernate em runtime, parâmetros posicionais, comparativo com MyBatis Dynamic SQL / QueryDSL / jOOQ / Spring Specifications / Query by Example). Sincronizado nas duas pastas.
- **Limpeza de termos aportuguesados** ("scape"): em `QueryTemplateDefault.java` → `queryTextScapedSubs`→`queryTextEscapedSubs`, `mapScapes`→`escapeMap`, `chrScaped`→`escapedChar`, valor `_PREF_TEMP_SCP_`→`_PREF_TEMP_ESC_`. Sem caracteres acentuados no `src`. (Mantido apenas o Javadoc que referencia o nome original C# `PreenchedorPrps.GetProperty`.)
- **Correção de teste**: em `buildQueryWithParenthesis`, `QueryMock` cru → `QueryMock<MyEntity>` (erro de inferência de generics do Hamcrest).
- **Build atual:** `mvn clean test` → **Tests run: 6, Failures: 0, Errors: 0 — BUILD SUCCESS**.
- As duas pastas (`query-util` aberta e `query-template` espelhada) continuam sincronizadas; a pendência de renomear a raiz (seção 7) permanece.
