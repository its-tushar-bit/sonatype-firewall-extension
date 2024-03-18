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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
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

  @Test
  public void testGetVulnerabilities() {
    Application application = tempEntity.newApplicationWithParent();

    ThirdPartyFile file1 = tempEntity.newThirdPartyFile("CycloneDX-bom.xml");
    ThirdPartyFile file2 = tempEntity.newThirdPartyFile("SPDX-spdx.json");

    tempEntity.newThirdPartySbomMetadata(file1.getId(), application.getId(), "ACTIVE", file1.getFilename());
    tempEntity.newThirdPartySbomMetadata(file2.getId(), application.getId(),  "ACTIVE", file2.getFilename());

    ThirdPartyFileCoordinate c1 = tempEntity.newThirdPartyFileCoordinate(file1, "s1", "f1", "n1", "v1");
    ThirdPartyFileCoordinate c2 = tempEntity.newThirdPartyFileCoordinate(file2, "s2", "f2", "n2", "v2");

    tempEntity.newThirdPartyCoordinateSecurity(c1, "r1", "d1", "l1", 3.5F, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(c1, "r2", "d2", "l2", 7.5F, "sd2", "f2");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r3", "d3", "l3", 1.5F, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r4", "d4", "l4", 0.5F, "sd4", "f4");

    List<ThirdPartySbomMetadataSummaryDTO> results = service.getSbomListForAppId(application.getId(), "asc",  5, 0);
    assertThat(results).hasSize(2);

    Collections.sort(results, Comparator.comparingInt(ThirdPartySbomMetadataSummaryDTO::getHigh));

    assertThat(results).hasSize(2);
    assertThat(results.get(0).getLow()).isEqualTo(2);
    assertThat(results.get(1).getLow()).isEqualTo(1);
    assertThat(results.get(1).getHigh()).isEqualTo(1);
  }
}
