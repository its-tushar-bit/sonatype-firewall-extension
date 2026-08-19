/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.dataaccess.security.ShiroSessionDAO;
import com.sonatype.insight.brain.security.InsightSessionManager;
import com.sonatype.insight.brain.security.SecurityFilterChainConfigurator;
import com.sonatype.insight.brain.security.ShiroAuthenticatorConfiguration;
import com.sonatype.insight.brain.security.ShiroRealmOrdering;
import com.sonatype.insight.brain.security.SpringShiroServletFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.lang.util.Destroyable;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.SessionListener;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.servlet.ProxiedFilterChain;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.session.mgt.WebSessionManager;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for Shiro security.
 * Defines the security-related beans used by the application context.
 *
 * <p>
 * This configuration provides:
 * <ul>
 * <li>SecurityManager and WebSecurityManager beans</li>
 * <li>Session management configuration</li>
 * <li>Filter chain manager configuration</li>
 * </ul>
 *
 * <p>
 * This configuration imports:
 * <ul>
 * <li>{@link ShiroAuthenticatorConfiguration} - for authenticator bean
 * (needs access to package-private class)</li>
 * <li>{@link SecurityFilterChainConfigurator} - for filter chain configuration
 * (needs access to package-private classes)</li>
 * </ul>
 *
 * <p>
 * <strong>Note on Realm registration:</strong> The Realm classes
 * (InternalRealm, UserTokenRealm, LdapRealm, CrowdRealm, ReverseProxyRealm,
 * SamlRealm, OAuthRealm) are auto-registered via their @Named and @Singleton
 * annotations and Spring's component scanning. They will be collected in the
 * Set&lt;Realm&gt; that is injected into the authenticator.
 *
 * @since 1.7
 */
@Configuration
@Import({
  ShiroAuthenticatorConfiguration.class
})
public class SecurityConfiguration
{
  public static final String SESSION_COOKIE_NAME = "CLMSESSIONID";

  /**
   * Shiro filter order - must run after other filters but before Jersey.
   * Jersey (order 100) must run after Shiro.
   */
  public static final int SHIRO_FILTER_ORDER = 50;

  /**
   * Register a ShiroFilter that extends AbstractShiroFilter for proper filter chain execution.
   * This bypasses the need for EnvironmentLoaderListener.
   */
  @Bean
  public FilterRegistrationBean<Filter> shiroFilterRegistration(
      DefaultWebSecurityManager securityManager,
      FilterChainResolver filterChainResolver)
  {

    Filter shiroFilter = new SpringShiroServletFilter(
        securityManager,
        filterChainResolver);

    FilterRegistrationBean<Filter> registration =
        new FilterRegistrationBean<>(shiroFilter);
    registration.addUrlPatterns("/*");
    registration.setName("shiroFilter");
    registration.setOrder(SHIRO_FILTER_ORDER);
    return registration;
  }

  /**
   * Creates the DisposableBean for Shiro lifecycle management.
   */
  @Bean
  public DisposableBean shiroDestroyer(FilterChainResolver filterChainResolver) {
    return new ShiroDestroyer(filterChainResolver);
  }

  /**
   * Creates the DefaultWebSecurityManager.
   * CLM-6473 - Pre-authentication deserialization vulnerability in IQ Server.
   * The RememberMeManager is explicitly set to null to prevent deserialization attacks.
   */
  @Bean
  @Primary
  public DefaultWebSecurityManager defaultWebSecurityManager(
      Authenticator authenticator,
      Set<Realm> realms,
      DefaultWebSessionManager sessionManager)
  {
    DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
    securityManager.setRememberMeManager(null);
    securityManager.setAuthenticator(authenticator);
    securityManager.setRealms(ShiroRealmOrdering.orderRealms(realms));
    securityManager.setSessionManager(sessionManager);

    // Set static SecurityManager so that async threads (which don't have ThreadContext) can access it
    SecurityUtils.setSecurityManager(securityManager);

    return securityManager;
  }

  /**
   * Exposes the WebSecurityManager interface.
   */
  @Bean
  public WebSecurityManager webSecurityManager(DefaultWebSecurityManager defaultWebSecurityManager) {
    return defaultWebSecurityManager;
  }

  @Bean
  public SecurityManager securityManager(DefaultWebSecurityManager defaultWebSecurityManager) {
    return defaultWebSecurityManager;
  }

  /**
   * Creates the InsightSessionManager (custom WebSessionManager).
   */
  @Bean
  @Primary
  public InsightSessionManager insightSessionManager(
      ShiroSessionDAO shiroSessionDAO,
      Set<SessionListener> sessionListeners)
  {
    InsightSessionManager sessionManager = new InsightSessionManager(shiroSessionDAO, sessionListeners);
    sessionManager.getSessionIdCookie().setName(SESSION_COOKIE_NAME);
    // Disable Shiro's default of adding SameSite=LAX. We want to add SameSite=NONE, but for https requests only
    // since it only works in conjunction with the Secure flag. This is done in SecureCookiesFilter
    sessionManager.getSessionIdCookie().setSameSite(null);
    return sessionManager;
  }

  /**
   * Exposes the DefaultWebSessionManager bean.
   */
  @Bean
  public DefaultWebSessionManager defaultWebSessionManager(InsightSessionManager insightSessionManager) {
    return insightSessionManager;
  }

  /**
   * Exposes the WebSessionManager interface.
   */
  @Bean
  public WebSessionManager webSessionManager(DefaultWebSessionManager defaultWebSessionManager) {
    return defaultWebSessionManager;
  }

  /**
   * Exposes the SessionDAO binding.
   */
  @Bean
  @Primary
  public SessionDAO sessionDAO(ShiroSessionDAO shiroSessionDAO) {
    return shiroSessionDAO;
  }

  /**
   * Creates the FilterChainManager.
   *
   * <p>
   * Note: The DefaultFilterChainManager automatically includes Shiro's default filters
   * like 'anon', 'authcBasic', 'user', etc. via its no-arg constructor.
   */
  @Bean
  public FilterChainManager filterChainManager() {
    DefaultFilterChainManager manager = new DefaultFilterChainManager();
    // DefaultFilterChainManager automatically adds standard filters like anon, authcBasic, etc.
    // We just need to add our custom filters
    return manager;
  }

  /**
   * Creates the PathMatchingFilterChainResolver.
   */
  @Bean
  public PathMatchingFilterChainResolver pathMatchingFilterChainResolver(FilterChainManager filterChainManager) {
    PathMatchingFilterChainResolver resolver = new LoggingPathMatchingFilterChainResolver();
    resolver.setFilterChainManager(filterChainManager);
    return resolver;
  }

  /**
   * Custom resolver that logs which chain is matched for debugging.
   */
  private static class LoggingPathMatchingFilterChainResolver
      extends PathMatchingFilterChainResolver
  {
    private static final Logger log =
        LoggerFactory.getLogger(LoggingPathMatchingFilterChainResolver.class);

    @Override
    public FilterChain getChain(
        ServletRequest request,
        ServletResponse response,
        FilterChain originalChain)
    {
      String requestURI = WebUtils
          .getPathWithinApplication((HttpServletRequest) request);

      // Log all available chains for debugging
      if (log.isTraceEnabled()) {
        log.trace("Available chains for path '{}': {}", requestURI, getFilterChainManager().getChainNames());
      }

      FilterChain chain = super.getChain(request, response, originalChain);

      if (chain != null && chain instanceof ProxiedFilterChain) {
        // The chain name is the path pattern that matched
        log.trace("Matched filter chain for path: {}", requestURI);
      }
      else if (chain == null) {
        log.trace("No filter chain matched for path: {}", requestURI);
      }

      return chain;
    }
  }

  /**
   * Exposes FilterChainResolver interface binding.
   */
  @Bean
  @Primary
  public FilterChainResolver filterChainResolver(PathMatchingFilterChainResolver resolver) {
    return resolver;
  }

  /**
   * DisposableBean to handle Shiro lifecycle.
   */
  private static class ShiroDestroyer
      implements DisposableBean
  {
    private final FilterChainResolver filterChainResolver;

    public ShiroDestroyer(FilterChainResolver filterChainResolver) {
      this.filterChainResolver = filterChainResolver;
    }

    @Override
    public void destroy() throws Exception {
      // Clear the static SecurityManager reference to prevent memory leaks in tests
      // where multiple Spring contexts may be created
      SecurityUtils.setSecurityManager(null);

      // Stop Shiro components, especially its thread pools which otherwise leak memory during tests
      if (filterChainResolver instanceof Destroyable) {
        ((Destroyable) filterChainResolver).destroy();
      }
    }
  }
}
