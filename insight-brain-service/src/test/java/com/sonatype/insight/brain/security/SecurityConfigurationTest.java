/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sonatype.insight.brain.dataaccess.security.ShiroSessionDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.spring.config.SecurityConfiguration;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.SessionListener;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class SecurityConfigurationTest
{
  private AnnotationConfigApplicationContext context;

  private DefaultWebSecurityManager securityManager;

  private DefaultWebSessionManager sessionManager;

  private InternalRealm internalRealm;

  private Set<Realm> realms;

  @BeforeEach
  public void setUp() {
    context = new AnnotationConfigApplicationContext();
    context.register(SecurityConfiguration.class, TestSecurityBeans.class);
    context.refresh();

    securityManager = context.getBean("defaultWebSecurityManager", DefaultWebSecurityManager.class);
    sessionManager = context.getBean("defaultWebSessionManager", DefaultWebSessionManager.class);
    internalRealm = context.getBean(InternalRealm.class);
    realms = new LinkedHashSet<>(context.getBeansOfType(Realm.class).values());
  }

  @AfterEach
  public void tearDown() {
    if (context != null) {
      context.close();
    }
  }

  @Test
  public void shouldWireInsightSessionManagerIntoSecurityManager() {
    assertThat(securityManager.getSessionManager()).isSameAs(sessionManager);
  }

  @Test
  public void shouldRegisterAllRealmsWithSecurityManager() {
    assertThat(realms).containsExactly(internalRealm);
    assertThat(securityManager.getRealms()).containsExactlyInAnyOrderElementsOf(realms);
  }

  @Test
  public void shouldDisableRememberMeManager() {
    assertThat(securityManager.getRememberMeManager()).isNull();
  }

  @Test
  public void shouldResolveDefaultWebSecurityManagerByType() {
    assertThat(context.getBean(DefaultWebSecurityManager.class)).isSameAs(securityManager);
  }

  @Configuration
  static class TestSecurityBeans
  {
    @Bean
    PasswordService passwordService() {
      return new PasswordService();
    }

    @Bean
    UserDAO userDAO() {
      return mock(UserDAO.class);
    }

    @Bean
    ShiroSessionDAO shiroSessionDAO() {
      return mock(ShiroSessionDAO.class);
    }

    @Bean
    InternalRealm internalRealm(PasswordService passwordService, UserDAO userDAO) {
      return new InternalRealm(passwordService, userDAO);
    }

    @Bean
    SessionListener testSessionListener() {
      return mock(SessionListener.class);
    }
  }
}
