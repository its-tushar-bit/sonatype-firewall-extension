/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Date;
import java.util.Objects;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "component_change_detection_configuration")
public class ComponentChangeDetectionConfiguration
    implements HasStringId
{
  @Id
  @Column(name = "component_change_detection_configuration_id")
  private String id;

  @Column(name = "version")
  private String version;

  @Column(name = "purl")
  private String purl;

  @Column(name = "component_hash")
  private String componentHash;

  @Column(name = "comparison_hash")
  private String comparisonHash;

  @Column(name = "added_time")
  private Date addedTime;

  public ComponentChangeDetectionConfiguration() {
  }

  public ComponentChangeDetectionConfiguration(
      final String version,
      final String purl,
      final String componentHash,
      final String comparisonHash,
      final Date addedTime)
  {
    this.version = version;
    this.purl = purl;
    this.componentHash = componentHash;
    this.comparisonHash = comparisonHash;
    this.addedTime = addedTime;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getPurl() {
    return purl;
  }

  public void setPurl(String purl) {
    this.purl = purl;
  }

  public String getComponentHash() {
    return componentHash;
  }

  public void setComponentHash(String componentHash) {
    this.componentHash = componentHash;
  }

  public String getComparisonHash() {
    return comparisonHash;
  }

  public void setComparisonHash(String comparisonHash) {
    this.comparisonHash = comparisonHash;
  }

  public Date getAddedTime() {
    return addedTime;
  }

  public void setAddedTime(Date addedTime) {
    this.addedTime = addedTime;
  }

  @Override
  public String toString() {
    return "ComponentChangeDetectionConfiguration{" +
        "id='" + id + '\'' +
        ", version='" + version + '\'' +
        ", purl='" + purl + '\'' +
        ", componentHash='" + componentHash + '\'' +
        ", comparisonHash='" + comparisonHash + '\'' +
        ", addedTime=" + addedTime +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentChangeDetectionConfiguration that = (ComponentChangeDetectionConfiguration) o;
    return Objects.equals(id, that.id) && Objects.equals(version, that.version) &&
        Objects.equals(purl, that.purl) && Objects.equals(componentHash, that.componentHash) &&
        Objects.equals(comparisonHash, that.comparisonHash) &&
        Objects.equals(addedTime, that.addedTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, version, purl, componentHash, comparisonHash, addedTime);
  }
}
