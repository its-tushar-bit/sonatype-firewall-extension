/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "source_control_user")
public class SourceControlUser
    implements HasStringId
{
  @Id
  @Column(name = "source_control_user_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "email")
  private String email;

  public SourceControlUser() {
  }

  public SourceControlUser(final String applicationId, final String email) {
    this.applicationId = applicationId;
    this.email = email;
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

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }
}
