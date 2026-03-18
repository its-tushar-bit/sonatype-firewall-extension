/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.ide;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserIdePolicyEvaluationDAOTest
    extends AbstractDbDAOTest
{
  private UserIdePolicyEvaluationDAO userIdePolicyEvaluationDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    userIdePolicyEvaluationDAO = daoFactory.createUserIdePolicyEvaluationDAO();
  }

  @Test
  public void testGetCount_oneUser() {
    assertThat(userIdePolicyEvaluationDAO.getCount()).isEqualTo(0);

    userIdePolicyEvaluationDAO.upsert("Jan");

    assertThat(userIdePolicyEvaluationDAO.getByUsername("Jan")).isNotNull();
    assertThat(userIdePolicyEvaluationDAO.getCount()).isEqualTo(1);
  }

  @Test
  public void testGetCount_oneUserWithUpdate() {
    userIdePolicyEvaluationDAO.upsert("Jan");
    userIdePolicyEvaluationDAO.upsert("Jan");

    assertThat(userIdePolicyEvaluationDAO.getByUsername("Jan")).isNotNull();
    assertThat(userIdePolicyEvaluationDAO.getCount()).isEqualTo(1);
  }

  @Test
  public void testGetCount_multipleUserWithUpdate() {
    userIdePolicyEvaluationDAO.upsert("Jan");
    userIdePolicyEvaluationDAO.upsert("Feb");
    userIdePolicyEvaluationDAO.upsert("Jan");
    userIdePolicyEvaluationDAO.upsert("Feb");

    assertThat(userIdePolicyEvaluationDAO.getByUsername("Jan")).isNotNull();
    assertThat(userIdePolicyEvaluationDAO.getByUsername("Feb")).isNotNull();
    assertThat(userIdePolicyEvaluationDAO.getCount()).isEqualTo(2);
  }

  @Test
  public void testGetCountSince_onlyCountUsersWithinRange() throws InterruptedException {
    final Date BEFORE_USERS = new Date();
    upsertUserIdePolicyEvaluationWithASmallDelay("Jan");
    final Date BETWEEN_USERS = new Date();
    upsertUserIdePolicyEvaluationWithASmallDelay("Feb");
    final Date AFTER_USERS = new Date();

    assertThat(userIdePolicyEvaluationDAO.getByUsername("Jan")).isNotNull();
    assertThat(userIdePolicyEvaluationDAO.getByUsername("Feb")).isNotNull();

    assertThat(userIdePolicyEvaluationDAO.getCountSince(BEFORE_USERS)).isEqualTo(2);
    assertThat(userIdePolicyEvaluationDAO.getCountSince(BETWEEN_USERS)).isEqualTo(1);
    assertThat(userIdePolicyEvaluationDAO.getCountSince(AFTER_USERS)).isEqualTo(0);
  }

  private void upsertUserIdePolicyEvaluationWithASmallDelay(String username) throws InterruptedException {
    // This is to prevent test flakiness in the case that the timestamps being the same
    TimeUnit.MILLISECONDS.sleep(5L);
    userIdePolicyEvaluationDAO.upsert(username);
    TimeUnit.MILLISECONDS.sleep(5L);
  }
}
