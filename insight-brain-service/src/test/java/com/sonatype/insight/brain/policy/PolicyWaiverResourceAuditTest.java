/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class PolicyWaiverResourceAuditTest
    extends AbstractAuditTest
{
  private static final String COMPONENT_HASH = "hash";

  @Test
  public void testGetPolicyWaiversByHash_Application() throws Exception {
    final Application application = tempEntity.newApplicationWithParent();
    restRequest(application).path("component", COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetPolicyWaiversByHash_Organization() throws Exception {
    final Organization organization = tempEntity.newOrganization();
    restRequest(organization).path("component", COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetPolicyWaiversByHash_Repository() throws Exception {
    final Repository repository = tempEntity.newRepository();
    restRequest(repository).path("component", COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetPolicyWaiversByHash_RepositoryContainer() throws Exception {
    restRequest(RepositoryContainer.SINGLETON).path("component", COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertRepositoryContainerData(auditDTO);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  public void testGetPolicyWaiversByHash_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    restRequest(application).path("component", COMPONENT_HASH).with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private HttpRequest restRequest(Owner owner) {
    return restRequest().path(PolicyWaiverResource.RESOURCE_PATH)
        .parameter(owner.getType(),
            owner.getType().equals(OwnerType.APPLICATION) ? owner.getPublicId() : owner.getId());
  }
}
