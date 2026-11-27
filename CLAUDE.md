# media-service

The audio and video equipment of the house (owner, 2026-11-24): the place for what plays sound and picture. Its first
job, epic HAS-508, is the unlock of the Harman Kardon amplifier, which hangs after every loss of the grid - the
administrator or a member with the permission `amplifier_unlock` presses a button on the media page of the dashboard.
Scaffolded in HAS-514 from `water-service` (the `new-service` skill); the unlock is HAS-515, the route of the gateway
HAS-516, the button HAS-517.

Part of the smart-home-automation-system organization — org-wide conventions, the
repository map and working rules come from the workspace-level context
(`organization-repository/claude/organization.md`). The user writes the code in this
repository themselves; Claude's default role here is analysis, code review and security
review - the owner had Claude scaffold the service (HAS-514) and implement the unlock (HAS-515), and reviews them in
IntelliJ.

## Rules of the owner (2026-11-24, moved here from `working-rules.md` with HAS-515)

- **The state of the amplifier cannot be read - never design a status or a detection.** Its page redirects to the same
  `bl_index.asp` hung or healthy, and only port 80 is open.
- **The unlock is a button only - never automatic** after the grid returns.
- A service of its own (not amx-service) - decided, do not re-ask.
- **The service blocks nothing by a role or a permission** (owner, 2026-11-27, HAS-517 - a rule of the whole project, in
  `working-rules.md`): who is offered the button is the dashboard's - the administrator on the media page, and a resident
  granted `amplifier_unlock` (the owner gives it to one more member). The service only finds the member who pressed.

## Role in the system

- Is called by: through `api-gateway-service` (`/home/media/**`, from HAS-516) - the dashboard (HAS-517).
- Calls: the amplifier on the LAN over HTTP (`media.amplifier.address`, from the manifest of deployment-tools) and
  `database-service` for the household profiles (`GET /home/household/profiles` over k8s DNS, `internal.service.database`).
- Uses libraries: `cholewa-commons` 2.0.0 (the error contract, `DomainExceptionProcessor`, `DownstreamErrors`, the
  required properties; Logbook in the reactive chain), `smart-home-sdk` 1.26.0 (`HouseholdProfile`).
- Database: none. Broker: none - no RabbitMQ starter, so no `NotificationPublisher` and nothing of the move of the
  RabbitMQ support in Boot 4.2 applies here.
- Integrates with hardware: the amplifier (GoAhead-Webs, HTTP/1.0, port 80) - described in deployment-tools, its address
  never in this public repository.

## Build & run

- Build + tests: `mvn verify` - no Docker needed; the tests call a `MockWebServer`, never the amplifier.
- Local run: `home,local` Spring profiles, port `6018` (Actuator `8018`); in-cluster port `6200`, Actuator `8200`.
  Needs `amplifier-address` (mandatory, no default) and database-service on 6005 - **a local unlock reaches the real
  amplifier**.
- Spring Boot **4.2.0**, logbook 4.2.0; own libraries on the latest releases, as the org rule asks of every service
  task. The plugins the Boot parent manages carry no version of their own; `tidy-maven-plugin` keeps its property.

## Specifics

- **The unlock** (`POST /amplifier/unlock?source=dashboard&member=`, package `amplifier`) - what is easy to break:
  - **One POST of the firmware form**, `/goform/formPostHandler`, `multipart/form-data`: the file part `appFirmware`
    (`unlock.txt`, `text/plain`, 7 bytes) and the field `appFirmwareFile=Upload` (`AmplifierClient`). **The success is a
    302 whose Location ends with `/bl_bcd_error.asp`** ("The file is an invalid BCD file" - verified on the device on
    2026-11-24, answered in 1.2 s): 202. Any other answer, a redirect elsewhere, a 302 without a Location, a socket
    closed after the connection - `502 AMPLIFIER_REFUSED`; no connection - `503 AMPLIFIER_UNAVAILABLE` (nothing was
    sent); no answer within `media.amplifier.timeout` - `504 AMPLIFIER_TIMEOUT` (it may have been taken). Sending the
    file again is harmless, so a refusal may be pressed again.
  - **Redirects are not followed** - the redirect is the answer (`followRedirect(false)` on the client of the amplifier,
    `WebClientConfig`). That client opens a new connection per unlock (`HttpClient.newConnection()`): the amplifier
    speaks HTTP/1.0 and closes after every answer. Its connect timeout and response timeout are both
    `media.amplifier.timeout` (PT5S, 1 - 30 s, a bare number is seconds - `AmplifierPropertiesTest`).
  - **Sent once, never retried; one at a time** (`AmplifierService`): the guard is a resource of `Mono.using`, taken when
    the call is subscribed and given back on completion, error and cancel - a cancel that came between taking it and
    calling the amplifier kept it for good in the first form (found by `AmplifierServiceTest`). A second unlock meanwhile
    is `409 UNLOCK_IN_PROGRESS`.
  - **The member** (`HouseholdMembers`): a switched-on member of the profiles of database-service, the name compared
    ignoring case and answered as the registry spells it - `400 UNKNOWN_MEMBER`, `503 REGISTRY_UNAVAILABLE` (the profiles
    not read). A trace of who pressed, **never a check of the role or a permission** (owner, 2026-11-27, HAS-517 - 0.3.0
    dropped the `403 NOT_PERMITTED` of 0.2.0): the dashboard offers the button to the administrator and to a member
    granted `amplifier_unlock`. Nothing behind the gateway is authenticated.
    `source` is `dashboard` only.
  - **`HouseholdProfilesClient` is the third copy** of the client of the profiles (appliance-service, gate-service,
    media-service) - three copies stay where they are (the rule of `cholewa-commons`).
  - **The codes are an enum** (`AmplifierError`), the names pinned by `AmplifierErrorTest` - the dashboard words each of
    them (HAS-517); a rename is made together with the dashboard. Every unlock is an INFO line with the member, the
    source and the outcome.
- **Stateless** apart from the guard of one unlock: the Deployment may roll (`RollingUpdate`) - a rollout can, for a few
  seconds, have two pods each with a guard of its own; two presses at once are harmless.
- **`MediaServiceApplicationTest` pins the cluster ports, the probe groups and the exposure** - a 404 on the readiness
  path crash-loops the pod - the required address of the amplifier and the full sampling of the traces.
- **Surefire activates the `test` profile for every class** (`spring.profiles.active` in the pom); the `includes`
  match only `**/*Test.java` and `**/*IT.java` - a class named `...Tests` silently stops running.
