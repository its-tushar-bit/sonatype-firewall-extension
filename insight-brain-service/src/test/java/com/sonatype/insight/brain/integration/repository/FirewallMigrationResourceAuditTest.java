/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class FirewallMigrationResourceAuditTest
    extends AbstractAuditTest
{
  private RepositoryDAO repositoryDAO;

  private RepositoryManager targetRepositoryManager;

  private Repository targetRepository;

  private RepositoryManager sourceRepositoryManager;

  private Repository sourceRepository;

  @Before
  public void before() {
    repositoryDAO = lookup(RepositoryDAO.class);
    targetRepositoryManager = tempEntity.newRepositoryManager();
    targetRepository = tempEntity.newRepository(targetRepositoryManager, "targetRepositoryPublicId");
    sourceRepositoryManager = tempEntity.newRepositoryManager();
    sourceRepository = tempEntity.newRepository(sourceRepositoryManager, "sourceRepositoryPublicId");
  }

  @Test
  public void testMigrateRepositoryHistory_QuarantineDisabled() throws Exception {
    migrateRepositoryHistoryRequest().post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MIGRATE_REPOSITORY, null);
    assertRepositoryData(auditDTO, targetRepository);
    assertRepositoryMigrateData(auditDTO);
  }

  @Test
  public void testMigrateRepositoryHistory_QuarantineEnabled() throws Exception {
    sourceRepository.setQuarantineEnabled(true);
    repositoryDAO.update(sourceRepository);
    migrateRepositoryHistoryRequest().post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MIGRATE_REPOSITORY, null);
    assertRepositoryMigrateData(auditDTO);
  }

  @Test
  public void testMigrateRepositoryHistory_Unauthorized() throws Exception {
    migrateRepositoryHistoryRequest().with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.MIGRATE_REPOSITORY, "unauthorized");
    assertRepositoryMigrateBasicData(auditDTO);
  }

  private HttpRequest migrateRepositoryHistoryRequest() {
    return restRequest().path(FirewallMigrationResource.RESOURCE_PATH, FirewallMigrationResource.HISTORY_PATH)
        .parameter(targetRepositoryManager.getInstanceId(), targetRepository.getPublicId())
        .query("sourceRepositoryManagerInstanceId", sourceRepositoryManager.getInstanceId())
        .query("sourceRepositoryPublicId", sourceRepository.getPublicId());
  }

  private void assertRepositoryMigrateData(AuditDTO auditDTO) {
    assertRepositoryMigrateBasicData(auditDTO);
    assertCustomData(auditDTO, "repositoryId", targetRepository.getId());
    assertCustomData(auditDTO, "sourceRepositoryId", sourceRepository.getId());
    assertCustomData(auditDTO, "quarantine", sourceRepository.isQuarantineEnabled() ? "enabled" : "disabled");
  }

  private void assertRepositoryMigrateBasicData(AuditDTO auditDTO) {
    assertCustomData(auditDTO, "repositoryManagerInstanceId", targetRepositoryManager.getInstanceId());
    assertCustomData(auditDTO, "repositoryPublicId", targetRepository.getPublicId());
    assertCustomData(auditDTO, "sourceRepositoryManagerInstanceId", sourceRepositoryManager.getInstanceId());
    assertCustomData(auditDTO, "sourceRepositoryPublicId", sourceRepository.getPublicId());
  }
}
