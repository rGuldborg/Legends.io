# Mejais Production Key Readiness

## Product Overview
- **Name:** Mejais – League of Legends draft assistant.
- **Purpose:** Mirrors live champ select through the local League Client API (LCU) and recommends champions based on aggregated high-ELO statistics stored in `data/snapshot.db`.
- **Scope:** Read-only companion application. Does not inject input, send commands, or alter client behavior.

## Data Sources
1. **Riot Web APIs (HTTPS):** Used offline via `CollectorRunner` to gather ranked match history and rebuild the local snapshot database.
2. **League Client API (LCU):** WebSocket subscription to `/lol-champ-select/v1/session` on `https://127.0.0.1:<lockfile-port>` for live mirroring.
3. **Static assets/Data Dragon:** Champion names/icons bundled locally.

## Security & Secrets
- API keys are **never** stored in the repo. They are only required when running the standalone snapshot kit on trusted machines; the distributed desktop client neither ships with nor requests a Riot API key.
- All Riot REST calls are made via `RiotApiClient` over HTTPS with Java’s `HttpClient`. The client honors Riot rate limits using `RiotRateLimiter` with default buckets of 20 req/s and 100 req/2m per region.
- LCU connections now validate Riot’s certificate by loading `riotgames.pem` from the client install (fallback to default trust store if unavailable). No trust-all managers ship in production.
- Verbose champ-select payload logging has been removed to avoid leaking player state into user logs.
- Snapshot database contains only aggregated champion stats (wins, roles, synergy/counter win rates). No PII, Riot IDs, or match identifiers are stored or redistributed.

## Game Integrity
- App is strictly advisory: it mirrors picks/bans and surfaces statistical recommendations. It does **not** automate decisions, block drafting options, or provide hidden information beyond what the client exposes during champ select.
- No attempts are made to infer, de-anonymize, or track players.
- UI contains Riot’s required legal boilerplate (Help tab + README).

## Operational Notes
- Remote snapshot updates are optional and configurable via the `SNAPSHOT_REMOTE_URL` system property/environment variable so production builds can point to an HTTPS endpoint that you control.
- A maintainer-only `snapshot-kit/` project (scripts + env templates + collector sources) produces `data/snapshot.db` on trusted machines using the production key; the packaged desktop client only consumes that SQLite file and never embeds or requests API credentials from end users.

## Requested Access
- **Production API Key:** LoL standard APIs + match-v5 for periodic aggregation.
- **Rate limits:** Default production (500 / 10s, 30 000 / 10m) are sufficient; the built-in limiter prevents bursting beyond Riot guidelines.

## Contact & Monitoring
- Include maintainer contact email/Discord handle in the Developer Portal entry.
- Log files avoid sensitive data; only aggregate stats and high-level status messages are written.

With the above controls in place Mejais complies with the General Policies, Developer Safety, and Game Integrity requirements and is ready for production review.***
