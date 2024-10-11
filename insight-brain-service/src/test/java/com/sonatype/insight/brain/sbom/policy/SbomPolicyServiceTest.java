/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.policy;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.inject.Inject;

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
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

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
        "specFormat",
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
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), sbomVersion, "Active", "fileName", "spec",
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
  public void testGetPolicyViolationsJsonNodeByFileCoordinateId_AppIdAndSbomVersionNotFound() throws IOException {
    String sbomVersion = "sbomVersion1";
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), sbomVersion, "Active", "fileName", "spec",
        "specFormat", "specVersion");
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ReportResourceTest/report-bom", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getPolicyViolationsJsonNodeByFileCoordinateId(app.getId(), "sbomVersion2",
            "86163fcc32524261bfd2bdbedb7eae43", null))
        .withMessage("Cannot find version sbomVersion2 for application with ID " + app.getId() + ".");
  }

  @Test
  public void testGetPolicyViolationsJsonNodeByFileCoordinateId() throws IOException {
    doTestGetPolicyViolationsByFileCoordinateId((sbomVersion, fileCoordinateId) -> {
      try {
        JsonNode jsonNode =
            service.getPolicyViolationsJsonNodeByFileCoordinateId(app.getId(), sbomVersion, fileCoordinateId, null);
        assertThat(jsonNode).isNotNull();
        return JsonUtils.asPojo(jsonNode, PolicyThreats.Component.class);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  @Test
  public void testGetPolicyViolationsByFileCoordinateId() throws IOException {
    doTestGetPolicyViolationsByFileCoordinateId((sbomVersion, fileCoordinateId) -> {
      try {
        return service.getPolicyViolationsByFileCoordinateId(app.getId(), sbomVersion, fileCoordinateId);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  private void doTestGetPolicyViolationsByFileCoordinateId(
      BiFunction<String, String, Component> function) throws IOException
  {
    String sbomVersion = "sbomVersion1";
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), sbomVersion, "Active", "fileName", "spec",
        "specFormat", "specVersion");
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ReportResourceTest/report-bom", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    PolicyThreats.Component component = function.apply(sbomVersion, "86163fcc32524261bfd2bdbedb7eae43");
    assertThat(component.policyThreatLevel).isEqualTo(9);
  }
}
