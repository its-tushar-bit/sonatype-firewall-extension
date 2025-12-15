/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.clients.AwsSecretsManagerClient;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import org.apache.directory.api.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.installer.scanner.InvisibleForScanner;

@Named
@Singleton
@InvisibleForScanner
public class MultiTenantEncryptionKeyStore
    implements EncryptionKeyStore, TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantEncryptionKeyStore.class);

  private final AwsSecretsManagerClient awsSecretsManagerClient;

  private final MultiTenantInsightConfig multiTenantInsightConfig;

  private final TenantMetadataDAO tenantMetadataDAO;

  private final TenantUtil tenantUtil;

  private final TenantReference<String> tenantKeyStore;

  @Inject
  public MultiTenantEncryptionKeyStore(
      final AwsSecretsManagerClient awsSecretsManagerClient,
      final MultiTenantInsightConfig multiTenantInsightConfig,
      final TenantMetadataDAO tenantMetadataDAO,
      final TenantUtil tenantUtil)
  {
    this(awsSecretsManagerClient, multiTenantInsightConfig, tenantMetadataDAO, tenantUtil, new TenantReference<>());
  }

  public MultiTenantEncryptionKeyStore(
      final AwsSecretsManagerClient awsSecretsManagerClient,
      final MultiTenantInsightConfig multiTenantInsightConfig,
      final TenantMetadataDAO tenantMetadataDAO,
      final TenantUtil tenantUtil,
      final TenantReference<String> tenantKeyStore)
  {
    this.awsSecretsManagerClient = awsSecretsManagerClient;
    this.multiTenantInsightConfig = multiTenantInsightConfig;
    this.tenantMetadataDAO = tenantMetadataDAO;
    this.tenantUtil = tenantUtil;
    this.tenantKeyStore = tenantKeyStore;
  }

  @Override
  public void register() {
    initializeKey();
  }

  @Override
  public boolean includeGlobalTenantDuringRegistration() {
    return true;
  }

  @Override
  public void initializeKey() {
    String encryptionKeyName;
    if (tenantUtil.isGlobalTenant()) {
      encryptionKeyName = multiTenantInsightConfig.getGlobalTenantEncryptionKeyName();
    }
    else {
      TenantMetadata tenantMetadata = tenantMetadataDAO.get();
      if (tenantMetadata == null) {
        log.warn("Tenant {} metadata not found. Unable to fetch tenant encryption key from AWS secrets manager. " +
            "This is normal if the tenant is onboarding.", tenantUtil.getTenantSlugForSynchronization());
        return;
      }

      encryptionKeyName = tenantMetadata.getEncryptionKeyName();
    }

    log.debug("Getting tenant {} encryption key {} from AWS.", tenantUtil.getTenantSlugForSynchronization(),
        encryptionKeyName);

    if (encryptionKeyName == null) {
      log.error(
          String.format("Tenant %s encryption key name not found.", tenantUtil.getTenantSlugForSynchronization()));
      return;
    }

    String encryptionKey;
    try {
      encryptionKey = awsSecretsManagerClient.getSecret(encryptionKeyName);
    }
    catch (RuntimeException e) {
      log.error(String.format("Tenant %s encryption key not found.", tenantUtil.getTenantSlugForSynchronization()), e);
      return;
    }

    if (Strings.isEmpty(encryptionKey)) {
      log.error(String.format("Tenant %s encryption key null or empty.", tenantUtil.getTenantSlugForSynchronization()));
    }
    else {
      tenantKeyStore.set(encryptionKey);
    }
  }

  @Override
  public String getKey() {
    String key = tenantKeyStore.get();

    if (Strings.isEmpty(key)) {
      initializeKey();
      key = tenantKeyStore.get();
    }

    if (Strings.isEmpty(key)) {
      throw new RuntimeException(
          String.format("Tenant %s encryption key not found.", tenantUtil.getTenantSlugForSynchronization()));
    }
    return key;
  }
}
