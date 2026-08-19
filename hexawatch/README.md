# hexawatch-DB

Per-user config store for the Sonatype Firewall browser extension. Stores which VRM
and which child repositories a user selected in the extension's Settings page, so
the extension can pull the same config on any browser after signing in.

Scoping: one row per `userCode`. IQ Server URL and credentials stay local in
`chrome.storage` on the extension side — they are needed to reach IQ in the first
place, so they can't be fetched from a remote config store.

## Run it

```sh
cd hexawatch
npm install
npm run db:up   # starts postgres:16 on localhost:5455 and runs migrations
npm start       # Express server on localhost:9090
```

Stop the DB with `npm run db:down`.

## API

- `GET  /health`
- `GET  /extension/config?userCode=<code>` — returns `{ok, config}` or `404`
- `PUT  /extension/config` — body:
  ```json
  {
    "userCode": "...",
    "iqServerUrl": "http://localhost:8070",
    "vrmId": "...",
    "vrmName": "test",
    "selectedRepoIds": ["r1", "r2"]
  }
  ```
  Upserts, returns `{ok, config}`.

## Env overrides

- `PORT` (default `9090`)
- `PGHOST` (default `localhost`)
- `PGPORT` (default `5455`)
- `PGDATABASE` / `PGUSER` / `PGPASSWORD` (all default to `hexawatch`)
