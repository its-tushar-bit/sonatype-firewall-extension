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
    manager.createChain("/**/authc", "authcBasic");
    bind(DefaultFilterChainManager.class).toInstance(manager);
    bindRealm().to(CLMRealm.class);
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