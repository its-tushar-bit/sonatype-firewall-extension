/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.nio.file.Path;
import javax.inject.Inject;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.SbomTestsHelper;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSbomResourceAuditTest
    extends AbstractAuditTest
{
  @Inject
  private InsightWork insightWork;

  @Before
  public void setUp() throws Exception {
    insightWork = lookup(InsightWork.class);
    setFeatures(LicensedFeature.SBOM_MANAGER);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SBOM_RESOURCE_PATH);
  }

  @Test
  public void testDeleteSbomVersion_Authorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path fileInWorkDirPath =
        SbomTestsHelper.createTestFileForSbomMetadata(insightWork.getSbomDir(app.getId()),
            getClass().getResource("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"));
    final ThirdPartySbomMetadata thirdPartySbomMetadata =
        tempEntity.createSbomMetadata(app.getId(), null, fileInWorkDirPath.getFileName().toString());

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSIONS_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion()).delete();
    assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_SBOM_VERSION, null);
    assertThat(auditDTO.data).containsEntry("applicationId", thirdPartySbomMetadata.getApplicationId());
    assertCustomData(auditDTO, "sbomVersion", thirdPartySbomMetadata.getSbomVersion());
  }

  @Test
  public void testDeleteSbomVersion_Unauthorized() throws Exception {
    ThirdPartySbomMetadata thirdPartySbomMetadata = tempEntity.createSbomMetadata();
    HttpResponse response = restRequest().with(unauthorizedUser()).path(ApiSbomResource.SBOM_VERSIONS_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion()).delete();
    assertResponseStatus(403, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_SBOM_VERSION, "unauthorized");
    assertThat(auditDTO.data).containsEntry("applicationId", thirdPartySbomMetadata.getApplicationId());
  }
}
