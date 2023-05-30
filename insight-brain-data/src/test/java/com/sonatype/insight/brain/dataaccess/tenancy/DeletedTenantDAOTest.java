/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tenancy;

import java.util.List;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DeletedTenantDAOTest extends AbstractDbDAOTest
{
  @Rule
  public TestName name = new TestName();

  DeletedTenantDAO underTest = new DeletedTenantDAO();

  @Test
  public void testCRUD() {
    // Create
    DeletedTenant deletedTenant = tempEntity.newDeletedTenant("t_" + name.getMethodName());

    // Update
    deletedTenant = underTest.getById(deletedTenant.getId());

    long newTime = System.currentTimeMillis();
    deletedTenant.setDeleteRequestedTimestamp(newTime);
    underTest.update(deletedTenant);
    deletedTenant = underTest.getById(deletedTenant.getId());
    assertThat(deletedTenant.getDeleteRequestedTimestamp()).isEqualTo(newTime);

    // Delete
    underTest.delete(deletedTenant);
    deletedTenant = underTest.getById(deletedTenant.getId());
    assertThat(deletedTenant).isNull();
  }

  @Test
  public void testGetTenantByTenantSlug() {
    String tenantSlug = "t_" + name.getMethodName();
    long createdTimestamp = System.currentTimeMillis();

    DeletedTenant deletedTenant = tempEntity.newDeletedTenant(tenantSlug, createdTimestamp);

    deletedTenant = underTest.getById(deletedTenant.getId());

    assertThat(deletedTenant.getId()).isEqualTo(tenantSlug);
    assertThat(deletedTenant.getDeleteRequestedTimestamp()).isEqualTo(createdTimestamp);
  }

  @Test
  public void testGetTenantsOlderThanRetentionTime() {
    tempEntity.newDeletedTenant("t_" + name.getMethodName());
    List<DeletedTenant> tenants = underTest.getAllTenantDeletionsOlderThanRetentionPeriod(1L);
    assertThat(tenants).isEmpty();

    String olderTenantName = "t_" + name.getMethodName() + "_older";
    Long fiveHoursAgo = System.currentTimeMillis() - (5 * 60 * 1000);
    tempEntity.newDeletedTenant(olderTenantName, fiveHoursAgo);
    tenants = underTest.getAllTenantDeletionsOlderThanRetentionPeriod(1L);

    assertThat(tenants).hasSize(1);
    assertThat(tenants.get(0).getId()).isEqualTo(olderTenantName);
  }

  @Test
  public void testIsScheduledForDeletion() {
    String tenantSlug = "t_" + name.getMethodName();

    tempEntity.newDeletedTenant(tenantSlug);

    assertThat(underTest.isScheduledForDeletion(tenantSlug)).isTrue();
    assertThat(underTest.isScheduledForDeletion(UUID.randomUUID().toString())).isFalse();
  }

  @Test
  public void testAttemptingToDeleteGlobalTenantThrowsException() {
    assertThatThrownBy(
        () -> underTest.insert(new DeletedTenant(GLOBAL_TENANT.tenantSlug)))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage("Scheduling the global tenant for deletion is not allowed");
  }
}
