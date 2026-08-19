/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.innersource;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inner_source_version")
public class InnerSourceVersion
    implements HasStringId
{
  @Id
  @Column(name = "inner_source_version_id")
  private String id;

  @Column(name = "inner_source_application_id")
  private String innerSourceApplicationId;

  @Column(name = "latest_version")
  private String latestVersion;

  @Column(name = "stage_type_id")
  private String stageTypeId;

  // for JPA
  public InnerSourceVersion() {
  }

  public InnerSourceVersion(String innerSourceApplicationIdId, String latestVersion, String stageTypeId) {
    this.innerSourceApplicationId = innerSourceApplicationIdId;
    this.latestVersion = latestVersion;
    this.stageTypeId = stageTypeId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getInnerSourceApplicationId() {
    return innerSourceApplicationId;
  }

  public void setInnerSourceApplicationId(String innerSourceApplicationId) {
    this.innerSourceApplicationId = innerSourceApplicationId;
  }

  public String getLatestVersion() {
    return latestVersion;
  }

  public void setLatestVersion(String latestVersion) {
    this.latestVersion = latestVersion;
  }

  public String getStageTypeId() {
    return stageTypeId;
  }

  public void setStageTypeId(String stageTypeId) {
    this.stageTypeId = stageTypeId;
  }

  @Override
  public String toString() {
    return "InnerSourceVersion{" +
        "id='" + id + '\'' +
        ", innerSourceApplicationId='" + innerSourceApplicationId + '\'' +
        ", latestVersion='" + latestVersion + '\'' +
        ", stageTypeId='" + stageTypeId + '\'' +
        '}';
  }
}
