# query-template

A lightweight utility that **conditionally assembles SQL/HQL queries** from a text template and binds their parameters, based on which properties of a *filter object* are participating in the query.

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
    <version>0.1.3</version>
</dependency>
```

---

## Core concepts

| Type                                                                                | Role                                                                                                                                                                                                                    |
|-------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `QueryTemplateConfig`                                                               | Fluent, interface-based configuration: query text, property mappers, tokens, named/positional mode, query helpers. Created with `QueryTemplateConfig.of(queryText, queryClass)`. `Q` is your target query type.         |
| `QueryTemplate`                                                                     | Immutable engine built from a config via `QueryTemplate.of(config)`. Produces a state and binds parameters.                                                                                                             |
| `QueryTemplateState`                                                                | Result of `buildQueryState(filter)`: exposes the final `getQueryString()` and the property/parameter bookkeeping.                                                                                                       |
| `PropertyMapperConfig` (via `addMapper` / `modifyMapper`)                           | Maps a filter property to a `ParticipatesInQuery` and named/positional binding callbacks; configures one or more parameter mappers.                                                                                   |
| `ParameterMapperConfig` (via `PropertyMapperConfig.addParameter` / `modifyParameter`) | Configures a query parameter name and its behavior, including `unpackListItems(boolean)` and `repeater(boolean)`.                                                                                                   |
| `ParticipatesInQuery`                                                               | Decides whether a property is considered "participating in the query".                                                                                                                                                  |
| `ParticipantCheckers`                                                               | Ready-made `ParticipatesInQuery` implementations: `NOTNULL`, `NUMBER_NOTZERO`, `STRING_NOTEMPTY`, `COLLECTION_NOTEMPTY`, `ARRAY_NOTEMPTY`, `FRAGMENT_INCLUSION`, `BOOLEAN_TRUE`.                                        |
| `AssignNamedParameter` / `AssignPositionalParameter`                                | Four-argument callbacks that bind a value onto `Q`; both receive `AssignedParameterInfo` with parameter metadata.                                                                                                     |
| `FragmentInclusion`                                                                 | Enum (`INCLUDE` / `DO_NOT_INCLUDE`) to include a fragment without binding any value.                                                                                                                                    |
| `SimpleTypeToken`                                                                   | Super-type-token helper to pass generic types, e.g. `new SimpleTypeToken<Query<Employee>>(){}.getRawType()`.                                                                                                            |
| `PropertyUtils`                                                                     | Reads a bean property via its getter using reflection.                                                                                                                                                                  |
| `AssignedParameterInfo<P>`                                                           | Metadata supplied to either callback: declared parameter name, expanded/repeated name, list index, and query position where available.                                                                               |
| `EvalRunner` (via `QueryTemplateConfig.evalRunnerCreator(Function)`)                | Adapter to a script engine used by `$eval$`. The application must configure a creator; no scripting engine is enabled by default.                                                                                     |
| `JSR233EvalRunner` / `JSR233EvalRunnerCreator`                                      | Optional JSR-223 adapter and reusable creator. The application supplies a compatible `ScriptEngine`; compiled mode requires an engine implementing `Compilable`.                                                     |

> **No hard dependency on Hibernate/JPA/JDBC or a scripting engine.** The engine manipulates query text and delegates value binding to callbacks. `$eval$` additionally requires an `EvalRunner` and a separately supplied script engine.

### DSL tokens

|  Token                         |  Meaning                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|  [filters]                     |  Start of the conditionally-assembled section.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|  [where]                       |  Emits where before the first included criterion.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|  [and] / [or] / [no_operator]  |  Connector preceding the criterion.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|  [(] / [)]                     |  Parentheses — only rendered if they end up with content.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|  [extra]                       |  Criterion included when at least one preceding parameter is participating in the query.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|  [repeat]                      |  Repeats the criterion once per element of a list parameter.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|  [Q:key]                       |  Inlines another QueryTemplate (a "query helper").                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|  [p1,p2,...]                   |  Property list — all must be participating in the query for the criterion to be included.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|  `$any$` (prefix)              |  Implies that if at least one listed property is participating in the query (instead of all)  the criterion will be included.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|  `$eval$` (prefix)             |  Evaluates the expression with the configured `EvalRunner`; include the criterion when it returns `Boolean.TRUE`. `pp`/`prpParticipations` are aliases for a `Map<String, Boolean>` of mapped property names to participation results; `pv`/`prpValues` are aliases for a `Map<String, Object>` of mapped property names to filter values. Map access syntax depends on the chosen engine/adapter. |
|  ! (prefix)                    |  Inverts the 'participating in the query' test of a parameter.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
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
        .participatesInQuery(ParticipantCheckers.STRING_NOTEMPTY)
        .onParticipatesNamed((Query<Employee> q, String p, String v, AssignedParameterInfo<String> info) -> q.setParameter(p, v))
        .done()
    .addMapper("minAge", Integer.class)
        .participatesInQuery(ParticipantCheckers.NOTNULL)
        .onParticipatesNamed((Query<Employee> q, String p, Integer v, AssignedParameterInfo<Integer> info) -> q.setParameter(p, v))
        .done();

QueryTemplate<Query<Employee>> template = QueryTemplate.of(config);

EmployeeFilter filter = new EmployeeFilter();
filter.setName("john");   // participating in the query
filter.setMinAge(null);   // not participating in the query

QueryTemplateState<Query<Employee>> state = template.buildQueryState(filter);
String finalSql = state.getQueryString();
// -> select e.* from EMPLOYEE e  where  e.name = :name  and  e.active = 'Y'

Query<Employee> query = session.createNativeQuery(finalSql, Employee.class);
template.setParamQuery(state, query); // binds only the used & participating in the query parameters
```

> Both callback interfaces receive four arguments. Use explicit lambda parameter types (`String name` for named, `int position` for positional) to disambiguate the `onParticipates...` overloads.

### 2. Positional parameters (`?`)

```java
QueryTemplateConfig<Query<Employee>> config =
    QueryTemplateConfig.of(sql, new SimpleTypeToken<Query<Employee>>(){}.getRawType())
        .convertNamedToPositionalParameters(true);

config.addMapper("name", String.class)
    .participatesInQuery(ParticipantCheckers.STRING_NOTEMPTY)
    .onParticipatesPositional((Query<Employee> q, int pos, String v, AssignedParameterInfo<String> pi) -> q.setParameter(pos, v))
    .done();

// finalSql uses '?' markers. Positions start at 1 by default; configure
// parameterBasePosition(int) if the target API uses a different base index.
```

### 3. List parameters: unpacked vs. packed

When items are unpacked, or a criterion is repeated, the callback receives one item per invocation. The mapper's `Class<P>` controls the callback's compile-time value type, so use `Object.class` for item callbacks; for packed lists, map the property as `Collection<Long>` because the callback receives the collection.

```java
// template: [ids][ e.id in (:ids)]

// (a) unpacked -> :ids expands to ":ids_0, :ids_1, ..."; bound item by item
config.addMapper("ids", Object.class)
    .participatesInQuery(ParticipantCheckers.COLLECTION_NOTEMPTY)
    .addParameter().unpackListItems(true).done()
    .onParticipatesNamed((Query<Employee> q, String p, Object item, AssignedParameterInfo<Object> info) -> q.setParameter(p, item))
    .done();
// -> e.id in (:ids_0, :ids_1)

// (b) packed -> keep a single :ids; bind the whole collection at once
config.modifyMapper("ids", new SimpleTypeToken<Collection<Long>>(){}.getRawType())
    .onParticipatesNamed((Query<Employee> q, String p, Collection<Long> v, AssignedParameterInfo<Collection<Long>> info) -> q.setParameterList(p, v))
    // The mapper and its parameter were configured in the unpacked example above.
    .modifyParameter().unpackListItems(false).done()
    .done();
// -> e.id in (:ids)
```

### 4. Repeat a criterion per element

As with unpacked lists, the callback is invoked once per item; the filter property itself remains a collection.

```java
// template: [codes][repeat][or][ e.code = :codes]
config.addMapper("codes", Object.class)
    .participatesInQuery(ParticipantCheckers.COLLECTION_NOTEMPTY)
    .addParameter().repeater(true).done()
    .onParticipatesNamed((Query<Employee> q, String p, Object item, AssignedParameterInfo<Object> info) -> q.setParameter(p, item))
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

```text
[$any$ a,b][ (t.a = :a or t.b = :b)]   -- included if a OR b participates
[!a][ t.a is null]                     -- included when 'a' does not participate
```


### 7. `$eval$` and script-based conditions
You can use the `$eval$` prefix to include a criterion based on a script or expression. It is evaluated by the `EvalRunner` configured through `QueryTemplateConfig.evalRunnerCreator(Function)`. The application supplies the runner and any required scripting engine; no engine is enabled by default.
The expression must return a Java `Boolean`; `null` is treated as `false`. Other result types are not converted automatically. `undefined` behavior is engine-specific and is not guaranteed by this library.
The runner is created from the current `QueryTemplateState` and receives these bindings for each mapped filter property:
- `pp` and `prpParticipations`: aliases for a `Map<String, Boolean>` of property names to the results of their `ParticipatesInQuery` checks.
- `pv` and `prpValues`: aliases for a `Map<String, Object>` of property names to their values in the filter.

The engine/adapter determines how to access map entries. For example, JavaScript-like engines may support `pp.jobTitle`; plain BeanShell uses `pp{"jobTitle"}` unless adapted. Only mapped filter properties are included in these maps. Configure the runner with `evalRunnerCreator(...)` and supply the engine dependency yourself. Treat templates/expressions as trusted code; do not evaluate untrusted scripts.

#### `JSR233EvalRunnerCreator`
Reusable `Function<QueryTemplateState<?>, EvalRunner>` that creates `JSR233EvalRunner` instances. It is not a singleton enforced by the class; reuse one creator when compiled-script caching is desired.
This allows you to create an `EvalRunner` that can evaluate scripts using the JSR-233 scripting API, providing a flexible way to include dynamic conditions in your query templates.

#### Example with a JSR-223 Groovy engine

The JSR-223 API is part of Java 11+, but a JavaScript engine is not guaranteed to be present. Nashorn ships with JDK 11 but was removed from later JDKs; GraalJS and Groovy require their own engine dependencies. The Groovy dependency below is one option.

```xml
<dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy-jsr223</artifactId>
    <version>4.0.24</version>
</dependency>
```
```java
    // If you are using spring this can be injected as a bean instead of manually creating it in the setUp method.
    private JSR233EvalRunnerCreator groovyEvalRunnerCreatorCompiled;

    private void setUp() {
        this.groovyEvalRunnerCreatorCompiled = 
        		new JSR233EvalRunnerCreator(
        				this.scriptEngineManager.getEngineByName("groovy"),
        				JSR233EvalRunnerCreator.JSR233ScriptMode.COMPILED);
    }
```
```java
String query = 
           "select free.att1 as empl.id, free.att2 as empl.name, free.att3 as empl.department, 'I' as empl.category \n" +
            "   from FREELANCERTB free  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [$eval$ pp.jobTitle && pp.departmentId && pp.managerId][   free.job_title = :jobTitle]  \n" +
            "     [$eval$ pp.jobTitle && (pp.departmentId || pp.managerId)][ ( free.job_title = :jobTitle and (free.department_Id = :departmentId or free.manager_id = :managerId) )]  \n" +
            "     [$eval$ !pp.jobTitle && !pp.managerId][ (free.job_title is null and free.manager_id is null)]  ";

config
    .evalRunnerCreator(this.groovyEvalRunnerCreatorCompiled)
    .addMapper("jobTitle", Integer.class)
        .participatesInQuery(ParticipantCheckers.NOTNULL)
        .onParticipatesNamed((Query<Employee> q, String p, Integer v, AssignedParameterInfo<Integer> info) -> q.setParameter(p, v))
        .addParameter("job_title").done()
    .addMapper("departmentId", Integer.class)
        .participatesInQuery(ParticipantCheckers.NOTNULL)
        .onParticipatesNamed((Query<Employee> q, String p, Integer v, AssignedParameterInfo<Integer> info) -> q.setParameter(p, v))
        .addParameter("department_id").done()
    .addMapper("managerId", Integer.class)
        .participatesInQuery(ParticipantCheckers.NOTNULL)
        .onParticipatesNamed((Query<Employee> q, String p, Integer v, AssignedParameterInfo<Integer> info) -> q.setParameter(p, v))
        .addParameter("manager_id").done()
    .done();
```

#### Example of `$eval$` using pure BeanShell :
```xml
<dependency>
    <groupId>org.apache-extras.beanshell</groupId>
    <artifactId>bsh</artifactId>
    <version>2.0b6</version>
</dependency>
```
```java
    /**
     * Pure BeanShell interpreter, it does not support javascript like field access to map values, e.g.:
     * <pre>
     * $eval$ pp.jobTitle && pp.departmentId && pp.managerId
     * </pre>
     * need to be modified to:
     * <pre>
     * $eval$ pp{"jobTitle"} && pp{"departmentId"} && pp{"managerId"}
     * </pre>
     * References:
     *   <a href="https://beanshell.org/manual/bshmanual.html#set(),%20get(),%20and%20unset():~:text=Equivalent%20to%3A%20h.put(}">Maps, BeanShell</a>
     */
    public static final class PureBeanShellEvalRunner implements EvalRunner {
        Interpreter	interpreter;
        public PureBeanShellEvalRunner() {
            interpreter = new Interpreter();
        }
        @Override
        public <Q> Object binding(QueryTemplateState<Q> preliminarState,
            String name,
            Object value) throws Throwable {
            interpreter.set(name, value);
            return interpreter.get(name);
        }

        @Override
        public <Q> void clearBindings(QueryTemplateState<Q> preliminarState) throws Throwable {
            interpreter.getNameSpace().clear();
        }

        @Override
        public <Q> Object eval(QueryTemplateState<Q> preliminarState,
            String script) throws Throwable {
            return interpreter.eval(script);
        }
    }
```
```java
String query = 
           "select free.att1 as empl.id, free.att2 as empl.name, free.att3 as empl.department, 'I' as empl.category \n" +
            "   from FREELANCERTB free  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [$eval$ pp{\"jobTitle\"} && pp{\"departmentId\"} && pp{\"managerId\"}][   free.job_title = :jobTitle]  \n" +
            "     [$eval$ pp{\"jobTitle\"} && (pp{\"departmentId\"} || pp{\"managerId\"})][ ( free.job_title = :jobTitle and (free.department_Id = :departmentId or free.manager_id = :managerId) )]  \n" +
            "     [$eval$ !pp{\"jobTitle\"} && !pp{\"managerId\"}][ (free.job_title is null and free.manager_id is null)]  ";

config
    .evalRunnerCreator(state -> new ScriptEngineEvalRunner(this.scriptEngineManager, ScriptEngineEvalRunnerSupport.GENERIC))
    .addMapper("jobTitle", Integer.class)
    ...
```

#### Example of `$eval$` using adapted BeanShell :
```java
    ...
    /**
     * Adapted BeanShell interpreter to support javascript like field access to map values, e.g.:
     * <pre>
     * $eval$ pp.filterPrp1 && pp.filterPrp2 && pp.filterPrp3
     * </pre>
     * References:
     *   <a href="https://beanshell.org/manual/bshmanual.html#set(),%20get(),%20and%20unset():~:text=set()%2C%20get()%2C%20and%20unset()">set(), get(), and unset()</a>
     */
    public static final class AdaptedBeanShellEvalRunner implements EvalRunner {
        Interpreter	interpreter;
        public AdaptedBeanShellEvalRunner() {
            interpreter = new Interpreter();
        }
        @Override
        public <Q> Object binding(QueryTemplateState<Q> preliminarState,
            String name,
            Object value) throws Throwable {
            if (value instanceof Map<?, ?>) {
                Map<String, ?> valueMap = (Map<String, ?>) value;
                interpreter.unset(name);
                interpreter.eval(name + "=object();");
                for (String keyItem : valueMap.keySet()) {
                    interpreter.set(name+"." + keyItem, valueMap.get(keyItem));
                }
            }
            return interpreter.get(name);
        }
        ...
    }
```
```java
String query = 
           "select free.att1 as empl.id, free.att2 as empl.name, free.att3 as empl.department, 'I' as empl.category \n" +
            "   from FREELANCERTB free  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [$eval$ pp.jobTitle && pp.departmentId && pp.managerId][   free.job_title = :jobTitle]  \n" +
            "     [$eval$ pp.jobTitle && (pp.departmentId || pp.managerId)][ ( free.job_title = :jobTitle and (free.department_Id = :departmentId or free.manager_id = :managerId) )]  \n" +
            "     [$eval$ !pp.jobTitle && !pp.managerId][ (free.job_title is null and free.manager_id is null)]  ";

config
    .evalRunnerCreator(state -> new ScriptEngineEvalRunner(this.scriptEngineManager, ScriptEngineEvalRunnerSupport.GENERIC))
    ...
```

#### Example of `$eval$`, custom functions, and variables:
Although the component was not designed to support script functions and variables, this is supported using a small trick.
```java
String query = 
            "select empl.id, empl.name, empl.departmentId, empl.managerId\n" +
            "   from EMPLOYEESTB empl  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [$eval$ var nameDepManagerVar = pp.name && (pp.departmentId || pp.managerId); ][no_operator][/*$eval$ var idNameDepVar = pp.id && (pp.name || pp.departmentId);*/]  \n" +
            "     [$eval$ function nameDepManagerFunc() { return !nameDepManagerVar; }          ][no_operator][/*$eval$ function nameDepManagerFunc() { return !nameDepManagerVar; }*/]  \n" +
            "     [$eval$ nameDepManagerVar   ][ ( empl.name = :name and ( empl.departmentId > empl.managerId ) ) ]  \n" +
            "     [$eval$ nameDepManagerFunc()][ ( empl.id is null and ( empl.departmentId <= empl.managerId ) ) ]  \n";

config
    .evalRunnerCreator(this.groovyEvalRunnerCreatorCompiled)
    ...
```

### 8. Example of Complex property and more than one parameter for a property:
```java
String query = 
            "select empl.id, empl.first_name, empl.last_name \n" +
            "   from EMPLOYEESTB empl  \n" +
            "   [filters]  \n" +
            "   [where]  \n" +
            "     [formalName][ (empl.first_name like :firstName or empl.last_name like :lastName)]";
```
```java
config
    .addMapper("formalName", FormalName.class).participatesInQuery(ParticipantCheckers.NOTNULL)
        .addParameter("firstName").done()
        .addParameter("lastName").done()
    .onParticipatesNamed(
            (query, name, value, parameterInfo) -> {
                if (name.equals("firstName")) {
                    query.setParameter(name, value.getFirstName(), StandardBasicTypes.STRING);
                } else if (name.equals("lastName")) {
                    query.setParameter(name, value.getLastName(), StandardBasicTypes.STRING);
                }
            }
    )
    .onParticipatesPositional(
            (query, currentPosition, value, parameterInfo) -> {
                if (parameterInfo.getName().equals("firstName")) {
                    query.setParameter(currentPosition, value.getFirstName(), StandardBasicTypes.STRING);
                } else if (parameterInfo.getName().equals("lastName")) {
                    query.setParameter(currentPosition, value.getLastName(), StandardBasicTypes.STRING);
                }
            }
    )
    .done();
```

### 9. Complex property, `[repeat]`, and dynamic parentheses

Assume `EmployeeFilter` has `getDepartmentId()` and `getFormalNamesArr()` properties, and each `FormalName` has `getFirstName()` and `getLastName()`. The outer parentheses are included only when `formalNamesArr` is non-empty; `[repeat]` creates one inner group for each item.

```java
String queryText =
    "select e.id, e.first_name, e.department_id \n" +
    "from EMPLOYEESTB e \n" +
    "[filters] \n" +
    "[where] \n" +
    "  [departmentId][e.department_id = :departmentId] \n" +
    "  [and][(] \n" +
    "    [formalNamesArr][repeat][or][(e.first_name like :firstNameArr and e.last_name like :lastNameArr)] \n" +
    "  [)]";

QueryTemplateConfig<Query<Employee>> config =
    QueryTemplateConfig.of(queryText, new SimpleTypeToken<Query<Employee>>(){}.getRawType());

config
    .addMapper("departmentId", Integer.class)
        .participatesInQuery(ParticipantCheckers.NOTNULL)
        .onParticipatesNamed((Query<Employee> q, String name, Integer value,
                AssignedParameterInfo<Integer> info) -> q.setParameter(name, value))
        .done()
    .addMapper("formalNamesArr", FormalName.class)
        .participatesInQuery(ParticipantCheckers.ARRAY_NOTEMPTY)
        .addParameter("firstNameArr").repeater(true).done()
        .addParameter("lastNameArr").repeater(true).done()
        .onParticipatesNamed((Query<Employee> q, String name, FormalName value,
                AssignedParameterInfo<FormalName> info) -> {
            if (name.startsWith("firstNameArr")) {
                q.setParameter(name, value.getFirstName());
            } else if (name.startsWith("lastNameArr")) {
                q.setParameter(name, value.getLastName());
            }
        })
        .done();

QueryTemplate<Query<Employee>> template = QueryTemplate.of(config);
EmployeeFilter filter = new EmployeeFilter();
filter.setDepartmentId(42);
filter.setFormalNamesArr(new FormalName[] {
    new FormalName("Ada", "Lovelace"),
    new FormalName("Grace", "Hopper")
});

String rendered = template.buildQueryState(filter).getQueryString();
// ... where e.department_id = :departmentId and (
//   (e.first_name like :firstNameArr_0 and e.last_name like :lastNameArr_0)
//   or (e.first_name like :firstNameArr_1 and e.last_name like :lastNameArr_1)
// )

filter.setFormalNamesArr(new FormalName[0]);
rendered = template.buildQueryState(filter).getQueryString();
// The entire AND group, including its parentheses, is omitted.
```

> Property names use Java **camelCase** (`filterPrp1`), which resolves to the getter `getFilterPrp1()` via `PropertyUtils`.

---

## How it compares to existing libraries

This library targets a specific niche: **text-based, token-driven conditional SQL/HQL** with no metamodel. Mature alternatives cover the same needs through different approaches:

| Approach                                 | Libraries                                                                                                             | Notes                                                                                                      |
|------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| Conditional fragment inclusion (closest) | **MyBatis Dynamic SQL** (`<if>/<where>/<foreach>`), **QueryDSL** `BooleanBuilder`, **Spring Data JPA Specifications** | Idiomatic conditional criteria; MyBatis is the closest to this token model.                                |
| Filter object → criteria                 | **Query by Example** (Spring Data / Hibernate)                                                                        | Only participating in the query fields become criteria; less flexible (no `or`, parentheses, sub-queries). |
| Type-safe programmatic builders          | **jOOQ**, **JPA Criteria API**, **Hibernate Criteria**                                                                | Strong for complex, fully-typed SQL.                                                                       |
| `IN (...)` list expansion                | **Spring `NamedParameterJdbcTemplate`**, MyBatis `<foreach>`, jOOQ                                                    | Matches the `parameterList` / `[repeat]` feature.                                                          |
| SQL text templating                      | **JDBI** + StringTemplate/FreeMarker, MyBatis + Velocity/FreeMarker                                                   | Closest in spirit (SQL-as-template with conditional blocks).                                               |

| `query-template` feature           | Ready-made equivalent                                                    |
|------------------------------------|--------------------------------------------------------------------------|
| Conditional clause inclusion       | MyBatis `<if>/<where>`, QueryDSL `BooleanBuilder`, Spring Specifications |
| Auto `where`/`and`/`or` management | MyBatis `<where>/<trim>`, jOOQ, Criteria                                 |
| `[repeat]` / list expansion        | MyBatis `<foreach>`, `NamedParameterJdbcTemplate`, jOOQ                  |
| Filter-object driven criteria      | Query by Example                                                         |
| Nested/reusable query helpers      | Spring Specifications, jOOQ CTEs/subqueries                              |

**When to prefer alternatives**
- Already on JPA/Hibernate and want type safety → **QueryDSL** or **Spring Data Specifications**.
- Want SQL-as-text with conditional blocks and mapper XML/fluent API → **MyBatis Dynamic SQL**.
- Complex, fully type-safe native SQL → **jOOQ**.

**Where `query-template` fits**
- You have hand-written SQL/HQL (including `union`, vendor-specific syntax) and want to toggle fragments based on a filter object, **without** adding a metamodel, annotations, or a full ORM query DSL.
- You want to stay **independent of any specific query API**: the engine only rewrites the query text; the actual parameter binding is delegated to your callbacks (Hibernate, JPA, JDBC, etc.).

---

## Tests

`QueryTemplateTest` (JUnit 4 + Hamcrest) covers conditional clauses, named and positional binding, packed/unpacked and repeated parameters, query helpers, alternate parameter names, `$any$`/negation, `$eval$` runners (Nashorn, GraalVM, Groovy, BeanShell), custom script variables/functions, and complex properties mapped to multiple query parameters.

```bash
mvn test
```

## License

MIT.
