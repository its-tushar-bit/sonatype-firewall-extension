/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.security.OAuth2User;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test: proves that a mid-stream exception in a
 * {@code fetchStream()} consumer does not leak a connection from the pool.
 *
 * <p>
 * Uses {@link OAuth2UserDAO#withAllUsersWithGroups(java.util.function.Consumer)}
 * because it takes a caller-supplied consumer — the test forces a throw after
 * processing the first row and verifies the connection is returned to the pool.
 */
@PostgresTest
public class OAuth2UserDAOPostgresTest
    extends AbstractDbDAOTest
{
  private OAuth2UserDAO oAuth2UserDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    oAuth2UserDAO = daoFactory.createOAuth2UserDAO();
  }

  @Test
  public void testConnectionReturnedWhenConsumerThrowsMidStream() {
    // Arrange: create and persist 3 OAuth2 users so the stream has multiple rows.
    // Created via tempEntity so they are cleaned up by the inherited TemporaryEntity rule.
    for (int i = 0; i < 3; i++) {
      tempEntity.newOAuth2User("user" + i, "First" + i, "Last" + i, "user" + i + "@example.com");
    }

    // Get the underlying DBCP pool to observe active connection count
    BasicDataSource bds = (BasicDataSource) databaseRule
        .getOperationalDataStore()
        .getDataSource();
    int baselineActive = bds.getNumActive();

    // Act: consume with a consumer that throws after seeing the first user
    List<OAuth2User> seen = new ArrayList<>();
    try {
      oAuth2UserDAO.withAllUsersWithGroups(user -> {
        seen.add(user);
        if (seen.size() == 1) {
          throw new RuntimeException("Simulated mid-stream failure");
        }
      });
    }
    catch (RuntimeException expected) {
      assertThat(expected).hasMessageContaining("Simulated mid-stream failure");
    }

    // Assert: the connection used by the stream was returned to the pool
    // (numActive should return to baseline once the stream is properly closed)
    assertThat(bds.getNumActive())
        .as("Connection should be returned to pool after mid-stream throw")
        .isEqualTo(baselineActive);

    // Sanity: we did process at least one row before the throw
    assertThat(seen).hasSize(1);
  }
}
