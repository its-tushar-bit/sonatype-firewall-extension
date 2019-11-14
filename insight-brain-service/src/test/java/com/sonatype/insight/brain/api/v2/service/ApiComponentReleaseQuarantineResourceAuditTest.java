/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;

import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Test;

public class ApiComponentReleaseQuarantineResourceAuditTest
    extends AbstractAuditTest
{
  private static final String COMPONENT_HASH = "hash";

  private static final String PATHNAME = "pathname";

  public static final PackageUrlIdentifier PACKAGE_URL_IDENTIFIER =
      new PackageUrlIdentifier("pkg:maven/g1/a1@v1?type=e1");

  @Test
  public void testReleaseQuarantineWithoutReEval() throws Exception {
    Date quarantineTime = new Date();
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, PATHNAME, COMPONENT_HASH,
            PACKAGE_URL_IDENTIFIER.ensureCompleteIdentifier(), quarantineTime, quarantineTime);

    releaseQuarantineRequest(repositoryComponent.getId()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RELEASE_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertUnquarantineData(auditDTO, repositoryComponent);
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_Unauthorized() throws Exception {
    Date quarantineTime = new Date();
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, PATHNAME, COMPONENT_HASH,
            PACKAGE_URL_IDENTIFIER.ensureCompleteIdentifier(), quarantineTime, quarantineTime);

    releaseQuarantineRequest(repositoryComponent.getId()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RELEASE_QUARANTINE, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  private HttpRequest releaseQuarantineRequest(String quarantineId) {
    return restRequest().path(PublicApiPaths.COMPONENT_QUARANTINE_RELEASE_PATH_V2)
        .parameter(quarantineId).body("waiver comment", MediaType.TEXT_PLAIN);
  }

  private void assertUnquarantineData(AuditDTO auditDTO, RepositoryComponent repositoryComponent) {
    assertCustomData(auditDTO, "componentHash", repositoryComponent.getHash());
    assertCustomData(auditDTO, "componentPathname", repositoryComponent.getPathname());
  }
}
