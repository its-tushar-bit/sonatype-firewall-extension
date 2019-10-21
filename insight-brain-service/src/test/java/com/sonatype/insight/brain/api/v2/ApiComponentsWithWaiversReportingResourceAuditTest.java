/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class ApiComponentsWithWaiversReportingResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiComponentsWithWaiversReportingResource.PATH);
  }

  @Test
  public void testGetComponentsWithWaivers_NoValues() throws Exception {
    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENTS_WITH_WAIVERS, null);
    assertCustomData(auditDTO, "numberOfApplicationComponents", 0);
    assertCustomData(auditDTO, "numberOfRepositoryComponents", 0);
  }

  @Test
  public void testGetComponentsWithWaivers_Applications() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy();

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1");

    PolicyWaiver policyWaiver = tempEntity.newWaiver("h1", policy.getId(), app.getId(), "Some comments here");
    tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), "h1", policyWaiver);

    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENTS_WITH_WAIVERS, null);
    assertCustomData(auditDTO, "numberOfApplicationComponents", 1);
    assertCustomData(auditDTO, "numberOfRepositoryComponents", 0);
  }

  @Test
  public void testGetComponentsWithWaivers_Repositories() throws Exception {
    Policy policy = tempEntity.newPolicy();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    Repository repo = tempEntity.newRepository();

    tempEntity.newRepositoryPolicyViolation(repo.getId(), 6, "pathName1", true, true, "actionId1", policy.getId(),
        policy.getName(), componentIdentifier, new Date());

    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENTS_WITH_WAIVERS, null);
    assertCustomData(auditDTO, "numberOfApplicationComponents", 0);
    assertCustomData(auditDTO, "numberOfRepositoryComponents", 1);
  }
}
