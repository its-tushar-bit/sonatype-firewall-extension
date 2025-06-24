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

@Entity
@Table(name = "inner_source_application")
public class InnerSourceApplication
    implements HasStringId
{
  @Id
  @Column(name = "inner_source_application_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "package_url")
  private String packageUrl;

  // for JPA
  public InnerSourceApplication() {
  }

  public InnerSourceApplication(String applicationId, String packageUrl) {
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

  public void setApplicationId(final String applicationId) {
    this.applicationId = applicationId;
  }

  public String getPackageUrl() {
    return packageUrl;
  }

  public void setPackageUrl(final String packageUrl) {
    this.packageUrl = packageUrl;
  }

  @Override
  public String toString() {
    return "InnerSourceApplication{" +
        "id='" + id + '\'' +
        ", applicationId='" + applicationId + '\'' +
        ", packageUrl='" + packageUrl + '\'' +
        '}';
  }
}
