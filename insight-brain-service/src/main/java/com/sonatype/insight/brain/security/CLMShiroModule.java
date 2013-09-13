/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.apache.shiro.mgt.SecurityManager;

import com.google.inject.binder.AnnotatedBindingBuilder;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.session.mgt.WebSessionManager;

//pulled in from benjamins poc, comments to come
//TODO: comment
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
    manager.createChain("/account/status", "anon");
    manager.createChain("/*assets/**", "anon");
    manager.createChain("/favicon.ico", "anon");
    manager.createChain("/crumbIssuer/**", "anon");
    manager.createChain("/**/*", "authcBasic");
    bind(DefaultFilterChainManager.class).toInstance(manager);
    bindRealm().to(CLMRealm.class);
  }
  
  private void addTemporaryAnonymousPaths( DefaultFilterChainManager manager ) {
    manager.createChain("/rest/application/services/names", "anon");
    manager.createChain("/rest/component/identified", "anon");
    manager.createChain("/rest/config/proprietary", "anon");
    manager.createChain("/rest/features", "anon");
    manager.createChain("/rest/ide/**", "anon");
    manager.createChain("/rest/label/application/*", "anon");
    manager.createChain("/rest/label/application/*/applicable", "anon");
    manager.createChain("/rest/label/component/**", "anon");
    manager.createChain("/rest/license", "anon");
    manager.createChain("/rest/licenseOverride/**", "anon");
    manager.createChain("/rest/policyWaiver/**", "anon");
    manager.createChain("/rest/policy/*/evaluate", "anon");
    manager.createChain("/rest/policy/*/*/export", "anon");
    manager.createChain("/rest/policy/*/*/import", "anon");
    manager.createChain("/rest/policy/actionType", "anon");
    manager.createChain("/rest/report/**", "anon");
    manager.createChain("/rest/ci/**", "anon");
    manager.createChain("/rest/session/environment", "anon");
    manager.createChain("/rest/rm/**", "anon");
    manager.createChain("/api/v1/**", "anon");
    manager.createChain("/tasks/**", "anon");
  }

  @Override
  protected void bindSessionManager(AnnotatedBindingBuilder<SessionManager> bind) {
    bind.to(WebSessionManager.class);
    bind(WebSessionManager.class).to(DefaultWebSessionManager.class);
    bind(DefaultWebSessionManager.class);
  }

  @Override
  protected void bindSecurityManager(AnnotatedBindingBuilder<? super SecurityManager> bind) {
    /*
     * NOTE: Not using bindWebSecurityManager() as in ShiroWebModule to avoid
     * https://issues.apache.org/jira/browse/SHIRO-435.
     */
    bind.toInstance(new DefaultWebSecurityManager());
    
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