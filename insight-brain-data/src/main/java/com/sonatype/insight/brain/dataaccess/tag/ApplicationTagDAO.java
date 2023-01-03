/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomDetailDAO;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomDetail;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.9
 */
public class ApplicationTagDAO
    extends AbstractOperationalSqlDAO<ApplicationTag>
{
  @Override
  protected ApplicationTag getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ApplicationTag entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<ApplicationTag> getByApplicationId(String appId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, appId);
    }
  }

  public List<ApplicationTag> getByApplicationId(TransactionContext tx, String appId) {
    String sQuery = "SELECT entity FROM ApplicationTag entity" + //
        " WHERE entity.applicationId=?1";
    return getList(tx, sQuery, appId);
  }

  public ApplicationTag getByApplicationIdAndTagId(String appId, String tagId) {
    String sQuery = "SELECT entity FROM ApplicationTag entity" + //
        " WHERE entity.applicationId=?1 AND entity.tagId=?2";
    return get(sQuery, appId, tagId);
  }

  public List<ApplicationTag> getByTagId(String tagId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByTagId(tx, tagId);
    }
  }

  public List<ApplicationTag> getByTagId(TransactionContext tx, String tagId) {
    String sQuery = "SELECT entity FROM ApplicationTag entity" + //
        " WHERE entity.tagId=?1";
    return getList(tx, sQuery, tagId);
  }

  /**
   * Retrieve list of Tags applied to any Applications in an Organization.
   */
  public List<ApplicationTag> getByOrganizationId(String organizationId) {
    String sQuery = "SELECT appTag FROM ApplicationTag appTag, Tag tag" + //
        " WHERE appTag.tagId = tag.id AND tag.organizationId =?1";
    return getList(sQuery, organizationId);
  }

  /**
   * @since 1.35
   */
  public List<ApplicationTag> getAll() {
    String sQuery = "SELECT entity FROM ApplicationTag entity" ;
    return getList(sQuery);
  }

  /**
   * @since 1.152
   */
  @Override
  public void delete(TransactionContext tx, ApplicationTag applicationTag) {
    // Cascade to vulnerability custom detail
    VulnerabilityCustomDetailDAO vulnerabilityCustomDetailDAO = new VulnerabilityCustomDetailDAO();
    for (VulnerabilityCustomDetail vulnerabilityCustomDetail : vulnerabilityCustomDetailDAO
        .getByApplicationTagId(tx, applicationTag.getId())) {
      vulnerabilityCustomDetailDAO.delete(tx, vulnerabilityCustomDetail);
    }
    super.delete(tx, applicationTag);
  }
}
