/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.sonatype.insight.brain.configuration.ldap.LdapRealm;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.ReverseProxyAuthenticationConfig;

import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import io.dropwizard.lifecycle.Managed;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.SessionListener;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.MemorySessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.session.mgt.WebSessionManager;

/**
 * Configures Shiro security.
 *
 * @since 1.7
 */
public class SecurityModule
    extends ShiroModule
{
  public static final String SESSION_COOKIE_NAME = "CLMSESSIONID";

  private static final String AUTHC_SCHEME = "nonBrowserPromptingBasic";

  private final boolean anonymousClientAccessAllowed;

  private final boolean csrfProtection;

  private final ReverseProxyAuthenticationConfig reverseProxyAuthentication;

  public SecurityModule(InsightConfig config) {
    this.anonymousClientAccessAllowed = config.isAnonymousClientAccessAllowed();
    this.csrfProtection = config.isCsrfProtection();
    this.reverseProxyAuthentication = config.getReverseProxyAuthentication();
  }

  @Override
  protected void configureShiro() {
    bind(Managed.class).toInstance(new Destroyer());
    expose(Managed.class);
    TypeLiteral<Collection<SessionListener>> sessionListenerCollectionType =
        new TypeLiteral<Collection<SessionListener>>()
      {
      };
    bind(sessionListenerCollectionType).toProvider(SessionListenerProvider.class);
    bindWebSecurityManager(bind(WebSecurityManager.class));
    expose(WebSecurityManager.class);
    bind(FilterChainResolver.class).to(PathMatchingFilterChainResolver.class);
    bind(PathMatchingFilterChainResolver.class);
    expose(FilterChainResolver.class);
    bind(FilterChainManager.class).to(DefaultFilterChainManager.class);
    DefaultFilterChainManager manager = new DefaultFilterChainManager();
    configureFilters(manager);
    configureFilterChains(manager);
    bind(DefaultFilterChainManager.class).toInstance(manager);
    bind(Authenticator.class).to(FirstSuccessfulRealmAuthenticator.class);
    bindRealm().to(InternalRealm.class);
    bindRealm().to(LdapRealm.class);
    bindRealm().to(ReverseProxyRealm.class);
    binder().requestInjection(new SessionCookieCustomizer());
  }

  private void configureFilters(DefaultFilterChainManager manager) {
    AntiCsrfFilter antiCsrfFilter = new AntiCsrfFilter(csrfProtection, reverseProxyAuthentication);
    bind(AntiCsrfFilter.class).toInstance(antiCsrfFilter);
    expose(AntiCsrfFilter.class);
    manager.addFilter("authcBasic", new UserFriendlyBasicHttpAuthenticationFilter());
    manager.addFilter("secureCookies", new SecureCookiesFilter());
    manager.addFilter("antiCsrf", antiCsrfFilter);
    manager.addFilter("reverseProxy", new ReverseProxyAuthenticationFilter(reverseProxyAuthentication));
    manager.addFilter("noSessionReverseProxy", new ReverseProxyAuthenticationFilter(reverseProxyAuthentication, false));
    manager.addFilter("authcNoChallengeBasic", new NoChallengeBasicHttpAuthenticationFilter());
    manager.addFilter("sessionExpirationCookie", new SessionExpirationCookieFilter());
    // change the auth type so browsers don't prompt for login details
    BasicHttpAuthenticationFilter.class.cast(manager.getFilter("authcBasic")).setAuthcScheme(AUTHC_SCHEME);
  }

  private void configureFilterChains(DefaultFilterChainManager manager) {
    configureFilterChainsForIntegrations(manager);

    String anonFilters = "anon, sessionExpirationCookie";
    // Activate the antiCsrf filter for static assets so that the first resource loaded for any given page sets the CSRF
    // token cookie. We want the cookie to be available for the front-end code as soon as possible so that subsequent
    // requests that are unsafe can access it.
    manager.createChain("/*assets/**", anonFilters + ", antiCsrf"); // assets for the web interface
    manager.createChain("/favicon.ico", anonFilters); // favicon for web interface
    manager.createChain("/rest/ide/brain/**", anonFilters); // only redirects
    manager.createChain("/rest/report/*/*/brain/**", anonFilters); // only redirects
    manager.createChain("/rest/user/session/logout", anonFilters); // client logout requires no auth
    manager.createChain("/rest/user-telemetry/javascript", anonFilters); // user-telemetry javascript
    manager.createChain("/rest/product/version", anonFilters); // product version info
    manager.createChain("/rest/version", anonFilters); // product version info
    manager.createChain("/tasks/**", anonFilters); // DW tasks exposed on admin port
    manager.createChain("/ui/links/**", anonFilters); // only redirects
    manager.createChain("/rest/config/systemNotice/fetch", anonFilters);

    // public REST API
    manager.createChain("/api/**", "noSessionCreation, antiCsrf[" + AntiCsrfFilter.EXPLICIT_AUTH_ALLOWED
        + "], noSessionReverseProxy, authcNoChallengeBasic");

    // login, only means to create sessions, also used by integrations for auth validation
    manager.createChain("/rest/user/session", "sessionExpirationCookie, antiCsrf["
        + AntiCsrfFilter.EXPLICIT_AUTH_ALLOWED + "], reverseProxy, authcBasic, secureCookies");

    configureFilterChainsForNonAjaxFormSubmissions(manager);

    // internal REST API
    manager.createChain("/**/*", "noSessionCreation, antiCsrf, reverseProxy, authcBasic, sessionExpirationCookie");
  }

  private void configureFilterChainsForNonAjaxFormSubmissions(DefaultFilterChainManager manager) {
    // old-school (i.e. non-AJAX) form submissions as done by IE9 can't use CSRF header
    String filters = "noSessionCreation, antiCsrf[" + AntiCsrfFilter.FORM_POST_ALLOWED
        + "], reverseProxy, authcBasic, sessionExpirationCookie";
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
  }

  private void configureFilterChainsForIntegrations(DefaultFilterChainManager manager) {
    // client integrations don't have CSRF tokens and need access via explicit auth
    String filters = "noSessionCreation, antiCsrf[" + AntiCsrfFilter.EXPLICIT_AUTH_ALLOWED
        + "], reverseProxy, sessionExpirationCookie, authcBasic";
    manager.createChain("/rest/ide/scan/**", filters);
    manager.createChain("/rest/integration/repositories/**", filters);
    manager.createChain("/rest/quality/evaluations/*/*", filters);
    manager.createChain("/rest/report/*/*/downloadBundle", filters);

    // for backward-compat, these can still support anonymous access
    filters += anonymousClientAccessAllowed ? "[permissive]" : "";
    manager.createChain("/rest/integration/applications", filters);
    manager.createChain("/rest/report/*/*/embedReport/**", filters);
    manager.createChain("/rest/application/services/names", filters);
    manager.createChain("/rest/application/validate/*", filters);
    manager.createChain("/rest/policy/*/evaluate", filters);
    manager.createChain("/rest/ci/scan/*", filters);
    manager.createChain("/rest/cli/scan/*", filters);
    manager.createChain("/rest/rm/scan/*", filters);
    manager.createChain("/rest/config/proprietary", filters);
    manager.createChain("/rest/policy/stages", filters); // licensed build stages
  }

  @Override
  protected void bindSessionManager(AnnotatedBindingBuilder<SessionManager> bind) {
    bind.to(WebSessionManager.class);
    bind(WebSessionManager.class).to(DefaultWebSessionManager.class);
    bind(DefaultWebSessionManager.class).in(Singleton.class);
    expose(DefaultWebSessionManager.class);
    bind(SessionDAO.class).to(MemorySessionDAO.class).in(Singleton.class);
    expose(SessionDAO.class);
  }

  @Override
  protected void bindSecurityManager(AnnotatedBindingBuilder<? super SecurityManager> bind) {
    /*
     * NOTE: Not using bindWebSecurityManager() as in ShiroWebModule to avoid
     * https://issues.apache.org/jira/browse/SHIRO-435.
     */
    bind.to(WebSecurityManager.class);
  }

  protected void bindWebSecurityManager(AnnotatedBindingBuilder<? super WebSecurityManager> bind) {
    // CLM-6473 - Pre-authentication deserialization vulnerability in IQ Server
    final DefaultWebSecurityManager defaultWebSecurityManager = new DefaultWebSecurityManager();
    defaultWebSecurityManager.setRememberMeManager(null);

    /*
     * NOTE: Not using bind.to( DefaultWebSecurityManager.class) to avoid
     * https://issues.apache.org/jira/browse/SHIRO-369.
     */
    bind.toInstance(defaultWebSecurityManager);
    /*
     * TODO: The above will trigger ShiroModule.BeanTypeListener which eventually sets a DefaultSessionManager
     * on the security manager, replacing the ServletContainerSessionManager it uses by default. Is this what we
     * need/want? Also see https://issues.apache.org/jira/browse/SHIRO-443.
     */
  }

  private static class SessionCookieCustomizer
  {
    @Inject
    public void customize(DefaultWebSessionManager sessionManager) {
      // customize cookie name to avoid clash with other webapps running on same host+contextRoot
      sessionManager.getSessionIdCookie().setName(SESSION_COOKIE_NAME);
    }
  }

  private class Destroyer
      implements Managed
  {
    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
      // stop Shiro components, especially its thread pools which otherwise leak memory during tests
      destroy();
    }
  }
}
