# SEO Module for CondationCMS

A CondationCMS module that provides SEO essentials: canonical URLs, robots meta tags, Open Graph, Twitter Cards, XML sitemap, and `robots.txt`.

## Features

- Canonical URL generation with configurable query parameter handling
- Robots meta tag with per-node control and query-parameter-aware indexing rules
- Open Graph meta tags
- Twitter Card meta tags
- XML sitemap at `/sitemap.xml`
- `robots.txt` at `/robots.txt` with hook-based extensibility

---

## Site Properties Configuration

All settings live in `site.yaml` (or the equivalent site properties file) under the `seo.*` namespace.

### General

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `seo.canonical.enabled` | boolean | `true` | Enable canonical URL output |
| `seo.opengraph.enabled` | boolean | `true` | Enable Open Graph meta tags |
| `seo.twitter.enabled` | boolean | `true` | Enable Twitter Card meta tags |
| `seo.sitemap.enabled` | boolean | `true` | Enable `/sitemap.xml` route and sitemap link in `robots.txt` |
| `seo.robotstxt.enabled` | boolean | `true` | Enable `/robots.txt` route |

---

### Canonical URL

The canonical URL is built from the node's URI plus the site's base URL. Query parameters from the current request are appended selectively based on the whitelist below.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `seo.canonical.queryParameters` | list of strings | `["page"]` | Query parameters to include in the canonical URL |

**Special behaviour for `page`:** only appended when `page > 1` — `/blog?page=1` canonicalises to `/blog`.

**Example:**

```toml
[seo.canonical]
enabled = true
queryParameters = ["page", "tag", "category"]
```

---

### Robots Meta Tag

The robots meta tag is emitted only when at least one rule applies. Possible values are `noindex,nofollow` (node-level opt-out) and `noindex,follow` (query-parameter-driven).

#### Per-node frontmatter

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `seo.index` | boolean | `true` | Set to `false` to emit `noindex,nofollow` for this node |
| `seo.follow` | boolean | `true` | Controls the `follow`/`nofollow` part when `seo.index` is `false` |

#### Query-parameter rules (site properties)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `seo.query.noindexParameters` | list of strings | `[]` | Parameters that always trigger `noindex,follow`, regardless of count (e.g. `sort`, `limit`) |
| `seo.query.contentParameters` | list of strings | `["page"]` | Parameters that represent indexable content (e.g. `tag`, `category`) |
| `seo.query.maxIndexableContentParameters` | integer | `-1` (disabled) | Maximum number of active content parameters before `noindex,follow` is triggered |

**Evaluation order:**

1. `seo.index: false` on the node → `noindex,nofollow` (stops further evaluation)
2. Any active `noindexParameters` present in the request → `noindex,follow`
3. Active content parameters (excluding `page` and `noindexParameters`) exceed `maxIndexableContentParameters` → `noindex,follow`
4. Otherwise: no robots tag is emitted (browser default: `index,follow`)

**Example — single curated filter indexable, navigation params always noindex:**

```toml
[seo.canonical]
enabled = true
queryParameters = ["page", "tag", "category", "sort"]

[seo.query]
noindexParameters = ["sort", "limit"]
contentParameters = ["tag", "category"]
maxIndexableContentParameters = 1
```

| URL | Canonical | Robots tag |
|-----|-----------|------------|
| `/blog` | `/blog` | — |
| `/blog?page=2` | `/blog?page=2` | — |
| `/blog?tag=java` | `/blog?tag=java` | — |
| `/blog?tag=java&page=3` | `/blog?tag=java&page=3` | — |
| `/blog?sort=price` | `/blog?sort=price` | `noindex,follow` |
| `/blog?tag=java&category=cms` | `/blog?tag=java&category=cms` | `noindex,follow` |
| `/blog?tag=java&utm_source=nl` | `/blog?tag=java` | — |

---

### Open Graph

| Key | Scope | Description |
|-----|-------|-------------|
| `seo.og.type` | site properties | Default OG type (fallback: `website`) |
| `seo.title` | node frontmatter | Shared title for OG and Twitter |
| `seo.description` | node frontmatter | Shared description |
| `seo.image` | node frontmatter | Shared image URL |
| `seo.url` | node frontmatter | Shared URL override |
| `seo.og.type` | node frontmatter | Per-node OG type override |
| `seo.og.url` | node frontmatter | Per-node OG URL override |
| `seo.og.title` | node frontmatter | Per-node OG title override |
| `seo.og.description` | node frontmatter | Per-node OG description override |
| `seo.og.image` | node frontmatter | Per-node OG image override |

---

### Twitter Cards

| Key | Scope | Description |
|-----|-------|-------------|
| `seo.twitter.card` | site properties | Default card type (fallback: `summary`) |
| `seo.twitter.site` | site properties | Twitter `@username` of the site |
| `seo.twitter.creator` | site properties | Twitter `@username` of the content creator |
| `seo.twitter.card` | node frontmatter | Per-node card type override |
| `seo.twitter.url` | node frontmatter | Per-node URL override |
| `seo.twitter.title` | node frontmatter | Per-node title override |
| `seo.twitter.description` | node frontmatter | Per-node description override |
| `seo.twitter.image` | node frontmatter | Per-node image override |
| `seo.twitter.site` | node frontmatter | Per-node site handle override |
| `seo.twitter.creator` | node frontmatter | Per-node creator handle override |

---

### Sitemap

The sitemap at `/sitemap.xml` includes all nodes where `seo.index` is not set to `false`. Each entry contains `<loc>` and `<lastmod>`.

---

### robots.txt

The `robots.txt` at `/robots.txt` is generated dynamically. When `seo.sitemap` is enabled, a `Sitemap:` directive pointing to `/sitemap.xml` is appended automatically.

Additional rules can be injected via the hook `module/seo/robotstxt`. The hook receives a `RobotsTxt` object and returns the modified version.

**Hook example:**

Javascript sample
```javascript
$hooks.registerFilter("module/seo/robotstxt", (robots) -> {
    robots.group("*").disallow("/admin");
    robots.group("Googlebot").allow("/");
    return robots;
});
```

Sample for java
```java
@Filter("module/seo/robotstxt")
public void extendRobotsTxt(RobotsTxt robots) {
    robots.group("*").disallow("/admin");
    robots.group("Googlebot").allow("/");
    return robots;
});
```

---

## Build

```bash
mvn clean package
```

Requires Java 25 and CondationCMS API `8.2.0`.

---

## License

GPL v3 — see [LICENSE](https://www.gnu.org/licenses/gpl-3.0.html).
