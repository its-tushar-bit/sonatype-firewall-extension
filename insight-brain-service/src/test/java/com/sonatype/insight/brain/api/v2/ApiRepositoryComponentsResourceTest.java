/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ApiRepositoryComponentsResourceTest
    extends AbstractResourceTest
{
  private RepositoryManager repositoryManager;

  private Repository repository;

  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  private OwnerComponentDAO ownerComponentDAO;

  private PolicyViolationDAO policyViolationDAO;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    repositoryManager = tempEntity.newRepositoryManager("test-nexus");
    repository = tempEntity.newRepository(repositoryManager, "maven-hosted");
    hostedRepositoryComponentDAO = daoFactory.createHostedRepositoryComponentDAO();
    ownerComponentDAO = daoFactory.createOwnerComponentDAO();
    policyViolationDAO = daoFactory.createPolicyViolationDAO();
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
    // HRC delete does not cascade owner_component (only the reverse FK exists). Scope the cascade
    // to rows owned by HRCs in THIS test's repository so parallel tests keep their state intact.
    // (Order: PV → LPE → PE → OC — reverse FK direction.)
    try (TransactionContext tx = ownerComponentDAO.createTransactionContext()) {
      List<String> hrcIds = hostedRepositoryComponentDAO.getByRepositoryId(tx, repository.getId())
          .stream()
          .map(HostedRepositoryComponent::getId)
          .collect(Collectors.toList());
      if (hrcIds.isEmpty()) {
        return;
      }
      tx.begin();
      tx.dsl()
          .deleteFrom(com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyViolation.POLICY_VIOLATION)
          .where(com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyViolation.POLICY_VIOLATION.OWNER_ID
              .in(hrcIds))
          .execute();
      tx.dsl()
          .deleteFrom(com.sonatype.insight.brain.jooq.generated.ods.tables.LastPolicyEvaluation.LAST_POLICY_EVALUATION)
          .where(
              com.sonatype.insight.brain.jooq.generated.ods.tables.LastPolicyEvaluation.LAST_POLICY_EVALUATION.OWNER_ID
                  .in(hrcIds))
          .execute();
      tx.dsl()
          .deleteFrom(com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyEvaluation.POLICY_EVALUATION)
          .where(com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyEvaluation.POLICY_EVALUATION.OWNER_ID
              .in(hrcIds))
          .execute();
      tx.dsl()
          .deleteFrom(com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerComponent.OWNER_COMPONENT)
          .where(com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerComponent.OWNER_COMPONENT.OWNER_ID
              .in(hrcIds))
          .execute();
      tx.commit();
    }
  }

  @Test
  public void testGetComponents_ReflectsFreshHrcData() throws Exception {
    HostedRepositoryComponent hrc =
        tempEntity.newHostedRepositoryComponent(repository, "lib/log4j-core-2.14.1.jar", "hash-log4j");
    OwnerComponent oc = new OwnerComponent(
        hrc.getId(),
        "build",
        new Date(),
        "hash-log4j",
        ComponentIdentifier.createMavenCoordinates("org.apache.logging.log4j", "log4j-core", "2.14.1"),
        MatchState.EXACT.getId(),
        "SONATYPE",
        false,
        List.of("lib/log4j-core-2.14.1.jar"));
    ownerComponentDAO.insert(oc);
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      hostedRepositoryComponentDAO.updateOwnerComponentId(tx, hrc.getId(), oc.getId());
      tx.commit();
    }

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .get()
        .getBodyText();

    assertThat(response).contains("\"matchStateId\":\"" + MatchState.EXACT.getId() + "\"");
    assertThat(response).contains("log4j-core-2.14.1.jar");
    assertThat(response).contains("\"totalCount\":1");
  }

  @Test
  public void testGetComponents_EmptyRepositoryWithFeatureFlag() throws Exception {
    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .get()
        .getBodyText();
    assertThat(response).contains("\"components\":[]");
    assertThat(response).contains("\"totalCount\":0");
    assertThat(response).contains("\"hasNextPage\":false");
    assertThat(response).contains("\"hasQueuedScans\":false");
  }

  @Test
  public void testGetComponents_QueuedScanFlag() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    tempEntity.newHostedComponentScanQueue(hrc.getId(), repository.getId(), "PENDING");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .get()
        .getBodyText();
    assertThat(response).contains("\"hasQueuedScans\":true");
  }

  @Test
  public void testGetComponents_ReturnsComponents() throws Exception {
    tempEntity.newHostedRepositoryComponent(repository, "log4j-core-2.14.1.jar", "hash1");
    tempEntity.newHostedRepositoryComponent(repository, "commons-text-1.9.0.jar", "hash2");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .get()
        .getBodyText();

    assertThat(response).contains("\"totalCount\":2");
    assertThat(response).contains("log4j-core-2.14.1.jar");
    assertThat(response).contains("commons-text-1.9.0.jar");
  }

  @Test
  public void testGetComponents_FilterByPathname() throws Exception {
    tempEntity.newHostedRepositoryComponent(repository, "log4j-core-2.14.1.jar", "hash1");
    tempEntity.newHostedRepositoryComponent(repository, "commons-text-1.9.0.jar", "hash2");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .query("filter", "log4j")
        .get()
        .getBodyText();

    assertThat(response).contains("log4j-core-2.14.1.jar");
    assertThat(response).doesNotContain("commons-text-1.9.0.jar");
  }

  @Test
  public void testGetComponents_FilterByComponentIdentifier() throws Exception {
    // Pathname contains no "log4j"; only the OC's coordinates JSON does. Match must hit via the
    // OWNER_COMPONENT.COMPONENT_ID_COORDINATES_JSON branch of the filter.
    HostedRepositoryComponent hrc =
        tempEntity.newHostedRepositoryComponent(repository, "libs/hash-abc.jar", "hashcoords");
    OwnerComponent oc = new OwnerComponent(hrc.getId(), "build", new Date(), "hashcoords",
        ComponentIdentifier.createMavenCoordinates("org.apache.logging.log4j", "log4j-core", "2.14.1"),
        MatchState.EXACT.getId(), "SONATYPE", false, List.of());
    ownerComponentDAO.insert(oc);
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      hostedRepositoryComponentDAO.updateOwnerComponentId(tx, hrc.getId(), oc.getId());
      tx.commit();
    }
    tempEntity.newHostedRepositoryComponent(repository, "libs/hash-xyz.jar", "hashother");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .query("filter", "log4j")
        .get()
        .getBodyText();

    assertThat(response).contains(hrc.getId());
    assertThat(response).contains("\"totalCount\":1");
  }

  @Test
  public void testGetComponents_Pagination() throws Exception {
    for (int i = 0; i < 30; i++) {
      tempEntity.newHostedRepositoryComponent(repository, "component-" + i + ".jar", "hash-" + i);
    }

    String page1 = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .query("page", "1")
        .query("pageSize", "25")
        .get()
        .getBodyText();

    assertThat(page1).contains("\"hasNextPage\":true");
    assertThat(page1).contains("\"page\":1");
    assertThat(page1).contains("\"totalCount\":30");
  }

  @Test
  public void testGetComponents_PageSizeCappedAt100() throws Exception {
    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .query("pageSize", "1000000")
        .get()
        .getBodyText();

    assertThat(response).contains("\"pageSize\":100");
  }

  @Test
  public void testGetComponents_WrongManager_Returns404() throws Exception {
    int status = restRequest()
        .path("/api/v2/repositories/wrong-manager/" + repository.getId() + "/components")
        .get()
        .getStatusCode();

    assertThat(status).isEqualTo(404);
  }

  @Test
  public void testGetComponents_FeatureDisabled_Returns404() throws Exception {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);

    int status = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .get()
        .getStatusCode();

    assertThat(status).isEqualTo(404);
  }

  @Test
  public void testGetComponent_ReturnsComponent() throws Exception {
    HostedRepositoryComponent hrc =
        tempEntity.newHostedRepositoryComponent(repository, "log4j-core-2.14.1.jar", "hash1");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/" + hrc.getId())
        .get()
        .getBodyText();

    assertThat(response).contains("log4j-core-2.14.1.jar");
  }

  @Test
  public void testGetComponent_NotFound_Returns404() throws Exception {
    int status = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/nonexistent-id")
        .get()
        .getStatusCode();

    assertThat(status).isEqualTo(404);
  }

  @Test
  public void testGetComponent_WithViolation_ReturnsViolationCount() throws Exception {
    HostedRepositoryComponent hrc =
        tempEntity.newHostedRepositoryComponent(repository, "log4j-core-2.14.1.jar", "hash1");
    PolicyEvaluation pe = seedOwnerComponentAndEvaluation(hrc, "hash1");
    Policy p = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "P");
    tempEntity.newPolicyViolation(pe, p);

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/" + hrc.getId())
        .get()
        .getBodyText();

    assertThat(response).contains("\"violationCount\":1");
  }

  @Test
  public void testGetViolations_ReturnsEmptyList() throws Exception {
    HostedRepositoryComponent hrc =
        tempEntity.newHostedRepositoryComponent(repository, "log4j-core-2.14.1.jar", "hash1");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/" + hrc.getId()
            + "/violations")
        .get()
        .getBodyText();

    assertThat(response).contains("\"violations\":[]");
    assertThat(response).contains("\"totalViolations\":0");
  }

  @Test
  public void testGetViolations_ReturnsViolations() throws Exception {
    HostedRepositoryComponent hrc =
        tempEntity.newHostedRepositoryComponent(repository, "log4j-core-2.14.1.jar", "hash1");
    PolicyEvaluation pe = seedOwnerComponentAndEvaluation(hrc, "hash1");
    Policy p = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "P");
    tempEntity.newPolicyViolation(pe, p);

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/" + hrc.getId()
            + "/violations")
        .get()
        .getBodyText();

    assertThat(response).contains("\"totalViolations\":1");
    assertThat(response).contains("policyName");
    assertThat(response).contains("\"threatLevel\":5");
    assertThat(response).contains("\"waived\":false");
  }

  @Test
  public void testGetViolations_ActiveOnlyExcludesWaivedAndFixed() throws Exception {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    PolicyEvaluation pe = seedOwnerComponentAndEvaluation(hrc, hrc.getHash());
    Policy p = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "P");
    tempEntity.newPolicyViolation(pe, p); // active
    PolicyViolation waived = tempEntity.newPolicyViolation(pe, p);
    waived.setWaiveTime(new Date());
    policyViolationDAO.update(waived);

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/" + hrc.getId() + "/violations")
        .get()
        .getBodyText();
    assertThat(response).contains("\"totalViolations\":1");
  }

  @Test
  public void testGetQueueStats_EmptyQueue() throws Exception {
    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/queue/stats")
        .get()
        .getBodyText();

    assertThat(response).contains("\"pending\":0");
    assertThat(response).contains("\"processing\":0");
    assertThat(response).contains("\"completed\":0");
    assertThat(response).contains("\"failed\":0");
    assertThat(response).contains("\"total\":0");
  }

  @Test
  public void testGetQueueStats_WithEntries() throws Exception {
    HostedRepositoryComponent hrc =
        tempEntity.newHostedRepositoryComponent(repository, "log4j-core-2.14.1.jar", "hash1");
    tempEntity.newHostedComponentScanQueue(hrc.getId(), repository.getId(), "PENDING");
    tempEntity.newHostedComponentScanQueue(hrc.getId(), repository.getId(), "IN_PROGRESS");
    tempEntity.newHostedComponentScanQueue(hrc.getId(), repository.getId(), "COMPLETED");
    tempEntity.newHostedComponentScanQueue(hrc.getId(), repository.getId(), "FAILED");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/queue/stats")
        .get()
        .getBodyText();

    assertThat(response).contains("\"pending\":1");
    assertThat(response).contains("\"processing\":1");
    assertThat(response).contains("\"completed\":1");
    assertThat(response).contains("\"failed\":1");
    assertThat(response).contains("\"total\":4");
  }

  @Test
  public void testGetComponents_ComponentCountReflectsDiscoveredOwnerComponents() throws Exception {
    // componentCount = COUNT(owner_component WHERE owner_id = hrc.id AND stage_type_id = <rendered>).
    HostedRepositoryComponent hrc =
        tempEntity.newHostedRepositoryComponent(repository, "archive-of-archives.war", "hashArchive");
    OwnerComponent outer = new OwnerComponent(hrc.getId(), "build", new Date(), "hashArchive", null,
        MatchState.EXACT.getId(), "SONATYPE", false, List.of());
    ownerComponentDAO.insert(outer);
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      hostedRepositoryComponentDAO.updateOwnerComponentId(tx, hrc.getId(), outer.getId());
      tx.commit();
    }
    // Two additional discovered inner components at the same stage — total build-stage OCs = 3.
    ownerComponentDAO.insert(new OwnerComponent(hrc.getId(), "build", new Date(), "hashInner1", null,
        MatchState.EXACT.getId(), "SONATYPE", false, List.of()));
    ownerComponentDAO.insert(new OwnerComponent(hrc.getId(), "build", new Date(), "hashInner2", null,
        MatchState.EXACT.getId(), "SONATYPE", false, List.of()));
    // A release-stage OC that must NOT count — the rendered row is at build.
    ownerComponentDAO.insert(new OwnerComponent(hrc.getId(), "release", new Date(), "hashInner3", null,
        MatchState.EXACT.getId(), "SONATYPE", false, List.of()));

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/" + hrc.getId())
        .get()
        .getBodyText();

    assertThat(response).contains("\"componentCount\":3");
  }

  @Test
  public void testGetComponent_MultiStageHrc_dtoConsistentlyPinnedToRepinnedStage() throws Exception {
    // Same HRC evaluated first at build then repinned to release. All rendered fields
    // (stageTypeId, scanId, violationCount, componentCount) must reference release, not build.
    HostedRepositoryComponent hrc =
        tempEntity.newHostedRepositoryComponent(repository, "libs/multistage.jar", "hashms");

    OwnerComponent buildOc = new OwnerComponent(hrc.getId(), "build", new Date(), "hashms", null,
        MatchState.EXACT.getId(), "SONATYPE", false, List.of());
    ownerComponentDAO.insert(buildOc);
    tempEntity.newPolicyEvaluation(hrc.getId(), "build", "scan-build-1",
        false, false, new Date(1_700_000_000_000L), ScanTriggerType.HOSTED_REPOSITORY_SCANNING);
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "P");
    tempEntity.newPolicyViolation(
        tempEntity.newPolicyEvaluation(hrc.getId(), "build", "scan-build-2",
            false, false, new Date(1_700_000_000_001L), ScanTriggerType.HOSTED_REPOSITORY_SCANNING),
        policy);

    OwnerComponent releaseOc = new OwnerComponent(hrc.getId(), "release", new Date(), "hashms", null,
        MatchState.EXACT.getId(), "SONATYPE", false, List.of());
    ownerComponentDAO.insert(releaseOc);
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      hostedRepositoryComponentDAO.updateOwnerComponentId(tx, hrc.getId(), releaseOc.getId());
      tx.commit();
    }
    tempEntity.newPolicyEvaluation(hrc.getId(), "release", "scan-release-1",
        false, false, new Date(1_800_000_000_000L), ScanTriggerType.HOSTED_REPOSITORY_SCANNING);

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/" + hrc.getId())
        .get()
        .getBodyText();

    assertThat(response).contains("\"stageTypeId\":\"release\"");
    assertThat(response).contains("\"scanId\":\"scan-release-1\"");
    assertThat(response).contains("\"violationCount\":0");
    assertThat(response).contains("\"componentCount\":1");
  }

  private PolicyEvaluation seedOwnerComponentAndEvaluation(HostedRepositoryComponent hrc, String hash) {
    OwnerComponent oc = new OwnerComponent(hrc.getId(), "build", new Date(), hash, null,
        MatchState.UNKNOWN.getId(), "SONATYPE", false, List.of());
    ownerComponentDAO.insert(oc);
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      hostedRepositoryComponentDAO.updateOwnerComponentId(tx, hrc.getId(), oc.getId());
      tx.commit();
    }
    return tempEntity.newPolicyEvaluation(hrc.getId(), "build", tempEntity.uuid(),
        false, false, new Date(), ScanTriggerType.HOSTED_REPOSITORY_SCANNING);
  }

}
