/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.relay;

import java.sql.SQLException;
import java.util.Date;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.model.relay.RelayConfiguration;

import org.jooq.exception.IntegrityConstraintViolationException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class RelayConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private RelayConfigurationDAO dao;

  private DAOSecretRotator daoSecretRotator;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRelayConfigurationDAO();
    daoSecretRotator = new DAOSecretRotator();
  }

  @Test
  public void testCRUD() {
    assertThat(dao.get()).isNull();

    RelayConfiguration cfg = newRelayConfiguration("api-key-1", "https://relay.example/webhook/abc/github",
        "signing-secret-1", "customer-1");
    Date registeredAt = cfg.getRegisteredAt();
    dao.set(cfg);

    RelayConfiguration loaded = dao.get();
    assertThat(loaded).isNotNull();
    assertThat(loaded.getId()).isEqualTo(RelayConfigurationDAO.SINGLETON_ENTITY_ID);
    assertThat(loaded.getApiKey()).isEqualTo("api-key-1");
    assertThat(loaded.getWebhookUrl()).isEqualTo("https://relay.example/webhook/abc/github");
    assertThat(loaded.getWebhookSigningSecret()).isEqualTo("signing-secret-1");
    assertThat(loaded.getCustomerId()).isEqualTo("customer-1");
    assertThat(loaded.getRegisteredAt()).isEqualTo(registeredAt);

    loaded.setApiKey("api-key-2");
    loaded.setWebhookUrl("https://relay.example/webhook/xyz/github");
    loaded.setWebhookSigningSecret("signing-secret-2");
    loaded.setCustomerId("customer-2");
    dao.set(loaded);

    RelayConfiguration updated = dao.get();
    assertThat(updated.getApiKey()).isEqualTo("api-key-2");
    assertThat(updated.getWebhookUrl()).isEqualTo("https://relay.example/webhook/xyz/github");
    assertThat(updated.getWebhookSigningSecret()).isEqualTo("signing-secret-2");
    assertThat(updated.getCustomerId()).isEqualTo("customer-2");

    dao.delete();
    assertThat(dao.get()).isNull();
  }

  @Test
  public void testInsert_EnforceSingleton() {
    dao.insert(newRelayConfiguration("k", "u", "s", "c"));

    assertThatExceptionOfType(IntegrityConstraintViolationException.class)
        .isThrownBy(() -> dao.insert(newRelayConfiguration("k2", "u2", "s2", "c2")));

    RelayConfiguration other = newRelayConfiguration("k3", "u3", "s3", "c3");
    other.setId("not-singleton-id");
    assertThatExceptionOfType(IntegrityConstraintViolationException.class)
        .isThrownBy(() -> dao.insert(other));
  }

  @Test
  public void testUpdate_EnforceSingleton() {
    dao.insert(newRelayConfiguration("k", "u", "s", "c"));

    RelayConfiguration other = newRelayConfiguration("k2", "u2", "s2", "c2");
    other.setId("not-singleton-id");
    dao.update(other);
    assertThat(dao.getAll())
        .extracting(RelayConfiguration::getId)
        .containsExactly(RelayConfigurationDAO.SINGLETON_ENTITY_ID);
  }

  @Test
  public void testSet_UpdateSingleton() {
    dao.set(newRelayConfiguration("k", "u", "s", "c"));
    dao.set(newRelayConfiguration("k", "https://relay.example/new", "s", "c"));
    assertThat(dao.get().getWebhookUrl()).isEqualTo("https://relay.example/new");
  }

  @Test
  public void testRotateEncryptedSecrets() throws SQLException {
    dao.set(newRelayConfiguration("apiKeyOld1", "https://relay.example/webhook", "secretOld1", "customer-1"));
    Function<String, String> secretRotator = secret -> secret.replace("Old", "New");

    daoSecretRotator.rotateEncryptedSecrets(dao, secretRotator);

    RelayConfiguration result = dao.get();
    assertThat(result.getApiKey()).isEqualTo("apiKeyNew1");
    // Only the first @RotatableSecret field is rotated by DAOSecretRotator; the
    // webhookSigningSecret column is untouched here. Both values are still encrypted
    // at write-time by the service layer via PasswordHandler.
    assertThat(result.getWebhookSigningSecret()).isEqualTo("secretOld1");
    assertThat(result.getWebhookUrl()).isEqualTo("https://relay.example/webhook");
    assertThat(result.getCustomerId()).isEqualTo("customer-1");
  }

  private RelayConfiguration newRelayConfiguration(
      String apiKey,
      String webhookUrl,
      String signingSecret,
      String customerId)
  {
    RelayConfiguration cfg = new RelayConfiguration();
    cfg.setApiKey(apiKey);
    cfg.setWebhookUrl(webhookUrl);
    cfg.setWebhookSigningSecret(signingSecret);
    cfg.setCustomerId(customerId);
    cfg.setRegisteredAt(new Date());
    return cfg;
  }
}
