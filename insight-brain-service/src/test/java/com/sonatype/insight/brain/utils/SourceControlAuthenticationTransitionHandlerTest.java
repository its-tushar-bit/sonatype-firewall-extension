/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.git.GitHubAppAuthStrategyCache;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.githubapp.GitHubAppDeletionService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SourceControlAuthenticationTransitionHandlerTest
    extends AbstractComponentTest
{
  private static final String TEST_GITHUB_APP_SLUG = "test-app";

  private static final String TEST_CLIENT_ID = "test-client-id";

  private static final String TEST_CLIENT_SECRET = "test-client-secret";

  private int appIdCounter = 10000;

  @Inject
  private GitHubAppDAO gitHubAppDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private GitHubAppDeletionService deletionService;

  @Mock
  private GitHubAppAuthStrategyCache mockCache;

  private SourceControlAuthenticationTransitionHandler handler;

  @Before
  public void setup() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);
    handler = new SourceControlAuthenticationTransitionHandler(gitHubAppDAO, mockCache, deletionService);
  }

  @Test
  public void testHandleAuthTransition_SwitchingToGitHubApp_ActivatesApp() {
    Application app = tempEntity.newApplicationWithParent();
    GitHubApp gitHubApp = createAndInsertGitHubApp(app.getId());
    gitHubApp.setActive(false);
    gitHubAppDAO.update(gitHubApp);

    SourceControl storedSC = createSourceControl(app.getId(), AuthenticationType.PAT);
    SourceControl newSC = createSourceControl(app.getId(), AuthenticationType.GITHUB_APP);
    newSC.setUsername("user");
    newSC.setToken("token");

    ApiSourceControlDTO dto = new ApiSourceControlDTO();
    dto.githubAppId = gitHubApp.getId();

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      handler.handleAuthTransition(tx, storedSC, newSC, dto);
    }

    assertThat(newSC.getUsername()).isNull();
    assertThat(newSC.getToken()).isNull();

    GitHubApp updated = gitHubAppDAO.getById(gitHubApp.getId());
    assertThat(updated.isActive()).isTrue();

    verify(mockCache).invalidate(app.getId());
  }

  @Test
  public void testHandleAuthTransition_SwitchingToGitHubApp_InvalidatesCacheByAppId() {
    Application app = tempEntity.newApplicationWithParent();
    GitHubApp gitHubApp = createAndInsertGitHubApp(app.getId());
    gitHubApp.setActive(false);
    gitHubAppDAO.update(gitHubApp);

    SourceControl storedSC = createSourceControl(app.getId(), AuthenticationType.PAT);
    SourceControl newSC = createSourceControl(app.getId(), AuthenticationType.GITHUB_APP);

    ApiSourceControlDTO dto = new ApiSourceControlDTO();
    dto.githubAppId = gitHubApp.getId();

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      handler.handleAuthTransition(tx, storedSC, newSC, dto);
    }

    verify(mockCache).invalidate(app.getId());
    verify(mockCache).invalidateByGitHubAppId(gitHubApp.getAppId());
  }

  @Test
  public void testHandleAuthTransition_SwitchingToGitHubApp_DeactivatesOldApps() {
    Application app = tempEntity.newApplicationWithParent();

    GitHubApp oldApp = createAndInsertGitHubApp(app.getId());
    oldApp.setActive(true);
    gitHubAppDAO.update(oldApp);

    GitHubApp newApp = createAndInsertGitHubApp(app.getId());
    newApp.setActive(false);
    gitHubAppDAO.update(newApp);

    SourceControl storedSC = createSourceControl(app.getId(), AuthenticationType.GITHUB_APP);
    SourceControl newSC = createSourceControl(app.getId(), AuthenticationType.GITHUB_APP);

    ApiSourceControlDTO dto = new ApiSourceControlDTO();
    dto.githubAppId = newApp.getId();

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      handler.handleAuthTransition(tx, storedSC, newSC, dto);
    }

    GitHubApp updatedOld = gitHubAppDAO.getById(oldApp.getId());
    GitHubApp updatedNew = gitHubAppDAO.getById(newApp.getId());

    assertThat(updatedOld.isActive()).isFalse();
    assertThat(updatedNew.isActive()).isTrue();

    verify(mockCache).invalidate(app.getId());
  }

  @Test(expected = NotFoundException.class)
  public void testHandleAuthTransition_SwitchingToGitHubApp_ThrowsWhenAppNotFound() {
    Application app = tempEntity.newApplicationWithParent();

    SourceControl storedSC = createSourceControl(app.getId(), AuthenticationType.PAT);
    SourceControl newSC = createSourceControl(app.getId(), AuthenticationType.GITHUB_APP);

    ApiSourceControlDTO dto = new ApiSourceControlDTO();
    dto.githubAppId = "non-existent-id";

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      handler.handleAuthTransition(tx, storedSC, newSC, dto);
    }
  }

  @Test(expected = BadRequestException.class)
  public void testHandleAuthTransition_SwitchingToGitHubApp_ThrowsWhenAppBelongsToDifferentOwner() {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();

    GitHubApp gitHubApp = createAndInsertGitHubApp(app2.getId());

    SourceControl storedSC = createSourceControl(app1.getId(), AuthenticationType.PAT);
    SourceControl newSC = createSourceControl(app1.getId(), AuthenticationType.GITHUB_APP);

    ApiSourceControlDTO dto = new ApiSourceControlDTO();
    dto.githubAppId = gitHubApp.getId();

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      handler.handleAuthTransition(tx, storedSC, newSC, dto);
    }
  }

  @Test
  public void testHandleAuthTransition_SwitchingFromGitHubAppToPAT_DeactivatesAllApps() {
    Application app = tempEntity.newApplicationWithParent();

    GitHubApp app1 = createAndInsertGitHubApp(app.getId());
    app1.setActive(true);
    gitHubAppDAO.update(app1);

    GitHubApp app2 = createAndInsertGitHubApp(app.getId());
    app2.setActive(false);
    gitHubAppDAO.update(app2);

    SourceControl storedSC = createSourceControl(app.getId(), AuthenticationType.GITHUB_APP);
    SourceControl newSC = createSourceControl(app.getId(), AuthenticationType.PAT);

    ApiSourceControlDTO dto = new ApiSourceControlDTO();

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      handler.handleAuthTransition(tx, storedSC, newSC, dto);
    }

    GitHubApp updated1 = gitHubAppDAO.getById(app1.getId());
    GitHubApp updated2 = gitHubAppDAO.getById(app2.getId());

    assertThat(updated1.isActive()).isFalse();
    assertThat(updated2.isActive()).isFalse();
  }

  @Test
  public void testHandleAuthTransition_SwitchingToGitHubAppWithoutAppId_DoesNothing() {
    Application app = tempEntity.newApplicationWithParent();

    SourceControl storedSC = createSourceControl(app.getId(), AuthenticationType.PAT);
    SourceControl newSC = createSourceControl(app.getId(), AuthenticationType.GITHUB_APP);

    ApiSourceControlDTO dto = new ApiSourceControlDTO();
    dto.githubAppId = null;

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      handler.handleAuthTransition(tx, storedSC, newSC, dto);
    }

    verify(mockCache, never()).invalidate(anyString());
  }

  private GitHubApp createAndInsertGitHubApp(String ownerId) {
    appIdCounter++;

    GitHubApp gitHubApp = new GitHubApp();
    gitHubApp.setId(UUID.randomUUID().toString());
    gitHubApp.setOwnerId(ownerId);
    gitHubApp.setAppId(appIdCounter);
    gitHubApp.setSlug(TEST_GITHUB_APP_SLUG);
    gitHubApp.setGithubOrganizationName("myOrg");
    gitHubApp.setLastUpdatedAt(new Date());
    gitHubApp.setClientId(TEST_CLIENT_ID);
    gitHubApp.setClientSecret(passwordHandler.encryptPassword(TEST_CLIENT_SECRET));
    gitHubApp.setPrivateKey(passwordHandler.encryptPassword(generateTestRsaPrivateKey()));
    gitHubApp.setInstallationId((long) appIdCounter);
    return tempEntity.newGitHubApp(gitHubApp);
  }

  private SourceControl createSourceControl(String ownerId, AuthenticationType authType) {
    return new SourceControl.Builder()
        .setOwnerId(ownerId)
        .setRepositoryUrl("https://github.com/test/repo")
        .setProvider(SourceControlProvider.GITHUB)
        .setAuthenticationType(authType)
        .build();
  }

  private String generateTestRsaPrivateKey() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      byte[] pkcs8EncodedKey = keyPair.getPrivate().getEncoded();
      return Base64.getEncoder().encodeToString(pkcs8EncodedKey);
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to generate test RSA key", e);
    }
  }

  @Test
  public void testHandleAuthTransition_SwitchingFromPATToPAT_DoesNotCallDeactivate() {
    Application app = tempEntity.newApplicationWithParent();

    GitHubAppDeletionService spyDeletionService = spy(deletionService);
    SourceControlAuthenticationTransitionHandler handlerWithSpy =
        new SourceControlAuthenticationTransitionHandler(gitHubAppDAO, mockCache, spyDeletionService);

    SourceControl storedSC = createSourceControl(app.getId(), AuthenticationType.PAT);
    storedSC.setUsername("old-user");
    storedSC.setToken("old-token");

    SourceControl newSC = createSourceControl(app.getId(), AuthenticationType.PAT);
    newSC.setUsername("new-user");
    newSC.setToken("new-token");

    ApiSourceControlDTO dto = new ApiSourceControlDTO();

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      tx.begin();
      handlerWithSpy.handleAuthTransition(tx, storedSC, newSC, dto);
      tx.commit();
    }

    verify(spyDeletionService, never()).deactivateGitHubApps(any(), eq(app.getId()));
  }

  @Test
  public void testHandleAuthTransition_SwitchingToGitHubApp_RollsBackActivationOnFailure() {
    Application app = tempEntity.newApplicationWithParent();
    GitHubApp gitHubApp = createAndInsertGitHubApp(app.getId());
    gitHubApp.setActive(false);
    gitHubAppDAO.update(gitHubApp);

    SourceControl storedSC = createSourceControl(app.getId(), AuthenticationType.PAT);
    SourceControl newSC = createSourceControl(app.getId(), AuthenticationType.GITHUB_APP);

    ApiSourceControlDTO dto = new ApiSourceControlDTO();
    dto.githubAppId = gitHubApp.getId();

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      tx.begin();
      handler.handleAuthTransition(tx, storedSC, newSC, dto);
      tx.rollback();
    }

    GitHubApp updated = gitHubAppDAO.getById(gitHubApp.getId());
    assertThat(updated.isActive()).isFalse();
  }

}
