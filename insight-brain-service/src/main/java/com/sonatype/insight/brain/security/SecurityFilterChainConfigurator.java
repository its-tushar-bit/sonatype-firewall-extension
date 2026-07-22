/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.security.oauth2.JwtAuthenticationFilter;
import com.sonatype.insight.brain.security.oauth2.OidcLoginFilter;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Collections;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.slf4j.LoggerFactory;

/**
 * Configures Shiro filter chains for security.
 *
 * <p>
 * This class is placed in the security package to access package-private filter classes
 * (MissingAuthenticationFilter, SamlFilter, UserFriendlyBasicHttpAuthenticationFilter)
 * that cannot be referenced from outside the package.
 *
 * <p>
 * Centralizes Shiro filter-chain registration for the Spring-based server.
 */
@Named
@Singleton
public class SecurityFilterChainConfigurator
{
  private static final String CUSTOM_CSRF_FILTERS =
      "noSessionCreation, clientIPAddressFilter, antiCsrf[%s], " +
          "reverseProxy, sessionExpirationCookie, secureCookies, authcJWT, authcBasic, requireAuth";

  private final FilterChainManager filterChainManager;

  private final AntiCsrfFilter antiCsrfFilter;

  private final ClientIPAddressFilter clientIPAddressFilter;

  private final ApiAccessControlFilter apiAccessControlFilter;

  private final UserFriendlyBasicHttpAuthenticationFilter basicHttpAuthenticationFilter;

  private final ReverseProxyAuthenticationFilter reverseProxyAuthenticationFilter;

  private final SecureCookiesFilter secureCookiesFilter;

  private final SessionExpirationCookieFilter sessionExpirationCookieFilter;

  private final MissingAuthenticationFilter missingAuthenticationFilter;

  private final SamlFilter samlFilter;

  private final InvalidRequestFilter invalidRequestFilter;

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  private final OidcLoginFilter oidcLoginFilter;

  private final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  @Inject
  public SecurityFilterChainConfigurator(
      FilterChainManager filterChainManager,
      AntiCsrfFilter antiCsrfFilter,
      ClientIPAddressFilter clientIPAddressFilter,
      ApiAccessControlFilter apiAccessControlFilter,
      UserFriendlyBasicHttpAuthenticationFilter basicHttpAuthenticationFilter,
      ReverseProxyAuthenticationFilter reverseProxyAuthenticationFilter,
      SecureCookiesFilter secureCookiesFilter,
      SessionExpirationCookieFilter sessionExpirationCookieFilter,
      MissingAuthenticationFilter missingAuthenticationFilter,
      SamlFilter samlFilter,
      InvalidRequestFilter invalidRequestFilter,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      OidcLoginFilter oidcLoginFilter,
      QuarantinedComponentAccessDAO quarantinedComponentAccessDAO)
  {
    this.filterChainManager = filterChainManager;
    this.antiCsrfFilter = antiCsrfFilter;
    this.clientIPAddressFilter = clientIPAddressFilter;
    this.apiAccessControlFilter = apiAccessControlFilter;
    this.basicHttpAuthenticationFilter = basicHttpAuthenticationFilter;
    this.reverseProxyAuthenticationFilter = reverseProxyAuthenticationFilter;
    this.secureCookiesFilter = secureCookiesFilter;
    this.sessionExpirationCookieFilter = sessionExpirationCookieFilter;
    this.missingAuthenticationFilter = missingAuthenticationFilter;
    this.samlFilter = samlFilter;
    this.invalidRequestFilter = invalidRequestFilter;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.oidcLoginFilter = oidcLoginFilter;
    this.quarantinedComponentAccessDAO = quarantinedComponentAccessDAO;
  }

  /**
   * Configures all filters and filter chains.
   * Called during Spring bean initialization.
   */
  @PostConstruct
  public void configure() {
    configureFilters();
    configureFilterChains(filterChainManager,
        quarantinedComponentAccessDAO.isAnonymousAccessEnabled());

    // Log configured chains for debugging
    LoggerFactory.getLogger(SecurityFilterChainConfigurator.class)
        .info("Configured {} filter chains: {}",
            filterChainManager.getChainNames().size(),
            filterChainManager.getChainNames());
  }

  private void configureFilters() {
    filterChainManager.addFilter("antiCsrf", antiCsrfFilter);
    filterChainManager.addFilter("authcBasic", basicHttpAuthenticationFilter);
    filterChainManager.addFilter("clientIPAddressFilter", clientIPAddressFilter);
    filterChainManager.addFilter("apiAccessControlFilter", apiAccessControlFilter);
    filterChainManager.addFilter("requireAuth", missingAuthenticationFilter);
    filterChainManager.addFilter("saml", samlFilter);
    filterChainManager.addFilter("reverseProxy", reverseProxyAuthenticationFilter);
    filterChainManager.addFilter("secureCookies", secureCookiesFilter);
    filterChainManager.addFilter("sessionExpirationCookie", sessionExpirationCookieFilter);
    filterChainManager.addFilter("sonatypeInvalidRequest", invalidRequestFilter);
    filterChainManager.addFilter("authcJWT", jwtAuthenticationFilter);
    filterChainManager.addFilter("oidc", oidcLoginFilter);
    filterChainManager.setGlobalFilters(Collections.singletonList("sonatypeInvalidRequest"));
  }

  private void configureFilterChains(FilterChainManager manager, boolean isAnonymousAccessEnabled) {
    configureFilterChainsForIntegrations(manager);

    String anonFilters = "anon, clientIPAddressFilter, secureCookies, sessionExpirationCookie";
    String anonUnrestrictedIPFilters = "anon, secureCookies, sessionExpirationCookie";
    String telemetryFilters = "anon, clientIPAddressFilter, secureCookies";

    // Activate the antiCsrf filter for static assets so that the first resource loaded for any given page sets the CSRF
    // token cookie. We want the cookie to be available for the front-end code as soon as possible so that subsequent
    // requests that are unsafe can access it.
    manager.createChain("/*assets/**", anonUnrestrictedIPFilters + ", antiCsrf"); // assets for the web interface
    manager.createChain("/rest/ide/brain/**", anonFilters); // only redirects
    manager.createChain("/rest/report/*/*/brain/**", anonFilters); // only redirects
    manager.createChain("/rest/user/session/logout", anonFilters); // client logout requires no auth
    manager.createChain("/rest/user-telemetry/javascript", telemetryFilters); // user-telemetry javascript
    manager.createChain("/rest/user-telemetry/config", telemetryFilters); // user-telemetry configuration
    manager.createChain("/rest/user-telemetry/events/**", telemetryFilters); // user-telemetry events
    manager.createChain("/rest/product/version", anonFilters); // product version info
    manager.createChain("/rest/product/license/validate", anonFilters); // product license info
    // endpoint used to decide whether we have unauthenticated pages or not
    manager.createChain("/rest/product/features/enableUnauthenticatedPages", anonFilters);
    manager.createChain("/rest/product/features/enableSsoOnly", anonFilters);
    manager.createChain("/rest/product/features/oauth2Enabled", anonFilters);
    manager.createChain("/rest/version", anonFilters); // product version info
    manager.createChain("/ui/links/**", anonFilters); // only redirects
    manager.createChain("/rest/config/systemNotice/fetch", anonFilters);
    manager.createChain("/api/v2/vulnerabilities",
        anonFilters + ", noSessionCreation, " +
            "reverseProxy[" + ReverseProxyAuthenticationFilter.NO_SESSION_CREATION + ",permissive], " +
            "authcJWT[permissive], authcBasic[permissive]");
    manager.createChain("/api/v2/vulnerabilities/*",
        anonFilters + ", noSessionCreation, " +
            "reverseProxy[" + ReverseProxyAuthenticationFilter.NO_SESSION_CREATION + ",permissive], " +
            "authcJWT[permissive], authcBasic[permissive]");
    if (isAnonymousAccessEnabled) {
      manager.createChain("/rest/repositories/quarantinedComponent/**", //
          anonFilters + ", noSessionCreation, " //
              + "reverseProxy[" + ReverseProxyAuthenticationFilter.NO_SESSION_CREATION + ",permissive], " //
              + "authcJWT[permissive], authcBasic[permissive]");
    }
    manager.createChain("/api/v2/endpoints/*",
        anonFilters + ", noSessionCreation, " +
            "reverseProxy[" + ReverseProxyAuthenticationFilter.NO_SESSION_CREATION + ",permissive], " +
            "authcJWT[permissive], authcBasic[permissive]");
    manager.createChain("/ping", anonUnrestrictedIPFilters);

    // Legal attribution report doesn't need CSRF check as it doesn't update server state (despite being POST form)
    manager.createChain("/api/v2/licenseLegalMetadata/application/*/stage/*/report",
        String.format(CUSTOM_CSRF_FILTERS, AntiCsrfFilter.FORM_POST_ALLOWED));
    manager.createChain("/rest/legal/attribution/multiApplication/activeUserFilter/report",
        String.format(CUSTOM_CSRF_FILTERS, AntiCsrfFilter.FORM_POST_ALLOWED));

    // The UI uses this path to get the config for anonymous access for the Quarantined Component View.
    // This must work for unauthenticated users when the anonymous access is enabled for this page.
    manager.createChain("/api/v2/firewall/quarantinedComponentView/configuration/anonymousAccess",
        anonFilters + ", antiCsrf");

    // public REST API and MCP endpoint share the same auth chain
    String apiFilters = "noSessionCreation, clientIPAddressFilter, " +
        "antiCsrf[" + AntiCsrfFilter.EXPLICIT_AUTH_ALLOWED + "], " +
        "reverseProxy[" + ReverseProxyAuthenticationFilter.NO_SESSION_CREATION + "], authcJWT, authcBasic, " +
        "saml, apiAccessControlFilter, requireAuth";
    manager.createChain("/mcp", apiFilters);
    manager.createChain("/mcp/**", apiFilters);
    manager.createChain("/api/**", apiFilters);

    // login, only means to create sessions, also used by integrations for auth validation
    manager.createChain("/rest/user/session", "sessionExpirationCookie, clientIPAddressFilter, antiCsrf["
        + AntiCsrfFilter.EXPLICIT_AUTH_ALLOWED +
        "], reverseProxy, secureCookies, authcJWT, authcBasic, saml, requireAuth");

    configureFilterChainsForNonAjaxFormSubmissions(manager);

    // SAML callbacks
    manager.createChain("/saml/**", "clientIPAddressFilter, secureCookies, saml");

    // OAuth callbacks
    manager.createChain("/oidc/**", "clientIPAddressFilter, secureCookies, oidc");

    // internal REST API
    manager.createChain("/**/*",
        "noSessionCreation, clientIPAddressFilter, antiCsrf, reverseProxy, secureCookies, authcJWT" +
            ", authcBasic, saml, requireAuth, sessionExpirationCookie");
  }

  private void configureFilterChainsForNonAjaxFormSubmissions(FilterChainManager manager) {
    // old-school (i.e. non-AJAX) form submissions as done by IE9 can't use CSRF header
    String filters = String.format(CUSTOM_CSRF_FILTERS, AntiCsrfFilter.FORM_POST_ALLOWED);
    manager.createChain("/rest/application/icon/*", filters);
    manager.createChain("/rest/application/icon/sync", filters);
    manager.createChain("/rest/organization/icon/*", filters);
    manager.createChain("/rest/organization/icon/sync", filters);
    manager.createChain("/rest/policy/*/*/import", filters);
    manager.createChain("/rest/policy/*/*/import/ie", filters);
    manager.createChain("/rest/product/license", filters);
    manager.createChain("/rest/scan/*", filters);
    manager.createChain("/rest/dashboard/export/newestRisks", filters);
    manager.createChain("/rest/dashboard/export/componentRisks", filters);
    manager.createChain("/rest/dashboard/export/applicationRisks", filters);
    manager.createChain("/rest/dashboard/export/policyWaivers", filters);
    manager.createChain("/rest/dashboard/export/policyWaiverRequests", filters);
    manager.createChain("/rest/dashboard/vulnerabilities/export", filters);
  }

  private void configureFilterChainsForIntegrations(FilterChainManager manager) {
    // client integrations don't have CSRF tokens and need access via explicit auth
    String filters = String.format(CUSTOM_CSRF_FILTERS, AntiCsrfFilter.EXPLICIT_AUTH_ALLOWED);
    manager.createChain("/rest/ide/scan/**", filters);
    // SLO violation feed (CLM-42077): internal integration endpoint consumed with explicit (API-token) auth, no
    // CSRF token. Declared here so the auth intent is encoded in the security config rather than relying on the
    // catch-all antiCsrf chain.
    manager.createChain("/rest/slo/**", filters);
    manager.createChain("/rest/integration/repositories/**", filters);
    manager.createChain("/rest/integration/artifactory/**", filters);
    manager.createChain("/rest/quality/evaluations/*/*", filters);
    manager.createChain("/rest/report/*/*/downloadBundle", filters);

    manager.createChain("/rest/integration/applications", filters);
    manager.createChain("/rest/application/services/names", filters);
    manager.createChain("/rest/application/validate/*", filters);
    manager.createChain("/rest/policy/*/evaluate", filters);
    manager.createChain("/rest/ci/scan/*", filters);
    manager.createChain("/rest/cli/scan/*", filters);
    manager.createChain("/rest/rm/scan/*", filters);
    manager.createChain("/rest/config/proprietary", filters);
    manager.createChain("/rest/policy/stages", filters);
    manager.createChain("/api/v2/repositories/**", filters);
  }

}
