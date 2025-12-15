/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.modules;

import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkAuth0Provider;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkLocalProvider;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkProvider;
import com.sonatype.insight.brain.security.DefaultEncryptionKeyStore;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.MultiTenantEncryptionKeyStore;
import com.sonatype.insight.brain.security.MultiTenantSsoUserService;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.users.MultiTenantUserDirectory;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

/**
 * MTIQ-specific override module that provides only the authentication bindings that differ from the standard
 * AuthenticationModule. This module should be used with Modules.override(new AuthenticationModule()).with(new
 * MtiqAuthenticationModule()).
 */
public class MtiqOnlyAuthModule
    extends DropwizardAwareModule<InsightConfig>
{
  private static final String NXIQ_ENABLE_LOCAL_JWK_PROVIDER_ENV_VAR = "NXIQ_ENABLE_LOCAL_JWK_PROVIDER";

  @Override
  protected void configure() {
    // MTIQ-specific authentication overrides - only the bindings that differ
    bind(UserDirectory.class).to(MultiTenantUserDirectory.class);
    bind(SsoUserService.class).to(MultiTenantSsoUserService.class);

    // We use @InvisibleForScanner on MultiTenantSsoUserService, so we need to create the multibinder directly
    Multibinder<TenantManaged> tenantManagedBinder = Multibinder.newSetBinder(binder(), TenantManaged.class);
    tenantManagedBinder.addBinding().to(MultiTenantSsoUserService.class);

    // Bind MtiqUserService interface to its implementation
    bind(com.sonatype.insight.brain.users.MtiqUserService.class)
        .to(com.sonatype.insight.brain.users.MultiTenantUserService.class);

    // EncryptionKeyStore binding depends on configuration
    MultiTenantInsightConfig mtiqConfig = (MultiTenantInsightConfig) configuration();
    if (mtiqConfig.isUsingDefaultEncryptionKeyStore()) {
      bind(EncryptionKeyStore.class).to(DefaultEncryptionKeyStore.class);
    }
    else {
      bind(EncryptionKeyStore.class).to(MultiTenantEncryptionKeyStore.class);

      // Only add MultiTenantEncryptionKeyStore to TenantManaged set when it's actually used
      tenantManagedBinder.addBinding().to(MultiTenantEncryptionKeyStore.class);
    }
  }

  @Provides
  @Singleton
  public MultiTenantJwkProvider provideMultiTenantJwkProvider() {
    boolean localJwkProviderEnabled = Boolean.parseBoolean(
        System.getenv().get(NXIQ_ENABLE_LOCAL_JWK_PROVIDER_ENV_VAR));

    if (localJwkProviderEnabled) {
      return new MultiTenantJwkLocalProvider();
    }
    return new MultiTenantJwkAuth0Provider((MultiTenantInsightConfig) configuration());
  }
}
