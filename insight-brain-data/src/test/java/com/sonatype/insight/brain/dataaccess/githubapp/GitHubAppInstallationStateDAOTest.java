/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.githubapp;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.githubapp.GitHubAppInstallationState;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for GitHubAppInstallationStateDAO
 */
public class GitHubAppInstallationStateDAOTest
    extends AbstractDbDAOTest
{
  private static final String STATE_TOKEN = "test-state-token-12345";

  private String githubAppId;

  private GitHubAppInstallationStateDAO dao;

  private final Set<String> tokensToCleanup = new HashSet<>();

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createGitHubAppInstallationStateDAO();

    // Create the referenced GitHubApp first to satisfy foreign key constraint
    githubAppId = tempEntity.newGitHubApp(organization.getId()).getId();

    // Insert a test token for all tests to use
    Date expiresAt = new Date(System.currentTimeMillis() + 900000); // 15 minutes from now
    GitHubAppInstallationState state = createInstallationState(STATE_TOKEN, githubAppId, expiresAt);
    dao.insert(state);

    registerTokenForCleanup(STATE_TOKEN);
  }

  @After
  public void tearDown() {
    // Clean up all tokens created during tests
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      for (String token : tokensToCleanup) {
        GitHubAppInstallationState state = dao.findByStateToken(tx, token);
        if (state != null) {
          dao.delete(tx, state);
        }
      }
      tx.commit();
    }
  }

  private void registerTokenForCleanup(String stateToken) {
    tokensToCleanup.add(stateToken);
  }

  @Test
  public void testInsert_Success() {
    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    GitHubAppInstallationState additionalState =
        createInstallationState("insert-test-token", githubAppId, expiresAt);

    registerTokenForCleanup("insert-test-token");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, additionalState);
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      GitHubAppInstallationState retrieved = dao.findByStateToken(tx, "insert-test-token");
      assertThat(retrieved).isNotNull();
      assertThat(retrieved.getStateToken()).isEqualTo("insert-test-token");
      assertThat(retrieved.getGithubAppId()).isEqualTo(githubAppId);
      assertThat(retrieved.getExpiresAt()).isEqualTo(expiresAt);
      assertThat(retrieved.getCreatedAt()).isNotNull();
      tx.commit();
    }
  }

  @Test
  public void testFindByStateToken_Found() {
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      GitHubAppInstallationState found = dao.findByStateToken(tx, STATE_TOKEN);
      assertThat(found).isNotNull();
      assertThat(found.getStateToken()).isEqualTo(STATE_TOKEN);
      assertThat(found.getGithubAppId()).isEqualTo(githubAppId);
      tx.commit();
    }
  }

  @Test
  public void testFindByStateToken_NotFound() {
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      GitHubAppInstallationState result = dao.findByStateToken(tx, "non-existent-token");
      assertThat(result).isNull();
      tx.commit();
    }
  }

  @Test
  public void testDelete_Success() {
    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    GitHubAppInstallationState stateToDelete =
        createInstallationState("delete-test-token", githubAppId, expiresAt);

    registerTokenForCleanup("delete-test-token");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, stateToDelete);
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      GitHubAppInstallationState found = dao.findByStateToken(tx, "delete-test-token");
      assertThat(found).isNotNull();
      dao.delete(tx, found);
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      GitHubAppInstallationState shouldBeNull = dao.findByStateToken(tx, "delete-test-token");
      assertThat(shouldBeNull).isNull();
      tx.commit();
    }
  }

  @Test
  public void testFindAndDeleteByToken_Success() {
    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    GitHubAppInstallationState state =
        createInstallationState("find-delete-token", githubAppId, expiresAt);

    registerTokenForCleanup("find-delete-token");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, state);
      tx.commit();
    }

    GitHubAppInstallationState found = dao.findAndDeleteByStateToken("find-delete-token");

    assertThat(found).isNotNull();
    assertThat(found.getStateToken()).isEqualTo("find-delete-token");
    assertThat(found.getGithubAppId()).isEqualTo(githubAppId);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      GitHubAppInstallationState shouldBeNull = dao.findByStateToken(tx, "find-delete-token");
      assertThat(shouldBeNull).isNull();
      tx.commit();
    }
  }

  @Test
  public void testFindAndDeleteByToken_NotFound() {
    GitHubAppInstallationState result = dao.findAndDeleteByStateToken("non-existent");
    assertThat(result).isNull();
  }

  @Test
  public void testIsExpired_NotExpired() {
    Date futureDate = new Date(System.currentTimeMillis() + 900000);
    GitHubAppInstallationState state = createInstallationState("not-expired", githubAppId, futureDate);

    assertThat(state.isExpired()).isFalse();
  }

  @Test
  public void testIsExpired_Expired() {
    Date pastDate = new Date(System.currentTimeMillis() - 60000);
    GitHubAppInstallationState state = createInstallationState("expired", githubAppId, pastDate);

    assertThat(state.isExpired()).isTrue();
  }

  @Test
  public void testInsertInstallationState_Success() {
    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    GitHubAppInstallationState state =
        createInstallationState("insert-helper-token", githubAppId, expiresAt);

    registerTokenForCleanup("insert-helper-token");

    dao.insert(state);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      GitHubAppInstallationState retrieved = dao.findByStateToken(tx, "insert-helper-token");
      assertThat(retrieved).isNotNull();
      assertThat(retrieved.getStateToken()).isEqualTo("insert-helper-token");
      tx.commit();
    }
  }

  @Test
  public void testStateToken_CaseSensitive() {
    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    GitHubAppInstallationState state =
        createInstallationState("CaseSensitiveToken", githubAppId, expiresAt);

    registerTokenForCleanup("CaseSensitiveToken");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, state);
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      GitHubAppInstallationState uppercase = dao.findByStateToken(tx, "CaseSensitiveToken");
      GitHubAppInstallationState lowercase = dao.findByStateToken(tx, "casesensitivetoken");

      assertThat(uppercase).isNotNull();
      assertThat(lowercase).isNull();
      tx.commit();
    }
  }

  @Test
  public void testExpiredToken_StillQueryable() {
    Date pastDate = new Date(System.currentTimeMillis() - 60000);
    GitHubAppInstallationState expiredState =
        createInstallationState("expired-queryable", githubAppId, pastDate);

    registerTokenForCleanup("expired-queryable");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, expiredState);
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      GitHubAppInstallationState found = dao.findByStateToken(tx, "expired-queryable");
      assertThat(found).isNotNull();
      assertThat(found.isExpired()).isTrue();
      tx.commit();
    }
  }

  @Test
  public void testInsert_WithNullId_ShouldGenerateId() {
    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    GitHubAppInstallationState state = new GitHubAppInstallationState();
    state.setId(null);
    state.setStateToken("null-id-test-token");
    state.setGithubAppId(githubAppId);
    state.setExpiresAt(expiresAt);
    state.setCreatedAt(new Date());

    registerTokenForCleanup("null-id-test-token");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, state);
      tx.commit();
    }

    assertThat(state.getId()).isNotNull();

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      GitHubAppInstallationState retrieved = dao.findByStateToken(tx, "null-id-test-token");
      assertThat(retrieved).isNotNull();
      assertThat(retrieved.getId()).isNotNull();
      assertThat(retrieved.getStateToken()).isEqualTo("null-id-test-token");
      tx.commit();
    }
  }

  @Test(expected = Exception.class)
  public void testInsert_WithNullGitHubAppId_ShouldFail() {
    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    GitHubAppInstallationState state = new GitHubAppInstallationState();
    state.setId("test-id-null-fk");
    state.setStateToken("null-github-app-id-token");
    state.setGithubAppId(null);
    state.setExpiresAt(expiresAt);
    state.setCreatedAt(new Date());

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, state);
      tx.commit();
    }
  }

  @Test
  public void testDeleteByGitHubAppId_MultipleStates() {
    Date expiresAt = new Date(System.currentTimeMillis() + 900000);
    tempEntity.newGitHubAppInstallationState("delete-token-1", githubAppId, "code-verifier-1", expiresAt);
    tempEntity.newGitHubAppInstallationState("delete-token-2", githubAppId, "code-verifier-2", expiresAt);
    tempEntity.newGitHubAppInstallationState("delete-token-3", githubAppId, "code-verifier-3", expiresAt);

    dao.deleteByGitHubAppId(githubAppId);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThat(dao.findByStateToken(tx, "delete-token-1")).isNull();
      assertThat(dao.findByStateToken(tx, "delete-token-2")).isNull();
      assertThat(dao.findByStateToken(tx, "delete-token-3")).isNull();
      assertThat(dao.findByStateToken(tx, STATE_TOKEN)).isNull();
      tx.commit();
    }
  }

  @Test
  public void testDeleteByGitHubAppId_NoStates() {
    dao.deleteByGitHubAppId(githubAppId);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThat(dao.findByStateToken(tx, STATE_TOKEN)).isNull();
      tx.commit();
    }
  }

  private GitHubAppInstallationState createInstallationState(
      String stateToken,
      String githubAppId,
      Date expiresAt)
  {
    GitHubAppInstallationState state = new GitHubAppInstallationState();
    state.setId(stateToken + "-id");
    state.setStateToken(stateToken);
    state.setGithubAppId(githubAppId);
    state.setExpiresAt(expiresAt);
    state.setCreatedAt(new Date());
    return state;
  }
}
