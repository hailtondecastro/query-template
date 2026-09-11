# query-template

A lightweight utility that **conditionally assembles SQL/HQL queries** from a text template and binds their parameters, based on which properties of a *filter object* are filled.

It is a Java 11 port of the C# `Comuns.NHibernate.QueryUtils` library. The query is written as plain SQL/HQL with a small **token grammar** (`[...]`) that marks the parts to be included only when the related parameters are present.

- **No hard dependency on Hibernate/JPA/JDBC** — the only runtime dependency is SLF4J. You bind parameters to *any* query API through callbacks.
- No entity metamodel or code generation required.
- Works with native SQL and HQL.
- Named **and positional** (`?`) parameters.
- Fluent, interface-based configuration.
- Automatically manages `where` / `and` / `or` connectors and parentheses.
- Supports list expansion (`IN (...)`), reusable sub-queries ("query helpers"), and negated/`any` conditions.

---

## Requirements

- Java 11+
- Maven
- **Runtime dependency: only SLF4J.** The library is decoupled from any query API: you provide a callback that binds values onto your query type `Q`.
- Hibernate is used **only in the test suite** (as an example target), declared with `test` scope.

## Build

```bash
mvn clean verify
# or with the wrapper
./mvnw clean verify
```

## Coordinates

```xml
<dependency>
    <groupId>io.github.hailtondecastro</groupId>
    <artifactId>query-template</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

---

## Core concepts

| Type | Role |
|---|---|
| `QueryTemplateConfig<Q>` | Fluent, interface-based configuration: query text, property mappers, tokens, named/positional mode, query helpers. Created with `QueryTemplateConfig.of(queryText, queryClass)`. `Q` is your target query type. |
| `QueryTemplate<Q>` | Immutable engine built from a config via `QueryTemplate.of(config)`. Produces a state and binds parameters. |
| `QueryTemplateState<Q>` | Result of `buildQueryState(filter)`: exposes the final `getQueryString()` and the parameter bookkeeping. |
| `PropertyMapperConfig` (via `addMapper` / `modifyMapper`) | Maps a filter property to a `FillVerifier`, a parameter-binding callback, and flags (`unpackListItems`, `repeater`). |
| `FillVerifier` / `FillVerifiers` | Decides whether a property is considered "filled". Ready-made verifiers: `NOTNULL`, `NUMBER_NOTZERO`, `STRING_NOTEMPTY`, `COLLECTION_NOTEMPTY`, `ARRAY_NOTEMPTY`, `FRAGMENT_INCLUSION`. |
| `AssignNamedParameterDelegate<Q,P>` / `AssignPositionalParameterDelegate<Q,P>` | Your callback that actually binds a value onto `Q` — this is what decouples the library from any specific query API. |
| `FragmentInclusion` | Enum (`INCLUDE` / `DO_NOT_INCLUDE`) to include a fragment without binding any value. |
| `SimpleTypeToken<T>` | Super-type-token helper to pass generic types, e.g. `new SimpleTypeToken<Query<Employee>>(){}.getRawType()`. |
| `PropertyUtils` | Reads a bean property via its getter using reflection. |

> **No hard dependency on Hibernate/JPA/JDBC.** The engine only manipulates the query *text* and delegates the actual value binding to your callbacks, so it works with any query API.

### DSL tokens

| Token | Meaning |
|---|---|
| `[filters]` | Start of the conditionally-assembled section. |
| `[where]` | Emits `where` before the first included criterion. |
| `[and]` / `[or]` / `[no_operator]` | Connector preceding the criterion. |
| `[(]` / `[)]` | Parentheses — only rendered if they end up with content. |
| `[extra]` | Criterion included when at least one preceding parameter is filled. |
| `[repeat]` | Repeats the criterion once per element of a list parameter. |
| `[Q:key]` | Inlines another `QueryTemplate` (a "query helper"). |
| `[p1,p2,...]` | Parameter list — all must be filled for the criterion to be included. |
| `$any$` (prefix) | Include if **at least one** listed parameter is filled (instead of all). |
| `!` (prefix) | Inverts the fill test of a parameter. |

---

## Usage

`Q` is your **target query type** — anything you can bind parameters onto (a Hibernate `Query`, a JDBC `PreparedStatement`, a `Map`, ...). The library never touches it directly; your callbacks do. The examples below use Hibernate's `org.hibernate.query.Query` as the target.

### 1. Configure (fluent) and build

```java
import io.github.querytemplate.*;
import org.hibernate.query.Query;

String sql =
    "select e.* from EMPLOYEE e \n" +
    "  [filters] \n" +
    "  [where] \n" +
    "    [name][ e.name = :name] \n" +
    "    [minAge][and][ e.age >= :minAge] \n" +
    "    [extra][ e.active = 'Y']";

QueryTemplateConfig<Query<Employee>> config =
    QueryTemplateConfig.of(sql, new SimpleTypeToken<Query<Employee>>(){}.getRawType());

config
    .addMapper("name", String.class)
        .fillVerifier(FillVerifiers.STRING_NOTEMPTY)
        .parameterCallback((Query<Employee> q, String p, String v) -> q.setParameter(p, v))
        .done()
    .addMapper("minAge", Integer.class)
        .fillVerifier(FillVerifiers.NOTNULL)
        .parameterCallback((Query<Employee> q, String p, Integer v) -> q.setParameter(p, v))
        .done();

QueryTemplate<Query<Employee>> template = QueryTemplate.of(config);

EmployeeFilter filter = new EmployeeFilter();
filter.setName("john");   // filled
filter.setMinAge(null);   // not filled

QueryTemplateState<Query<Employee>> state = template.buildQueryState(filter);
String finalSql = state.getQueryString();
// -> select e.* from EMPLOYEE e  where  e.name = :name  and  e.active = 'Y'

Query<Employee> query = session.createNativeQuery(finalSql, Employee.class);
template.setParamQuery(state, query); // binds only the used & filled parameters
```

> Use explicit lambda parameter types (`String p` vs. `int p`) so the compiler picks the named vs. positional callback overload.

### 2. Positional parameters (`?`)

```java
QueryTemplateConfig<Query<Employee>> config =
    QueryTemplateConfig.of(sql, new SimpleTypeToken<Query<Employee>>(){}.getRawType())
        .convertNamedToPositionalParameters(true);

config.addMapper("name", String.class)
    .fillVerifier(FillVerifiers.STRING_NOTEMPTY)
    .parameterCallback((Query<Employee> q, int pos, String v) -> q.setParameter(pos, v))
    .done();

// finalSql uses '?' markers; setParamQuery binds by 1-based position.
```

### 3. List parameters: unpacked vs. packed

```java
// template: [ids][ e.id in (:ids)]

// (a) unpacked -> :ids expands to ":ids_0, :ids_1, ..."; bound item by item
config.addMapper("ids", new SimpleTypeToken<Collection<Long>>(){}.getRawType())
    .fillVerifier(FillVerifiers.COLLECTION_NOTEMPTY)
    .unpackListItems(true)
    .parameterCallback((Query<Employee> q, String p, Long v) -> q.setParameter(p, v))
    .done();
// -> e.id in (:ids_0, :ids_1)

// (b) packed -> keep a single :ids; bind the whole collection at once
config.modifyMapper("ids", new SimpleTypeToken<Collection<Long>>(){}.getRawType())
    .unpackListItems(false)
    .parameterCallback((Query<Employee> q, String p, Collection<Long> v) -> q.setParameterList(p, v))
    .done();
// -> e.id in (:ids)
```

### 4. Repeat a criterion per element

```java
// template: [codes][repeat][or][ e.code = :codes]
config.addMapper("codes", new SimpleTypeToken<Collection<Integer>>(){}.getRawType())
    .fillVerifier(FillVerifiers.COLLECTION_NOTEMPTY)
    .repeater(true)
    .parameterCallback((Query<Employee> q, String p, Integer v) -> q.setParameter(p, v))
    .done();
// -> e.code = :codes_0  or  e.code = :codes_1  ...
```

### 5. Reusable sub-query (query helper)

```java
String helper = "select id from EMPLOYEE e [filters] [where] [name][ e.name = :name]";

QueryTemplateConfig<Query<Employee>> config =
    QueryTemplateConfig.of("select * from ( [Q:empHelper] ) x",
        new SimpleTypeToken<Query<Employee>>(){}.getRawType());
config.addQueryHelper("empHelper", helper);
// configure the mappers (name, ...) then:
QueryTemplate<Query<Employee>> template = QueryTemplate.of(config);
```

### 6. `$any$` and negation (DSL)

```
[$any$ a,b][ (t.a = :a or t.b = :b)]   -- included if a OR b is filled
[!a][ t.a is null]                     -- included when 'a' is NOT filled
```

> Property names use Java **camelCase** (`filterPrp1`), which resolves to the getter `getFilterPrp1()` via `PropertyUtils`.

---

## How it compares to existing libraries

This library targets a specific niche: **text-based, token-driven conditional SQL/HQL** with no metamodel. Mature alternatives cover the same needs through different approaches:

| Approach | Libraries | Notes |
|---|---|---|
| Conditional fragment inclusion (closest) | **MyBatis Dynamic SQL** (`<if>/<where>/<foreach>`), **QueryDSL** `BooleanBuilder`, **Spring Data JPA Specifications** | Idiomatic conditional criteria; MyBatis is the closest to this token model. |
| Filter object → criteria | **Query by Example** (Spring Data / Hibernate) | Only filled fields become criteria; less flexible (no `or`, parentheses, sub-queries). |
| Type-safe programmatic builders | **jOOQ**, **JPA Criteria API**, **Hibernate Criteria** | Strong for complex, fully-typed SQL. |
| `IN (...)` list expansion | **Spring `NamedParameterJdbcTemplate`**, MyBatis `<foreach>`, jOOQ | Matches the `parameterList` / `[repeat]` feature. |
| SQL text templating | **JDBI** + StringTemplate/FreeMarker, MyBatis + Velocity/FreeMarker | Closest in spirit (SQL-as-template with conditional blocks). |

| `query-template` feature | Ready-made equivalent |
|---|---|
| Conditional clause inclusion | MyBatis `<if>/<where>`, QueryDSL `BooleanBuilder`, Spring Specifications |
| Auto `where`/`and`/`or` management | MyBatis `<where>/<trim>`, jOOQ, Criteria |
| `[repeat]` / list expansion | MyBatis `<foreach>`, `NamedParameterJdbcTemplate`, jOOQ |
| Filter-object driven criteria | Query by Example |
| Nested/reusable query helpers | Spring Specifications, jOOQ CTEs/subqueries |

**When to prefer alternatives**
- Already on JPA/Hibernate and want type safety → **QueryDSL** or **Spring Data Specifications**.
- Want SQL-as-text with conditional blocks and mapper XML/fluent API → **MyBatis Dynamic SQL**.
- Complex, fully type-safe native SQL → **jOOQ**.

**Where `query-template` fits**
- You have hand-written SQL/HQL (including `union`, vendor-specific syntax) and want to toggle fragments based on a filter object, **without** adding a metamodel, annotations, or a full ORM query DSL.
- You want to stay **independent of any specific query API**: the engine only rewrites the query text; the actual parameter binding is delegated to your callbacks (Hibernate, JPA, JDBC, etc.).

---

## Tests

`QueryTemplateTest` (JUnit 4 + Hamcrest) demonstrates the main scenarios: conditional clauses, parentheses, `[repeat]`, query helpers, `$any$` and negation.

```bash
mvn test
```

## License

MIT.
