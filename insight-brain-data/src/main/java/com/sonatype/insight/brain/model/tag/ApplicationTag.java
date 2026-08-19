/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * Represents a Tag that has been applied to an Application
 *
 * @since 1.9
 */
@Entity
@Table(name = "application_tag")
public class ApplicationTag
    implements HasStringId
{
  @Id
  @Column(name = "application_tag_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "tag_id")
  private String tagId;

  public ApplicationTag() {

  }

  public ApplicationTag(String applicationId, String tagId) {
    this.applicationId = applicationId;
    this.tagId = tagId;
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

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public String getTagId() {
    return tagId;
  }

  public void setTagId(String tagId) {
    this.tagId = tagId;
  }
}
