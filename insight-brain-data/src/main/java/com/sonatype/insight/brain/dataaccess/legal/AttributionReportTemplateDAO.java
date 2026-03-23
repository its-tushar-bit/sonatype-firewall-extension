/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Date;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.AttributionReportTemplate.ATTRIBUTION_REPORT_TEMPLATE;

/**
 * @since 1.120
 */
@Named
@Singleton
public class AttributionReportTemplateDAO
    extends AbstractOperationalSqlDAO<AttributionReportTemplate>
{
  @Inject
  public AttributionReportTemplateDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  private void checkAttributionReportTemplateNameLength(AttributionReportTemplate attributionReportTemplate) {
    if (attributionReportTemplate.getTemplateName().length() > 250) {
      throw new InvalidNameException("Report template name is too long");
    }
  }

  @Override
  public void insert(TransactionContext tx, AttributionReportTemplate entity) {
    checkAttributionReportTemplateNameLength(entity);
    if (entity.getLastUpdatedAt() == null) {
      entity.setLastUpdatedAt(new Date());
    }
    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, AttributionReportTemplate entity) {
    checkAttributionReportTemplateNameLength(entity);
    if (getById(tx, entity.getId()) == null) {
      throw new BadRequestException(
          "Cannot update attribution report template with id " + entity.getId() +
              " because it does not exist.");
    }
    entity.setLastUpdatedAt(new Date());
    super.update(tx, entity);
  }

  public void deleteById(String attributionReportId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .deleteFrom(ATTRIBUTION_REPORT_TEMPLATE)
          .where(ATTRIBUTION_REPORT_TEMPLATE.ATTRIBUTION_REPORT_TEMPLATE_ID.eq(attributionReportId))
          .execute();
      tx.commit();
    }
  }

  public AttributionReportTemplate getByTemplateName(String templateName) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(ATTRIBUTION_REPORT_TEMPLATE)
          .where(ATTRIBUTION_REPORT_TEMPLATE.TEMPLATE_NAME.eq(templateName))
          .fetchOne());
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return ATTRIBUTION_REPORT_TEMPLATE;
  }

  @Override
  public Class<AttributionReportTemplate> getEntityClass() {
    return AttributionReportTemplate.class;
  }
}
