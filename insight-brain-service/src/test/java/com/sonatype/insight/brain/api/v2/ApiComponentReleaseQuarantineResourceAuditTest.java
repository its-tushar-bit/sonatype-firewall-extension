/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiComponentReleaseQuarantineResourceAuditTest
    extends AbstractAuditTest
{
  private static final String COMPONENT_HASH = "hash";

  private static final String PATHNAME = "pathname";

  public static final PackageUrlIdentifier PACKAGE_URL_IDENTIFIER =
      new PackageUrlIdentifier("pkg:maven/g1/a1@v1?type=e1");

  private static final String REPO_MAN_INSTANCE_ID = "repoManagerInstanceId";

  private static final String REPO_PUBLIC_ID = "repoPublicId";

  private PolicyWaiverDAO policyWaiverDAO;

  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Before
  public void setUp() {
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);
    repositoryPolicyViolationDAO = lookup(RepositoryPolicyViolationDAO.class);
  }

  @Test
  public void testReleaseQuarantineWithoutReEval() throws Exception {
    Date quarantineTime = new Date();
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, PATHNAME, COMPONENT_HASH,
            PACKAGE_URL_IDENTIFIER.ensureCompleteIdentifier(), quarantineTime, quarantineTime);

    Policy policy1 = tempEntity.newPolicy(repository.getParentOwnerId());
    Policy policy2 = tempEntity.newPolicy(repository.getParentOwnerId());

    RepositoryPolicyViolation repositoryPolicyViolation1 =
        createRepositoryPolicyViolation(repositoryComponent, false, 10, policy1, Action.ID_FAIL);

    RepositoryPolicyViolation repositoryPolicyViolation2 =
        createRepositoryPolicyViolation(repositoryComponent, false, 10, policy2, Action.ID_FAIL);

    releaseQuarantineRequest(repositoryComponent.getId()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RELEASE_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertUnquarantineData(auditDTO, repositoryComponent);

    PolicyWaiver policyWaiver1 = getSavedPolicyWaiver(repositoryPolicyViolation1.getId());
    PolicyWaiver policyWaiver2 = getSavedPolicyWaiver(repositoryPolicyViolation2.getId());

    List<AuditDTO> waiverAuditDTOs = assertAuditLogs(AuditEvent.CREATE_WAIVER, 2, null);
    assertPolicyWaiverData(waiverAuditDTOs.get(0), policy1, policyWaiver1, repository);
    assertPolicyWaiverData(waiverAuditDTOs.get(1), policy2, policyWaiver2, repository);
  }

  private PolicyWaiver getSavedPolicyWaiver(String repositoryPolicyViolationId) {
    RepositoryPolicyViolation repositoryPolicyViolation =
        repositoryPolicyViolationDAO.getById(repositoryPolicyViolationId);
    return policyWaiverDAO.getByIdNotNull(repositoryPolicyViolation.getPolicyWaiverId());
  }

  private RepositoryPolicyViolation createRepositoryPolicyViolation(
      final RepositoryComponent repositoryComponent,
      final boolean waived,
      final int threatLevel,
      final Policy policy,
      final String action)
  {
    return tempEntity.newRepositoryPolicyViolation(repositoryComponent.getRepositoryId(), threatLevel,
        repositoryComponent.getPathname(), waived, action, policy.getId(), policy.getName(),
        repositoryComponent.getComponentIdentifier());
  }

  @Test(expected = ConditionTimeoutException.class)
  public void testReleaseQuarantineWithoutReEval_NoWaivers() throws Exception {
    Date quarantineTime = new Date();
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, PATHNAME, COMPONENT_HASH,
            PACKAGE_URL_IDENTIFIER.ensureCompleteIdentifier(), quarantineTime, quarantineTime);

    releaseQuarantineRequest(repositoryComponent.getId()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RELEASE_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertUnquarantineData(auditDTO, repositoryComponent);

    // make sure there is no waiver auditing attempted
    assertAuditLog(AuditEvent.CREATE_WAIVER, null);
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
        .parameter(quarantineId)
        .body("waiver comment", MediaType.TEXT_PLAIN);
  }

  private void assertUnquarantineData(AuditDTO auditDTO, RepositoryComponent repositoryComponent) {
    assertCustomData(auditDTO, "componentHash", repositoryComponent.getHash());
    assertCustomData(auditDTO, "componentPathname", repositoryComponent.getPathname());
  }

  private void assertPolicyWaiverData(
      AuditDTO auditDTO,
      Policy policy,
      PolicyWaiver policyWaiver,
      Repository repository)
  {
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "policyId", policy.getId());
    assertCustomData(auditDTO, "policyName", policy.getName());
    assertCustomData(auditDTO, "policyWaiverId", policyWaiver.getId());
    assertCustomData(auditDTO, "comment", policyWaiver.getComment());
    assertCustomData(auditDTO, "componentHash", policyWaiver.getHash());
    if (policyWaiver.getConstraintFacts() == null) {
      assertCustomData(auditDTO, "policyConstraints", null);
    }
    else {
      assertCustomObject(auditDTO, "policyConstraints",
          policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
    }
  }
}
