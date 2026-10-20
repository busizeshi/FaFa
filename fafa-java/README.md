# fafa-java

Maven aggregation project for three independently runnable applications and one shared library:

- `fafa-gateway`: external HTTP entry point and routing.
- `fafa-admin`: operations API; calls Biz over the shared Dubbo contract.
- `fafa-biz`: application business service, organized by DDD context.
- `fafa-common`: shared response/error/request-ID conventions and RPC contracts; not deployable.

## Requirements

- JDK 21
- Maven Wrapper (preferred) or Maven 3.9+

## Build

```powershell
.\mvnw.cmd -DskipTests package
```

The version matrix lives in the root `pom.xml`. Spring Boot and Spring Cloud versions follow their compatibility line; update the related BOMs together and rebuild all modules.

## Run locally

Start Nacos first using `deploy/docker-compose/compose.dev.yaml`, then run each application in a separate terminal:

```powershell
.\mvnw.cmd -pl fafa-biz spring-boot:run
.\mvnw.cmd -pl fafa-admin spring-boot:run
.\mvnw.cmd -pl fafa-gateway spring-boot:run
```

`/actuator/health` reports process health. Gateway forwards `/api/admin/**`, `/api/biz/**`, and `/api/ai/**`; the initial connectivity example is `GET /api/admin/internal/admin/biz-ping` through Gateway. Keep Nacos, database and external credentials outside Git.

## Javadoc

Public contracts in `fafa-common` should have Javadoc. Generate the project API docs with:

```powershell
.\mvnw.cmd -pl fafa-common javadoc:javadoc
```
