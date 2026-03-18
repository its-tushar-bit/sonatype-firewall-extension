/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class ApiComponentsInQuarantineReportingResourceAuditTest
    extends AbstractAuditTest
{
  private Repository repo1;

  private Repository repo2;

  @Before
  public void setup() {
    repo1 = tempEntity.newRepository("rm1", "r1", "maven2");
    repo2 = tempEntity.newRepository("rm2", "r2", "maven3");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest()
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiComponentsInQuarantineReportingResource.PATH);
  }

  @Test
  public void testGetComponentsInQuarantine_WithNoComponents() throws Exception {
    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_QUARANTINED_COMPONENTS, null);
    assertCustomData(auditDTO, "numberOfQuarantinedComponents", 0);
  }

  @Test
  public void testGetComponentsInQuarantine_WithQuarantinedComponents() throws Exception {
    createRepositoryComponent(repo1, "pathname1", true, false);
    createRepositoryComponent(repo2, "pathname2", true, false);

    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_QUARANTINED_COMPONENTS, null);
    assertCustomData(auditDTO, "numberOfQuarantinedComponents", 2);
  }

  @Test
  public void testGetComponentsInQuarantine_WithQuarantinedAndNonQuarantinedComponents() throws Exception {
    // quarantined components
    createRepositoryComponent(repo1, "pathname1", true, false);
    createRepositoryComponent(repo1, "pathname2", true, false);
    createRepositoryComponent(repo2, "pathname3", true, false);

    // non-quarantined components
    createRepositoryComponent(repo1, "pathname4", false, false);
    createRepositoryComponent(repo2, "pathname5", false, false);
    createRepositoryComponent(repo2, "pathname6", false, false);

    // component released from quarantine
    createRepositoryComponent(repo2, "pathname7", true, true);

    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_QUARANTINED_COMPONENTS, null);
    assertCustomData(auditDTO, "numberOfQuarantinedComponents", 3);
  }

  @Test
  public void testGetComponentsInQuarantine_WithNoQuarantinedComponents() throws Exception {
    createRepositoryComponent(repo1, "pathname1", false, false);
    createRepositoryComponent(repo2, "pathname2", false, false);

    restRequest().get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_QUARANTINED_COMPONENTS, null);
    assertCustomData(auditDTO, "numberOfQuarantinedComponents", 0);
  }

  private void createRepositoryComponent(
      Repository repo,
      String pathname,
      boolean isQuarantined,
      boolean isReleasedFromQuarantine)
  {
    RepositoryComponent repositoryComponent = new RepositoryComponent();
    repositoryComponent.setRepositoryId(repo.getId());
    repositoryComponent.setPathname(pathname);
    repositoryComponent.setTime(new Date());
    repositoryComponent.setHash("hash");
    repositoryComponent.setComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    repositoryComponent.setMatchStateId(MatchState.EXACT.getId());
    repositoryComponent.setIdentificationSourceId(IdentificationSource.SONATYPE.getId());
    repositoryComponent.setLastEvaluationTime(new Date());
    if (isQuarantined) {
      repositoryComponent.setQuarantineTime(new Date());
    }
    if (isReleasedFromQuarantine) {
      repositoryComponent.setUnquarantineTimeForManualRelease(new Date());
    }
    tempEntity.newRepositoryComponent(repositoryComponent);
  }
}
