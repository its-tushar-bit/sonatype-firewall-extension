/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sonatype.insight.brain.configuration.ldap.LdapRealm;
import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.security.oauth2.ShiroJsonWebTokenValidator;
import java.util.Collection;
import org.apache.shiro.realm.CachingRealm;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class RealmCachingDisabledTest
{
  @Test
  public void shouldDisableCachingForAllSpringManagedRealmBeans() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RealmBeans.class)) {
      Collection<CachingRealm> cachingRealms = context.getBeansOfType(CachingRealm.class).values();

      assertThat(cachingRealms)
          .extracting(Object::getClass)
          .containsExactlyInAnyOrder(
              InternalRealm.class,
              LdapRealm.class,
              UserTokenRealm.class,
              CrowdRealm.class,
              ReverseProxyRealm.class,
              SamlRealm.class,
              OAuth2Realm.class);

      // Authentication and/or authorization caching may not be cluster-friendly and should be disabled
      // see https://github.com/sonatype/insight-brain/pull/5475 for more information
      assertThat(cachingRealms).extracting(CachingRealm::getCacheManager).containsOnlyNulls();
    }
  }

  @Configuration
  static class RealmBeans
  {
    @Bean
    PasswordService passwordService() {
      return new PasswordService();
    }

    @Bean
    LdapService ldapService() {
      return mock(LdapService.class);
    }

    @Bean
    UserTokenService userTokenService() {
      return mock(UserTokenService.class);
    }

    @Bean
    CrowdClientFactory crowdClientFactory() {
      return mock(CrowdClientFactory.class);
    }

    @Bean
    LdapServerDAO ldapServerDAO() {
      return mock(LdapServerDAO.class);
    }

    @Bean
    UserTokenDAO userTokenDAO() {
      return mock(UserTokenDAO.class);
    }

    @Bean
    UserDAO userDAO() {
      return mock(UserDAO.class);
    }

    @Bean
    SsoUserService ssoUserService() {
      return mock(SsoUserService.class);
    }

    @Bean
    SamlConfigurationService samlConfigurationService() {
      return mock(SamlConfigurationService.class);
    }

    @Bean
    OAuth2ConfigurationDAO oAuth2ConfigurationDAO() {
      return mock(OAuth2ConfigurationDAO.class);
    }

    @Bean
    ShiroJsonWebTokenValidator shiroJsonWebTokenValidator() {
      return mock(ShiroJsonWebTokenValidator.class);
    }

    @Bean
    InternalRealm internalRealm(PasswordService passwordService, UserDAO userDAO) {
      return new InternalRealm(passwordService, userDAO);
    }

    @Bean
    LdapRealm ldapRealm(LdapService ldapService, LdapServerDAO ldapServerDAO) {
      return new LdapRealm(ldapService, ldapServerDAO);
    }

    @Bean
    UserTokenRealm userTokenRealm(
        PasswordService passwordService,
        LdapService ldapService,
        UserTokenService userTokenService,
        CrowdClientFactory crowdClientFactory,
        LdapServerDAO ldapServerDAO,
        UserTokenDAO userTokenDAO,
        UserDAO userDAO,
        SsoUserService ssoUserService)
    {
      return new UserTokenRealm(passwordService, ldapService, userTokenService, crowdClientFactory, ldapServerDAO,
          userTokenDAO, userDAO, ssoUserService);
    }

    @Bean
    CrowdRealm crowdRealm(CrowdClientFactory crowdClientFactory) {
      return new CrowdRealm(crowdClientFactory);
    }

    @Bean
    ReverseProxyRealm reverseProxyRealm(LdapService ldapService, UserDAO userDAO) {
      return new ReverseProxyRealm(ldapService, userDAO);
    }

    @Bean
    SamlRealm samlRealm(SsoUserService ssoUserService, SamlConfigurationService samlConfigurationService) {
      return new SamlRealm(ssoUserService, samlConfigurationService);
    }

    @Bean
    OAuth2Realm oAuth2Realm(
        OAuth2ConfigurationDAO oAuth2ConfigurationDAO,
        ShiroJsonWebTokenValidator shiroJsonWebTokenValidator,
        SsoUserService ssoUserService)
    {
      return new OAuth2Realm(oAuth2ConfigurationDAO, shiroJsonWebTokenValidator, ssoUserService);
    }
  }
}
