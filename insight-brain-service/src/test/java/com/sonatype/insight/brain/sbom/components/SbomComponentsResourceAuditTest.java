/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.io.File;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class SbomComponentsResourceAuditTest extends AbstractAuditTest
{
  private Application app;

  private InsightWork work;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent();
    work = getCLMServer().getInstance(InsightWork.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SbomComponentsResource.RESOURCE_BASE_PATH);
  }

  @Test
  public void testGetComponentsDetails() throws Exception {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE,
            thirdPartyFile.getFilename());
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");
    ThirdPartyCoordinateSecurity vulnerability =
        tempEntity.newThirdPartyCoordinateSecurity(component, "cve", "d1", "l1", 9, "d1", "f1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerability, "cve", "resolved",
            "code_not_reachable", "response", "details");
    testProductLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsResourceTest", tempDir), reportFile);
    HttpResponse response = restRequest().path(SbomComponentsResource.COMPONENT_DETAILS_PATH)
        .parameter(app.getId(),sbomMetadata.getSbomVersion(), component.getHash()).get();

    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_SBOM_COMPONENT_DETAILS, null);
    assertComponentDetailsCustomData(auditDTO, component.getHash());
  }

  @Test
  public void testGetComponentsDetails_NotFound() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.SBOM_MANAGER);
    HttpResponse response = restRequest().path(SbomComponentsResource.COMPONENT_DETAILS_PATH)
        .parameter(app.getId(),"any", "any").get();

    assertResponseStatus(404, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_SBOM_COMPONENT_DETAILS, "not-found");
    assertComponentDetailsCustomData(auditDTO, "any");
  }

  @Test
  public void testGetComponentsDetails_unlicensed() throws Exception {
    HttpResponse response = restRequest().path(SbomComponentsResource.COMPONENT_DETAILS_PATH)
        .parameter(app.getId(),"any", "any").get();

    assertResponseStatus(402, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_SBOM_COMPONENT_DETAILS, "unlicensed");
    assertCustomData(auditDTO, "applicationId", app.getId());
  }

  private void assertComponentDetailsCustomData(AuditDTO auditDTO, String componentHash) {
    assertCustomData(auditDTO, "applicationId", app.getId());
    assertCustomData(auditDTO, "componentHash", componentHash);
  }
}
