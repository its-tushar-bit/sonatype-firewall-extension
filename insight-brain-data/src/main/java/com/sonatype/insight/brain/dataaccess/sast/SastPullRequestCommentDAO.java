/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sast;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sast.SastPullRequestComment;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SastPullRequestComment.SAST_PULL_REQUEST_COMMENT;

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
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(SAST_PULL_REQUEST_COMMENT)
          .where(SAST_PULL_REQUEST_COMMENT.PULL_REQUEST_URL.eq(pullRequestUrl))
          .fetchOne());
    }
  }

  public SastPullRequestComment getBySastScanId(final String sastScanId) {
    // Will only find zero or one row due to the column unique constraint.
    try (final TransactionContext tx = createTransactionContext()) {
      return getBySastScanId(tx, sastScanId);
    }
  }

  public SastPullRequestComment getBySastScanId(final TransactionContext tx, final String sastScanId) {
    // Will only find zero or one row due to the column unique constraint.
    return toEntity(tx.dsl()
        .selectFrom(SAST_PULL_REQUEST_COMMENT)
        .where(SAST_PULL_REQUEST_COMMENT.SAST_SCAN_ID.eq(sastScanId))
        .fetchOne());
  }

  @Override
  public Table<?> getJooqTable() {
    return SAST_PULL_REQUEST_COMMENT;
  }

  @Override
  public Class<SastPullRequestComment> getEntityClass() {
    return SastPullRequestComment.class;
  }
}
