/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
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
    tempEntity.newInnerSourceApplication(InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier).getPackageUrl(),
        app);

    assertThat(innerSourceService.getComponentLatestVersion(componentIdentifier)).isNull();
  }

  @Test
  public void testGetComponentLatestVersion_ComponentFoundWithVersion() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    Application app = tempEntity.newApplicationWithParent();
    String version = "1.0.0";
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication(
            InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier).getPackageUrl(), app);
    tempEntity.newInnerSourceVersion(innerSourceApplication, version, StageTypes.RELEASE.getId());

    assertThat(innerSourceService.getComponentLatestVersion(componentIdentifier)).isEqualTo(version);
  }

  @Test
  public void testGetComponentLatestVersionByStage_NullComponentIdentifier() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> innerSourceService.getComponentLatestVersionByStage(null, StageTypes.RELEASE.getId()))
        .withMessage("componentIdentifier is required");
  }

  @Test
  public void testGetComponentLatestVersionByStage_NullStage() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> innerSourceService.getComponentLatestVersionByStage(componentIdentifier, null))
        .withMessage("stage is required");
  }

  @Test
  public void testGetComponentLatestVersionByStage_EmptyStage() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> innerSourceService.getComponentLatestVersionByStage(componentIdentifier, ""))
        .withMessage("stage is required");
  }

  @Test
  public void testGetComponentLatestVersionByStage_ComponentNotFound() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> innerSourceService.getComponentLatestVersionByStage(componentIdentifier, StageTypes.RELEASE.getId()))
        .withMessage("InnerSource component not found for " + componentIdentifier);
  }

  @Test
  public void testGetComponentLatestVersionByStage_StageNotFound() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    Application app = tempEntity.newApplicationWithParent();
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication(
            InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier).getPackageUrl(), app);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.0.0", StageTypes.BUILD.getId());

    assertThat(
        innerSourceService.getComponentLatestVersionByStage(componentIdentifier, StageTypes.RELEASE.getId())).isNull();
  }

  @Test
  public void testGetComponentLatestVersionByStage_VersionFound() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    Application app = tempEntity.newApplicationWithParent();
    String version = "1.0.0";
    String stage = StageTypes.RELEASE.getId();
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication(
            InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier).getPackageUrl(), app);
    tempEntity.newInnerSourceVersion(innerSourceApplication, version, stage);

    assertThat(innerSourceService.getComponentLatestVersionByStage(componentIdentifier, stage)).isEqualTo(version);
  }

  @Test
  public void testIsInnerSourceComponent_NullComponentIdentifier() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> innerSourceService.isInnerSourceComponent(null))
        .withMessage("componentIdentifier is required");
  }

  @Test
  public void testIsInnerSourceComponent_ComponentNotFound() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");

    assertThat(innerSourceService.isInnerSourceComponent(componentIdentifier)).isFalse();
  }

  @Test
  public void testIsInnerSourceComponent_ComponentFound() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newInnerSourceApplication(
        InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier).getPackageUrl(), app);

    assertThat(innerSourceService.isInnerSourceComponent(componentIdentifier)).isTrue();
  }
}
