/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.EXPERIMENTAL_ONBOARDING_RESOURCE_PATH;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

@Category(SlowTest.class)
public class ApiScmOnboardingResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(EXPERIMENTAL_ONBOARDING_RESOURCE_PATH);
  }

  @Test
  public void testImportRepositories() throws Exception {
    Organization org = tempEntity.newOrganization();
    ImportScmOrganizationRequest importRequest = new ImportScmOrganizationRequest();
    importRequest.scmHostUrl = "https://somescm/owner";
    importRequest.importLimit = 0; //to throw a bad request

    HttpResponse response = restRequest()
        .path(UriBuilder.fromPath(ApiScmOnboardingResource.IMPORT_REPO_PATH)
            .build(org.getId()).toString())
        .body(importRequest)
        .post();

    assertResponseStatus(400, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.SOURCE_CONTROL_IMPORT, "bad-request");
    assertOrganizationData(auditDTO, org);
    assertStandardData(auditDTO, AuditEvent.SOURCE_CONTROL_IMPORT, "bad-request");
    assertCustomData(auditDTO, "scmHostUrl","https://somescm/owner");
    assertCustomData(auditDTO, "importLimit",0);
    assertCustomData(auditDTO, "desiredSubOrganizationCount",0);
  }
}
