/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.githubapp;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import com.sonatype.insight.brain.security.RotatableSecret;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.201
 */
@Entity
@Table(name = "github_app")
public class GitHubApp
    implements HasStringId
{
  @Id
  @Column(name = "github_app_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "github_organization_name")
  private String githubOrganizationName;

  @Column(name = "app_id")
  private Integer appId;

  @Column(name = "slug")
  private String slug;

  @Column(name = "client_id")
  private String clientId;

  @RotatableSecret
  @Column(name = "client_secret")
  private String clientSecret;

  @RotatableSecret
  @Column(name = "private_key")
  private String privateKey;

  /**
   * App-level webhook secret returned by GitHub during manifest conversion when the manifest
   * sets {@code hook_attributes}. Encrypted at rest. Plaintext is forwarded to the relay during
   * installation-setup auto-registration so the relay can verify webhook signatures from
   * GitHub. Null for Apps registered before the relay integration was enabled or via flows
   * that did not request a webhook (the customer must then configure the secret manually on
   * the App and re-register with the relay).
   *
   * <p>
   * Note: {@link DAOSecretRotator} rotates only the first {@code @RotatableSecret} per
   * entity; this field is not rotatable for the same reason as the private key.
   */
  @Column(name = "webhook_secret")
  private String webhookSecret;

  @Column(name = "installation_id")
  private Long installationId;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_updated_at")
  private Date lastUpdatedAt;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = false;

  /**
   * Health of the link from this GitHub App to the SCM webhook relay. See
   * {@link RelayLinkState} for the state machine. Stored as a {@code varchar(16)} string
   * with a {@code DEFAULT 'UNREGISTERED'} so rows that pre-date the relay integration
   * (or are created with the relay feature gate off) start in a sane state.
   */
  @Column(name = "relay_link_state", nullable = false)
  private String relayLinkState = RelayLinkState.UNREGISTERED;

  /**
   * Number of consecutive relay-registration attempts that have been made for this App.
   * Reset to {@code 0} on a successful registration and on every {@code FAILED -> ERROR}
   * sweep tick. See {@link RelayLinkState#MAX_ATTEMPTS} for the per-row cap.
   */
  @Column(name = "relay_link_attempts", nullable = false)
  private int relayLinkAttempts = 0;

  public GitHubApp() {
    // Default constructor for JPA
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getGithubOrganizationName() {
    return githubOrganizationName;
  }

  public void setGithubOrganizationName(String githubOrganizationName) {
    this.githubOrganizationName = githubOrganizationName;
  }

  public Integer getAppId() {
    return appId;
  }

  public void setAppId(Integer appId) {
    this.appId = appId;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }

  public Long getInstallationId() {
    return installationId;
  }

  public void setInstallationId(Long installationId) {
    this.installationId = installationId;
  }

  public Date getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public void setLastUpdatedAt(Date lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }

  public String getWebhookSecret() {
    return webhookSecret;
  }

  public void setWebhookSecret(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public String getRelayLinkState() {
    return relayLinkState;
  }

  public void setRelayLinkState(String relayLinkState) {
    this.relayLinkState = relayLinkState;
  }

  public int getRelayLinkAttempts() {
    return relayLinkAttempts;
  }

  public void setRelayLinkAttempts(int relayLinkAttempts) {
    this.relayLinkAttempts = relayLinkAttempts;
  }
}
