/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class InnerSourceServiceTest
    extends AbstractComponentH2Test
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

  @Test
  public void testGetLatestVersionsByStage_NullStage() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> innerSourceService.getLatestVersionsByStage(
            List.of(ComponentIdentifier.createNpmCoordinates("p", "v")), null))
        .withMessage("stage is required");
  }

  @Test
  public void testGetLatestVersionsByStage_EmptyStage() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> innerSourceService.getLatestVersionsByStage(
            List.of(ComponentIdentifier.createNpmCoordinates("p", "v")), ""))
        .withMessage("stage is required");
  }

  @Test
  public void testGetLatestVersionsByStage_NullOrEmptyInputReturnsEmpty() {
    assertThat(innerSourceService.getLatestVersionsByStage(null, StageTypes.RELEASE.getId())).isEmpty();
    assertThat(innerSourceService.getLatestVersionsByStage(Collections.emptyList(), StageTypes.RELEASE.getId()))
        .isEmpty();
  }

  @Test
  public void testGetLatestVersionsByStage_BatchesMultipleComponents() {
    Application app = tempEntity.newApplicationWithParent();
    String stage = StageTypes.RELEASE.getId();
    ComponentIdentifier compA = ComponentIdentifier.createNpmCoordinates("comp-a", "0.0.0");
    ComponentIdentifier compB = ComponentIdentifier.createNpmCoordinates("comp-b", "0.0.0");
    ComponentIdentifier compNotInnerSource = ComponentIdentifier.createNpmCoordinates("comp-c", "0.0.0");
    ComponentIdentifier compWithoutReleaseVersion = ComponentIdentifier.createNpmCoordinates("comp-d", "0.0.0");

    InnerSourceApplication innerA = tempEntity.newInnerSourceApplication(
        InnerSourceUtils.getVersionlessPackageUrl(compA).getPackageUrl(), app);
    InnerSourceApplication innerB = tempEntity.newInnerSourceApplication(
        InnerSourceUtils.getVersionlessPackageUrl(compB).getPackageUrl(), app);
    // compWithoutReleaseVersion is registered but has no release-stage row
    tempEntity.newInnerSourceApplication(
        InnerSourceUtils.getVersionlessPackageUrl(compWithoutReleaseVersion).getPackageUrl(), app);
    tempEntity.newInnerSourceVersion(innerA, "1.2.3", stage);
    tempEntity.newInnerSourceVersion(innerB, "4.5.6", stage);

    Map<String, String> result = innerSourceService.getLatestVersionsByStage(
        List.of(compA, compB, compNotInnerSource, compWithoutReleaseVersion),
        stage);

    assertThat(result)
        .hasSize(2)
        .containsEntry(InnerSourceUtils.getVersionlessPackageUrl(compA).getPackageUrl(), "1.2.3")
        .containsEntry(InnerSourceUtils.getVersionlessPackageUrl(compB).getPackageUrl(), "4.5.6");
  }

  @Test
  public void testGetLatestVersionsByStage_FiltersOutNullComponentIdentifiers() {
    Application app = tempEntity.newApplicationWithParent();
    String stage = StageTypes.RELEASE.getId();
    ComponentIdentifier compA = ComponentIdentifier.createNpmCoordinates("comp-a", "0.0.0");

    InnerSourceApplication innerA = tempEntity.newInnerSourceApplication(
        InnerSourceUtils.getVersionlessPackageUrl(compA).getPackageUrl(), app);
    tempEntity.newInnerSourceVersion(innerA, "1.2.3", stage);

    // null entries are dropped; the request still resolves
    Map<String, String> result = innerSourceService.getLatestVersionsByStage(
        Arrays.asList(compA, null), stage);

    assertThat(result)
        .hasSize(1)
        .containsEntry(InnerSourceUtils.getVersionlessPackageUrl(compA).getPackageUrl(), "1.2.3");
  }
}
