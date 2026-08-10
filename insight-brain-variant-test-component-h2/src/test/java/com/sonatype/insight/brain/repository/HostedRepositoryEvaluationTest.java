/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Hosted Repository Evaluation feature flag.
 *
 * @since 1.203
 */
@ComponentH2Test
public class HostedRepositoryEvaluationTest
    extends AbstractComponentH2Test
{
  @AfterEach
  public void resetFeatureFlag() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Test
  public void testIsEnabled_DefaultsToFalse() {
    assertThat(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled()).isFalse();
  }

  @Test
  public void testSetEnabled_EnablesFlag() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    assertThat(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled()).isTrue();
  }

  @Test
  public void testSetEnabled_DisablesFlag() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
    assertThat(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled()).isFalse();
  }
}
