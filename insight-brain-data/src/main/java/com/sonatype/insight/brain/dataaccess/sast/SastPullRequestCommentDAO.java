/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sast.SastPullRequestComment;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class SastPullRequestCommentDAO
    extends AbstractOperationalSqlDAO<SastPullRequestComment>
{
  @Inject
  public SastPullRequestCommentDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public SastPullRequestComment getByPullRequestUrl(final String pullRequestUrl) {
    // Will only find zero or one row due to the column unique constraint.
    final String sQuery = "SELECT entity FROM SastPullRequestComment entity WHERE entity.pullRequestUrl=?1";
    return get(sQuery, pullRequestUrl);
  }

  public SastPullRequestComment getBySastScanId(final String sastScanId) {
    // Will only find zero or one row due to the column unique constraint.
    try (final TransactionContext tx = createTransactionContext()) {
      return getBySastScanId(tx, sastScanId);
    }
  }

  public SastPullRequestComment getBySastScanId(final TransactionContext tx, final String sastScanId) {
    // Will only find zero or one row due to the column unique constraint.
    final String sQuery = "SELECT entity FROM SastPullRequestComment entity WHERE entity.sastScanId=?1";
    return get(tx, sQuery, sastScanId);
  }
}
