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
  public void insert(TransactionContext tx, AttributionReportTemplate attributionReportTemplate) {
    checkAttributionReportTemplateNameLength(attributionReportTemplate);
    if (attributionReportTemplate.getLastUpdatedAt() == null) {
      attributionReportTemplate.setLastUpdatedAt(new Date());
    }
    super.insert(tx, attributionReportTemplate);
  }

  @Override
  public void update(TransactionContext tx, AttributionReportTemplate attributionReportTemplate) {
    checkAttributionReportTemplateNameLength(attributionReportTemplate);
    if (getById(tx, attributionReportTemplate.getId()) == null) {
      throw new BadRequestException(
          "Cannot update attribution report template with id " + attributionReportTemplate.getId() +
              " because it does not exist.");
    }
    attributionReportTemplate.setLastUpdatedAt(new Date());
    super.update(tx, attributionReportTemplate);
  }

  public void deleteById(String attributionReportId) {
    final String sQuery = "DELETE FROM AttributionReportTemplate entity WHERE entity.id=?1";
    createQuery(sQuery, attributionReportId).executeUpdate();
  }

  public AttributionReportTemplate getByTemplateName(String templateName) {
    final String sQuery = "SELECT entity FROM AttributionReportTemplate entity" + //
        " WHERE entity.templateName=?1";
    return get(sQuery, templateName);
  }
}
