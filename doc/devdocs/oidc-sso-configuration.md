# OIDC/SSO Configuration

This document explains how OIDC/SSO authentication works in Nexus IQ Server, including the database schema, API endpoints, and JWT claim mapping.

## Overview

OIDC authentication uses two related configuration tables that work together:

1. **`oidc_configuration`** - OAuth/OIDC client settings (how to authenticate with the IdP)
2. **`oauth2_configuration`** - JWT claim mappings (how to extract user identity from tokens)

Both tables share `idp_issuer` as the primary key, linking the OIDC client config to the claim mapping config.

## Database Schema

### Table: `oidc_configuration`

Stores OAuth/OIDC client credentials and endpoints.

| Column | Type | Description |
|--------|------|-------------|
| `idp_issuer` | varchar(255) | PK - Identity Provider issuer URL (e.g., `https://auth0.com/`) |
| `client_id` | varchar(255) | OAuth client ID |
| `client_secret` | varchar(255) | OAuth client secret |
| `idp_authorization_url` | varchar(255) | Authorization endpoint URL |
| `idp_token_url` | varchar(255) | Token endpoint URL |
| `authorization_custom_params_json` | text | JSON object of custom params for auth request |
| `token_request_custom_params_json` | text | JSON object of custom params for token request |

### Table: `oauth2_configuration`

Stores JWT claim mappings for extracting user identity.

| Column | Type | Description |
|--------|------|-------------|
| `idp_issuer` | varchar(255) | PK - Must match `oidc_configuration.idp_issuer` |
| `idp_jwks_url` | varchar(255) | JWKS endpoint for token verification |
| `idp_jws_algorithm` | varchar(255) | Signing algorithm (e.g., `RS256`) |
| `idp_jwks` | text | Cached JWKS keys (optional) |
| `username_claim` | varchar(255) | JWT claim for username |
| `first_name_claim` | varchar(255) | JWT claim for first name |
| `last_name_claim` | varchar(255) | JWT claim for last name |
| `email_claim` | varchar(255) | JWT claim for email |
| `groups_claim` | varchar(255) | JWT claim for group membership |
| `exact_match_claims_json` | text | JSON object for claim-based access restrictions |

> **Note:** The claim columns have no database-level defaults. When a claim column is NULL, `OAuth2Realm` applies runtime defaults: `nickname` for username, `given_name` for first name, `family_name` for last name, `email` for email, and `groups` for group membership.

## API Endpoints

### On-Premises (Single Tenant)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v2/config/oidc` | Get current OIDC configuration |
| `PUT` | `/api/v2/config/oidc` | Create or update OIDC configuration |
| `DELETE` | `/api/v2/config/oidc` | Delete OIDC configuration |

**Permission Required**: `CONFIGURE_SYSTEM`

### Multi-Tenant (MTIQ)

| Method | Path | Description |
|--------|------|-------------|
| `PUT` | `/admin/tenants/{tenantSlug}/sso` | Update tenant SSO configuration |
| `POST` | `/admin/tenants/{tenantSlug}/sso/sync` | Sync SSO provider data sources |

**Permission Required**: MTIQ Admin

### Request/Response Format

The API uses `SsoConfigurationDTO` which contains both configuration objects:

```json
{
  "oAuth2Configuration": {
    "idpIssuer": "https://your-idp.example.com/",
    "idpJwksUrl": "https://your-idp.example.com/.well-known/jwks.json",
    "idpJwsAlgorithm": "RS256",
    "usernameClaim": "email",
    "firstNameClaim": "given_name",
    "lastNameClaim": "family_name",
    "emailClaim": "email",
    "groupsClaim": "groups"
  },
  "oidcConfiguration": {
    "idpIssuer": "https://your-idp.example.com/",
    "clientId": "your-client-id",
    "clientSecret": "your-client-secret",
    "idpAuthorizationUrl": "https://your-idp.example.com/authorize",
    "idpTokenUrl": "https://your-idp.example.com/oauth/token"
  }
}
```

**Important**: The `idpIssuer` in both objects must match exactly.

## Authentication Flow

### 1. User Initiates Login

User is redirected to the IdP authorization URL with client credentials.

### 2. IdP Returns JWT

After successful authentication, the IdP returns a JWT containing claims:

```json
{
  "sub": "auth0|abc123",
  "email": "alice@company.com",
  "given_name": "Alice",
  "family_name": "Smith",
  "groups": ["developers", "admins"]
}
```

### 3. JWT Validation

`OAuth2Realm` validates the JWT using:
- JWKS URL for public key retrieval
- Configured signing algorithm

### 4. Claim Extraction

`OAuth2Realm.buildPrincipal()` extracts user identity using configured claims:

```java
String username = jwtToken.getValueFromClaimOrDefaultClaim(
    configuration.getUsernameClaim(),   // e.g., "email"
    NICKNAME_CLAIM                      // fallback
);
```

### 5. Username Resolution

`OAuth2Realm.getPrincipalUsername()` resolves the username with this fallback chain:

```
configured username_claim (or "nickname" if unconfigured) → email → sub (subject)
```

If the resolved username claim is blank, the email claim value is used. If that is also blank, the JWT `sub` (subject) claim is used as a last resort.

### 6. User Created/Updated

The extracted user info is stored in `oauth2_user` and `oauth2_user_group` tables for caching.

### 7. Authorization

Groups from the `groups_claim` are used for role-based access control.

## Common Configurations

### Using Email as Username

If you want users to be identified by email instead of the default `sub` or `nickname`:

```sql
UPDATE oauth2_configuration
SET username_claim = 'email'
WHERE idp_issuer = 'https://your-idp.example.com/';
```

Or via API (showing only the relevant fields — a full PUT requires all required fields from both objects):

```json
{
  "oAuth2Configuration": {
    "usernameClaim": "email"
  }
}
```

### Restricting Access by Claim

Use `exact_match_claims_json` to restrict access to users with specific claim values:

```json
{
  "exact_match_claims_json": "{\"org_id\":\"org_RxlGhCFqwV6KLSkI\"}"
}
```

This ensures only users with `org_id = "org_RxlGhCFqwV6KLSkI"` in their JWT can authenticate.

### Custom Authorization Parameters

Add custom parameters to the authorization request:

```json
{
  "authorization_custom_params_json": "{\"audience\":\"https://api.example.com\",\"scope\":\"openid profile email\"}"
}
```

## Related Tables

### `oauth2_user`

Caches authenticated user information.

| Column | Type | Description |
|--------|------|-------------|
| `oauth2_user_id` | varchar(255) | PK |
| `username` | varchar(255) | Unique username |
| `first_name` | varchar(255) | User's first name |
| `last_name` | varchar(255) | User's last name |
| `email` | varchar(255) | User's email |
| `groups_json` | text | JSON array of group names |

### `oauth2_group`

Caches group information.

### `oauth2_user_group`

Maps users to groups.

## Troubleshooting

### Usernames Are Wrong

Check `username_claim` in `oauth2_configuration`:

```sql
SELECT username_claim FROM oauth2_configuration WHERE idp_issuer = '<your-issuer>';
```

### Users Can't Authenticate

1. Verify `idp_issuer` matches exactly in both tables
2. Check JWKS URL is accessible
3. Verify signing algorithm matches IdP configuration
4. Check `exact_match_claims_json` restrictions

### Token Validation Fails

1. Ensure `idp_jwks_url` is correct and accessible
2. Verify `idp_jws_algorithm` matches the IdP (usually `RS256`)
3. Check token expiration and clock skew

### Groups Not Applied

1. Verify `groups_claim` matches the claim name in the JWT
2. Check that the IdP is including groups in the token
3. Ensure groups exist in the `oauth2_group` table

## Code References

| Component | Location |
|-----------|----------|
| OIDC Resource | `insight-brain-service/src/main/java/com/sonatype/insight/brain/api/v2/ApiOidcConfigurationResource.java` |
| OIDC Service | `insight-brain-service/src/main/java/com/sonatype/insight/brain/api/v2/service/ApiOidcConfigurationService.java` |
| OAuth2 Realm | `insight-brain-service/src/main/java/com/sonatype/insight/brain/security/oauth2/OAuth2Realm.java` |
| OIDC Login Filter | `insight-brain-service/src/main/java/com/sonatype/insight/brain/security/oauth2/OidcLoginFilter.java` |
| OAuth2 Config Entity | `insight-brain-data/src/main/java/com/sonatype/insight/brain/model/configuration/oauth2/OAuth2Configuration.java` |
| OIDC Config Entity | `insight-brain-data/src/main/java/com/sonatype/insight/brain/model/configuration/oauth2/OidcConfiguration.java` |
| MTIQ Tenant SSO | `nexus-mtiq-server/src/main/java/com/sonatype/insight/brain/api/admin/TenantSsoConfigurationResource.java` |

## IdP-Specific URL Patterns

The JSON structure is the same for all providers (see [Request/Response Format](#requestresponse-format) above). The table below shows the provider-specific URLs and claim differences:

| Field | Auth0 | Azure AD (v2.0) |
|-------|-------|------------------|
| `idpIssuer` | `https://{tenant}.auth0.com/` | `https://login.microsoftonline.com/{tenant-id}/v2.0` |
| `idpJwksUrl` | `https://{tenant}.auth0.com/.well-known/jwks.json` | `https://login.microsoftonline.com/{tenant-id}/discovery/v2.0/keys` |
| `idpAuthorizationUrl` | `https://{tenant}.auth0.com/authorize` | `https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/authorize` |
| `idpTokenUrl` | `https://{tenant}.auth0.com/oauth/token` | `https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/token` |
| `usernameClaim` | `email` | `preferred_username` |
