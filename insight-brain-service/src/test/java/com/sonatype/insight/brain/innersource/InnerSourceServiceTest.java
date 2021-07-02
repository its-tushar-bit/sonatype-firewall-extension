/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class InnerSourceServiceTest
    extends AbstractComponentTest
{
  @Inject
  private InnerSourceService innerSourceService;

  @Test
  public void testGetComponentLatestVersion_NullComponentIdentifier() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> innerSourceService.getComponentLatestVersion(null))
        .withMessage("componentIdentifier is required");
  }

  @Test
  public void testGetComponentLatestVersion_ComponentNotFound() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> innerSourceService.getComponentLatestVersion(componentIdentifier))
        .withMessage("InnerSource component not found for " + componentIdentifier);
  }

  @Test
  public void testGetComponentLatestVersion_ComponentFoundWithoutVersion() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceComponent(InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier).getPackageUrl(),
        app);

    assertThat(innerSourceService.getComponentLatestVersion(componentIdentifier)).isNull();
  }

  @Test
  public void testGetComponentLatestVersion_ComponentFoundWithVersion() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    Application app = tempEntity.newApplicationWithParent();
    String version = "1.0.0";
    tempEntity.newInnerSourceComponent(InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier).getPackageUrl(),
        app, version);

    assertThat(innerSourceService.getComponentLatestVersion(componentIdentifier)).isEqualTo(version);
  }
}
