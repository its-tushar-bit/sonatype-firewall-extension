/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.140
 */
@Entity
@Table(name = "source_control_configuration")
public class SourceControlConfiguration
    implements HasStringId
{
  public static final String DEFAULT_SOURCE_CONTROL_CLONE_DIR = "source-control";

  public static final String DEFAULT_BRANCH_MONITORING_START_TIME = "00:00";

  public static final int DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS = 24;

  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

  public static final int DEFAULT_PULL_REQUEST_MONITORING_INTERVAL_SECONDS = 60;

  @Id
  @Column(name = "source_control_configuration_id")
  private String id;

  @Column(name = "clone_directory")
  private String cloneDirectory = DEFAULT_SOURCE_CONTROL_CLONE_DIR;

  @Column(name = "git_implementation")
  @Enumerated(EnumType.STRING)
  private GitImplementation gitImplementation;

  @Column(name = "pr_comment_purge_window")
  private Integer prCommentPurgeWindow;

  @Column(name = "pr_event_purge_window")
  private Integer prEventPurgeWindow;

  @Column(name = "git_executable")
  private String gitExecutable;

  @Column(name = "git_timeout_seconds")
  private int gitTimeoutSeconds;

  @Column(name = "commit_username")
  private String commitUsername;

  @Column(name = "commit_email")
  private String commitEmail;

  @Column(name = "use_username_in_repository_clone_url")
  private boolean useUsernameInRepositoryCloneUrl;

  @Column(name = "default_branch_monitoring_start_time")
  private String defaultBranchMonitoringStartTimeString;

  @Column(name = "default_branch_monitoring_interval_hours")
  private int defaultBranchMonitoringIntervalHours = DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS;

  @Column(name = "pull_request_monitoring_interval_seconds")
  private int pullRequestMonitoringIntervalSeconds = DEFAULT_PULL_REQUEST_MONITORING_INTERVAL_SECONDS;

  @Column(name = "gpg_signing_key")
  private String gpgSigningKey;

  @Column(name = "gpg_passphrase")
  private String gpgPassphrase;

  @Transient
  private LocalTime defaultBranchMonitoringStartTime;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getCloneDirectory() {
    return cloneDirectory;
  }

  public void setCloneDirectory(String cloneDirectory) {
    this.cloneDirectory = cloneDirectory;
  }

  public GitImplementation getGitImplementation() {
    return gitImplementation;
  }

  public void setGitImplementation(GitImplementation gitImplementation) {
    this.gitImplementation = gitImplementation;
  }

  public Integer getPrCommentPurgeWindow() {
    return prCommentPurgeWindow;
  }

  public void setPrCommentPurgeWindow(Integer prCommentPurgeWindow) {
    this.prCommentPurgeWindow = prCommentPurgeWindow;
  }

  public Integer getPrEventPurgeWindow() {
    return prEventPurgeWindow;
  }

  public void setPrEventPurgeWindow(Integer prEventPurgeWindow) {
    this.prEventPurgeWindow = prEventPurgeWindow;
  }

  public String getGitExecutable() {
    return gitExecutable;
  }

  public void setGitExecutable(String gitExecutable) {
    this.gitExecutable = gitExecutable;
  }

  public int getGitTimeoutSeconds() {
    return gitTimeoutSeconds;
  }

  public void setGitTimeoutSeconds(int gitTimeoutSeconds) {
    this.gitTimeoutSeconds = gitTimeoutSeconds;
  }

  public String getCommitUsername() {
    return commitUsername;
  }

  public void setCommitUsername(String commitUsername) {
    this.commitUsername = commitUsername;
  }

  public String getCommitEmail() {
    return commitEmail;
  }

  public void setCommitEmail(String commitEmail) {
    this.commitEmail = commitEmail;
  }

  public boolean isUseUsernameInRepositoryCloneUrl() {
    return useUsernameInRepositoryCloneUrl;
  }

  public void setUseUsernameInRepositoryCloneUrl(boolean useUsernameInRepositoryCloneUrl) {
    this.useUsernameInRepositoryCloneUrl = useUsernameInRepositoryCloneUrl;
  }

  public String getDefaultBranchMonitoringStartTimeString() {
    return defaultBranchMonitoringStartTimeString;
  }

  public void setDefaultBranchMonitoringStartTimeString(String defaultBranchMonitoringStartTimeString) {
    this.defaultBranchMonitoringStartTimeString = defaultBranchMonitoringStartTimeString;
    if (this.defaultBranchMonitoringStartTimeString == null) {
      defaultBranchMonitoringStartTime = null;
    }
    else {
      defaultBranchMonitoringStartTime =
          LocalTime.parse(this.defaultBranchMonitoringStartTimeString, DATE_TIME_FORMATTER);
    }
  }

  public int getDefaultBranchMonitoringIntervalHours() {
    return defaultBranchMonitoringIntervalHours;
  }

  public void setDefaultBranchMonitoringIntervalHours(int defaultBranchMonitoringIntervalHours) {
    this.defaultBranchMonitoringIntervalHours = defaultBranchMonitoringIntervalHours;
  }

  public LocalTime getDefaultBranchMonitoringStartTime() {
    if (defaultBranchMonitoringStartTimeString == null) {
      return null;
    }
    if (defaultBranchMonitoringStartTime == null) {
      defaultBranchMonitoringStartTime = LocalTime.parse(defaultBranchMonitoringStartTimeString, DATE_TIME_FORMATTER);
    }
    return defaultBranchMonitoringStartTime;
  }

  public void setDefaultBranchMonitoringStartTime(LocalTime defaultBranchMonitoringStartTime) {
    this.defaultBranchMonitoringStartTime = defaultBranchMonitoringStartTime;
    if (this.defaultBranchMonitoringStartTime == null) {
      defaultBranchMonitoringStartTimeString = null;
    }
    else {
      defaultBranchMonitoringStartTimeString = DATE_TIME_FORMATTER.format(this.defaultBranchMonitoringStartTime);
    }
  }

  public int getPullRequestMonitoringIntervalSeconds() {
    return pullRequestMonitoringIntervalSeconds;
  }

  public void setPullRequestMonitoringIntervalSeconds(int pullRequestMonitoringIntervalSeconds) {
    this.pullRequestMonitoringIntervalSeconds = pullRequestMonitoringIntervalSeconds;
  }

  public String getGpgSigningKey() {
    return gpgSigningKey;
  }

  public void setGpgSigningKey(String gpgSigningKey) {
    this.gpgSigningKey = gpgSigningKey;
  }

  public String getGpgPassphrase() {
    return gpgPassphrase;
  }

  public void setGpgPassphrase(String gpgPassphrase) {
    this.gpgPassphrase = gpgPassphrase;
  }
}
