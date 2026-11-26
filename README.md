# media-service

[![CI](https://github.com/smart-home-automation-system/media-service/actions/workflows/CI.yml/badge.svg)](https://github.com/smart-home-automation-system/media-service/actions/workflows/CI.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=smart-home-automation-system_media-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=smart-home-automation-system_media-service)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=smart-home-automation-system_media-service&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=smart-home-automation-system_media-service)

![GitHub Release Date - Published_At](https://img.shields.io/github/release-date/smart-home-automation-system/media-service?style=plastic)
![GitHub Release](https://img.shields.io/github/v/release/smart-home-automation-system/media-service?style=plastic)

---

![GitHub top language](https://img.shields.io/github/languages/top/smart-home-automation-system/media-service?style=plastic)
![Java](https://img.shields.io/badge/java-21-yellow?style=plastic)
![SpringBoot](https://img.shields.io/badge/SpringBoot-4.2.0-blue?style=plastic)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=smart-home-automation-system_media-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=smart-home-automation-system_media-service)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=smart-home-automation-system_media-service&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=smart-home-automation-system_media-service)

![GitHub issues](https://img.shields.io/github/issues/smart-home-automation-system/media-service?style=plastic)
![GitHub contributors](https://img.shields.io/github/contributors/smart-home-automation-system/media-service?style=plastic)
![GitHub pull requests](https://img.shields.io/github/issues-pr-raw/smart-home-automation-system/media-service?style=plastic)

![GitHub last commit](https://img.shields.io/github/last-commit/smart-home-automation-system/media-service?style=plastic)
![GitHub commit activity](https://img.shields.io/github/commit-activity/m/smart-home-automation-system/media-service?style=plastic)

---

# Description

The audio and video equipment of the house, part of the
[smart-home-automation-system](https://github.com/smart-home-automation-system). Its first job (epic HAS-508) is the
unlock of the amplifier: the amplifier hangs after every loss of the grid, and a member of the household with the
permission `amplifier_unlock` presses a button in the dashboard to bring it back.

**Today the service is a scaffold** (HAS-514): it answers its probes and exposes its metrics, nothing more. The unlock
comes with HAS-515.

- No database and no broker - the service is stateless.
- Base path `/home/media`, routed by `api-gateway-service` (from HAS-516).
- Spring Boot 4.2.0, Java 21; `cholewa-commons` 2.0.0 (the error contract), `smart-home-sdk` 1.26.0 (the household
  profiles).

## Run locally

```
mvn verify
```

needs no Docker. The `local` Spring profile listens on port `6018` (Actuator `8018`):

```
java -jar target/media-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=home,local
```

In the cluster the service listens on `6200`, Actuator on `8200`.

## Actuator

| Endpoint | Purpose |
|---|---|
| `GET /actuator/health/readiness` | readiness probe |
| `GET /actuator/health/liveness` | liveness probe |
| `GET /actuator/prometheus` | metrics, scraped by Prometheus under the label `app` |
| `GET /actuator/info` | exposed, empty (no build info is generated) |

Logs are JSON (Logstash format) in the cluster and plain text under `local` and `test`; every request carries its
`traceId` (Micrometer Tracing with Brave, no exporter - the trace lives in the logs).
