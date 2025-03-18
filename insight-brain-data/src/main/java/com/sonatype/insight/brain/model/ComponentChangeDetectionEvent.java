/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Date;
import java.util.Objects;

import com.sonatype.insight.json.store.ISODateSerializer;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "component_change_detection_event")
public class ComponentChangeDetectionEvent
    implements HasStringId
{
  @Id
  @Column(name = "component_change_detection_event_id")
  private String id;

  @Column(name = "purl")
  private String purl;

  @Column(name = "component_evaluation_data")
  private String componentEvaluationData;

  @Column(name = "added_time")
  @JsonSerialize(using = ISODateSerializer.class)
  private Date addedTime;

  public ComponentChangeDetectionEvent() {
  }

  public ComponentChangeDetectionEvent(String purl, String componentEvaluationData, Date addedTime) {
    this.purl = purl;
    this.componentEvaluationData = componentEvaluationData;
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

  public String getPurl() {
    return purl;
  }

  public void setPurl(String purl) {
    this.purl = purl;
  }

  public String getComponentEvaluationData() {
    return componentEvaluationData;
  }

  public void setComponentEvaluationData(String componentEvaluationData) {
    this.componentEvaluationData = componentEvaluationData;
  }

  public Date getAddedTime() {
    return addedTime;
  }

  public void setAddedTime(Date addedTime) {
    this.addedTime = addedTime;
  }

  @Override
  public String toString() {
    return "ComponentChangeDetectionEvent{" +
        "id='" + id + '\'' +
        ", purl='" + purl + '\'' +
        ", componentEvaluationData='" + componentEvaluationData + '\'' +
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
    ComponentChangeDetectionEvent that = (ComponentChangeDetectionEvent) o;
    return Objects.equals(id, that.id) && Objects.equals(purl, that.purl) &&
        Objects.equals(componentEvaluationData, that.componentEvaluationData) &&
        Objects.equals(addedTime, that.addedTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, purl, componentEvaluationData, addedTime);
  }
}
