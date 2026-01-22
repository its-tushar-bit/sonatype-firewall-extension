/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;

import com.sonatype.insight.brain.clients.AwsSecretsManagerClient;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.MtiqBatchJob;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import io.dropwizard.servlets.tasks.Task;
import org.apache.directory.api.util.Strings;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class RotateEncryptionKeyTask
    extends Task
    implements InsightJob, MtiqBatchJob
{
  private static final Logger log = LoggerFactory.getLogger(RotateEncryptionKeyTask.class);

  public static final String NAME = "RotateEncryptionKeyTask";

  public static String QUERY_PARAM_NEW_ENCRYPTION_KEY_NAME = "newEncryptionKeyName";

  private static final String ROTATE_ERROR = "Tenant encryption key rotation error";

  private final AwsSecretsManagerClient awsSecretsManagerClient;

  private final DAOSecretRotator daoSecretRotator;

  private final PasswordHandler passwordHandler;

  private final ReloadTenantEncryptionKeyJob reloadTenantEncryptionKeyJob;

  private final Set<RotatableSecrets> rotatableSecrets;

  private final TenantMetadataDAO tenantMetadataDAO;

  private final TenantUtil tenantUtil;

  private final TaskScheduler taskScheduler;

  private final EncryptionKeyStore encryptionKeyStore;

  @Inject
  public RotateEncryptionKeyTask(
      final Set<RotatableSecrets> rotatableSecrets,
      final AwsSecretsManagerClient awsSecretsManagerClient,
      final DAOSecretRotator daoSecretRotator,
      final PasswordHandler passwordHandler,
      final ReloadTenantEncryptionKeyJob reloadTenantEncryptionKeyJob,
      final TenantMetadataDAO tenantMetadataDAO,
      final TenantUtil tenantUtil,
      final TaskScheduler taskScheduler,
      final EncryptionKeyStore encryptionKeyStore)
  {
    super("triggerRotateEncryptionKey");
    this.awsSecretsManagerClient = awsSecretsManagerClient;
    this.daoSecretRotator = daoSecretRotator;
    this.passwordHandler = passwordHandler;
    this.rotatableSecrets = rotatableSecrets;
    this.reloadTenantEncryptionKeyJob = reloadTenantEncryptionKeyJob;
    this.tenantMetadataDAO = tenantMetadataDAO;
    this.tenantUtil = tenantUtil;
    this.taskScheduler = taskScheduler;
    this.encryptionKeyStore = encryptionKeyStore;
  }

  @Override
  public String getJobName() {
    return NAME;
  }

  /**
   * This task rotates the tenant encryption key by decrypting all secrets using the current key and re-encrypt
   * them using the new key. The new key is fetched from AWS Secrets Manager using the provided key name.
   * ---
   * Do not migrate webhook secrets as these use a secret set by the client in the configuration.
   * ---
   * This task should be run during a maintenance window and the tenant schema or database should be backed
   * up before the task is started.
   * If this task fails, the issue should be identified, fixed and then this task should be re-run.
   * ---
   * The `RotatableSecrets` interface represents a component that contains secrets which can be rotated.
   * Implementations of this interface should inherit from the abstract class AbstractOperationalSqlDAO.
   *
   * @param parameters a map containing the parameters for the task execution.
   * The first key should be "newEncryptionKeyName", which holds a list of strings where the first element is the name
   * of the new encryption key to be used.
   */
  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter printWriter) {
    String newEncryptionKeyName = parameters.getOrDefault(QUERY_PARAM_NEW_ENCRYPTION_KEY_NAME, List.of()).stream()
        .findFirst().orElse(null);

    if (newEncryptionKeyName == null) {
      throw new BadRequestException("Param newEncryptionKeyName not provided.");
    }

    taskScheduler.scheduleOneTimeTask(this, Map.of(QUERY_PARAM_NEW_ENCRYPTION_KEY_NAME, newEncryptionKeyName));
    printWriter.write("Scheduled run of " + getJobName() + "\n");
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(() -> {
      String newEncryptionKeyName = context.getMergedJobDataMap().getString(QUERY_PARAM_NEW_ENCRYPTION_KEY_NAME);
      rotateEncryptionKey(newEncryptionKeyName);
    }, log, ROTATE_ERROR);
  }

  // Visible for testing
  void rotateEncryptionKey(final String newEncryptionKeyName) {
    log.info("Starting task to use updated encryption key {} and rotate associated secrets.", newEncryptionKeyName);

    if (!isMultiTenantEncryptionKeyStore()) {
      log.warn("Not rotating tenant encryption key. Encryption key store is not a MultiTenantEncryptionKeyStore.");
      return;
    }

    if (tenantUtil.isGlobalTenant()) {
      log.warn("Not rotating tenant encryption key. Global tenant does not have an encryption key.");
      return;
    }

    if (isSecretKeyNameInvalid(newEncryptionKeyName)) {
      log.error("Not rotating tenant encryption key. Key name is invalid: {}", newEncryptionKeyName);
      return;
    }

    String currentEncryptionKeyValue = getCurrentEncryptionKeyValue();
    if (currentEncryptionKeyValue == null) {
      return;
    }

    String newEncryptionKeyValue = getNewEncryptionKeyValue(newEncryptionKeyName);
    if (newEncryptionKeyValue == null) {
      return;
    }

    Function<String, String> secretRotator =
        secret -> rotateEncryptedSecret(secret, currentEncryptionKeyValue, newEncryptionKeyValue);

    boolean successfullyRotated = rotateSecrets(newEncryptionKeyName, secretRotator);
    if (!successfullyRotated) {
      return;
    }

    updateEncryptionKey(newEncryptionKeyName, newEncryptionKeyValue);
  }

  private boolean isMultiTenantEncryptionKeyStore() {
    return encryptionKeyStore instanceof MultiTenantEncryptionKeyStore;
  }

  private boolean isSecretKeyNameInvalid(String keyName) {
    return keyName == null || !keyName.contains("mtiq-");
  }

  private boolean rotateSecrets(String newEncryptionKeyName, Function<String, String> secretRotator) {
    for (RotatableSecrets rotatableSecret : rotatableSecrets) {
      if (rotatableSecret instanceof AbstractOperationalSqlDAO) {
        try {
          daoSecretRotator.rotateEncryptedSecrets((AbstractOperationalSqlDAO<?>) rotatableSecret, secretRotator);
        }
        catch (RuntimeException | SQLException e) {
          log.error("Tenant encryption key rotation failed. Unable to rotate encrypted secrets using new tenant " +
                  "encryption key {}. Failed to rotate secrets for: {}", newEncryptionKeyName,
              rotatableSecret.getClass(), e);
          return false;
        }
      }
      else {
        log.error("Not rotating tenant encryption key. RotatableSecrets implementation is not an instance of " +
            "AbstractOperationalSqlDAO. Unable to rotate secrets for: {}", rotatableSecret.getClass());
        return false;
      }
    }
    return true;
  }

  private void updateEncryptionKey(String newEncryptionKeyName, String newEncryptionKeyValue) {
    tenantMetadataDAO.setEncryptionKeyName(newEncryptionKeyName);
    encryptionKeyStore.initializeKey();

    if (encryptionKeyStore.getKey().equals(newEncryptionKeyValue)) {
      taskScheduler.scheduleOneTimeTaskForAllOtherNodes(reloadTenantEncryptionKeyJob);
      log.info("Encryption key successfully rotated");
    }
    else {
      log.error("Encryption key rotation failed, new key value does not match expected value");
    }
  }

  private String getCurrentEncryptionKeyValue() {
    try {
      String currentEncryptionKeyValue = encryptionKeyStore.getKey();
      if (Strings.isEmpty(currentEncryptionKeyValue)) {
        throw new IllegalArgumentException("current encryption key value is empty.");
      }
      return currentEncryptionKeyValue;
    }
    catch (RuntimeException e) {
      log.error("Not rotating tenant encryption key: ", e);
      return null;
    }
  }

  private String getNewEncryptionKeyValue(String newEncryptionKeyName) {
    try {
      String newEncryptionKeyValue = awsSecretsManagerClient.getSecret(newEncryptionKeyName);
      if (Strings.isEmpty(newEncryptionKeyValue)) {
        throw new IllegalArgumentException("new encryption key value is empty.");
      }
      return newEncryptionKeyValue;
    }
    catch (RuntimeException e) {
      log.error("Not rotating tenant encryption key: ", e);
      return null;
    }
  }

  private String rotateEncryptedSecret(
      final String secret,
      final String oldEncryptionKeyValue,
      final String newEncryptionKeyValue)
  {
    if (secret == null) {
      return null;
    }
    if (secret.isEmpty()) {
      return "";
    }
    if ("{}".equals(secret)) {
      // PlexusCipher encrypts empty strings (i.e. "") to "{}". We do not need to rotate empty secrets.
      return secret;
    }

    String decryptedSecret = decryptSecret(secret, oldEncryptionKeyValue, newEncryptionKeyValue);
    try {
      return passwordHandler.encryptPassword(decryptedSecret, newEncryptionKeyValue);
    }
    catch (IllegalStateException e) {
      throw new RuntimeException("Unable to rotate encrypted secret, unable to encrypt secret", e);
    }
  }

  private String decryptSecret(
      final String secret,
      final String oldEncryptionKeyValue,
      final String newEncryptionKeyValue)
  {
    if (!passwordHandler.isEncrypted(secret)) {
      // If the secret is not encrypted return the secret, so it can be rotated. This allows the task to be run on
      // tables with secrets that are not yet encrypted.
      return secret;
    }

    try {
      return passwordHandler.decryptPassword(secret, oldEncryptionKeyValue);
    }
    catch (IllegalStateException e) {
      // If decryption fails, the secret could have been encrypted with the new key in a previous attempt to run
      // the task, so try to decrypt it with the new key, this allows the task to be re-run if it fails.
      try {
        return passwordHandler.decryptPassword(secret, newEncryptionKeyValue);
      }
      catch (IllegalStateException ignored) {
        // Rethrow the original exception if decryption with the new key also fails.
        throw new RuntimeException("Unable to rotate encrypted secret, unable to decrypt secret.", e);
      }
    }
  }
}
