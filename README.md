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
unlock of the amplifier: the amplifier hangs after every loss of the grid, and the administrator - or a member of the
household granted `amplifier_unlock` in the dashboard - presses a button on the media page to bring it back.

- No database and no broker - the service is stateless.
- Base path `/home/media`, routed by `api-gateway-service` (from HAS-516); the button is on the media page of the
  dashboard (HAS-517).
- Spring Boot 4.2.0, Java 21; `cholewa-commons` 2.0.0 (the error contract), `smart-home-sdk` 1.26.0 (the household
  profiles).

## API

| Method | Path | Answer |
|---|---|---|
| `POST` | `/home/media/amplifier/unlock?source=dashboard&member=<name>` | `202` once the amplifier took the file |

The unlock (HAS-515) sends one `POST /goform/formPostHandler` to the web page of the amplifier - a `multipart/form-data`
form with a text file of a few bytes as the part `appFirmware` (`unlock.txt`) and the field `appFirmwareFile=Upload`.
The amplifier refuses the file as an invalid BCD file, answers `302` to `.../bl_bcd_error.asp`, and works again. The
call is **sent once and never retried**, and only **one at a time**.

**The state of the amplifier cannot be read**: its page redirects to the same `bl_index.asp` hung or healthy, and only
port 80 is open. The answer of the unlock is the only outcome - do not build a status or a detection on the amplifier.

`member` must be a switched-on member of the household registry, found in `GET /home/household/profiles` of
`database-service` - a trace of who pressed. **The service checks no role and no permission** (since 0.3.0, HAS-517): who
is offered the button is decided by the dashboard (the administrator, and a member granted `amplifier_unlock`); nothing
behind the gateway is authenticated. `source` is `dashboard`.

| Status | Code | When |
|---|---|---|
| `400` | `UNKNOWN_MEMBER` | no switched-on member of that name |
| `409` | `UNLOCK_IN_PROGRESS` | another unlock is on its way to the amplifier |
| `502` | `AMPLIFIER_REFUSED` | the amplifier answered anything but the redirect, or closed the connection |
| `503` | `AMPLIFIER_UNAVAILABLE` | no connection to the amplifier - nothing was sent |
| `503` | `REGISTRY_UNAVAILABLE` | the household profiles could not be read |
| `504` | `AMPLIFIER_TIMEOUT` | no answer within `media.amplifier.timeout` - the file may have been taken |

Every refusal above carries its `code` in the error contract of `cholewa-commons`; a malformed request - no `member`,
one longer than 50 characters, or a `source` other than `dashboard` - is a `400` of the validation without a code.
Every unlock is one INFO line with the member, the source and the outcome.

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `media.amplifier.address` | none - `amplifier-address`, required | the host of the amplifier on the LAN, from the manifest |
| `media.amplifier.timeout` | `PT5S` | the wait for the connection and for the answer (a bare number is seconds, 1 - 30 s) |
| `internal.service.database` | `http://database-service:6200` | database-service, for the household profiles |

The address never appears in this repository; the service refuses to start without it (`cholewa.required-properties`).

## Run locally

```
mvn verify
```

needs no Docker - the tests call a `MockWebServer`, never the amplifier. The `local` Spring profile listens on port
`6018` (Actuator `8018`) and reads the profiles from database-service on `6005`; a local run needs `amplifier-address`
and **a local unlock reaches the real amplifier**:

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
