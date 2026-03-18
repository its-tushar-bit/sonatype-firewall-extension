/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.Comparator;
import java.util.Map;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DataRetentionPolicyDAOTest
    extends AbstractDbDAOTest
{
  private DataRetentionPolicyDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createDataRetentionPolicyDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    DataRetentionPolicy policy = new DataRetentionPolicy(organization.getId(), "contextId", true, 12, 7);
    dao.insert(policy);
    assertThat(policy.getId()).isNotNull();

    // Read
    policy = dao.getById(policy.getId());
    assertThat(policy.getOwnerId()).isEqualTo(organization.getId());
    assertThat(policy.getContextId()).isEqualTo("contextId");
    assertThat(policy.isPurgingEnabled()).isTrue();
    assertThat(policy.getMaxCount()).isEqualTo(12);
    assertThat(policy.getMaxAgeInDays()).isEqualTo(7);

    // Update
    policy.setPurgingEnabled(false);
    policy.setMaxCount(null);
    policy.setMaxAgeInDays(null);
    dao.update(policy);

    // Read
    policy = dao.getById(policy.getId());
    assertThat(policy.getOwnerId()).isEqualTo(organization.getId());
    assertThat(policy.getContextId()).isEqualTo("contextId");
    assertThat(policy.isPurgingEnabled()).isFalse();
    assertThat(policy.getMaxCount()).isNull();
    assertThat(policy.getMaxAgeInDays()).isNull();

    // Delete
    dao.delete(policy);
    assertThat(dao.getById(policy.getId())).isNull();
  }

  @Test
  public void testUniquenessConstraint() {
    dao.insert(new DataRetentionPolicy(organization.getId(), "contextId", true, 1, null));
    assertThatExceptionOfType(PersistenceException.class)
        .isThrownBy(() -> dao.insert(new DataRetentionPolicy(organization.getId(), "contextId")))
        .withCauseInstanceOf(EntityExistsException.class);
  }

  @Test
  public void testDefaultPoliciesForRootOrganization() {
    assertThat(dao.getByOwnerId(Organization.ROOT_ORGANIZATION_ID).values())
        .extracting(DataRetentionPolicy::getContextId)
        .containsExactlyInAnyOrder( //
            Stage.ID_DEVELOP, //
            Stage.ID_SOURCE, //
            Stage.ID_BUILD, //
            Stage.ID_STAGE_RELEASE, //
            Stage.ID_RELEASE, //
            Stage.ID_OPERATE, //
            DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING, //
            DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);
  }

  @Test
  public void testGetAll() {
    assertThat(dao.getAll()).hasSize(8);
  }

  @Test
  public void testInsert_ValidateMaxCount_LowerBound() {
    DataRetentionPolicy policy = new DataRetentionPolicy(organization.getId(), "contextId", true, 0, null);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(policy))
        .withMessageContaining("count must be positive");

    policy.setMaxCount(policy.getMaxCount() + 1);
    dao.insert(policy);
  }

  @Test
  public void testInsert_ValidateMaxCount_UpperBound() {
    DataRetentionPolicy policy = new DataRetentionPolicy(organization.getId(), "contextId", true, 10000, null);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(policy))
        .withMessageContaining("count must be less than 10000");

    policy.setMaxCount(policy.getMaxCount() - 1);
    dao.insert(policy);
  }

  @Test
  public void testUpdate_ValidateMaxCount_LowerBound() {
    DataRetentionPolicy policy = new DataRetentionPolicy(organization.getId(), "contextId", true, 1, null);
    dao.insert(policy);
    policy.setMaxCount(0);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(policy))
        .withMessageContaining("count must be positive");

    policy.setMaxCount(policy.getMaxCount() + 1);
    dao.update(policy);
  }

  @Test
  public void testUpdate_ValidateMaxCount_UpperBound() {
    DataRetentionPolicy policy = new DataRetentionPolicy(organization.getId(), "contextId", true, 1, null);
    dao.insert(policy);
    policy.setMaxCount(10000);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(policy))
        .withMessageContaining("count must be less than 10000");

    policy.setMaxCount(policy.getMaxCount() - 1);
    dao.update(policy);
  }

  @Test
  public void testInsert_ValidateMaxAgeInDays_LowerBound() {
    DataRetentionPolicy policy = new DataRetentionPolicy(organization.getId(), "contextId", true, null, 0);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(policy))
        .withMessageContaining("age must be positive");

    policy.setMaxAgeInDays(policy.getMaxAgeInDays() + 1);
    dao.insert(policy);
  }

  @Test
  public void testInsert_ValidateMaxAgeInDays_UpperBound() {
    DataRetentionPolicy policy = new DataRetentionPolicy(organization.getId(), "contextId", true, null, 50 * 365);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(policy))
        .withMessageContaining("age must be less than 50 years");

    policy.setMaxAgeInDays(policy.getMaxAgeInDays() - 1);
    dao.insert(policy);
  }

  @Test
  public void testUpdate_ValidateMaxAgeInDays_LowerBound() {
    DataRetentionPolicy policy = new DataRetentionPolicy(organization.getId(), "contextId", true, null, 1);
    dao.insert(policy);
    policy.setMaxAgeInDays(0);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(policy))
        .withMessageContaining("age must be positive");

    policy.setMaxAgeInDays(policy.getMaxAgeInDays() + 1);
    dao.update(policy);
  }

  @Test
  public void testUpdate_ValidateMaxAgeInDays_UpperBound() {
    DataRetentionPolicy policy = new DataRetentionPolicy(organization.getId(), "contextId", true, null, 1);
    dao.insert(policy);
    policy.setMaxAgeInDays(50 * 365);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(policy))
        .withMessageContaining("age must be less than 50 years");

    policy.setMaxAgeInDays(policy.getMaxAgeInDays() - 1);
    dao.update(policy);
  }

  @Test
  public void testInsert_ValidateAnyCriteria() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.insert(new DataRetentionPolicy(organization.getId(), "contextId", true, null, null)))
        .withMessageContaining("without criteria for what to purge");
  }

  @Test
  public void testUpdate_ValidateAnyCriteria() {
    DataRetentionPolicy policy = new DataRetentionPolicy(organization.getId(), "contextId");
    dao.insert(policy);
    policy.setPurgingEnabled(true);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(policy))
        .withMessageContaining("without criteria for what to purge");
  }

  @Test
  public void testGetByOwnerId() {
    DataRetentionPolicy policy1 = new DataRetentionPolicy(application.getId(), "contextId");
    dao.insert(policy1);
    DataRetentionPolicy policy2 = new DataRetentionPolicy(organization.getId(), "contextId");
    dao.insert(policy2);
    Map<String, DataRetentionPolicy> policiesByContext = dao.getByOwnerId(organization.getId());
    assertThat(policiesByContext).containsOnlyKeys("contextId");
    assertThat(policiesByContext.values()).extracting(DataRetentionPolicy::getId).containsExactly(policy2.getId());
  }

  @Test
  public void testGetByOwnerIdAndContextId() {
    DataRetentionPolicy policy1 = new DataRetentionPolicy(application.getId(), "contextId");
    dao.insert(policy1);
    DataRetentionPolicy policy2 = new DataRetentionPolicy(organization.getId(), "contextId");
    dao.insert(policy2);
    DataRetentionPolicy policy3 = new DataRetentionPolicy(organization.getId(), "anotherContextId");
    dao.insert(policy3);
    assertThat(dao.getByOwnerIdAndContextId(organization.getId(), "contextId"))
        .usingComparator(Comparator.comparing(DataRetentionPolicy::getId))
        .isEqualTo(policy2);
  }
}
