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
@Table(name = "source_control_pull_request_result")
public class SourceControlPullRequestResult
    implements HasStringId
{
  @Id
  @Column(name = "source_control_pull_request_result_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "pull_request_result_json")
  private String pullRequestResultJson;

  public SourceControlPullRequestResult() {
  }

  public SourceControlPullRequestResult(String applicationId, String pullRequestResultJson) {
    this.applicationId = applicationId;
    this.pullRequestResultJson = pullRequestResultJson;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public String getPullRequestResultJson() {
    return pullRequestResultJson;
  }

  public void setPullRequestResultJson(String pullRequestResultJson) {
    this.pullRequestResultJson = pullRequestResultJson;
  }
}
