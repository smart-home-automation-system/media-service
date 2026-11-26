# media-service

The audio and video equipment of the house (owner, 2026-11-24): the place for what plays sound and picture. Its first
job, epic HAS-508, is the unlock of the Harman Kardon amplifier, which hangs after every loss of the grid - a member
with the permission `amplifier_unlock` presses a button in the dashboard. Scaffolded in HAS-514 from `water-service`
(the `new-service` skill); the unlock itself is HAS-515, the route of the gateway HAS-516, the button HAS-517.

Part of the smart-home-automation-system organization — org-wide conventions, the
repository map and working rules come from the workspace-level context
(`organization-repository/claude/organization.md`). The user writes the code in this
repository themselves; Claude's default role here is analysis, code review and security
review - the owner had Claude scaffold the service (HAS-514) and reviews it in IntelliJ.

## Role in the system

- Is called by: through `api-gateway-service` (`/home/media/**`, from HAS-516) - the dashboard.
- Calls: nothing yet. With HAS-515: the amplifier on the LAN over HTTP and `database-service` for the household
  profiles (`GET /home/household/profiles` over k8s DNS).
- Uses libraries: `cholewa-commons` 2.0.0 (the error contract, `GlobalErrorExceptionHandler`; Logbook in the reactive
  chain), `smart-home-sdk` 1.26.0 (`HouseholdProfile`, `MemberPermission.AMPLIFIER_UNLOCK`).
- Database: none. Broker: none - no RabbitMQ starter, so no `NotificationPublisher` and nothing of the move of the
  RabbitMQ support in Boot 4.2 applies here.
- Integrates with hardware: the amplifier (HAS-515).

## Build & run

- Build + tests: `mvn verify` - no Docker needed.
- Local run: `home,local` Spring profiles, port `6018` (Actuator `8018`); in-cluster port `6200`, Actuator `8200`.
- Spring Boot **4.2.0**, logbook 4.2.0; own libraries on the latest releases, as the org rule asks of every service
  task. The plugins the Boot parent manages carry no version of their own; `tidy-maven-plugin` keeps its property.

## Specifics

- **Stateless**: nothing in memory between calls, so the Deployment may roll (`RollingUpdate`) - unlike the services
  that hold a state and run `Recreate`.
- **No mandatory variable yet**: `cholewa.required-properties` comes with the first one (the address of the amplifier,
  HAS-515), which never gets a default and never appears in this public repository.
- **`MediaServiceApplicationTest` pins the cluster ports, the probe groups and the exposure** - a 404 on the readiness
  path crash-loops the pod - and the full sampling of the traces.
- **Surefire activates the `test` profile for every class** (`spring.profiles.active` in the pom); the `includes`
  match only `**/*Test.java` and `**/*IT.java` - a class named `...Tests` silently stops running.
