# Sonatype Firewall — Browser Extension

A Chrome (Manifest V3) extension that brings **Sonatype Repository Firewall** verdicts directly into the developer's browser. Inspired by Socket's web extension, but powered by the data Sonatype already has — IQ Server policy, integrity ratings, ABF, Golden Versions, and your Nexus proxy.

## Features (MVP)

| # | Feature | Notes |
|---|---|---|
| 1 | **Inline risk badge** on package pages | npm, PyPI, Maven Central — color-coded by policy verdict |
| 2 | **"Would Firewall block this here?"** | Org-aware policy verdict (block / quarantine / warn / allow) |
| 3 | **Integrity rating + threat types** | Sonatype's existing taxonomy (trojan, typosquat, hijack, …) |
| 4 | **CVE list with reachability** | Includes Sonatype IDs + CVSS |
| 5 | **Golden Version recommendation** | Inline next-safe-version hint |
| 6 | **Install command rewrite** | `npm i pkg` → `npm i --registry=<your Nexus> pkg` |
| 7 | **One-click waiver request** | Submits to IQ Waiver REST API |
| 8 | **ABF match indicator** | Flags binaries matching known-malicious fingerprints |
| 9 | **Mock + real IQ Server adapter** | Toggle in Settings; mock works offline |

## Architecture

See `docs/HLD.md` (or the conversation that generated this project) for the full HLD diagram. In short:

```
Content scripts (npm/PyPI/Maven) ──► Background service worker ──► IQ Server / Mock
                                              │
                                              ▼
                                  chrome.storage (settings + cache)
```

## Quick start

```bash
# 1. install deps
npm install

# 2. start the mock IQ Server (port 8765)
npm run mock

# 3. in another terminal, build the extension
npm run build      # one-shot
# or
npm run dev        # watch mode

# 4. load it in Chrome
#    - open chrome://extensions
#    - enable Developer mode
#    - "Load unpacked" → select the dist/ directory

# 5. demo URLs (with mock data already populated)
#    https://www.npmjs.com/package/lodash/v/4.17.10           → critical CVE, Golden Fix
#    https://www.npmjs.com/package/event-stream/v/3.3.6       → MALICIOUS (trojan)
#    https://www.npmjs.com/package/colors/v/1.4.44-liberty-2  → MALICIOUS (hijack)
#    https://www.npmjs.com/package/express/v/4.17.1           → high CVE, Golden Fix
#    https://www.npmjs.com/package/lodash/v/4.17.21           → ALLOWED
#    https://pypi.org/project/ctx/0.1.2/                       → MALICIOUS
#    https://central.sonatype.com/artifact/org.apache.logging.log4j/log4j-core/2.14.1
#                                                              → critical (Log4Shell)
```

## Project layout

```
sonatype-firewall-extension/
├── src/
│   ├── manifest.json           # Chrome MV3 manifest
│   ├── types.ts                # shared types (FirewallVerdict, settings, …)
│   ├── lib/
│   │   ├── settings.ts         # chrome.storage wrapper
│   │   ├── iq-client.ts        # IQ Server REST client (mock-compatible)
│   │   └── cache.ts            # in-memory verdict cache w/ TTL
│   ├── background/index.ts     # service worker — message router
│   ├── content/
│   │   ├── shared.ts           # badge injection + install rewriter
│   │   ├── npm.ts              # npmjs.com
│   │   ├── pypi.ts             # pypi.org
│   │   └── maven.ts            # central.sonatype.com & search.maven.org
│   ├── popup/                  # action popup (React)
│   ├── options/                # settings page (React)
│   └── icons/                  # 16/48/128 placeholders
├── mock-iq/
│   ├── server.js               # Express mock IQ Server
│   └── data.js                 # sample components + policy logic
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.js
└── postcss.config.js
```

## Endpoints exposed by the mock IQ Server

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | liveness check (used by Settings → "Test connection") |
| GET | `/api/v2/components/info?purl=…` | component metadata |
| POST | `/api/v2/policy/evaluate` | policy verdict for a PURL |
| POST | `/api/v2/firewall/verdict` | combined component + policy + reachability (what the extension calls) |
| POST | `/api/v2/remediation` | Golden Version |
| POST | `/api/v2/waivers` | submit a waiver |

These shapes mirror IQ Server REST conventions; swapping in a real IQ Server is a `iqServerUrl` change in Settings.

## Differentiation vs. Socket's extension

| Capability | Socket | This extension |
|---|---|---|
| Risk badge on package pages | ✓ | ✓ |
| Threat-type breakdown | ✓ | ✓ (Sonatype taxonomy) |
| **Org-aware "would block here?"** | ✗ | ✓ |
| **Per-org / per-stage policy reasons** | ✗ | ✓ |
| **Quarantine status read-back** | ✗ | ✓ |
| **One-click waiver from page** | ✗ | ✓ |
| **Golden Version inline** | ✗ | ✓ |
| **Install command → Nexus proxy rewrite** | ✗ | ✓ |
| **ABF fingerprint match** | ✗ | ✓ |
| **Reachability hint for org's apps** | ✗ | ✓ (when scan exists) |
| **Air-gapped / SAGE compatible** | ✗ | ✓ (BFF runs in customer network) |

## Roadmap (post-MVP)

- Stack Overflow / GitHub overlay (PR diffs that add deps)
- AI/ML Hugging Face risk
- Namespace-confusion warning
- IndexedDB persistence (offline mode)
- Full SSO via customer IdP (currently bearer token only)
- SBOM upload + bulk evaluation in popup

## License

Internal proof-of-concept — not for distribution.
