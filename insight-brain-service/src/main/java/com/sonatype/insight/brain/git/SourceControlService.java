/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class SourceControlService
{
  private final SourceControlPullRequestCommentDAO sourceControlPullRequestCommentDAO;

  private final SourceControlDefaultBranchCommitHistoryDAO commitHistoryDAO;

  private final SourceControlDAO sourceControlDAO;

  private final SourceControlUtils sourceControlUtils;

  @Inject
  public SourceControlService(
      final SourceControlPullRequestCommentDAO sourceControlPullRequestCommentDAO,
      final SourceControlDefaultBranchCommitHistoryDAO commitHistoryDAO,
      final SourceControlDAO sourceControlDAO,
      final SourceControlUtils sourceControlUtils)
  {
    this.sourceControlPullRequestCommentDAO = sourceControlPullRequestCommentDAO;
    this.commitHistoryDAO = commitHistoryDAO;
    this.sourceControlDAO = sourceControlDAO;
    this.sourceControlUtils = sourceControlUtils;
  }

  public void onRepositoryUrlUpdated(SourceControlEvent sourceControlEvent) {
    try (TransactionContext tx = sourceControlDAO.createTransactionContext()) {
      tx.begin();
      SourceControl sourceControl = sourceControlDAO.getByOwnerId(sourceControlEvent.getApplicationId());
      if (sourceControl != null) {
        sourceControlDAO.updatePollTimeAndErrorCounts(tx, sourceControl.getId(), sourceControlEvent.getCreateTime(), 0);
        sourceControlPullRequestCommentDAO.deleteByApplicationId(tx, sourceControl.getOwnerId());
        commitHistoryDAO.deleteByApplicationId(tx, sourceControl.getOwnerId());
        sourceControlUtils.deleteCheckoutDirectory(sourceControlEvent.getApplicationId());
      }
      tx.commit();
    }
  }
}
