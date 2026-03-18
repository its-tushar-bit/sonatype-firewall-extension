/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.webhook;

import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.25.0
 */
@Entity
@Table(name = "webhook")
public class Webhook
    implements HasStringId
{
  public static final String FAKE_SECRET_KEY = "#~FAKE~SECRET~KEY~#";

  @Id
  @Column(name = "webhook_id")
  private String id;

  @Column(name = "url")
  private String url;

  @Column(name = "secret_key")
  private String secretKey;

  @Column(name = "description")
  private String description;

  @ElementCollection(targetClass = WebhookEventType.class)
  @CollectionTable(name = "webhook_event_type", joinColumns = @JoinColumn(name = "webhook_id"))
  @Column(name = "event_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private Set<WebhookEventType> eventTypes;

  public Webhook() {
  }

  public Webhook(final String url, final String secretKey) {
    this(url, secretKey, null);
  }

  public Webhook(
      final String url,
      final String secretKey,
      final Set<WebhookEventType> eventTypes)
  {
    this(url, secretKey, eventTypes, null);
  }

  public Webhook(
      final String url,
      final String secretKey,
      final Set<WebhookEventType> eventTypes,
      final String description)
  {
    this.url = url;
    this.secretKey = secretKey;
    this.eventTypes = eventTypes;
    this.description = description;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(final String secretKey) {
    this.secretKey = secretKey;
  }

  public Set<WebhookEventType> getEventTypes() {
    return eventTypes;
  }

  public void setEventTypes(final Set<WebhookEventType> eventTypes) {
    this.eventTypes = eventTypes;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }
}
