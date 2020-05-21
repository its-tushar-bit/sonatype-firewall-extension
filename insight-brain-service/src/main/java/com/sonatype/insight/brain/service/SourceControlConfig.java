/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public class SourceControlConfig
{
  static final String DEFAULT_SOURCE_CONTROL_CLONE_DIR = "source-control";

  @Nullable
  private String cloneDirectory;

  private File sonatypeWorkDir;

  private String gitImplementation;

  /**
   * Purge window for PR comment records in days
   */
  private Integer prCommentPurgeWindow;

  /**
   * @since 1.83
   */
  private String gitExecutable;

  /**
   * Return the {@link #cloneDirectory} as a {@link File}. If not set will default to {@link
   * #DEFAULT_SOURCE_CONTROL_CLONE_DIR}. If {@link #cloneDirectory} is not a fully qualified path then it will be
   * created under the {@link #sonatypeWorkDir} which needs to be set with {@link #setCloneDirectory(String)}. Note that
   * this will happen automatically when called via {@link InsightConfig#getSourceControl()}.
   */
  public File getCloneDirectory() {
    if (StringUtils.isBlank(cloneDirectory)) {
      cloneDirectory = DEFAULT_SOURCE_CONTROL_CLONE_DIR;
    }

    File file = new File(cloneDirectory);
    if (!file.isAbsolute()) {
      file = new File(sonatypeWorkDir, cloneDirectory);
    }

    return file;
  }

  public void setCloneDirectory(final String cloneDirectory) {
    this.cloneDirectory = cloneDirectory;
  }

  public void setSonatypeWorkDir(final File sonatypeWorkDir) {
    this.sonatypeWorkDir = sonatypeWorkDir;
  }

  public String getGitImplementation() {
    return gitImplementation;
  }

  public void setGitImplementation(final String gitImplementation) {
    this.gitImplementation = gitImplementation;
  }

  public String getGitExecutable() {
    return gitExecutable;
  }

  public void setGitExecutable(final String gitExecutable) {
    this.gitExecutable = gitExecutable;
  }

  public Integer getPrCommentPurgeWindow() {
    return prCommentPurgeWindow;
  }

  public void setPrCommentPurgeWindow(final Integer prCommentPurgeWindow) {
    this.prCommentPurgeWindow = prCommentPurgeWindow;
  }
}
