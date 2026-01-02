/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.sql.SQLException;
import java.util.Set;
import java.util.function.Function;

import com.sonatype.insight.brain.clients.AwsSecretsManagerClient;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.test.LogOutput;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Category(SlowTest.class)
public class RotateEncryptionKeyTaskTest
    extends AbstractMultiTenantDatabaseTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(RotateEncryptionKeyTask.class);

  @Mock
  AwsSecretsManagerClient awsSecretsManagerClient;

  SourceControlDAO sourceControlDAO;

  ArtifactoryConnectionDAO artifactoryConnectionDAO;

  @Mock
  ReloadTenantEncryptionKeyJob reloadTenantEncryptionKeyJob;

  @Mock
  private TaskScheduler taskScheduler;

  private final PasswordHandler passwordHandler = new PasswordHandler(null);

  private final MultiTenantInsightConfig multiTenantInsightConfig = new MultiTenantInsightConfig();

  private final TenantUtil tenantUtil = new TenantUtil();

  private TenantMetadataDAO tenantMetadataDAO;

  @Spy
  private DAOSecretRotator spyDaoSecretRotator = new DAOSecretRotator();

  private MultiTenantEncryptionKeyStore multiTenantEncryptionKeyStore;

  private RotateEncryptionKeyTask underTest;

  @Before
  @Override
  public void setup() {
    super.setup();

    sourceControlDAO = daoFactory.createSourceControlDAO();
    artifactoryConnectionDAO = daoFactory.createArtifactoryConnectionDAO();
    tenantMetadataDAO = new TenantMetadataDAO(databaseRule.getOperationalDataStore());

    multiTenantEncryptionKeyStore =
        new MultiTenantEncryptionKeyStore(awsSecretsManagerClient, multiTenantInsightConfig,
            tenantMetadataDAO, tenantUtil);

    Set<RotatableSecrets> rotatableSecrets = Set.of(artifactoryConnectionDAO, sourceControlDAO);

    underTest = new RotateEncryptionKeyTask(
        rotatableSecrets,
        awsSecretsManagerClient,
        spyDaoSecretRotator,
        passwordHandler,
        reloadTenantEncryptionKeyJob,
        tenantMetadataDAO,
        tenantUtil,
        taskScheduler,
        multiTenantEncryptionKeyStore
    );
  }

  @Test
  public void rotateEncryptionKey() {
    testAsNewTenant(t1 -> {
      String oldEncryptionKeyName = "mtiq-test/oldEncryptionKeyName";
      String oldEncryptionKeyValue = "oldEncryptionKeyValue";
      String newEncryptionKeyName = "mtiq-test/newEncryptionKeyName_" + t1.tenantSlug;
      String newEncryptionKeyValue = "newEncryptionKeyValue";
      tenantMetadataDAO.insert(
          new TenantMetadata("appId", "appName", "connId", "connName", oldEncryptionKeyName, null, null));

      when(awsSecretsManagerClient.getSecret(oldEncryptionKeyName)).thenReturn(oldEncryptionKeyValue);
      when(awsSecretsManagerClient.getSecret(newEncryptionKeyName)).thenReturn(newEncryptionKeyValue);

      underTest.rotateEncryptionKey(newEncryptionKeyName);

      verify(spyDaoSecretRotator).rotateEncryptedSecrets(eq(artifactoryConnectionDAO), any());
      verify(spyDaoSecretRotator).rotateEncryptedSecrets(eq(sourceControlDAO), any());

      assertThat(tenantMetadataDAO.get().getEncryptionKeyName()).isEqualTo(newEncryptionKeyName);
      assertThat(multiTenantEncryptionKeyStore.getKey()).isEqualTo(newEncryptionKeyValue);
      verify(taskScheduler).scheduleOneTimeTaskForAllOtherNodes(reloadTenantEncryptionKeyJob);
    });
  }

  @Test
  public void rotateEncryptionKey_verifySecretRotator() {
    testAsNewTenant(t1 -> {
      String oldEncryptionKeyName = "mtiq-test/oldEncryptionKeyName";
      String oldEncryptionKeyValue = "oldEncryptionKeyValue";
      String newEncryptionKeyName = "mtiq-test/newEncryptionKeyName_" + t1.tenantSlug;
      String newEncryptionKeyValue = "newEncryptionKeyValue";
      tenantMetadataDAO.insert(
          new TenantMetadata("appId", "appName", "connId", "connName", oldEncryptionKeyName, null, null));

      when(awsSecretsManagerClient.getSecret(oldEncryptionKeyName)).thenReturn(oldEncryptionKeyValue);
      when(awsSecretsManagerClient.getSecret(newEncryptionKeyName)).thenReturn(newEncryptionKeyValue);

      underTest.rotateEncryptionKey(newEncryptionKeyName);

      ArgumentCaptor<Function<String, String>> secretRotatorArgument = ArgumentCaptor.forClass(Function.class);
      verify(spyDaoSecretRotator).rotateEncryptedSecrets(eq(sourceControlDAO), secretRotatorArgument.capture());
      Function<String, String> secretRotator = secretRotatorArgument.getValue();

      final String testSecretValue = "abcd_efg-12345";
      String oldEncryptedSecret = passwordHandler.encryptPassword(testSecretValue, oldEncryptionKeyValue);
      assertThat(
          passwordHandler.decryptPassword(secretRotator.apply(oldEncryptedSecret), newEncryptionKeyValue)).isEqualTo(
          testSecretValue);

      assertThat(secretRotator.apply(null)).isEqualTo(null);
      assertThat(secretRotator.apply("")).isEqualTo("");
      assertThat(secretRotator.apply("{}")).isEqualTo("{}");
    });
  }

  @Test
  public void rotateEncryptionKey_secretRotatorWorksWithMigratedSecrets() {
    testAsNewTenant(t1 -> {
      String oldEncryptionKeyName = "mtiq-test/oldEncryptionKeyName";
      String oldEncryptionKeyValue = "oldEncryptionKeyValue";
      String newEncryptionKeyName = "mtiq-test/newEncryptionKeyName_" + t1.tenantSlug;
      String newEncryptionKeyValue = "newEncryptionKeyValue";
      tenantMetadataDAO.insert(
          new TenantMetadata("appId", "appName", "connId", "connName", oldEncryptionKeyName, null, null));

      when(awsSecretsManagerClient.getSecret(oldEncryptionKeyName)).thenReturn(oldEncryptionKeyValue);
      when(awsSecretsManagerClient.getSecret(newEncryptionKeyName)).thenReturn(newEncryptionKeyValue);

      underTest.rotateEncryptionKey(newEncryptionKeyName);

      ArgumentCaptor<Function<String, String>> secretRotatorArgument = ArgumentCaptor.forClass(Function.class);
      verify(spyDaoSecretRotator).rotateEncryptedSecrets(eq(sourceControlDAO), secretRotatorArgument.capture());
      Function<String, String> secretRotator = secretRotatorArgument.getValue();

      final String testSecretValue = "abcd_efg-12345";

      String oldEncryptedSecret = passwordHandler.encryptPassword(testSecretValue, oldEncryptionKeyName);
      assertThrows("Unable to decrypt secret", RuntimeException.class, () -> secretRotator.apply(oldEncryptedSecret));

      String newEncryptedSecret = passwordHandler.encryptPassword(testSecretValue, newEncryptionKeyValue);
      assertThat(passwordHandler.decryptPassword(secretRotator.apply(newEncryptedSecret), newEncryptionKeyValue))
          .isEqualTo(testSecretValue);
    });
  }

  @Test
  public void rotateEncryptionKey_secretRotatorEncryptsNonEncryptedSecrets() {
    testAsNewTenant(t1 -> {
      String oldEncryptionKeyName = "mtiq-test/oldEncryptionKeyName";
      String oldEncryptionKeyValue = "oldEncryptionKeyValue";
      String newEncryptionKeyName = "mtiq-test/newEncryptionKeyName_" + t1.tenantSlug;
      String newEncryptionKeyValue = "newEncryptionKeyValue";
      tenantMetadataDAO.insert(
          new TenantMetadata("appId", "appName", "connId", "connName", oldEncryptionKeyName, null, null));

      when(awsSecretsManagerClient.getSecret(oldEncryptionKeyName)).thenReturn(oldEncryptionKeyValue);
      when(awsSecretsManagerClient.getSecret(newEncryptionKeyName)).thenReturn(newEncryptionKeyValue);

      underTest.rotateEncryptionKey(newEncryptionKeyName);

      ArgumentCaptor<Function<String, String>> secretRotatorArgument = ArgumentCaptor.forClass(Function.class);
      verify(spyDaoSecretRotator).rotateEncryptedSecrets(eq(sourceControlDAO), secretRotatorArgument.capture());
      Function<String, String> secretRotator = secretRotatorArgument.getValue();

      final String notEncryptedSecretValue = "abcd_efg-12345";

      assertThat(passwordHandler.decryptPassword(secretRotator.apply(notEncryptedSecretValue),
          newEncryptionKeyValue)).isEqualTo(notEncryptedSecretValue);
    });
  }

  @Test
  public void rotateEncryptionKey_globalTenant() {
    testAsGlobalTenant(t1 -> {
      String newEncryptionKeyName = "mtiq-test/newEncryptionKeyName_" + t1.tenantSlug;

      underTest.rotateEncryptionKey(newEncryptionKeyName);

      assertThat(logOutput).atWarnLevel()
          .contains("Not rotating tenant encryption key. Global tenant does not have an encryption key.");
    });
  }

  @Test
  public void rotateEncryptionKey_invalidEncryptionKeyName() {
    testAsNewTenant(t1 -> {
      underTest.rotateEncryptionKey(null);

      assertThat(logOutput).atErrorLevel()
          .contains("Not rotating tenant encryption key. Key name is invalid: null");

      String nameMissingMtiqPrefix = "test/nameMissingMtiqPrefix";

      underTest.rotateEncryptionKey(nameMissingMtiqPrefix);

      assertThat(logOutput).atErrorLevel()
          .contains("Not rotating tenant encryption key. Key name is invalid: test/nameMissingMtiqPrefix");
    });
  }

  @Test
  public void rotateEncryptionKey_encryptionFails() {
    testAsNewTenant(t1 -> {
      String oldEncryptionKeyName = "mtiq-test/oldEncryptionKeyName";
      String oldEncryptionKeyValue = "oldEncryptionKeyValue";
      String newEncryptionKeyName = "mtiq-test/newEncryptionKeyName_" + t1.tenantSlug;
      String newEncryptionKeyValue = "newEncryptionKeyValue";
      tenantMetadataDAO.insert(
          new TenantMetadata("appId", "appName", "connId", "connName", oldEncryptionKeyName, null, null));

      when(awsSecretsManagerClient.getSecret(oldEncryptionKeyName)).thenReturn(oldEncryptionKeyValue);
      when(awsSecretsManagerClient.getSecret(newEncryptionKeyName)).thenReturn(newEncryptionKeyValue);

      doThrow(IllegalStateException.class).doNothing().when(spyDaoSecretRotator)
          .rotateEncryptedSecrets(isA(SourceControlDAO.class), isA(Function.class));

      underTest.rotateEncryptionKey(newEncryptionKeyName);

      assertThat(logOutput).atErrorLevel().contains(
          "Tenant encryption key rotation failed. Unable to rotate encrypted secrets using new tenant encryption key " +
              newEncryptionKeyName).contains(
          "Failed to rotate secrets for: class com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO");

      assertThat(tenantMetadataDAO.get().getEncryptionKeyName()).isEqualTo(oldEncryptionKeyName);
      assertThat(multiTenantEncryptionKeyStore.getKey()).isEqualTo(oldEncryptionKeyValue);
    });
  }

  @Test
  public void rotateEncryptionKey_encryptionSqlFails() {
    testAsNewTenant(t1 -> {
      String oldEncryptionKeyName = "mtiq-test/oldEncryptionKeyName";
      String oldEncryptionKeyValue = "oldEncryptionKeyValue";
      String newEncryptionKeyName = "mtiq-test/newEncryptionKeyName_" + t1.tenantSlug;
      String newEncryptionKeyValue = "newEncryptionKeyValue";
      tenantMetadataDAO.insert(
          new TenantMetadata("appId", "appName", "connId", "connName", oldEncryptionKeyName, null, null));

      when(awsSecretsManagerClient.getSecret(oldEncryptionKeyName)).thenReturn(oldEncryptionKeyValue);
      when(awsSecretsManagerClient.getSecret(newEncryptionKeyName)).thenReturn(newEncryptionKeyValue);

      doThrow(SQLException.class).doNothing().when(spyDaoSecretRotator)
          .rotateEncryptedSecrets(isA(SourceControlDAO.class), isA(Function.class));

      underTest.rotateEncryptionKey(newEncryptionKeyName);

      assertThat(logOutput).atErrorLevel().contains(
          "Tenant encryption key rotation failed. Unable to rotate encrypted secrets using new tenant encryption key " +
              newEncryptionKeyName).contains(
          "Failed to rotate secrets for: class com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO");

      assertThat(tenantMetadataDAO.get().getEncryptionKeyName()).isEqualTo(oldEncryptionKeyName);
      assertThat(multiTenantEncryptionKeyStore.getKey()).isEqualTo(oldEncryptionKeyValue);
    });
  }
}
