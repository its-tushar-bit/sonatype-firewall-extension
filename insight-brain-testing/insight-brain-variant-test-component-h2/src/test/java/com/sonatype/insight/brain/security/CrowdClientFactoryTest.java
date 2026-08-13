/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class CrowdClientFactoryTest
    extends AbstractComponentH2Test
{
  @Rule
  public LogOutput logOutput = new LogOutput(CrowdClientFactory.class);

  @Inject
  private CrowdClientFactory crowdClientFactory;

  @Inject
  private PasswordHandler passwordHandler;

  @Test
  public void testCreateCrowdClient_FeatureDisabled() {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);

    assertThat(crowdClientFactory.createCrowdClient()).isNull();
  }

  @Test
  public void testCreateCrowdClient_NotConfigured() {
    assertThat(crowdClientFactory.createCrowdClient()).isNull();
  }

  @Test
  public void testCreateCrowdClient_Configured() {
    tempEntity.newCrowdConfiguration("http://localhost:8095/crowd", "iq server",
        passwordHandler.encryptPassword("password".toCharArray()));

    assertThat(crowdClientFactory.createCrowdClient()).isNotNull();
  }

  @Test
  public void testCreateCrowdClient_Configured_BadConfiguration() {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration("badUrl", "iq server",
        passwordHandler.encryptPassword("password".toCharArray()));

    assertThat(crowdClientFactory.createCrowdClient()).isNull();
    logOutput.assertThat()
        .atErrorLevel()
        .contains(String.format(
            "Failed to create a Crowd REST client for serverUrl '%s', applicationName '%s', " +
                "and applicationPassword '****'. Your Crowd configuration may be invalid.",
            crowdConfiguration.getServerUrl(), crowdConfiguration.getApplicationName()));
  }
}
