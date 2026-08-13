/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.tenant.auth.provisionig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ActionResolverTest
{
  @Mock
  private Auth0ProvisioningService mockAuth0ProvisioningService;

  private ActionResolver actionResolver;

  @BeforeEach
  public void before() {
    actionResolver = new ActionResolver(mockAuth0ProvisioningService);
  }

  @Test
  public void testPerform_provision() {
    TenantParameters parameters = new TenantParameters();
    parameters.setAction("provision");

    actionResolver.perform(parameters);
    verify(mockAuth0ProvisioningService).provision(parameters);
  }

  @Test
  public void testPerform_invalidAction() {
    TenantParameters parameters = new TenantParameters();
    parameters.setAction("blah");

    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> actionResolver.perform(parameters));
  }
}
