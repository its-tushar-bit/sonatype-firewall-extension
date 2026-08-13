/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import com.sonatype.insight.brain.label.ComponentLabelService.AppliedLabels;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.repository.hosted.HrcOwnerTypeFeatureGuard;
import com.sonatype.insight.brain.security.SecurityAspectControl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins that {@link ComponentLabelResource} forwards {@link OwnerType#HOSTED_REPOSITORY_COMPONENT}
 * verbatim to the service. The path-level regex allowlists {@code hosted_repository_component}, so
 * a Mockito call-through here is what proves the request no longer 404s at the JAX-RS layer for
 * HRC owners.
 */
@RunWith(MockitoJUnitRunner.class)
public class ComponentLabelResourceHostedRepositoryComponentTest
{
  private static final String HRC_ID = "hrc-1";

  private static final String HASH = "hash-abc";

  @Mock
  private ComponentLabelService componentLabelService;

  @InjectMocks
  private ComponentLabelResource resource;

  private MockedStatic<HrcOwnerTypeFeatureGuard> guardMock;

  @Before
  public void bypassHrcFeatureGuard() {
    // The runtime HrcOwnerTypeFeatureGuard added in CLM-44276 (Bhavat review) delegates to
    // SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled(), which requires
    // Spring-wired static state that a plain MockitoJUnitRunner doesn't provide. Since the guard
    // itself is exercised end-to-end in ComponentLabelResourceHostedRepositoryComponentRoutingTest,
    // it's fine to no-op it here so this test can focus on its own concern (delegation forwarding).
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
  public void getComponentLabels_forwardsHrcOwnerTypeToService() {
    AppliedLabels expected = new AppliedLabels();
    when(componentLabelService.getComponentLabels(OwnerType.HOSTED_REPOSITORY_COMPONENT, HRC_ID, HASH))
        .thenReturn(expected);

    AppliedLabels actual = resource.getComponentLabels(OwnerType.HOSTED_REPOSITORY_COMPONENT, HRC_ID, HASH);

    assertThat(actual).isSameAs(expected);
    verify(componentLabelService).getComponentLabels(OwnerType.HOSTED_REPOSITORY_COMPONENT, HRC_ID, HASH);
  }
}
