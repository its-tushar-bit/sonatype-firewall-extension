/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tenancy;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DeletedTenantDAOTest
    extends AbstractDbDAOTest
{
  @Rule
  public TestName name = new TestName();

  private DeletedTenantDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createDeletedTenantDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    DeletedTenant deletedTenant = tempEntity.newDeletedTenant("t_" + name.getMethodName());

    // Update - test lastUpdated since created is immutable after insert
    deletedTenant = dao.getById(deletedTenant.getId());
    Date originalCreated = deletedTenant.getCreated();

    Date newTime = new Date();
    deletedTenant.setLastUpdated(newTime);
    dao.update(deletedTenant);
    deletedTenant = dao.getById(deletedTenant.getId());
    assertThat(deletedTenant.getLastUpdated()).isEqualTo(newTime);
    // Verify created was not changed
    assertThat(deletedTenant.getCreated()).isEqualTo(originalCreated);

    // Delete
    dao.delete(deletedTenant);
    deletedTenant = dao.getById(deletedTenant.getId());
    assertThat(deletedTenant).isNull();
  }

  @Test
  public void testGetTenantByTenantSlug() {
    String tenantSlug = "t_" + name.getMethodName();
    Date createdDate = new Date();

    DeletedTenant deletedTenant = tempEntity.newDeletedTenant(tenantSlug, createdDate);

    deletedTenant = dao.getById(deletedTenant.getId());

    assertThat(deletedTenant.getId()).isEqualTo(tenantSlug);
    assertThat(deletedTenant.getCreated()).isEqualTo(createdDate);
  }

  @Test
  public void testGetAllTenantDeletions() {
    List<String> tenantIds = Arrays.asList(
        "t_1_" + name.getMethodName(),
        "t_2_" + name.getMethodName(),
        "t_3_" + name.getMethodName());
    tenantIds.forEach(tempEntity::newDeletedTenant);

    List<DeletedTenant> tenants = dao.getAllTenantDeletions();

    assertThat(tenants).hasSize(3);
    assertThat(tenants).extracting(DeletedTenant::getId).containsExactlyInAnyOrder(tenantIds.toArray(new String[0]));
  }

  @Test
  public void testGetAllTenantDeletions_filterDeletedTenants() {
    List<String> tenantIds = Arrays.asList(
        "t_1_" + name.getMethodName(),
        "t_2_" + name.getMethodName(),
        "t_3_" + name.getMethodName());
    tenantIds.forEach(tempEntity::newDeletedTenant);
    tempEntity.newDeletedTenantWithDeleteCompleted("t_4_" + name.getMethodName());

    List<DeletedTenant> tenants = dao.getAllTenantDeletions();

    assertThat(tenants).hasSize(3);
    assertThat(tenants).extracting(DeletedTenant::getId).containsExactlyInAnyOrder(tenantIds.toArray(new String[0]));
  }

  @Test
  public void testGetTenantsOlderThanRetentionTime() {
    tempEntity.newDeletedTenant("t_" + name.getMethodName());
    List<DeletedTenant> tenants = dao.getAllTenantDeletionsOlderThanRetentionPeriod(1L);
    assertThat(tenants).isEmpty();

    String olderTenantName = "t_" + name.getMethodName() + "_older";
    Date fiveHoursAgo = new Date(System.currentTimeMillis() - (5 * 60 * 60 * 1000));
    tempEntity.newDeletedTenant(olderTenantName, fiveHoursAgo);
    tenants = dao.getAllTenantDeletionsOlderThanRetentionPeriod(1L);

    assertThat(tenants).hasSize(1);
    assertThat(tenants.get(0).getId()).isEqualTo(olderTenantName);
  }

  @Test
  public void testGetTenantsOlderThanRetentionTime_filterDeletedTenants() {
    tempEntity.newDeletedTenant("t_" + name.getMethodName());
    List<DeletedTenant> tenants = dao.getAllTenantDeletionsOlderThanRetentionPeriod(1L);
    assertThat(tenants).isEmpty();

    String olderTenantName = "t_1_" + name.getMethodName();
    Date fiveHoursAgo = new Date(System.currentTimeMillis() - (5 * 60 * 60 * 1000));
    tempEntity.newDeletedTenant(olderTenantName, fiveHoursAgo);
    tempEntity.newDeletedTenantWithDeleteCompleted("t_2_" + name.getMethodName(), fiveHoursAgo);
    tenants = dao.getAllTenantDeletionsOlderThanRetentionPeriod(1L);

    assertThat(tenants).hasSize(1);
    assertThat(tenants.get(0).getId()).isEqualTo(olderTenantName);
  }

  @Test
  public void testIsScheduledForDeletion() {
    String tenantSlug = "t_" + name.getMethodName();

    tempEntity.newDeletedTenant(tenantSlug);

    assertThat(dao.isScheduledForDeletion(tenantSlug)).isTrue();
    assertThat(dao.isScheduledForDeletion(UUID.randomUUID().toString())).isFalse();
  }

  @Test
  public void testIsScheduledForDeletion_filterDeletedTenants() {
    String tenantSlug = "t_" + name.getMethodName();

    tempEntity.newDeletedTenantWithDeleteCompleted(tenantSlug);

    assertThat(dao.isScheduledForDeletion(tenantSlug)).isFalse();
  }

  @Test
  public void testAttemptingToDeleteGlobalTenantThrowsException() {
    assertThatThrownBy(
        () -> dao.insert(new DeletedTenant(GLOBAL_TENANT.tenantSlug)))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Scheduling the global tenant for deletion is not allowed");
  }
}
