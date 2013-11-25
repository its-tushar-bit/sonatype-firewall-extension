/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Singleton;

import com.sonatype.insight.brain.ldap.LdapRealm;

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
public class CLMShiroModule
    extends ShiroModule
{
  @Override
  protected void configureShiro() {
    bindWebSecurityManager(bind(WebSecurityManager.class));
    expose(WebSecurityManager.class);
    bind(FilterChainResolver.class).to(PathMatchingFilterChainResolver.class);
    bind(PathMatchingFilterChainResolver.class);
    expose(FilterChainResolver.class);
    bind(FilterChainManager.class).to(DefaultFilterChainManager.class);
    DefaultFilterChainManager manager = new DefaultFilterChainManager();
    addTemporaryAnonymousPaths(manager);
    manager.createChain("/*assets/**", "anon"); // assets for the web interface
    manager.createChain("/cip/**", "anon"); // assets for report CIP
    manager.createChain("/favicon.ico", "anon"); // favicon for web interface
    manager.createChain("/crumbIssuer/**", "anon"); // Hudson integration
    manager.createChain("/rest/ide/asset/**", "anon"); // assets for the IDE CIP and details view
    manager.createChain("/rest/ide/brain/**", "anon"); // only redirects
    manager.createChain("/rest/report/*/*/embedReport/**", "anon"); // backward-compat with non-authenticating clients
    manager.createChain("/rest/report/*/*/brain/**", "anon"); // only redirects
    manager.createChain("/rest/session/environment", "anon"); // client environment gathering
    manager.createChain("/rest/version", "anon"); // product version info
    manager.createChain("/tasks/**", "anon"); // DW tasks exposed on admin port
    manager.createChain("/ui/links/**", "anon"); // only redirects
    manager.createChain("/**/*", "authcBasic");
    //change the auth type so browsers dont prompt for login details
    BasicHttpAuthenticationFilter.class.cast(manager.getFilter("authcBasic")).setAuthcScheme("nonBrowserPromptingBasic");
    bind(DefaultFilterChainManager.class).toInstance(manager);
    bind(Authenticator.class).to(FirstSuccessfulRealmAuthenticator.class);
    bindRealm().to(CLMRealm.class);
    bindRealm().to(LdapRealm.class);
  }
  
  // to be removed once all clients use authentication
  private void addTemporaryAnonymousPaths( DefaultFilterChainManager manager ) {
    manager.createChain("/rest/application/services/names", "anon");
    manager.createChain("/rest/application/validate/*", "anon");
    manager.createChain("/rest/config/proprietary", "anon");
    manager.createChain("/rest/policy/*/evaluate", "anon");
    manager.createChain("/rest/ci/validate/*", "anon");
    manager.createChain("/rest/ci/scan/*", "anon");
    manager.createChain("/rest/rm/**", "anon");
  }

  @Override
  protected void bindSessionManager(AnnotatedBindingBuilder<SessionManager> bind) {
    bind.to(WebSessionManager.class);
    bind(WebSessionManager.class).to(DefaultWebSessionManager.class).in(Singleton.class);
    bind(DefaultWebSessionManager.class);
    bind(SessionDAO.class).to(MemorySessionDAO.class).in(Singleton.class);
    expose(SessionDAO.class);
  }

  @Override
  protected void bindSecurityManager(AnnotatedBindingBuilder<? super SecurityManager> bind) {
    /*
     * NOTE: Not using bindWebSecurityManager() as in ShiroWebModule to avoid
     * https://issues.apache.org/jira/browse/SHIRO-435.
     */
    bind.to( WebSecurityManager.class );
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
}