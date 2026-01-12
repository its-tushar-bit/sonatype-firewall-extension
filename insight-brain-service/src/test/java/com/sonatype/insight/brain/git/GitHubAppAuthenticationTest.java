/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the GitHub App Authentication feature flag.
 *
 * NOTE: This is a stub test that verifies basic feature flag operations (enable/disable/default state).
 * Comprehensive functional tests for GitHub App authentication will be added when the feature is
 * fully implemented in subsequent tickets.
 */
public class GitHubAppAuthenticationTest
    extends AbstractComponentTest
{
  @Test
  public void testIsEnabled_DefaultsToFalse() {
    assertThat(SystemConfigurationPropertyFeature.GITHUB_APP_AUTHENTICATION.isEnabled()).isFalse();
  }

  @Test
  public void testSetEnabled_EnablesFlag() {
    SystemConfigurationPropertyFeature.GITHUB_APP_AUTHENTICATION.setEnabled(true);
    assertThat(SystemConfigurationPropertyFeature.GITHUB_APP_AUTHENTICATION.isEnabled()).isTrue();
  }

  @Test
  public void testSetEnabled_DisablesFlag() {
    SystemConfigurationPropertyFeature.GITHUB_APP_AUTHENTICATION.setEnabled(true);
    SystemConfigurationPropertyFeature.GITHUB_APP_AUTHENTICATION.setEnabled(false);
    assertThat(SystemConfigurationPropertyFeature.GITHUB_APP_AUTHENTICATION.isEnabled()).isFalse();
  }
}
