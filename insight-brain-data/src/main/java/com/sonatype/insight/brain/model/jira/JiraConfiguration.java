/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.jira;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.sonatype.insight.brain.security.RotatableSecret;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @since 1.139
 */
@Entity
@Table(name = "jira_configuration")
public class JiraConfiguration
    implements HasStringId
{
  @Id
  @Column(name = "jira_configuration_id")
  private String id;

  @Column(name = "url")
  private String url;

  @Column(name = "username")
  private String username;

  @RotatableSecret
  @Column(name = "password")
  private char[] password;

  @Column(name = "custom_fields_json")
  private String customFieldsJson;

  @Transient
  private Map<String, Object> customFields;

  public JiraConfiguration() {
  }

  public JiraConfiguration(
      final String url,
      final String username,
      final char[] password,
      final Map<String, Object> customFields)
  {
    this.url = url;
    this.username = username;
    this.password = password;
    setCustomFields(customFields);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public char[] getPassword() {
    return password;
  }

  public void setPassword(char[] password) {
    this.password = password;
  }

  public String getCustomFieldsJson() {
    return customFieldsJson;
  }

  public void setCustomFieldsJson(String customFieldsJson) {
    this.customFieldsJson = customFieldsJson;
  }

  public Map<String, Object> getCustomFields() {
    if (customFieldsJson == null) {
      return null;
    }
    if (customFields == null) {
      try {
        customFields = JsonUtils.parse(customFieldsJson, new TypeReference<Map<String, Object>>()
        {
        });
      }
      catch (IOException e) {
        throw new UncheckedIOException(e.getMessage(), e);
      }
    }
    return customFields;
  }

  public void setCustomFields(Map<String, Object> customFields) {
    this.customFields = customFields;
    if (this.customFields == null) {
      customFieldsJson = null;
    }
    else {
      customFieldsJson = JsonUtils.writeUnformatted(this.customFields);
    }
  }
}
