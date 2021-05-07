/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.dataaccess.TransactionContext;

public class SourceControlPullRequestDAO
    extends AbstractOperationalSqlDAO<SourceControlPullRequest>
{
  @Override
  public SourceControlPullRequest getById(TransactionContext tx, String id) {
    return get(tx, "SELECT entity FROM SourceControlPullRequest entity WHERE entity.id=?1", id);
  }

  public List<SourceControlPullRequest> getAll() {
    String sQuery = "SELECT entity FROM SourceControlPullRequest entity";
    return getList(sQuery);
  }

  void deleteByRepositoryUrl(TransactionContext tx, String repositoryUrl) {
    repositoryUrl = repositoryUrl.trim().toLowerCase(Locale.ENGLISH);
    getByRepositoryUrl(tx, repositoryUrl).forEach(entity -> delete(tx, entity));
  }

  private List<SourceControlPullRequest> getByRepositoryUrl(TransactionContext tx, String repositoryUrl) {
    repositoryUrl = repositoryUrl.trim().toLowerCase(Locale.ENGLISH);
    String sQuery = "SELECT entity FROM SourceControlPullRequest entity WHERE entity.repositoryUrl=?1";
    return getList(tx, sQuery, repositoryUrl);
  }

  /**
   * Returns the number of the records for which the last detected update time falls in the given date range.
   * At least the start or the end of the range must be specified i.e. not {@code null}.
   * @param startDate start of the date range; can be {@code null}, in which case the range has no left boundary
   * @param endDate end of the date range; can be {@code null}, in which case the range has no right boundary
   */
  public int getCountByUpdateTimeRange(Date startDate, Date endDate) {
    String sQuery = "SELECT COUNT(entity.id) FROM SourceControlPullRequest entity";
    if (startDate == null) {
      if (endDate == null) {
        throw new IllegalArgumentException("Either startDate or endDate must not be null.");
      }
      else {
        // endDate is provided
        sQuery += " WHERE entity.lastDetectedUpdateTime<?1";
        return getSingle(Long.class, sQuery, endDate).intValue();
      }
    }
    else {
      if (endDate == null) {
        // startDate is provided
        sQuery += " WHERE entity.lastDetectedUpdateTime>=?1";
        return getSingle(Long.class, sQuery, startDate).intValue();
      }
      else {
        // startDate and endDate are provided
        sQuery += " WHERE entity.lastDetectedUpdateTime>=?1 AND entity.lastDetectedUpdateTime<?2";
        return getSingle(Long.class, sQuery, startDate, endDate).intValue();
      }
    }
  }
}
