/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.relay;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.security.RotatableSecret;
import com.sonatype.insight.model.HasStringId;

/**
 * Singleton configuration for the SCM webhook relay integration. The {@code apiKey} and
 * {@code webhookSigningSecret} columns are encrypted at rest via PasswordHandler.
 *
 * <p>
 * Encryption-key rotation: only {@code apiKey} carries {@link RotatableSecret} because
 * {@code DAOSecretRotator} re-encrypts a single annotated field per entity (same constraint
 * applies to {@code GitHubApp.privateKey}). After a key rotation the webhook signing secret
 * becomes undecryptable; recovery is to re-register, which rotates both fields.
 */
@Entity
@Table(name = "relay_configuration")
public class RelayConfiguration
    implements HasStringId
{
  @Id
  @Column(name = "relay_configuration_id")
  private String id;

  @RotatableSecret
  @Column(name = "api_key")
  private String apiKey;

  @Column(name = "webhook_url")
  private String webhookUrl;

  @Column(name = "webhook_signing_secret")
  private String webhookSigningSecret;

  @Column(name = "customer_id")
  private String customerId;

  @Column(name = "registered_at")
  private Date registeredAt;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getWebhookUrl() {
    return webhookUrl;
  }

  public void setWebhookUrl(String webhookUrl) {
    this.webhookUrl = webhookUrl;
  }

  public String getWebhookSigningSecret() {
    return webhookSigningSecret;
  }

  public void setWebhookSigningSecret(String webhookSigningSecret) {
    this.webhookSigningSecret = webhookSigningSecret;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public Date getRegisteredAt() {
    return registeredAt;
  }

  public void setRegisteredAt(Date registeredAt) {
    this.registeredAt = registeredAt;
  }
}
