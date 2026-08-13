/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiAppliedLicenseOverridesDTO;
import com.sonatype.insight.brain.license.LicenseOverrideService;
import com.sonatype.insight.brain.license.LicenseOverrideService.AppliedLicenseOverrides;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.repository.hosted.HrcOwnerTypeFeatureGuard;
import com.sonatype.insight.brain.security.SecurityAspectControl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins that {@link ApiLicenseOverrideResource} forwards {@link OwnerType#HOSTED_REPOSITORY_COMPONENT}
 * verbatim to the service. The path-level regex allowlists {@code hosted_repository_component}, so
 * a Mockito call-through here is what proves the request no longer 404s at the JAX-RS layer for
 * HRC owners.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiLicenseOverrideResourceHostedRepositoryComponentTest
{
  private static final String HRC_ID = "hrc-1";

  @Mock
  private LicenseOverrideService licenseOverrideService;

  @InjectMocks
  private ApiLicenseOverrideResource resource;

  private MockedStatic<HrcOwnerTypeFeatureGuard> guardMock;

  @Before
  public void bypassHrcFeatureGuard() {
    // The runtime HrcOwnerTypeFeatureGuard added in CLM-44276 (Bhavat review) delegates to
    // SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled(), which requires
    // Spring-wired static state that a plain MockitoJUnitRunner doesn't provide. Since the guard
    // itself is exercised end-to-end in
    // ApiLicenseOverrideResourceHostedRepositoryComponentRoutingTest, it's fine to no-op it here so
    // this test can focus on its own concern (delegation forwarding).
    guardMock = Mockito.mockStatic(HrcOwnerTypeFeatureGuard.class);
    // AspectJ compile-time weaving wraps @Authorize on the service call site. Bypass so the
    // handler reaches its delegate on the mocked service.
    SecurityAspectControl.disableEnforcement();
  }

  @After
  public void restoreHrcFeatureGuard() {
    SecurityAspectControl.enableEnforcement();
    guardMock.close();
  }

  @Test
  public void getAppliedLicenseOverrides_forwardsHrcOwnerTypeToService() {
    ComponentIdentifier componentIdentifier = new ComponentIdentifier();
    AppliedLicenseOverrides serviceResult = new AppliedLicenseOverrides();
    ApiAppliedLicenseOverridesDTO expected = serviceResult.toDto();
    when(licenseOverrideService.getAppliedLicenseOverridesForRead(
        OwnerType.HOSTED_REPOSITORY_COMPONENT, HRC_ID, componentIdentifier))
            .thenReturn(serviceResult);

    ApiAppliedLicenseOverridesDTO actual = resource.getAppliedLicenseOverrides(
        OwnerType.HOSTED_REPOSITORY_COMPONENT, HRC_ID, componentIdentifier);

    assertThat(actual.licenseOverridesByOwner).isEqualTo(expected.licenseOverridesByOwner);
    verify(licenseOverrideService).getAppliedLicenseOverridesForRead(
        OwnerType.HOSTED_REPOSITORY_COMPONENT, HRC_ID, componentIdentifier);
  }
}
