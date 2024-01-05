/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.tenancy.MultiTenantTenantManagedInitializer;

import com.google.inject.ConfigurationException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class MultiTenantInsightBrainServiceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Test
  public void shouldExcludeDefaultTenantManagedInitializer() {
    assertThat(getCLMServer().getInstance(TenantManagedInitializer.class))
        .isInstanceOf(MultiTenantTenantManagedInitializer.class);

    assertThatExceptionOfType(ConfigurationException.class).isThrownBy(
        () -> getCLMServer().getInstance(DefaultTenantManagedInitializer.class)
    ).withMessageContaining("DefaultTenantManagedInitializer is not explicitly bound");
  }
}
