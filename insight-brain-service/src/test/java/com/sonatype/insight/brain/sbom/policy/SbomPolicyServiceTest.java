/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.policy;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.Component;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class SbomPolicyServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SbomPolicyService service;

  @Inject
  private InsightWork work;

  private Application app;

  private Organization org;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplicationWithParent(org);
  }

  @Test
  public void testGetPolicyViolationsReportEntry_AppIdAndSbomVersionNotFound() throws IOException {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(),
        "spec",
        ACTIVE,
        "specVersion");
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ReportResourceTest/report-bom", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getPolicyViolationsReportEntry(app.getId(), "sbomVersion2"))
        .withMessage("Cannot find version sbomVersion2 for application with ID " + app.getId() + ".");
  }

  @Test
  public void testGetPolicyViolationsReportEntry() throws IOException {
    doTestGetPolicyViolations(sbomVersion -> {
      try {
        ReportEntry reportEntry = service.getPolicyViolationsReportEntry(app.getId(), sbomVersion);
        assertThat(reportEntry).isNotNull();
        return JsonUtils.parse(reportEntry.buf, PolicyThreats.class);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  @Test
  public void testGetPolicyViolations() throws Exception {
    doTestGetPolicyViolations(sbomVersion -> {
      try {
        return service.getPolicyViolations(app.getId(), sbomVersion);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  private void doTestGetPolicyViolations(Function<String, PolicyThreats> function) throws IOException {
    String sbomVersion = "sbomVersion1";
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), sbomVersion, ACTIVE, "fileName", "spec",
        "specFormat", "specVersion");
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ReportResourceTest/report-bom", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    PolicyThreats policyThreats = function.apply(sbomVersion);

    assertThat(policyThreats.aaData)
        .extracting(policyThreat -> policyThreat.policyId)
        .containsOnly("644a8c0052eb42b2829d6f9fcaba7ea3");
  }

  @Test
  public void testGetPolicyViolationsJsonNodeByFileCoordinateIdOrHash_NoComponentRefAndHash() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> service.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), "version", null, null, null,
                null, null))
        .withMessage("componentRef, fileCoordinateId and hash cannot be both null or empty.");
  }

  @Test
  public void testGetPolicyViolationsJsonNodeByComponentRefAndSbomVersionNotFound() throws IOException {
    String sbomVersion = "sbomVersion1";
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), sbomVersion, ACTIVE, "fileName", "spec",
        "specFormat", "specVersion");
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ReportResourceTest/report-bom", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), "sbomVersion2",
            "eb95f7c60bd3ae19e4ee272c96b62ca473614987", "86163fcc32524261bfd2bdbedb7eae43", null, null, null))
        .withMessage("Cannot find version sbomVersion2 for application with ID " + app.getId() + ".");
  }

  @Test
  public void testGetPolicyViolationsJsonNodeByFileCoordinateIdOrHash_ByComponentRef() throws IOException {
    String sbomVersion = "sbomVersion1";
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), sbomVersion, ACTIVE, "fileName", "spec",
        "specFormat", "specVersion");
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ReportResourceTest/report-bom-componentRefs", tempDir),
        reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, thirdPartyScan.getScanId());
    JsonNode jsonNode =
        service.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), sbomVersion,
            "eb95f7c60bd3ae19e4ee272c96b62ca473614987",
            null, null, null, null);
    assertThat(jsonNode).isNotNull();
    Component cp = JsonUtils.asPojo(jsonNode, Component.class);
    assertThat(cp.policyThreatLevel).isEqualTo(9);
  }

  @Test
  public void testGetPolicyViolationsJsonNodeByFileCoordinateIdOrHash_ByComponentRefNotFound() throws IOException {
    doTestGetPolicyViolationsByFileCoordinateIdOrHash((sbomVersion, componentRef, fileCoordinateId) -> {
      try {
        JsonNode jsonNode = service.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), sbomVersion,
            componentRef, "some-fake-file-coordinate-id", null, null, null);
        assertThat(jsonNode).isNull();
        return JsonUtils.asPojo(jsonNode, PolicyThreats.Component.class);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, true);
  }

  @Test
  public void testGetPolicyViolationsJsonNodeByFileCoordinateIdOrHash_ByHash() throws IOException {
    doTestGetPolicyViolationsByFileCoordinateIdOrHash((sbomVersion, componentRef, fileCoordinateId) -> {
      try {
        JsonNode jsonNode = service.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), sbomVersion, null,
            componentRef, "1249e25aebb15358bedd", null, null);
        assertThat(jsonNode).isNotNull();
        return JsonUtils.asPojo(jsonNode, PolicyThreats.Component.class);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, false);
  }

  @Test
  public void testGetPolicyViolationsJsonNodeByFileCoordinateIdOrHash_ByHashNotFound() throws IOException {
    doTestGetPolicyViolationsByFileCoordinateIdOrHash((sbomVersion, componentRef, fileCoordinateId) -> {
      try {
        JsonNode jsonNode = service.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), sbomVersion,
            componentRef, "some-fake-file-coordinate-id", "some-fake-file-hash", null, null);
        assertThat(jsonNode).isNull();
        return JsonUtils.asPojo(jsonNode, PolicyThreats.Component.class);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, true);
  }

  @Test
  public void testGetPolicyViolationsJsonNodeByFileCoordinateIdOrHash_PreferComponentRef() throws IOException {
    doTestGetPolicyViolationsByFileCoordinateIdOrHash((sbomVersion, componentRef, fileCoordinateId) -> {
      try {
        JsonNode jsonNode = service.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), sbomVersion,
            componentRef, fileCoordinateId, "1a667c9d419dc4f185c9", null, null);
        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.get("hash").asText()).isEqualTo("1249e25aebb15358bedd");
        return JsonUtils.asPojo(jsonNode, PolicyThreats.Component.class);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, false);
  }

  @Test
  public void testGetPolicyViolationsByFileCoordinateIdOrHash_ByFileCoordinateId() throws IOException {
    doTestGetPolicyViolationsByFileCoordinateIdOrHash((sbomVersion, componentRef, fileCoordinateId) -> {
      try {
        Component result = service.getPolicyViolationsByFileCoordinateIdOrHash(app.getId(), sbomVersion, componentRef,
            fileCoordinateId, null, null, null);
        assertThat(result).isNotNull();
        return result;
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, false);
  }

  @Test
  public void testGetPolicyViolationsByFileCoordinateIdOrHash_ByFileCoordinateIdNotFound() throws IOException {
    doTestGetPolicyViolationsByFileCoordinateIdOrHash((sbomVersion, componentRef, fileCoordinateId) -> {
      try {
        Component result = service.getPolicyViolationsByFileCoordinateIdOrHash(app.getId(), sbomVersion,
            componentRef, "some-fake-file-coordinate-id", null, null, null);
        assertThat(result).isNull();
        return result;
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, true);
  }

  @Test
  public void testGetPolicyViolationsByFileCoordinateIdOrHash_ByHash() throws IOException {
    doTestGetPolicyViolationsByFileCoordinateIdOrHash((sbomVersion, componentRef, fileCoordinateId) -> {
      try {
        Component result = service.getPolicyViolationsByFileCoordinateIdOrHash(app.getId(), sbomVersion,
            componentRef, "some-fake-file-coordinate-id", "1249e25aebb15358bedd", null, null);
        assertThat(result).isNotNull();
        return result;
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, false);
  }

  @Test
  public void testGetPolicyViolationsByFileCoordinateIdOrHash_ByHashNotFound() throws IOException {
    doTestGetPolicyViolationsByFileCoordinateIdOrHash((sbomVersion, componentRef, fileCoordinateId) -> {
      try {
        Component result = service.getPolicyViolationsByFileCoordinateIdOrHash(app.getId(), sbomVersion,
            componentRef, "some-fake-file-coordinate-id", "some-fake-file-hash", null, null);
        assertThat(result).isNull();
        return result;
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, true);
  }

  @Test
  public void testGetPolicyViolationsByFileCoordinateIdOrHash_PreferFileCoordinateId() throws IOException {
    doTestGetPolicyViolationsByFileCoordinateIdOrHash((sbomVersion, componentRef, fileCoordinateId) -> {
      try {
        Component result = service.getPolicyViolationsByFileCoordinateIdOrHash(app.getId(), sbomVersion,
            componentRef, fileCoordinateId, "1a667c9d419dc4f185c9", null, null);
        assertThat(result).isNotNull();
        assertThat(result.hash).isEqualTo("1249e25aebb15358bedd");
        return result;
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, false);
  }

  private void doTestGetPolicyViolationsByFileCoordinateIdOrHash(
      TriFunction<String, String, String, Component> function,
      boolean isEmptyResult) throws IOException
  {
    String sbomVersion = "sbomVersion1";
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), sbomVersion, ACTIVE, "fileName", "spec",
        "specFormat", "specVersion");
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ReportResourceTest/report-bom", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    PolicyThreats.Component component = function.apply(sbomVersion,
        "eb95f7c60bd3ae19e4ee272c96b62ca473614987", "86163fcc32524261bfd2bdbedb7eae43");
    if (isEmptyResult) {
      assertThat(component).isNull();
    }
    else {
      assertThat(component.policyThreatLevel).isEqualTo(9);
    }
  }
}
