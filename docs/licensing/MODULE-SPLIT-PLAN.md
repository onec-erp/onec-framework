# Open-core split plan: extracting commercial modules

Goal: make **this repo** a clean Apache-2.0 open-source project, and move the commercial
modules into a separate **private `onec-enterprise` repo** that consumes the OSS core as
published Maven artifacts.

## What moves where

| Module | Today | Destination | Difficulty |
|--------|-------|-------------|------------|
| `onec-guesty-starter` | this repo, leaf (deps: framework, framework-starter) | `onec-enterprise` | **Easy** — clean leaf, no in-repo consumers |
| `onec-hospedajes-starter` | this repo, leaf | `onec-enterprise` | **Easy** — only `example` consumes it |
| `onec-auth-starter` (incl. OIDC/SSO) | this repo | **stays open source** | — |
| `example` (hospedajes integration) | this repo, depends on hospedajes | OSS demo drops the vertical integration | **Easy** |

> **Scope note:** authentication — including OIDC / single sign-on and the resource-server mode —
> stays entirely in the open-source `onec-auth-starter`. An earlier draft of this plan proposed
> carving an `onec-enterprise-auth-starter` out of it; that was **descoped**. Only the vertical
> connectors (Guesty, SES.HOSPEDAJES) are commercial.

Net result: this repo's `settings.gradle.kts` drops `onec-guesty-starter` and
`onec-hospedajes-starter`; the `example` drops its `:onec-hospedajes-starter` dependency and the
`com.example.integration.hospedajes` package. `onec-auth-starter` is unchanged.

---

## The consumption model (the important decision)

The private repo is a **separate Gradle build** that depends on the OSS core as normal
external artifacts — NOT a composite build or submodule. This is what makes the open-core
boundary real.

```
                publish (public registry: Maven Central or public GH Packages)
  onec-framework ─────────────────────────────────────────────►  com.onec:onec-framework:0.2.0
   (Apache-2.0)                                                    com.onec:onec-framework-starter:0.2.0
                                                                         │ consumed as a normal dependency
                                                                         ▼
  onec-enterprise ──────────────────────────────────────────►  com.onec.enterprise:onec-guesty-starter:0.2.0
   (Commercial)    publish (PRIVATE registry / licensed access)  com.onec.enterprise:onec-hospedajes-starter:0.2.0
```

- OSS modules keep `group = "com.onec"`.
- Commercial modules re-group to **`group = "com.onec.enterprise"`** so the license boundary
  is visible in the coordinate.
- Versions can stay aligned (enterprise `0.2.0` targets core `0.2.0`) via a version catalog.

---

## Phase 0 — Licensing foundation (this repo) ✅ done in this change
- `LICENSE` (Apache-2.0), `NOTICE`, `licenses/HEADER-OSS.txt`.
- Add SPDX headers to OSS sources (`SPDX-License-Identifier: Apache-2.0`). Automate with
  Spotless `licenseHeader` so CI enforces it on every file.
- Add a `CONTRIBUTING.md` + a CLA or DCO so inbound contributions are licensable (needed if
  you ever want to dual-license core code).

Example Spotless wiring in the root `build.gradle.kts` (`subprojects { ... }`):
```kotlin
spotless {
    java {
        target("src/**/*.java")
        licenseHeaderFile(rootProject.file("licenses/HEADER-OSS.txt"))
    }
}
```

## Phase 1 — Publish the OSS core publicly first
Nothing can move until the core is consumable as an artifact by an *outside* build.
- Decide registry: **Maven Central** (best for adoption; requires group ownership +
  signing) or **public GitHub Packages**.
- Publish `0.2.0` of all `com.onec` modules. Verify a throwaway external project can resolve
  `com.onec:onec-framework-starter:0.2.0`.
- Existing `publish-packages.yml` already supports partial, dependency-closed releases — keep
  it for the core.

## Phase 2 — Extract guesty + hospedajes (mechanical)
Per module:
1. **Preserve history** into the new repo with `git filter-repo` (subdirectory split):
   ```bash
   git clone <this-repo> /tmp/guesty-split && cd /tmp/guesty-split
   git filter-repo --path onec-guesty-starter/ --path-rename onec-guesty-starter/:onec-guesty-starter/
   # then add as a remote/subtree of the onec-enterprise repo
   ```
2. In `onec-enterprise`, change the module `build.gradle.kts`:
   - `group`/publication coordinate → `com.onec.enterprise`.
   - Replace project deps with external ones:
     ```kotlin
     // before (this repo)
     api(project(":onec-framework"))
     implementation(project(":onec-framework-starter"))
     // after (onec-enterprise repo)
     api("com.onec:onec-framework:0.2.0")
     implementation("com.onec:onec-framework-starter:0.2.0")
     ```
   - Point `publishing { repositories { maven { url = ... } } }` at the **private** registry.
   - Swap the OSS header for `licenses/HEADER-COMMERCIAL.txt`.
3. In **this repo**: remove the two `include(...)` lines from `settings.gradle.kts` and delete
   the directories.

## Phase 3 — Authentication stays open source (descoped)
An earlier draft carved an `onec-enterprise-auth-starter` (OIDC / resource-server) out of
`onec-auth-starter`. **That split was descoped** — all authentication, including OIDC / single
sign-on and the stateless resource-server mode, remains in the open-source `onec-auth-starter`.
No auth code moves; nothing to do here.

## Phase 4 — Fix the example
`example` depends on `:onec-hospedajes-starter` and wires it through the
`com.example.integration.hospedajes` package.
- **OSS `example`:** remove the hospedajes dependency, the `integration/hospedajes` package, and
  the `onec.hospedajes.*` config block; keep it a pure-core demo (framework, ui, auth, mcp, print,
  mail, desktop). This is the public "hello world ERP".
- **Vertical showcase** (the hospedajes/guesty integration + Tauri desktop bundle): the integration
  source is carried into `onec-enterprise` for reference and can be rebuilt as a `rentals-example`
  there — the sales demo and reference "vertical solution".

## Phase 5 — CI / publishing / docs
- `onec-enterprise` gets its own `ci.yml` + `publish-packages.yml` (pointed at the private
  registry, building against released `com.onec` artifacts).
- README in this repo: add a "Commercial modules" section linking to onec-enterprise +
  `docs/licensing/COMMERCIAL-LICENSE.md`; state plainly what's Apache vs. commercial.
- Update `AGENTS.md` / `BUILDING_ERPS_WITH_AGENTS.md` so agents don't reference moved modules.

---

## Order of operations (do not reorder)
1. Phase 0 + Phase 1 — license + publish core publicly. **Blocking.**
2. Phase 2 — extract guesty/hospedajes into `onec-enterprise` (consuming the published core).
3. Phase 4 — slim the OSS example, rebuild the showcase in onec-enterprise.
4. Phase 5 — CI/docs cleanup.

## Risks / gotchas
- **Don't move `com.onec.auth.spi`** — `onec-ui-starter` (Apache-2.0) depends on it. The SPI
  is the free contract; only implementations are commercial.
- **License irreversibility:** once you tag a release as Apache-2.0, that release is forever
  Apache-2.0. Make sure no proprietary code is in `com.onec` modules *before* the public
  `0.2.0` tag.
- **Contributor IP:** add a DCO/CLA before accepting outside PRs, or you can't relicense.
- **Version skew:** pin enterprise modules to an exact core version; a version catalog shared
  via a small published BOM avoids drift.
