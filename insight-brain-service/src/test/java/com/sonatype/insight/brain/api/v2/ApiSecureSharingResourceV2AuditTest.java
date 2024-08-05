/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.nio.file.Path;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SbomAction;
import com.sonatype.insight.brain.thirdparty.SbomStatus;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.file.SbomFormat;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.setupScenarioWithMetadataComponentSecurityLicenseAndVex;

public class ApiSecureSharingResourceV2AuditTest
    extends AbstractAuditTest
{
  @Before
  public void before() throws Exception {
    setFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(true);
  }

  @Test
  public void testExportSbom() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    InsightWork insightWork = getCLMServer().getInstance(InsightWork.class);
    Path zippedBom = mockOriginalSbom(this.getClass(), "valid-cyclonedx-result-bom.xml",
        insightWork.getSbomDir(application.getId()).toPath());
    String sbomVersion = tempEntity.newRandomHash();
    setupScenarioWithMetadataComponentSecurityLicenseAndVex(tempEntity, application, zippedBom, sbomVersion,
        "CycloneDx", "1.5", SbomFormat.XML);

    HttpResponse response = restRequest()
        .path(PublicApiPaths.DISTRIBUTE_PATH)
        .path(ApiSecureSharingResourceV2.SBOM_VERSION_PATH)
        .parameter(application.getId(), sbomVersion)
        .get();

    assertResponseStatus(200, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_SBOM_VERSION, null);
    assertCustomData(auditDTO, "applicationId", application.getId());
    assertCustomData(auditDTO, "sbomVersion", sbomVersion);
    assertCustomData(auditDTO, "status", SbomStatus.ACTIVE.name());
    assertCustomData(auditDTO, "operation", SbomAction.READ.name());
  }
}
