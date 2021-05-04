/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.List;

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
}
