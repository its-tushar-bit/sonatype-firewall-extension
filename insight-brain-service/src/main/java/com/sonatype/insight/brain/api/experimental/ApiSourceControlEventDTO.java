/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Date;

public class ApiSourceControlEventDTO
{
  private String id;

  private String user;

  private String applicationId;

  private String type;

  private int priority;

  private String status;

  // Do not expose the status/error details, they may contain sensitive data.
  // This was flagged in a pen test.
  // See https://sonatype.atlassian.net/browse/CLM-29901 for details.
  // private String statusDetails;
  // private String errorDetails;

  private Date createTime;

  private Date startTime;

  private Date completeTime;

  private Long timeWaiting;

  private Long timeExecuting;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getCompleteTime() {
    return completeTime;
  }

  public void setCompleteTime(Date completeTime) {
    this.completeTime = completeTime;
  }

  public Long getTimeWaiting() {
    return timeWaiting;
  }

  public void setTimeWaiting(Long timeWaiting) {
    this.timeWaiting = timeWaiting;
  }

  public Long getTimeExecuting() {
    return timeExecuting;
  }

  public void setTimeExecuting(Long timeExecuting) {
    this.timeExecuting = timeExecuting;
  }

  @Override
  public String toString() {
    return "ApiSourceControlEventDTO{" +
        "id='" + id + '\'' +
        ", user='" + user + '\'' +
        ", applicationId='" + applicationId + '\'' +
        ", type='" + type + '\'' +
        ", priority=" + priority +
        ", status='" + status + '\'' +
        ", createTime=" + createTime +
        ", startTime=" + startTime +
        ", completeTime=" + completeTime +
        ", timeWaiting=" + timeWaiting +
        ", timeExecuting=" + timeExecuting +
        '}';
  }
}
