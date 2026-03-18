/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AutoPolicyWaiverDAOTest
    extends AbstractDbDAOTest
{
  private AutoPolicyWaiverDAO dao;

  private AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createAutoPolicyWaiverDAO();
    autoPolicyWaiverExclusionDAO = daoFactory.createAutoPolicyWaiverExclusionDAO();
  }

  @Test
  public void testCRUD() {
    AutoPolicyWaiver autoPolicyWaiverInstance = new AutoPolicyWaiver(
        "fake",
        7,
        true,
        true,
        "creatorId",
        "creatorName",
        new Date());

    // Create
    dao.insert(autoPolicyWaiverInstance);

    // Read
    AutoPolicyWaiver autoPolicyWaiver = dao.getById(autoPolicyWaiverInstance.getId());
    assertThat(autoPolicyWaiver.getId()).isEqualTo(autoPolicyWaiverInstance.getId());
    assertThat(autoPolicyWaiver.getOwnerId()).isEqualTo(autoPolicyWaiverInstance.getOwnerId());
    assertThat(autoPolicyWaiver.getThreatLevel()).isEqualTo(autoPolicyWaiverInstance.getThreatLevel());
    assertThat(autoPolicyWaiver.hasReachability()).isTrue();
    assertThat(autoPolicyWaiver.hasPathForward()).isTrue();
    assertThat(autoPolicyWaiver.getCreatorId()).isEqualTo(autoPolicyWaiverInstance.getCreatorId());
    assertThat(autoPolicyWaiver.getCreatorName()).isEqualTo(autoPolicyWaiverInstance.getCreatorName());
    assertThat(autoPolicyWaiver.getCreateTime()).isEqualTo(autoPolicyWaiverInstance.getCreateTime());

    // Update
    autoPolicyWaiver = dao.getById(autoPolicyWaiver.getId());
    autoPolicyWaiver.setThreatLevel(6);
    dao.update(autoPolicyWaiver);
    autoPolicyWaiver = dao.getById(autoPolicyWaiver.getId());
    assertThat(autoPolicyWaiver.getThreatLevel()).isEqualTo(6);

    // Delete
    dao.delete(autoPolicyWaiver);
    autoPolicyWaiver = dao.getById(autoPolicyWaiver.getId());
    assertThat(autoPolicyWaiver).isNull();
  }

  @Test
  public void testGetByIdAndOwnerIdNotNull_null() {
    assertThatThrownBy(() -> dao.getByIdAndOwnerIdNotNull("fakeId", "fakeOwnerId")).isInstanceOf(
        NotFoundException.class)
        .hasMessage("Cannot find a waiver with ID fakeId for owner fakeOwnerId.");
  }

  @Test
  public void testGetByIdAndOwnerIdNotNull() {
    AutoPolicyWaiver autoPolicyWaiverInstance = new AutoPolicyWaiver(
        "fake",
        7,
        true,
        true,
        "creatorId",
        "creatorName",
        new Date());
    dao.insert(autoPolicyWaiverInstance);

    AutoPolicyWaiver queryResult =
        dao.getByIdAndOwnerIdNotNull(autoPolicyWaiverInstance.getId(), autoPolicyWaiverInstance.getOwnerId());
    assertThat(queryResult).isNotNull();
    assertThat(queryResult.getId()).isEqualTo(autoPolicyWaiverInstance.getId());
    assertThat(queryResult.getOwnerId()).isEqualTo(autoPolicyWaiverInstance.getOwnerId());
    assertThat(queryResult.getThreatLevel()).isEqualTo(autoPolicyWaiverInstance.getThreatLevel());
    assertThat(queryResult.hasReachability()).isEqualTo(autoPolicyWaiverInstance.hasReachability());
    assertThat(queryResult.hasPathForward()).isEqualTo(autoPolicyWaiverInstance.hasPathForward());
    assertThat(queryResult.getCreatorId()).isEqualTo(autoPolicyWaiverInstance.getCreatorId());
    assertThat(queryResult.getCreatorName()).isEqualTo(autoPolicyWaiverInstance.getCreatorName());
    assertThat(queryResult.getCreateTime()).isEqualTo(autoPolicyWaiverInstance.getCreateTime());
  }

  @Test
  public void testDelete_CascadesToAutoPolicyWaiverExclusions() {
    AutoPolicyWaiver autoPolicyWaiverInstanceOne = new AutoPolicyWaiver(
        "fake",
        7,
        true,
        true,
        "creatorId",
        "creatorName",
        new Date());
    dao.insert(autoPolicyWaiverInstanceOne);

    AutoPolicyWaiver autoPolicyWaiverInstanceTwo = new AutoPolicyWaiver(
        "other",
        3,
        false,
        false,
        "creator",
        "creator",
        new Date());
    dao.insert(autoPolicyWaiverInstanceTwo);

    AutoPolicyWaiverExclusion exclusionOne = new AutoPolicyWaiverExclusion(
        "fake",
        "creatorId",
        "creatorName",
        new Date(),
        autoPolicyWaiverInstanceOne.getId(),
        "fakeScanId",
        "fakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    autoPolicyWaiverExclusionDAO.insert(exclusionOne);

    AutoPolicyWaiverExclusion exclusionTwo = new AutoPolicyWaiverExclusion(
        "other",
        "creator",
        "creator",
        new Date(),
        autoPolicyWaiverInstanceTwo.getId(),
        "otherScanId",
        "otherHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    autoPolicyWaiverExclusionDAO.insert(exclusionTwo);

    dao.delete(autoPolicyWaiverInstanceOne);

    List<AutoPolicyWaiver> autoPolicyWaivers = dao.getAll();
    assertThat(autoPolicyWaivers).hasSize(1);

    List<AutoPolicyWaiverExclusion> autoPolicyWaiverExclusions =
        autoPolicyWaiverExclusionDAO.getAll();
    assertThat(autoPolicyWaiverExclusions).hasSize(1).allSatisfy(exclusionInstance -> {
      assertThat(exclusionInstance.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverInstanceTwo.getId());
    });
  }
}
