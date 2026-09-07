# StreamFlix

A Netflix-styled browser for the [iptv-org](https://github.com/iptv-org/iptv) public channel
index — a Java/Spring Boot backend that fetches and parses the M3U playlist at
`https://iptv-org.github.io/iptv/index.m3u`, and a React frontend that browses it by category,
searches it, and plays channels with `hls.js`.

```
┌─────────────────────┐        M3U playlist        ┌──────────────────────────┐
│  iptv-org (GitHub)   │ ─────────────────────────▶ │  Spring Boot backend      │
│  index.m3u            │                            │  - parses & caches       │
└─────────────────────┘                            │  - REST API /api/channels │
                                                     │  - stream proxy /api/proxy│
                                                     └───────────┬──────────────┘
                                                                 │ JSON + proxied HLS
                                                                 ▼
                                                     ┌──────────────────────────┐
                                                     │  React frontend (Vite)   │
                                                     │  - browse rows by category│
                                                     │  - search                │
                                                     │  - hls.js player          │
                                                     └──────────────────────────┘
```

## Why a proxy?

Most free-to-air IPTV origins don't send CORS headers, so a browser can't fetch their
`.m3u8`/`.ts` files directly. `GET /api/proxy?url=<stream url>` fetches the manifest,
rewrites every segment/variant reference to point back through the proxy, and streams
segments through the backend — so playback works the same regardless of the origin's
CORS policy. It includes a basic SSRF guard (only `http(s)`, blocks loopback/private
IP targets) since the URLs it fetches come from a crowd-sourced, third-party playlist.

## Prerequisites

- Java 17+ and Maven 3.9+
- Node 18+ and npm

## Run the backend

```bash
cd backend
mvn spring-boot:run
```

Starts on `http://localhost:8080`. On boot it downloads and parses the playlist
(~8,000+ channels — the first fetch can take a few seconds); it then re-refreshes every
6 hours (`streamflix.playlist.refresh-interval-ms` in `application.yml`). Check progress:

```bash
curl http://localhost:8080/api/channels/status
```

## Run the frontend

```bash
cd frontend
cp .env.example .env.local   # optional - defaults work with the Vite dev proxy
npm install
npm run dev
```

Open `http://localhost:5173`. In dev, Vite proxies `/api/*` to `http://localhost:8080`
(see `vite.config.js`), so no CORS config is needed locally beyond what's already in
`CorsConfig.java` for other origins.

## API

| Endpoint | Description |
|---|---|
| `GET /api/channels?page=&size=` | Paginated list of all channels |
| `GET /api/channels/{id}` | A single channel |
| `GET /api/channels/categories` | Category names with channel counts |
| `GET /api/channels/category/{name}?page=&size=` | Channels in one category |
| `GET /api/channels/search?q=&page=&size=` | Search by name/category/country |
| `GET /api/channels/status` | Catalog load state, counts, last refresh time |
| `POST /api/channels/refresh` | Force an immediate playlist refresh |
| `GET /api/proxy?url=` | Proxies/rewrites a stream URL for CORS-safe playback |

## Configuration

`backend/src/main/resources/application.yml`:
- `streamflix.playlist.url` — swap for any other iptv-org playlist (e.g. a single-country
  or single-category list) to work with a smaller, faster-loading catalog during development.
  See the [full list of playlists](https://github.com/iptv-org/iptv#playlists).
- `streamflix.cors.allowed-origins` — comma-separated origins allowed to call the API.

## Known limitations

- **Stream availability**: this is a crowd-sourced index of publicly available, mostly
  free-to-air streams. Individual channels regularly go offline, move, or become
  geo-restricted — that's a property of the source data, not a bug in this project. The
  player surfaces a "channel unavailable" state and the UI encourages trying another one.
- **Proxy buffers responses in memory** rather than streaming them chunk-by-chunk, which
  is fine for a learning project but not for high-traffic production use — swap in true
  reactive streaming (`Flux<DataBuffer>`) if you productionize this.
- Respect the terms of the channels you access and the [iptv-org project's guidelines](https://github.com/iptv-org/iptv) —
  it only indexes streams that are already public.
