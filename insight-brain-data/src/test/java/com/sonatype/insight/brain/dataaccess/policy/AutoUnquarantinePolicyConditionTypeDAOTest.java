/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.AutoUnquarantinePolicyConditionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AutoUnquarantinePolicyConditionTypeDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testInsert_validConditionType() {
    // SETUP
    final String id = new IntegrityRatingConditionType().getId();
    final AutoUnquarantinePolicyConditionType entity = new AutoUnquarantinePolicyConditionType(id);
    AutoUnquarantinePolicyConditionTypeDAO dao = new AutoUnquarantinePolicyConditionTypeDAO();

    // EXECUTE
    dao.insert(entity);

    // VERIFY
    assertThat(dao.getById(id).getId()).isEqualTo(id);
  }

  @Test
  public void testInsert_invalidConditionType() {
    // SETUP
    final AutoUnquarantinePolicyConditionType entity = new AutoUnquarantinePolicyConditionType("invalidId");
    AutoUnquarantinePolicyConditionTypeDAO dao = new AutoUnquarantinePolicyConditionTypeDAO();

    // EXECUTE & Verify
    assertThatThrownBy(() -> dao.insert(entity)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid condition type id: 'invalidId'");
  }

  @Test
  public void testInsert_alreadyExist() {
    // SETUP
    tempEntity.newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);
    final String id = new IntegrityRatingConditionType().getId();
    final AutoUnquarantinePolicyConditionType entity = new AutoUnquarantinePolicyConditionType(id);
    AutoUnquarantinePolicyConditionTypeDAO dao = new AutoUnquarantinePolicyConditionTypeDAO();

    // EXECUTE & Verify
    assertThatThrownBy(() -> dao.insert(entity)).isInstanceOf(BadRequestException.class)
        .hasMessage("The condition type already exists: IntegrityRating");
  }

  @Test
  public void testInsert_notSupportedConditionType() {
    // SETUP
    final AutoUnquarantinePolicyConditionType entity =
        new AutoUnquarantinePolicyConditionType(AgeInDaysConditionType.ID);
    AutoUnquarantinePolicyConditionTypeDAO dao = new AutoUnquarantinePolicyConditionTypeDAO();

    // EXECUTE & Verify
    assertThatThrownBy(() -> dao.insert(entity)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Condition type with id 'AgeInDays' does not support auto release from quarantine.");
  }

  @Test
  public void testGetAll() {
    // SETUP
    tempEntity.newAutoUnquarantinePolicyConditionType(IntegrityRatingConditionType.ID);
    tempEntity.newAutoUnquarantinePolicyConditionType(LicenseConditionType.ID);

    AutoUnquarantinePolicyConditionTypeDAO dao = new AutoUnquarantinePolicyConditionTypeDAO();

    // EXECUTE
    final List<AutoUnquarantinePolicyConditionType> entities = dao.getAll();

    // VERIFY
    assertThat(entities.size()).isEqualTo(2);
    assertThat(entities).extracting(AutoUnquarantinePolicyConditionType::getId)
        .contains(IntegrityRatingConditionType.ID);
    assertThat(entities).extracting(AutoUnquarantinePolicyConditionType::getId).contains(LicenseConditionType.ID);
  }

  @Test
  public void testGetAll_noRecords() {
    // SETUP
    AutoUnquarantinePolicyConditionTypeDAO dao = new AutoUnquarantinePolicyConditionTypeDAO();

    // EXECUTE
    final List<AutoUnquarantinePolicyConditionType> entities = dao.getAll();

    // VERIFY
    assertThat(entities.size()).isZero();
  }
}
