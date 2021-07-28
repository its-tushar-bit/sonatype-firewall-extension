/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.120
 */
public class AttributionReportTemplateDAO
    extends AbstractOperationalSqlDAO<AttributionReportTemplate>
{
  @Override
  public AttributionReportTemplate getById(TransactionContext tx, String id) {
    final String sQuery = "SELECT entity FROM AttributionReportTemplate entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  @Override
  public void insert(TransactionContext tx, AttributionReportTemplate attributionReportTemplate) {
    if (attributionReportTemplate.getLastUpdatedAt() == null) {
      attributionReportTemplate.setLastUpdatedAt(new Date());
    }
    super.insert(tx, attributionReportTemplate);
  }

  @Override
  public void update(TransactionContext tx, AttributionReportTemplate attributionReportTemplate) {
    if (getById(tx, attributionReportTemplate.getId()) == null) {
      throw new BadRequestException(
          "Cannot update obligation report with id " + attributionReportTemplate.getId() +
              " because it does not exist.");
    }
    attributionReportTemplate.setLastUpdatedAt(new Date());
    super.update(tx, attributionReportTemplate);
  }

  public List<AttributionReportTemplate> getAll() {
    final String sQuery = "SELECT entity FROM AttributionReportTemplate entity";
    return getList(sQuery);
  }

  public void deleteById(String attributionReportId) {
    final String sQuery = "DELETE FROM AttributionReportTemplate entity WHERE entity.id=?1";
    createQuery(sQuery, attributionReportId).executeUpdate();
  }

  public AttributionReportTemplate getByTitle(String reportTemplateTitle) {
    final String sQuery = "SELECT entity FROM AttributionReportTemplate entity" + //
        " WHERE entity.documentTitle=?1";
    return get(sQuery, reportTemplateTitle);
  }
}
