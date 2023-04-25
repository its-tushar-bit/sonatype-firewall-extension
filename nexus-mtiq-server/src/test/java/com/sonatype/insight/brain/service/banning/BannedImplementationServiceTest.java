/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.insight.brain.api.v2.ApiCrowdConfigurationResourceV2;
import com.sonatype.insight.brain.api.v2.ApiProxyServerConfigurationResource;
import com.sonatype.insight.brain.service.DefaultTenantManagedInitializer;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class BannedImplementationServiceTest
{
  private BannedImplementationService underTest;

  @Test
  public void test_DefaultClassesAreBanned() {
    underTest = new BannedImplementationService();
    assertThat(underTest.isBanned(DefaultTenantManagedInitializer.class)).isTrue();
  }

  @Test
  public void test_AnyOtherClassesAreNotBanned() {
    underTest = new BannedImplementationService();
    assertThat(underTest.isBanned(BannedImplementationServiceTest.class)).isFalse();
  }

  @Test
  public void test_MultipleBannedTypesCanBeUsedForBanning() {
    underTest = new BannedImplementationService(Arrays.asList(
      new DefaultBannedImplementation(),
        new OtherBannedImplementation()
    ));

    assertThat(underTest.isBanned(DefaultTenantManagedInitializer.class)).isTrue();
    assertThat(underTest.isBanned(BannedImplementationServiceTest.class)).isTrue();
  }

  @Test
  public void test_CanBanByPackage() {
    underTest = new BannedImplementationService(Collections.singletonList(
        new BannedImplementationByPackage("com.sonatype.insight.brain.service.banning")
    ));

    assertThat(underTest.isBanned(BannedImplementationServiceTest.class)).isTrue();
    assertThat(underTest.isBanned(DefaultTenantManagedInitializer.class)).isFalse();
  }

  @Test
  public void test_PermanentlyBannedRestClassesAreBanned() {
    underTest = new BannedImplementationService();
    assertThat(underTest.isBanned(ApiCrowdConfigurationResourceV2.class)).isTrue();
  }

  @Test
  public void test_MilestoneOneBannedRestClassesAreBanned() {
    underTest = new BannedImplementationService();
    assertThat(underTest.isBanned(ApiProxyServerConfigurationResource.class)).isTrue();
  }

  private static class OtherBannedImplementation
      implements BannedImplementation
  {
    @Override
    public boolean isBanned(Class<?> clazz) {
      return BannedImplementationServiceTest.class.equals(clazz);
    }
  }
}
