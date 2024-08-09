/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.version.VersionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

public class DeveloperEnablementServiceTest
    extends AbstractComponentTest
{
  @Mock
  private VersionService versionService;

  @Mock
  private ProductLicense productLicense;

  private DeveloperEnablementService developerEnablementService;

  @Before
  public void before() {
    developerEnablementService = new DeveloperEnablementService(versionService, productLicense);
  }

  @Test
  public void testShouldEnableDeveloperProduct_TrueWithEligibleLifecycleEditionAndEligibleVersion() {
    setEligibleLifecycleEdition();
    setEligibleLifecycleVersion();

    final boolean shouldEnableDeveloper = developerEnablementService.shouldEnableDeveloperProduct();
    assertThat(shouldEnableDeveloper).isTrue();
  }

  @Test
  public void testShouldEnableDeveloperProduct_FalseWithIneligibleLifecycleEditionAndEligibleVersion() {
    setIneligibleLifecycleEdition();
    setEligibleLifecycleVersion();

    final boolean shouldEnableDeveloper = developerEnablementService.shouldEnableDeveloperProduct();
    assertThat(shouldEnableDeveloper).isFalse();
  }

  @Test
  public void testShouldEnableDeveloperProduct_FalseWithEligibleLifecycleEditionAndIneligibleVersion() {
    setEligibleLifecycleEdition();
    setIneligibleLifecycleVersion();

    final boolean shouldEnableDeveloper = developerEnablementService.shouldEnableDeveloperProduct();
    assertThat(shouldEnableDeveloper).isFalse();
  }

  @Test
  public void testShouldEnableDeveloperProduct_FalseWithIneligibleLifecycleEditionAndIneligibleVersion() {
    setIneligibleLifecycleEdition();
    setIneligibleLifecycleVersion();

    final boolean shouldEnableDeveloper = developerEnablementService.shouldEnableDeveloperProduct();
    assertThat(shouldEnableDeveloper).isFalse();
  }

  private void setEligibleLifecycleEdition() {
    doReturn(true)
        .when(productLicense)
        .hasProduct(anyString());
  }

  private void setIneligibleLifecycleEdition() {
    doReturn(false)
        .when(productLicense)
        .hasProduct(anyString());
  }

  private void setEligibleLifecycleVersion() {
    doReturn("1.181.0")
        .when(versionService)
        .getVersion();
    doReturn(1)
        .when(versionService)
        .compare(anyString(), anyString());
  }

  private void setIneligibleLifecycleVersion() {
    doReturn("1.178.0")
        .when(versionService)
        .getVersion();
    doReturn(-1)
        .when(versionService)
        .compare(anyString(), anyString());
  }
}
