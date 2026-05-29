/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.UUID;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ArtifactoryRepositoryServiceWrapperTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private ArtifactoryRepositoryServiceWrapper wrapper;

  @Test
  public void testAllDelegatedServiceMethodsInvoked() {
    // need a mock service for this test, all we want to verify is that the delegate is invoked
    ArtifactoryRepositoryService artifactoryRepositoryService = mock(ArtifactoryRepositoryService.class);
    wrapper =
        new ArtifactoryRepositoryServiceWrapper(artifactoryRepositoryService, repositoryDAO, repositoryManagerDAO);

    String repositoryManagerInstanceId = "foo";
    String repositoryPublicId = "bar";

    wrapper.setAuditEnabled(repositoryManagerInstanceId, repositoryPublicId, true, null);
    verify(artifactoryRepositoryService).setAuditEnabled(repositoryManagerInstanceId, repositoryPublicId, true, null);

    wrapper.getPolicyEvaluationSummary(repositoryManagerInstanceId, repositoryPublicId, null);
    verify(artifactoryRepositoryService).getPolicyEvaluationSummary(repositoryManagerInstanceId, repositoryPublicId,
        null);

    wrapper.getRepositoryResultsUrl(repositoryManagerInstanceId, repositoryPublicId, null);
    verify(artifactoryRepositoryService).getRepositoryResultsUrl(repositoryManagerInstanceId, repositoryPublicId,
        null);

    RepositoryComponentEvaluationDataRequestList list = new RepositoryComponentEvaluationDataRequestList();
    wrapper.evaluateComponents(repositoryManagerInstanceId, repositoryPublicId, list, true, "agent");
    verify(artifactoryRepositoryService).evaluateComponents(repositoryManagerInstanceId, repositoryPublicId, list, true,
        "agent");

    wrapper.setQuarantine(repositoryManagerInstanceId, repositoryPublicId, true, null);
    verify(artifactoryRepositoryService).setQuarantine(repositoryManagerInstanceId, repositoryPublicId, true, null);

    wrapper.removeComponent(repositoryManagerInstanceId, repositoryPublicId, "pathname", null);
    verify(artifactoryRepositoryService).removeComponent(repositoryManagerInstanceId, repositoryPublicId, "pathname",
        null);

    wrapper.getUnquarantinedComponents(repositoryManagerInstanceId, repositoryPublicId, 0L, null);
    verify(artifactoryRepositoryService).getUnquarantinedComponents(repositoryManagerInstanceId, repositoryPublicId, 0L,
        null);

    wrapper.getQuarantinedComponentReportUrl(repositoryManagerInstanceId, repositoryPublicId, "testPathname", null);
    verify(artifactoryRepositoryService).getQuarantinedComponentReportUrl(repositoryManagerInstanceId,
        repositoryPublicId, "testPathname", null);
  }

  @Test
  public void testLegacyValueSent() {
    String repositoryId = randomRepositoryId();
    String legacyRepositoryManagerId = ArtifactoryRepositoryServiceWrapper.getLegacyRepositoryInstanceId(repositoryId);

    // insert a record with a legacy repository manager id
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(legacyRepositoryManagerId);
    tempEntity.newRepository(repositoryManager, repositoryId);

    assertMigration(legacyRepositoryManagerId, legacyRepositoryManagerId, repositoryId, 1, 1, 1);
  }

  @Test
  public void testMigration() {
    // GIVEN: we have four legacy records in the database. Records 1 & 2 are from one Artifactory instance,
    // 3 is from a second instance, 4 is from the second as well (and will not yet get migrated)
    String repositoryId1 = randomRepositoryId();
    String repositoryId2 = randomRepositoryId();
    String repositoryId3 = randomRepositoryId();
    String repositoryId4 = randomRepositoryId();
    String legacyRMInstanceId1 = ArtifactoryRepositoryServiceWrapper.getLegacyRepositoryInstanceId(repositoryId1);
    String legacyRMInstanceId2 = ArtifactoryRepositoryServiceWrapper.getLegacyRepositoryInstanceId(repositoryId2);
    String legacyRMInstanceId3 = ArtifactoryRepositoryServiceWrapper.getLegacyRepositoryInstanceId(repositoryId3);
    String legacyRMInstanceId4 = ArtifactoryRepositoryServiceWrapper.getLegacyRepositoryInstanceId(repositoryId4);

    // insert legacy records
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager(legacyRMInstanceId1);
    tempEntity.newRepository(repositoryManager1, repositoryId1);
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager(legacyRMInstanceId2);
    tempEntity.newRepository(repositoryManager2, repositoryId2);
    RepositoryManager repositoryManager3 = tempEntity.newRepositoryManager(legacyRMInstanceId3);
    tempEntity.newRepository(repositoryManager3, repositoryId3);
    RepositoryManager repositoryManager4 = tempEntity.newRepositoryManager(legacyRMInstanceId4);
    tempEntity.newRepository(repositoryManager4, repositoryId4);

    String newRMInstanceId1 = "artifactory-one";
    String newRMInstanceId2 = "artifactory-two";

    // 4 total repository managers at this point (1 added and 1 removed)
    assertMigration(newRMInstanceId1, legacyRMInstanceId1, repositoryId1, 1, 0, 4);
    // 3 total repository managers (1 added and 2 removed)
    assertMigration(newRMInstanceId1, legacyRMInstanceId2, repositoryId2, 2, 0, 3);
    // Still 3 total repository managers (2 added and 3 removed)
    assertMigration(newRMInstanceId2, legacyRMInstanceId3, repositoryId3, 1, 0, 3);
    // 2 total repository managers (2 added and 4 removed)
    assertMigration(newRMInstanceId2, legacyRMInstanceId4, repositoryId4, 2, 0, 2);

    // now test that it is idempotent by re-running the first record (no more legacy ID)
    assertMigration(newRMInstanceId1, newRMInstanceId1, repositoryId1, 2, 2, 2);
  }

  @Test
  public void testMigration_configuredIsTrueAfterMigration() {
    String repositoryId = randomRepositoryId();
    String legacyRMInstanceId = ArtifactoryRepositoryServiceWrapper.getLegacyRepositoryInstanceId(repositoryId);
    String newRMInstanceId = "artifactory-configured-check";

    RepositoryManager legacyRepositoryManager = tempEntity.newRepositoryManager(legacyRMInstanceId);
    tempEntity.newRepository(legacyRepositoryManager, repositoryId);

    String resultInstanceId = wrapper.getRepositoryManagerInstanceId(newRMInstanceId, repositoryId);

    RepositoryManager migratedRepositoryManager = repositoryManagerDAO.getByInstanceId(resultInstanceId);
    assertThat(migratedRepositoryManager).isNotNull();
    assertThat(migratedRepositoryManager.isConfigured()).isTrue();
  }

  private void assertMigration(
      final String repositoryManagerInstanceId,
      final String legacyRepositoryManagerInstanceId, // note: this may be the same as the first parameter
      final String repositoryId,
      final int size,
      final int legacySize,
      final int totalSize)
  {
    // GIVEN: we get a hold of the original repository manager (for later assertions)
    RepositoryManager legacyRepositoryManager = repositoryManagerDAO.getByInstanceId(legacyRepositoryManagerInstanceId);

    // WHEN: we get the RM instance ID via the *NEW* RM instance ID
    String resultInstanceId = wrapper.getRepositoryManagerInstanceId(repositoryManagerInstanceId, repositoryId);

    // THEN: we should be able to load the value by its instance ID and verify it
    RepositoryManager resultRepositoryManager = repositoryManagerDAO.getByInstanceId(resultInstanceId);
    assertThat(resultRepositoryManager.getInstanceId()).isEqualTo(repositoryManagerInstanceId);
    if (repositoryManagerInstanceId.equals(legacyRepositoryManagerInstanceId)) {
      assertThat(resultRepositoryManager.getId()).isEqualTo(legacyRepositoryManager.getId());
    }
    else {
      assertThat(resultRepositoryManager.getId()).isNotEqualTo(legacyRepositoryManager.getId());
    }

    // AND: There should be the correct amount of repositories attached to that repository manager ID
    assertThat(repositoryDAO.getByRepositoryManagerId(resultRepositoryManager.getId())).hasSize(size);

    // AND: There should be the correct amount of repositories attached by the legacy repository manager instance ID
    assertThat(repositoryDAO.getByRepositoryManagerId(legacyRepositoryManager.getId())).hasSize(legacySize);

    // AND: There should still be the correct total repository manager records
    assertThat(repositoryManagerDAO.getAll()).hasSize(totalSize);
  }

  @Test
  public void testAlreadyMigrated() {
    String newRepositoryManagerId = "i-am-artifactory";
    String repositoryId = randomRepositoryId();

    // insert a record with a new and proper repository manager id
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(newRepositoryManagerId);

    // load it out and it should still be the new value
    String result = wrapper.getRepositoryManagerInstanceId(repositoryManager.getInstanceId(), repositoryId);
    assertThat(result).isEqualTo(newRepositoryManagerId);
  }

  @Test
  public void testNewRepository() {
    String newRepositoryManagerId = "i-am-artifactory";
    String repositoryId = randomRepositoryId();

    String result = wrapper.getRepositoryManagerInstanceId(newRepositoryManagerId, repositoryId);
    assertThat(result).isEqualTo(newRepositoryManagerId);
  }

  private String randomRepositoryId() {
    return UUID.randomUUID().toString();
  }
}
