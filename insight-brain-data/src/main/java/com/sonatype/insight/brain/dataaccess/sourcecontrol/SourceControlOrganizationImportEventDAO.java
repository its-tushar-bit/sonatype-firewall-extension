/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent;
import com.sonatype.insight.dataaccess.TransactionContext;

public class SourceControlOrganizationImportEventDAO
    extends AbstractOperationalSqlDAO<SourceControlOrganizationImportEvent>
{
  public SourceControlOrganizationImportEvent getByOrganizationAndEventId(final String orgId, final String eventId) {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT entity FROM SourceControlOrganizationImportEvent entity" + //
          " WHERE entity.id=?1 AND entity.organizationId=?2";
      return get(tx, sQuery, eventId, orgId);
    }
  }

  public List<SourceControlOrganizationImportEvent> getByOrganizationId(String orgId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOrganizationId(tx, orgId);
    }
  }

  public List<SourceControlOrganizationImportEvent> getByOrganizationId(
      final TransactionContext tx,
      final String orgId)
  {
    String sQuery = "SELECT entity FROM SourceControlOrganizationImportEvent entity" + //
        " WHERE entity.organizationId=?1";
    return getList(tx, sQuery, orgId);
  }
}
