# onec Commercial License (DRAFT — not legal advice)

> ⚠️ This is a starting-point sketch for the **separate `onec-enterprise` repo**, not for
> this open-source repo. Have a lawyer review before you ship it. It is written for the
> open-core model: the Apache-2.0 core stays free; the modules below are licensed, not sold.

**SPDX identifier:** `LicenseRef-onec-Commercial`
**Applies to:** modules published under the `com.onec.enterprise` group —
`onec-enterprise-auth-starter`, `onec-guesty-starter`, `onec-hospedajes-starter`, and
future commercial modules. It does **not** apply to any `com.onec` (Apache-2.0) artifact.

---

## 1. Grant
Subject to a valid, paid Subscription and these terms, onec-erp grants You a
non-exclusive, non-transferable, non-sublicensable license to install and run the
Licensed Modules in object form, in production and non-production, **for the
Subscription term**, up to the entitlements in Your Order (e.g. number of deployments,
tenants, or named seats).

## 2. Source availability (choose one model)
- **Option A — Closed (recommended for connectors + SSO).** Object (JAR) only. No source.
- **Option B — Source-available (BSL-style).** You receive source and may read/modify it for
  Your own use, but may **not** offer the Licensed Modules (or a derivative) to third
  parties as a hosted or managed service that competes with onec. Each release converts to
  Apache-2.0 on the fourth anniversary of its publication ("Change Date").

> Recommendation: ship **guesty/hospedajes/enterprise-auth as Option A**. Reserve Option B
> for a future "agent builder" module where community trust/inspection matters more.

## 3. Restrictions
You may not: (a) redistribute, resell, or sublicense the Licensed Modules; (b) remove or
alter license notices; (c) use them beyond Your purchased entitlements; (d) circumvent any
license key / entitlement check; (e) [Option A] decompile or reverse engineer except as
permitted by law.

## 4. Subscription, term, termination
License is valid only while the Subscription is paid and current. On expiry or material
breach, the production grant ends and You must stop running the Licensed Modules in
production. Sections 6–9 survive.

## 5. Support & updates
Updates and support are provided per Your tier (see PRICING.md) only during an active
Subscription.

## 6. Warranty disclaimer
THE LICENSED MODULES ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND.

## 7. Limitation of liability
TO THE MAXIMUM EXTENT PERMITTED BY LAW, ONEC-ERP'S TOTAL LIABILITY IS LIMITED TO THE FEES
PAID IN THE 12 MONTHS PRECEDING THE CLAIM. NO LIABILITY FOR INDIRECT OR CONSEQUENTIAL
DAMAGES.

## 8. Third-party / open-source components
The Licensed Modules depend on Apache-2.0 `com.onec` artifacts and other OSS, each under
its own license. Nothing here restricts Your rights under those licenses.

## 9. Governing law
[Jurisdiction TBD].

---

## Suggested commercial packaging (sketch — pairs with the licensing memo)
| Tier | Who | Includes | Rough model |
|------|-----|----------|-------------|
| **Community** | Everyone | Apache-2.0 core + free auth/ui/mcp/print/mail/kafka | Free |
| **Connectors** | Teams needing a vertical | guesty / hospedajes / future connectors | Per-deployment / annual |
| **Enterprise** | Orgs needing SSO, audit, multi-tenancy | enterprise-auth (OIDC/SSO) + future multi-tenancy | Per-seat or per-tenant / annual |
| **Cloud** | Don't want to self-host | Hosted, all of the above | Per-tenant + per-seat / monthly |
