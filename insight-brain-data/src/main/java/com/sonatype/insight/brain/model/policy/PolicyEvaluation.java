/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.clm.dto.model.policy.Stage;

public class PolicyEvaluation
{
  private Stage stage;

  private String scanId;

  private boolean isReevaluation;

  private boolean isForMonitoring;

  private long time;

  private String user;

  public PolicyEvaluation() {
  }

  public PolicyEvaluation(Stage stage, String scanId) {
    this(stage, scanId, false /* isReevaluation */, false /* isForMonitoring */);
  }

  public PolicyEvaluation(Stage stage, String scanId, boolean isReevaluation) {
    this(stage, scanId, isReevaluation, false /* isForMonitoring */);
  }

  public PolicyEvaluation(Stage stage, String scanId, boolean isReevaluation, boolean isForMonitoring) {
    this.stage = stage;
    this.scanId = scanId;
    this.isReevaluation = isReevaluation;
    this.isForMonitoring = isForMonitoring;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public Stage getStage() {
    return stage;
  }

  public void setStage(Stage stage) {
    this.stage = stage;
  }

  public String getScanId() {
    return scanId;
  }

  public void setScanId(String scanId) {
    this.scanId = scanId;
  }

  public boolean isReevaluation() {
    return isReevaluation;
  }

  public void setReevaluation(boolean isReevaluation) {
    this.isReevaluation = isReevaluation;
  }

  public boolean isForMonitoring() {
    return isForMonitoring;
  }

  public void setForMonitoring(boolean isForMonitoring) {
    this.isForMonitoring = isForMonitoring;
  }
}
