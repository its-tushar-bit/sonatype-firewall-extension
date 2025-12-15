/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.sonatype.insight.brain.audit.AuditAuthenticationListener;
import com.sonatype.insight.brain.audit.AuditSessionListener;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlPasswordFactory;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.brain.security.ApiAccessControlFilter;
import com.sonatype.insight.brain.security.AuthenticationLoggingFilter;
import com.sonatype.insight.brain.security.ClearRolePermissionCache;
import com.sonatype.insight.brain.security.ClientIPAddressFilter;
import com.sonatype.insight.brain.security.ContentTypeOptionsHeaderFilter;
import com.sonatype.insight.brain.security.CrowdClientFactory;
import com.sonatype.insight.brain.security.CrowdRealm;
import com.sonatype.insight.brain.security.CspHeaderFilter;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.HttpHeaderValidatorFilter;
import com.sonatype.insight.brain.security.IdPLogoutUrlBuilder;
import com.sonatype.insight.brain.security.InsightSessionManager;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.InvalidRequestFilter;
import com.sonatype.insight.brain.security.JavaLangErrorHandlerAuthListener;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.security.OAuth2SsoUserProvider;
import com.sonatype.insight.brain.security.PasswordService;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.security.ReverseProxyAuthenticationFilter;
import com.sonatype.insight.brain.security.ReverseProxyRealm;
import com.sonatype.insight.brain.security.RolePermissionService;
import com.sonatype.insight.brain.security.RoleService;
import com.sonatype.insight.brain.security.SamlDeploymentManager;
import com.sonatype.insight.brain.security.SamlMetadataTool;
import com.sonatype.insight.brain.security.SamlRealm;
import com.sonatype.insight.brain.security.SamlSessionIdMapper;
import com.sonatype.insight.brain.security.SamlSsoUserProvider;
import com.sonatype.insight.brain.security.SecureCookiesFilter;
import com.sonatype.insight.brain.security.SessionExpirationCookieFilter;
import com.sonatype.insight.brain.security.UserService;
import com.sonatype.insight.brain.security.UserTokenRealm;
import com.sonatype.insight.brain.security.UserTokenService;
import com.sonatype.insight.brain.security.oauth2.JwtAuthenticationFilter;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.security.oauth2.OidcLoginFilter;
import com.sonatype.insight.brain.security.oauth2.ShiroJsonWebTokenValidator;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * Guice module providing explicit bindings for Security components. This replaces Sisu's automatic @Named component
 * discovery.
 */
public class AuthenticationModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    // Security filters (only public ones)
    bind(AntiCsrfFilter.class);
    bind(ApiAccessControlFilter.class).in(Singleton.class);
    bind(AuthenticationLoggingFilter.class).in(Singleton.class);
    bind(ClientIPAddressFilter.class).in(Singleton.class);
    bind(ContentTypeOptionsHeaderFilter.class).in(Singleton.class);
    bind(CspHeaderFilter.class).in(Singleton.class);
    bind(HttpHeaderValidatorFilter.class).in(Singleton.class);
    bind(InvalidRequestFilter.class).in(Singleton.class);
    bind(ReverseProxyAuthenticationFilter.class).in(Singleton.class);
    bind(SecureCookiesFilter.class).in(Singleton.class);
    bind(SessionExpirationCookieFilter.class).in(Singleton.class);
    bind(JwtAuthenticationFilter.class).in(Singleton.class);
    bind(OidcLoginFilter.class).in(Singleton.class);

    // Note: MissingAuthenticationFilter, SamlFilter, and UserFriendlyBasicHttpAuthenticationFilter are package-private
    // They are bound in the security/SecurityModule (Shiro module) via requestInjection()

    // Security services and components
    bind(ClearRolePermissionCache.class);
    bind(CrowdClientFactory.class);
    bind(CrowdRealm.class);
    bind(CurrentUser.class);
    bind(IdPLogoutUrlBuilder.class);
    bind(InsightSessionManager.class);
    bind(InternalRealm.class);
    bind(MembershipMappingService.class);
    bind(OAuth2Realm.class);
    bind(OAuth2SsoUserProvider.class);
    bind(PasswordService.class);
    bind(PermissionService.class);
    bind(ReverseProxyRealm.class);
    bind(RolePermissionService.class);
    bind(RoleService.class);
    bind(SamlDeploymentManager.class);
    bind(SamlMetadataTool.class);
    bind(SamlPasswordFactory.class);
    bind(SamlRealm.class);
    bind(SamlSessionIdMapper.class);
    bind(SamlSsoUserProvider.class);
    bind(ShiroJsonWebTokenValidator.class);
    bind(UserService.class);
    bind(UserTokenRealm.class);
    bind(UserTokenService.class);

    // Shiro listeners - these are package-private but need to be bound for injection into provider methods
    bind(AuditSessionListener.class);
    bind(AuditAuthenticationListener.class);
    bind(JavaLangErrorHandlerAuthListener.class);

    // Note: SessionDAO is bound in security/SecurityModule (Shiro module)
    // so we don't bind it here to avoid duplicates
  }
}
