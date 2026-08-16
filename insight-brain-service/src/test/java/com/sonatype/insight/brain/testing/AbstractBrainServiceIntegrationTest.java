/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.security.SsoUserService;

import org.junit.After;
import org.junit.jupiter.api.AfterEach;
import org.junit.Rule;

/**
 * Base integration test class for regular single-tenant IQ. {@link TemporaryEntity} resides here to manipulate data for
 * the single tenant.
 */
public abstract class AbstractBrainServiceIntegrationTest
    extends AbstractBaseIntegrationTest
{
  @Rule(order = 2)
  public TemporaryEntity tempEntity = new TemporaryEntity(databaseContainerRule)
  {
    @Override
    public void after() {
      super.after();
      afterDatabaseReset();
    }
  };

  @Override
  public void setUpTestLicenseThreatGroups() {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
  }

  @After
  @AfterEach
  public void disableSso() {
    if (testCLMServer == null || !testCLMServer.isRunning()) {
      return;
    }

    disableSsoWithOAuth2();
    disableSsoWithSaml();
    loadSsoConfiguration();
  }

  public void enableSsoWithOAuth2() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    tempEntity.newOAuth2Configuration();
    loadSsoConfiguration();
  }

  public void disableSsoWithOAuth2() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(false);
    loadSsoConfiguration();
  }

  public void enableSsoWithSaml() {
    tempEntity.newSamlConfiguration();
    loadSsoConfiguration();
  }

  public void disableSsoWithSaml() {
    SamlConfigurationService samlConfigurationService = lookup(SamlConfigurationService.class);
    samlConfigurationService.delete();
    loadSsoConfiguration();
  }

  private void loadSsoConfiguration() {
    SsoUserService ssoUserService = lookup(SsoUserService.class);
    ssoUserService.loadSsoConfiguration();
  }
}
