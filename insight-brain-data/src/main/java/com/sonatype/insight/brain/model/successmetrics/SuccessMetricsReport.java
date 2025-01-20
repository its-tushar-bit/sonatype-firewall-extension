/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.successmetrics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.37
 */
@Entity
@Table(name = "success_metrics_report")
public class SuccessMetricsReport
    implements HasStringId
{
  @Id
  @Column(name = "success_metrics_report_id")
  private String id;

  @Column(name = "username")
  private String username;

  @Column(name = "name")
  private String name;

  @Column(name = "name_lowercase_no_whitespace")
  private String nameLowercaseNoWhitespace;

  @Column(name = "scope_json")
  private String scopeJson;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "include_latest_data")
  private boolean includeLatestData;

  public SuccessMetricsReport() {
  }

  public SuccessMetricsReport(final String name) {
    setName(name);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    nameLowercaseNoWhitespace = NameHelper.normalize(name);
    this.name = name;
  }

  public String getNameLowercaseNoWhitespace() {
    return nameLowercaseNoWhitespace;
  }

  /**
   * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
   * nameLowercaseNoWhitespace field. If this method is not defined, jackson will set/access the
   * nameLowercaseNoWhitespace field directly via reflection, possibly setting it to an incorrect value.
   *
   * @deprecated This method should not be used explicitly.
   */
  @Deprecated
  @SuppressWarnings("unused")
  private void setNameLowercaseNoWhitespace(String nameLowercaseNoWhitespace) {
  }

  public String getScopeJson() {
    return scopeJson;
  }

  public void setScopeJson(final String scopeJson) {
    this.scopeJson = scopeJson;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Set<String> getScopeApplicationIds() {
    return getScope().applicationIds;
  }

  public Set<String> getScopeOrganizationIds() {
    return getScope().organizationIds;
  }

  private static class Scope
  {
    public Set<String> applicationIds;

    public Set<String> organizationIds;
  }

  private Scope getScope() {
    try {
      return JsonUtils.parse(scopeJson, Scope.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException("Failed to read scope for success metrics report " + id, e);
    }
  }

  public boolean getIncludeLatestData() {
    return includeLatestData;
  }

  public void setIncludeLatestData(boolean includeLatestData) {
    this.includeLatestData = includeLatestData;
  }
}
