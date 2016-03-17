/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.sonatype.insight.brain.ldap.LdapRealm;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.ReverseProxyAuthenticationConfig;

import com.google.inject.binder.AnnotatedBindingBuilder;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.mgt.SecurityManager;
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
    AntiCsrfFilter antiCsrfFilter = new AntiCsrfFilter(csrfProtection);
    bind(AntiCsrfFilter.class).toInstance(antiCsrfFilter);
    expose(AntiCsrfFilter.class);
    manager.addFilter("authcBasic", new UserFriendlyBasicHttpAuthenticationFilter());
    manager.addFilter("authcBasicMandatory", new BasicHttpAuthenticationMandatoryFilter());
    manager.addFilter("secureCookies", new SecureCookiesFilter());
    manager.addFilter("antiCsrf", antiCsrfFilter);
    manager.addFilter("reverseProxy", new ReverseProxyAuthenticationFilter(reverseProxyAuthentication));
    // change the auth type so browsers don't prompt for login details
    BasicHttpAuthenticationFilter.class.cast(manager.getFilter("authcBasic")).setAuthcScheme(AUTHC_SCHEME);
  }

  private void configureFilterChains(DefaultFilterChainManager manager) {
    configureFilterChainsForIntegrations(manager);

    manager.createChain("/*assets/**", "anon"); // assets for the web interface
    manager.createChain("/favicon.ico", "anon"); // favicon for web interface
    manager.createChain("/rest/ide/brain/**", "anon"); // only redirects
    manager.createChain("/rest/report/*/*/brain/**", "anon"); // only redirects
    manager.createChain("/rest/user/session/logout", "anon"); // client logout requires no auth, will simply do nothing
                                                              // if not authenticated
    manager.createChain("/rest/product/version", "anon"); // product version info
    manager.createChain("/rest/version", "anon"); // product version info
    manager.createChain("/tasks/**", "anon"); // DW tasks exposed on admin port
    manager.createChain("/ui/links/**", "anon"); // only redirects

    // public REST API, no sessions supported/allowed, no SSO support
    manager.createChain("/api/**", "noSessionCreation, authcBasicMandatory");

    // login, only means to create sessions, also used by integrations for auth validation
    manager.createChain("/rest/user/session", "antiCsrf[" + AntiCsrfFilter.EXPLICIT_AUTH_ALLOWED
        + "], reverseProxy, authcBasic, secureCookies");

    configureFilterChainsForNonAjaxFormSubmissions(manager);

    // internal REST API
    manager.createChain("/**/*", "noSessionCreation, antiCsrf, reverseProxy, authcBasic");
  }

  private void configureFilterChainsForNonAjaxFormSubmissions(DefaultFilterChainManager manager) {
    // old-school (i.e. non-AJAX) form submissions as done by IE9 can't use CSRF header
    String filters = "noSessionCreation, antiCsrf[" + AntiCsrfFilter.FORM_POST_ALLOWED + "], reverseProxy, authcBasic";
    manager.createChain("/rest/application/icon", filters);
    manager.createChain("/rest/application/icon/sync", filters);
    manager.createChain("/rest/organization/icon", filters);
    manager.createChain("/rest/organization/icon/sync", filters);
    manager.createChain("/rest/policy/*/*/import", filters);
    manager.createChain("/rest/policy/*/*/import/ie", filters);
    manager.createChain("/rest/product/license", filters);
    manager.createChain("/rest/scan/*", filters);
  }

  private void configureFilterChainsForIntegrations(DefaultFilterChainManager manager) {
    // client integrations don't have CSRF tokens and need access via explicit auth
    String filters = "noSessionCreation, antiCsrf[" + AntiCsrfFilter.EXPLICIT_AUTH_ALLOWED
        + "], reverseProxy, authcBasic";
    manager.createChain("/rest/ide/scan/**", filters);
    manager.createChain("/rest/integration/repositories/**", filters);
    manager.createChain("/rest/quality/evaluations/*/*", filters);
    manager.createChain("/rest/report/*/*/downloadBundle", filters);

    // for backward-compat, these can still support anonymous access
    filters += (anonymousClientAccessAllowed ? "[permissive]" : "");
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
    /*
     * NOTE: Not using bind.to( DefaultWebSecurityManager.class) to avoid
     * https://issues.apache.org/jira/browse/SHIRO-369.
     */
    bind.toInstance(new DefaultWebSecurityManager());
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
}
