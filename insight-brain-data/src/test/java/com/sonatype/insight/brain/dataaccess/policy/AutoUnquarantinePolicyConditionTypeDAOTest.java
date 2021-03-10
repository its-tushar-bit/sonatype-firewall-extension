/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.AutoUnquarantinePolicyConditionType;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
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
    tempEntity.newAutoUnquarantinePolicyConditionType(new IntegrityRatingConditionType());
    final String id = new IntegrityRatingConditionType().getId();
    final AutoUnquarantinePolicyConditionType entity = new AutoUnquarantinePolicyConditionType(id);
    AutoUnquarantinePolicyConditionTypeDAO dao = new AutoUnquarantinePolicyConditionTypeDAO();

    // EXECUTE & Verify
    assertThatThrownBy(() -> dao.insert(entity)).isInstanceOf(BadRequestException.class)
        .hasMessage("The condition type already exists: IntegrityRating");
  }

  @Test
  public void testGetAll() {
    // SETUP
    tempEntity.newAutoUnquarantinePolicyConditionType(new IntegrityRatingConditionType());
    tempEntity.newAutoUnquarantinePolicyConditionType(new HygieneRatingConditionType());

    AutoUnquarantinePolicyConditionTypeDAO dao = new AutoUnquarantinePolicyConditionTypeDAO();

    // EXECUTE
    final List<AutoUnquarantinePolicyConditionType> entities = dao.getAll();

    // VERIFY
    assertThat(entities.size()).isEqualTo(2);
    assertThat(entities.get(0).getId()).isEqualTo(HygieneRatingConditionType.ID);
    assertThat(entities.get(1).getId()).isEqualTo(IntegrityRatingConditionType.ID);
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
