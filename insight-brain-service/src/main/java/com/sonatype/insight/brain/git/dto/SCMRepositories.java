/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.dto;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.git.ScmResultStatus;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import static com.sonatype.insight.brain.git.ScmResultStatus.SUCCESS;

/**
 * DTO object encapsulating a collection of SCM repositories that are not yet configured for this IQ server,
 * and some contextual information detailing how many of the available SCM repositories are already configured
 * for this IQ server.
 */
public class SCMRepositories
{
  /**
   * How many repositories are visible to this IQ instance.
   */
  public int totalRepositories;

  /**
   * All repositories that are not already configured for SCM in this IQ instance.
   */
  public List<SCMRepository> availableRepositories;

  /**
   * This enum contains the success or error code, respectively.
   */
  public ScmResultStatus status;

  public SCMRepositories() {
  }

  public SCMRepositories(
      final int totalRepositories,
      final List<SCMRepository> availableRepositories)
  {
    this.totalRepositories = totalRepositories;
    this.availableRepositories = availableRepositories;
    this.status = SUCCESS;
  }

  public SCMRepositories(final ScmResultStatus status) {
    this.status = status;
    this.availableRepositories = Collections.emptyList();
  }

  public int getTotalRepositories() {
    return totalRepositories;
  }

  public void setTotalRepositories(final int totalRepositories) {
    this.totalRepositories = totalRepositories;
  }

  public List<SCMRepository> getAvailableRepositories() {
    return availableRepositories;
  }

  public void setAvailableRepositories(final List<SCMRepository> availableRepositories) {
    this.availableRepositories = availableRepositories;
  }

  public ScmResultStatus getStatus() {
    return status;
  }
}
