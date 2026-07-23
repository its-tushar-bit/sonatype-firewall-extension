<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Authenticated username in the request log

Draft date: 20 Jul 2026 (CLM-42654; follows CLM-41689)

## Background

The IQ request log can be rendered three different ways, and all three read from the shared logback-access
`IAccessEvent`. The username of the authenticated caller is *not* directly available on that event: the
logback-access `ch.qos.logback.access.jetty.RequestWrapper.getRemoteUser()` is stubbed to `return null`, so any
render path that sources the user from `getRemoteUser()` produces no username.

`AuthenticationLoggingFilter` works around this by publishing the username as a request attribute,
`com.sonatype.insight.requestlog.remoteUser` (constant `AuthenticationLoggingFilter.REQUEST_LOG_REMOTE_USER_ATTRIBUTE`),
for authenticated requests only. Each render path then reads that attribute instead of `getRemoteUser()`.

| Render path | Selector | How the username is surfaced | Fixed in |
| --- | --- | --- | --- |
| Classic Jetty `CustomRequestLog` | `%u` | A bare `Request.AuthenticationState` carrying the principal | CLM-41689 |
| logback-access **pattern** | `%user` / `%u` | Rewritten to `%reqAttribute{com.sonatype.insight.requestlog.remoteUser}` | CLM-41689 |
| logback-access **`access-json`** | `layout: {type: access-json}` | `RemoteUserAccessJsonLayout` sets `remoteUser` from the request attribute | CLM-42654 |

## `access-json` implementation (CLM-42654)

Operators keep configuring `layout: {type: access-json}` unchanged. Internally, `RequestLoggingConfiguration`
rewrites the appender's `layout.type` to `iq-access-json` (`RemoteUserAccessJsonLayoutFactory.TYPE_NAME`), a
discoverable factory registered in `META-INF/services/io.dropwizard.logging.common.layout.DiscoverableLayoutFactory`.
That factory builds `RemoteUserAccessJsonLayout`, an `AccessJsonLayout` subclass whose `toJsonMap` sets the canonical
top-level `remoteUser` field from the request attribute (`"-"` when absent, i.e. anonymous). It inherits every stock
`access-json` option (`includes`, `requestHeaders`, `responseHeaders`, `requestAttributes`, `customFieldNames`,
`additionalFields`).

Why a custom layout and not `customFieldNames`: `customFieldNames` only renames a field's key; it cannot change the
value source. The stock `remoteUser` value is hardwired to `IAccessEvent.getRemoteUser()` (the stubbed `null`), so no
rename can inject the username.

## JSON-shape change for downstream consumers (SIEM / log ingestion)

This is a behavioural change to the `access-json` output. Anything that parses these logs should be aware:

- **`remoteUser` was previously absent** for authenticated requests. The stock layout builds the field from
  `getRemoteUser()` (the stubbed `null`), and its `MapBuilder` **omits null-valued fields entirely** — so the key did
  not appear at all (it was *not* present-as-`null`).
- **`remoteUser` is now always present**: the authenticated username, or `"-"` for anonymous requests. This matches
  the classic and pattern paths.
- **It is populated even if an operator excludes `REMOTE_USER` via `includes`.** Surfacing the username is the point
  of this layout, so `includes` cannot suppress `remoteUser`. Operators who need the field gone cannot remove it
  through `access-json` configuration. (Pinned by `accessJsonLayoutRendersRemoteUserEvenWhenExcludedViaIncludes`.)
- **The key is always literally `remoteUser`.** `customFieldNames` renaming of `remoteUser` is not honoured for the
  populated value; the canonical key is always used.

### Open action

Confirm with the JSON-log consumers (Support / SIEM ingestion) which key they key off of before release, per the
CLM-42654 acceptance criteria. The change is backwards-compatible for consumers that treat a missing `remoteUser` as
"no user", but consumers that assumed the field never appeared, or that relied on a `customFieldNames` rename of it,
will see the new always-present `remoteUser`.
