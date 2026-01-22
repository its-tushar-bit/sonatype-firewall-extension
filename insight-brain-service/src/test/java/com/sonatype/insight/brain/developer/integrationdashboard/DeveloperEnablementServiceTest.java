/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import jakarta.inject.Provider;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.version.VersionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService.MIN_DEVELOPER_COMPATIBLE_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

public class DeveloperEnablementServiceTest
    extends AbstractComponentTest
{
  @Mock
  private VersionService versionService;

  @Mock
  private ProductLicense productLicense;

  @Mock
  private Provider<HdsClient> hdsClientProvider;

  @Mock
  private HdsClient hdsClient;

  private DeveloperEnablementService developerEnablementService;

  @Before
  public void before() {
    doReturn(hdsClient)
        .when(hdsClientProvider)
        .get();
    developerEnablementService = new DeveloperEnablementService(versionService, productLicense, hdsClientProvider);
  }

  @Test
  public void testShouldEnableDeveloperProduct_TrueWithEligibleLifecycleEditionAndEligibleMinVersion() {
    setLifecycleVersion("1.181.0");
    setEligibleLifecycleEdition();
    setEligibleLifecycleVersionMin();
    setNoUpperBoundVersion();

    final boolean shouldEnableDeveloper = developerEnablementService.shouldEnableDeveloperProduct();
    assertThat(shouldEnableDeveloper).isTrue();
  }

  @Test
  public void testShouldEnableDeveloperProduct_FalseWithIneligibleLifecycleEditionAndEligibleMinVersion() {
    setLifecycleVersion("1.181.0");
    setIneligibleLifecycleEdition();
    setEligibleLifecycleVersionMin();
    setNoUpperBoundVersion();

    final boolean shouldEnableDeveloper = developerEnablementService.shouldEnableDeveloperProduct();
    assertThat(shouldEnableDeveloper).isFalse();
  }

  @Test
  public void testShouldEnableDeveloperProduct_FalseWithEligibleLifecycleEditionAndIneligibleMinVersion() {
    setLifecycleVersion("1.178.0");
    setEligibleLifecycleEdition();
    setIneligibleLifecycleVersionMin();
    setNoUpperBoundVersion();

    final boolean shouldEnableDeveloper = developerEnablementService.shouldEnableDeveloperProduct();
    assertThat(shouldEnableDeveloper).isFalse();
  }

  @Test
  public void testShouldEnableDeveloperProduct_FalseWithIneligibleLifecycleEditionAndIneligibleMinVersion() {
    setLifecycleVersion("1.178.0");
    setIneligibleLifecycleEdition();
    setIneligibleLifecycleVersionMin();
    setNoUpperBoundVersion();

    final boolean shouldEnableDeveloper = developerEnablementService.shouldEnableDeveloperProduct();
    assertThat(shouldEnableDeveloper).isFalse();
  }

  @Test
  public void testShouldEnableDeveloperProduct_TrueWithEligibleMaxVersion() {
    setLifecycleVersion("1.184.0");
    setEligibleLifecycleEdition();
    setEligibleLifecycleVersionMin();
    setEligibleLifecycleVersionMax();

    final boolean shouldEnableDeveloper = developerEnablementService.shouldEnableDeveloperProduct();
    assertThat(shouldEnableDeveloper).isTrue();
  }

  @Test
  public void testShouldEnableDeveloperProduct_FalseWithIneligibleMaxVersion() {
    setLifecycleVersion("1.191.0");
    setEligibleLifecycleEdition();
    setEligibleLifecycleVersionMin();
    setIneligibleLifecycleVersionMax();

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

  private void setLifecycleVersion(final String version) {
    doReturn(version)
        .when(versionService)
        .getVersion();
  }

  private void setEligibleLifecycleVersionMin() {
    doReturn(1)
        .when(versionService)
        .compare(anyString(), eq(MIN_DEVELOPER_COMPATIBLE_VERSION));
  }

  private void setIneligibleLifecycleVersionMin() {
    doReturn(-1)
        .when(versionService)
        .compare(anyString(), eq(MIN_DEVELOPER_COMPATIBLE_VERSION));
  }

  private void setNoUpperBoundVersion() {
    doReturn("")
        .when(hdsClient)
        .get(any(), anyString());
  }

  private void setEligibleLifecycleVersionMax() {
    final String upperBound = "1.190.0";
    doReturn(upperBound)
        .when(hdsClient)
        .get(any(), anyString());
    doReturn(-1)
        .when(versionService)
        .compare(anyString(), eq(upperBound));
  }

  private void setIneligibleLifecycleVersionMax() {
    final String upperBound = "1.190.0";
    doReturn(upperBound)
        .when(hdsClient)
        .get(any(), anyString());
    doReturn(1)
        .when(versionService)
        .compare(anyString(), eq(upperBound));
  }
}
