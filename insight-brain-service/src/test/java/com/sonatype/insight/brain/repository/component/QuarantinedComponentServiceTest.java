/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class QuarantinedComponentServiceTest
    extends AbstractComponentTest
{
  @Mock
  private DbQuarantinedComponentAccessManager quarantinedComponentAccessManager;

  @Inject
  private QuarantinedComponentService quarantinedComponentService;

  @Override
  public void configure(Binder binder) {
    binder.bind(DbQuarantinedComponentAccessManager.class).toInstance(quarantinedComponentAccessManager);
    super.configure(binder);
  }

  @Test
  public void testGetQuarantinedComponent() {
    when(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token")).thenReturn("id");

    QuarantinedComponentDto quarantinedComponentDto = quarantinedComponentService.getQuarantinedComponent("token");

    assertThat(quarantinedComponentDto).isNotNull();
    assertThat(quarantinedComponentDto.success).isTrue();
    assertThat(quarantinedComponentDto.repositoryComponentId).isEqualTo("id");
  }
}
