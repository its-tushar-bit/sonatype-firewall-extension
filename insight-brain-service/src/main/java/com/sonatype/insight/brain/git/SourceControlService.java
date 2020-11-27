/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class SourceControlService
{
  private final SourceControlPullRequestCommentDAO sourceControlPullRequestCommentDAO =
      new SourceControlPullRequestCommentDAO();

  private final SourceControlDefaultBranchCommitHistoryDAO commitHistoryDAO =
      new SourceControlDefaultBranchCommitHistoryDAO();

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  @Inject
  public SourceControlService() { }

  public void onRepositoryUrlUpdated(SourceControlEvent sourceControlEvent) {
    try (TransactionContext tx = sourceControlDAO.createTransactionContext()) {
      tx.begin();
      SourceControl sourceControl = sourceControlDAO.getByOwnerId(sourceControlEvent.getApplicationId());
      if (sourceControl != null) {
        sourceControlDAO.updatePollTimeAndErrorCounts(tx, sourceControl.getId(), sourceControlEvent.getCreateTime(), 0);
        sourceControlPullRequestCommentDAO.deleteByApplicationId(tx, sourceControl.getOwnerId());
        commitHistoryDAO.deleteByApplicationId(tx, sourceControl.getOwnerId());
      }
      tx.commit();
    }
  }
}
