/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControlOrganizationImportEvent.SOURCE_CONTROL_ORGANIZATION_IMPORT_EVENT;

@Named
@Singleton
public class SourceControlOrganizationImportEventDAO
    extends AbstractOperationalSqlDAO<SourceControlOrganizationImportEvent>
{
  @Inject
  public SourceControlOrganizationImportEventDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public SourceControlOrganizationImportEvent getByOrganizationAndEventId(final String orgId, final String eventId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(SOURCE_CONTROL_ORGANIZATION_IMPORT_EVENT)
          .where(SOURCE_CONTROL_ORGANIZATION_IMPORT_EVENT.SOURCE_CONTROL_ORGANIZATION_IMPORT_EVENT_ID.eq(eventId))
          .and(SOURCE_CONTROL_ORGANIZATION_IMPORT_EVENT.ORGANIZATION_ID.eq(orgId))
          .fetchOne());
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
    return tx.dsl()
        .selectFrom(SOURCE_CONTROL_ORGANIZATION_IMPORT_EVENT)
        .where(SOURCE_CONTROL_ORGANIZATION_IMPORT_EVENT.ORGANIZATION_ID.eq(orgId))
        .fetch(this::toEntity);
  }

  @Override
  public Table<?> getJooqTable() {
    return SOURCE_CONTROL_ORGANIZATION_IMPORT_EVENT;
  }

  @Override
  public Class<SourceControlOrganizationImportEvent> getEntityClass() {
    return SourceControlOrganizationImportEvent.class;
  }
}
