/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.innersource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.98
 * @deprecated use {@link InnerSourceApplication} and {@link InnerSourceVersion} instead
 */
@Entity
@Table(name = "inner_source_component")
@Deprecated
public class InnerSourceComponent
    implements HasStringId
{
  @Id
  @Column(name = "inner_source_component_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "package_url")
  private String packageUrl;

  @Column(name = "latest_version")
  private String latestVersion;

  // for JPA
  public InnerSourceComponent() {
  }

  public InnerSourceComponent(String applicationId, String packageUrl) {
    this.applicationId = applicationId;
    this.packageUrl = packageUrl;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(final String application) {
    this.applicationId = application;
  }

  public String getPackageUrl() {
    return packageUrl;
  }

  public void setPackageUrl(final String purl) {
    this.packageUrl = purl;
  }

  public String getLatestVersion() {
    return latestVersion;
  }

  public void setLatestVersion(String latestVersion) {
    this.latestVersion = latestVersion;
  }

  @Override
  public String toString() {
    return "InnerSourceComponent{" +
        "id='" + id + '\'' +
        ", applicationId='" + applicationId + '\'' +
        ", packageUrl='" + packageUrl + '\'' +
        ", latestVersion='" + latestVersion + '\'' +
        '}';
  }
}
