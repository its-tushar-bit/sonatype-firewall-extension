/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SbomStatus;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.brain.utils.SbomTestsHelper;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiSbomServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiSbomService service;

  @Inject
  private ThirdPartySbomMetadataDAO dao;

  @Inject
  private InsightWork insightWork;

  @Test
  public void testDeleteSbomVersion() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    Path fileInWorkDirPath =
        SbomTestsHelper.createTestFileForSbomMetadata(insightWork.getSbomDir(app.getId()),
            getClass().getResource("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"));
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withFilename(fileInWorkDirPath.getFileName().toString())
        .build();

    service.deleteSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion());

    final ThirdPartySbomMetadata retrievedSbomMetadata =
        dao.getByApplicationIdAndSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion());

    assertThat(retrievedSbomMetadata).isNull();
    assertThat(fileInWorkDirPath).doesNotExist();
  }

  @Test
  public void testDeleteSbomVersion_NotFoundInvalidVersion() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.deleteSbomVersion(sbomMetadata.getApplicationId(), "invalidVersion"))
        .withMessage(
            "Cannot find version invalidVersion for application with ID " + sbomMetadata.getApplicationId() + ".");
  }

  @Test
  public void testGetSbomVersion_CuerrentStateNotSupported() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> service.getSbomVersion("invalidAppId", "invalidSbomVersion", ApiSbomService.SBOM_STATE_CURRENT))
        .withMessage("Retrieving the current state of the sbom is not supported yet.");
  }

  @Test
  public void testGetSbomVersion_UnsupportedState() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> service.getSbomVersion("invalidAppId", "invalidSbomVersion", "dummyState"))
        .withMessage("Invalid sbom state dummyState");
  }

  @Test
  public void testGetSbomVersion_NotFoundInvalidVersion() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> service.getSbomVersion(sbomMetadata.getApplicationId(), "invalidVersion",
                ApiSbomService.SBOM_STATE_ORIGINAL))
        .withMessage(
            "Cannot find version invalidVersion for application with ID " + sbomMetadata.getApplicationId() + ".");
  }

  @Test
  public void testGetSbomVersion_FindOnlyActiveSboms_Xml() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    Path fileInWorkDirPath =
        SbomTestsHelper.createTestFileForSbomMetadata(insightWork.getSbomDir(app.getId()),
            getClass().getResource(
                "/" + getClass().getSimpleName() + "/cb4e10e0f3a94fd98bee955b53f9474c7343830902282944835.xml.gz"));
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withFilename(fileInWorkDirPath.getFileName().toString())
        .withStatus(SbomStatus.ACTIVE.name())
        .build();

    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withStatus(SbomStatus.PENDING.name())
        .build();

    Response response = service.getSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(),
        ApiSbomService.SBOM_STATE_ORIGINAL);
    byte[] fileA = Files.readAllBytes(
        Paths.get(getClass().getResource("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml").getPath()));
    byte[] fileB = (byte[]) response.getEntity();
    assertThat(Arrays.equals(fileA, fileB)).isTrue();
  }

  @Test
  public void testGetSbomVersion_FindOnlyActiveSboms_Json() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    Path fileInWorkDirPath =
        SbomTestsHelper.createTestFileForSbomMetadata(insightWork.getSbomDir(app.getId()),
            getClass().getResource(
                "/" + getClass().getSimpleName() + "/668bbb2087354637b030de2bc1a3faf76935110932971722768.json.gz"));
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withFilename(fileInWorkDirPath.getFileName().toString())
        .withStatus(SbomStatus.ACTIVE.name())
        .build();

    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withStatus(SbomStatus.PENDING.name())
        .build();

    Response response = service.getSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(),
        ApiSbomService.SBOM_STATE_ORIGINAL);
    byte[] fileA = Files.readAllBytes(
        Paths.get(getClass().getResource("/" + getClass().getSimpleName() + "/spdx.json").getPath()));
    byte[] fileB = (byte[]) response.getEntity();
    assertThat(Arrays.equals(fileA, fileB)).isTrue();
  }

  @Test
  public void testGetSbomVersion_NoActiveSboms() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(SbomStatus.PENDING.name())
        .build();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> service.getSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(),
                ApiSbomService.SBOM_STATE_ORIGINAL))
        .withMessage(
            "Cannot find version " + sbomMetadata.getSbomVersion() + " for application with ID " +
                sbomMetadata.getApplicationId() + ".");
  }
}
