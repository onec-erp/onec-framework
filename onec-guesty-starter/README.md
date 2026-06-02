# onec-guesty-starter

Spring Boot starter that integrates a oneC application with the [Guesty Open API](https://open-api-docs.guesty.com/)
— the PMS/channel-manager used for short-term rental listings, reservations, guests and calendars.

It provides a thin typed client (`GuestyClient`), a convenience facade (`GuestyService`), and a token
manager that handles Guesty's OAuth 2.0 client-credentials flow while respecting its strict
**5-token-requests-per-24h** cap.

## Enabling

Add the dependency and switch it on:

```yaml
onec:
  guesty:
    enabled: true
    client-id: ${GUESTY_CLIENT_ID}
    client-secret: ${GUESTY_CLIENT_SECRET}
```

Auto-configuration (`onec.guesty.enabled=true` + `spring-web` on the classpath) then exposes
`GuestyTokenManager`, `GuestyClient` and `GuestyService` beans.

### Configuration keys

| Key | Default | Purpose |
|-----|---------|---------|
| `onec.guesty.enabled` | `false` | Master switch. |
| `onec.guesty.client-id` / `client-secret` | — | OAuth client credentials. |
| `onec.guesty.access-token` | — | A pre-minted token, served verbatim (no token endpoint calls). |
| `onec.guesty.base-url` | `https://open-api.guesty.com/v1` | API base. |
| `onec.guesty.auth-url` | `https://open-api.guesty.com/oauth2/token` | Token endpoint. |
| `onec.guesty.timeout-ms` | `30000` | Connect/read timeout. |
| `onec.guesty.token.cache-file` | `build/guesty/token.json` | Persists the token across restarts so a redeploy doesn't burn one of the 5 daily requests. Empty to disable. |
| `onec.guesty.token.refresh-skew-ms` | `300000` | Refresh this long before stated expiry. |
| `onec.guesty.retry.max-attempts` | `3` | Attempts per call (429/5xx are retried with backoff). |
| `onec.guesty.retry.backoff-ms` | `1000` | Base backoff, doubled each attempt. |

### Token handling

Guesty allows only **5 token requests per 24h**, and each token is valid 24h. The `GuestyTokenManager`
caches the token in memory and (by default) on disk, reuses it until it nears expiry, and refreshes
once automatically on a `401`. Never request tokens yourself — let the manager own them. If you mint
tokens out of band, hand one over via `onec.guesty.access-token` and the endpoint is never called.

## Usage

```java
@Service
class SyncService {
    private final GuestyService guesty;

    SyncService(GuestyService guesty) { this.guesty = guesty; }

    void run() {
        // Auto-paginated convenience methods:
        List<Listing> listings = guesty.allListings();
        List<Reservation> arrivals = guesty.arrivalsBetween("2026-06-01", "2026-06-30");
        List<Reservation> forListing = guesty.reservationsForListing("6986ec65dd286e001ca3f572");

        // Single-call typed client:
        GuestyClient client = guesty.client();
        Listing one = client.getListing("6986ec65dd286e001ca3f572");
        Page<Reservation> page = client.listReservations(Map.of("limit", 25, "skip", 0));
        List<CalendarDay> cal = client.getCalendar(one.id(),
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31"));
        Reservation created = client.createReservation(
                CreateReservationRequest.of(one.id(), "2026-08-01", "2026-08-08", guestId));

        // Escape hatch for any endpoint not modelled here:
        Map<?, ?> raw = client.get("/owners", Map.of("limit", 10), Map.class);
    }
}
```

List endpoints accept a raw query map (`limit`, `skip`, `fields` (space-separated), `sort`,
`filters`, and endpoint-specific keys), so the full API surface is reachable without a method per
filter. Non-2xx responses raise `GuestyApiException` carrying the status and body.

## Live probe

`GuestyLiveProbeTest` exercises the real API; it is skipped unless credentials are in the environment:

```bash
GUESTY_CLIENT_ID=... GUESTY_CLIENT_SECRET=... ./gradlew :onec-guesty-starter:test
# or reuse an already-minted token (respects the 5/24h cap):
GUESTY_CLIENT_ID=... GUESTY_ACCESS_TOKEN=eyJ... ./gradlew :onec-guesty-starter:test
```
