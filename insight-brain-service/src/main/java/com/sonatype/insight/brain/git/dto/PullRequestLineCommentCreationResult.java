/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.git.dto;

import java.util.LinkedList;
import java.util.List;

import com.sonatype.insight.brain.git.PullRequestLineCommentDTO;

/**
 * Holds information about a PR line comments creation as it is collected/used during the PR commenting flow
 */
public class PullRequestLineCommentCreationResult
{
  private List<PullRequestLineCommentDTO> pullRequestLineCommentDtoList;

  private final List<Exception> exceptionList;

  public PullRequestLineCommentCreationResult() {
    pullRequestLineCommentDtoList = new LinkedList<>();
    exceptionList = new LinkedList<>();
  }

  public List<PullRequestLineCommentDTO> getPullRequestLineCommentDtoList() {
    return pullRequestLineCommentDtoList;
  }

  public List<Exception> getExceptionList() {
    return exceptionList;
  }

  public void setPullRequestLineCommentDtoList(final List<PullRequestLineCommentDTO> newPullRequestLineCommentDtoList) {
    this.pullRequestLineCommentDtoList = newPullRequestLineCommentDtoList;
  }

  public void addException(final Exception exception) {
    exceptionList.add(exception);
  }

  public boolean hasExceptions() {
    return !exceptionList.isEmpty();
  }
}
