# NTT_CardsService


Microservicio bancario desarrollado en el Bootcamp de Microservicios.
Este repositorio corresponde al Entrega Final del proyecto, donde se implementa el microservicio de tarjetas.

- Estado: Entrega Final
- Dominio: Bancario / Tarjetas
- Arquitectura: Microservicios, Event-driven, Reactive (WebFlux)
- Contrato: OpenAPI (src/main/resources/api.yml)

## Índice
- Descripción
- Arquitectura y Diagramas
- Collections Pruebas APIs
- Tecnologías
- Repos Relacionados
- Configuración real (puertos, perfiles y variables)
- Ejecución local
- Ejecución con Docker Compose
- Salud y diagnóstico
- SonarQube
  


## Descripción

Este microservicio expone APIs para:
- Gestionar tarjetas (CRUD).
- Enlazar tarjetas a cuentas.
- Realizar débitos y coordinar transferencias.
- Consultar movimientos y saldos.
- Pagar créditos asociados con tarjeta.

Se integra de forma reactiva con otros microservicios (Cuentas, Créditos y Transacciones) y publica/consume eventos en Kafka para orquestar operaciones.

## Arquitectura y Diagramas

## Arquitectura general del ecosistema:
![Imagen de WhatsApp 2025-09-23 a las 17 22 32_3a609d96](https://github.com/user-attachments/assets/e65d7f30-71cf-4cd7-99dc-bf90f4b0ab16)


## Diagramas UML (Interacción entre Clientes, Transacciones, Cuentas y Créditos):
![](https://github.com/user-attachments/assets/288b7378-24f4-4be6-97f2-167f06baee26)


## Diagrama de Secuencia CRUD Tarjetas:
![diagramacards](https://github.com/user-attachments/assets/f3cd647b-bdfa-4fc2-81ec-014d635067a5)

## Tecnologías

- Java 11 + Spring Boot WebFlux
- Spring Security (Resource Server, JWT - Keycloak)
- Spring Data Reactive MongoDB
- Redis (caché)
- Apache Kafka
- Spring Cloud Config, Eureka
- Resilience4j (timeouts)
- Jackson, OpenAPI
- JUnit 5, Mockito, Reactor Test
- Docker/Docker Compose
  
## Collections Pruebas APIs:
https://github.com/DianaPortal/postman-collections-ms-NTTDATA

## Repos Relacionados

- Config repo (.properties): https://github.com/ArturoRoncal2704/nttdata-config-repo
- Config Server: https://github.com/ArturoRoncal2704/nttdata-config-serve
- API Gateway: https://github.com/ArturoRoncal2704/ntt-api-gateway
- Eureka Server: https://github.com/ArturoRoncal2704/ntt-eureka-server
- Infra Kafka/Redis: https://github.com/DianaPortal/infra-docker-kafka-redis
- Keycloak: https://github.com/ArturoRoncal2704/infra-keycloak


## Estructura del Proyecto

- src/main/java/com/nttdata/cards_service
  - adapter: capa API (delegates, mappers)
  - cache: servicios de caché y claves
  - config: configuración (JWT, Jackson, Kafka, Mongo, Redis, Resilience, WebClient, Seguridad)
  - integration: clientes a Cuentas, Créditos y Transacciones
  - kafka: consumidores/productores y eventos de dominio
  - model: entidades y value objects
  - repository: repositorios (Mongo)
  - service: lógica de dominio y orquestación
- src/main/resources
  - api.yml (contrato OpenAPI)
  - application.properties (config)
- src/test/java/... (tests unitarios/servicios/adapters)

## Integraciones y Mensajería

- Integraciones HTTP (reactivo):
  - AccountsClient (Cuentas)
  - CreditClient (Créditos)
  - TransactionsClient (Transacciones)
    
    ■ Integraciones HTTP
    
      - service.credits.base-url=http://localhost:8585/api (docker: http://credits-service:8585/api)      
      - service.accounts.base-url=http://localhost:8085/api (docker: http://account-service:8085/api)    
     -  service.transactions.base-url=http://localhost:8083/api/v1 (docker: http://host.docker.internal:8083/api/v1 si no está dockerizado)


- Kafka (eventos dominio):
  - Consumers:
    - CardLinkRequestConsumer
    - CardOperationRequestedConsumer
  - Producers:
    - CardLinkResultProducer
    - CardOperationResultProducer
    - PrimaryBalanceUpdatedProducer
  - Eventos:
    - CardLinkRequestEvent / CardLinkResultEvent
    - CardDebitRequestedEvent / CardDebitTransferInRequestedEvent

Los nombres de tópicos se gestionan en config: KafkaTopicsConfig.

## Configuración  (puertos, perfiles y variables)

- Puerto del servicio: 8090
- Perfil por defecto en contenedor: docker
- Dockerfile:
  - Runtime: eclipse-temurin:11-jre-alpine
  - Healthcheck: GET http://localhost:8090/actuator/health
    
## Seguridad -JWT- Keycloak

- Autenticación: Bearer JWT
- Decodificación JWT: CustomReactiveJwtDecoder
- Config de filtros y acceso: SecurityConfig
- Cabecera:
  - Authorization: Bearer <token>

  - spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8091/realms/nttdatabank
  - app.auth.allowed-issuers=http://keycloak:8091/realms/nttdatabank,http://localhost:8091/realms/nttdatabank

## Eureka
  - eureka.client.service-url.defaultZone=http://localhost:8761/eureka (docker: http://eureka-server:8761/eureka)
  - eureka.instance.appname=cards-service

## Ejecución local
Opción A: sin Config Server (usa application.properties local)
Opción B: con Config Server local
- Arranca Config Server en http://localhost:8888

## Ejecución con Docker Compose

Prepara la red compartida de infraestructura:
Antes de levantar los contenedores:

```powershell
docker network create infra-net
```

docker-compose del servicio (raíz del proyecto)

- Arrancar:
  ```powershell
  docker compose up -d --build
  ```
- Requiere que el resto de servicios (config-server, eureka, kafka/redis, keycloak, etc.) también estén en la red infra-net o con nombres de host equivalentes.

## Salud y diagnóstico

- Health: http://localhost:8090/actuator/health
- Logs: revisar stdout del contenedor
  ```powershell
  docker logs -f cards-service
  ```

## SonarQube

Propiedades (en config repo):
- sonar.host.url=http://localhost:9000
- sonar.projectKey=cards_service
- Reporte Jacoco: target/site/jacoco/jacoco.xml

Ejecución:
```powershell
.\mvnw clean verify sonar:sonar -Dsonar.login=$env:SONAR_TOKEN
```  
