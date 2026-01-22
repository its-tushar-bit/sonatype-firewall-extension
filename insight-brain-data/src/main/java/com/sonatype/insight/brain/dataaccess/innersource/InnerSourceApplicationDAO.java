/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.innersource;

import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;

@Named
@Singleton
public class InnerSourceApplicationDAO
    extends AbstractOperationalSqlDAO<InnerSourceApplication>
{
  @Inject
  public InnerSourceApplicationDAO(
      OperationalDataStore operationalDataStore)
  {
    super(operationalDataStore);
  }

  public List<InnerSourceApplication> getByApplicationId(TransactionContext tx, String appId) {
    String sQuery = "SELECT entity FROM InnerSourceApplication entity WHERE entity.applicationId=?1";
    return getList(tx, sQuery, appId);
  }

  public List<InnerSourceApplication> getByApplicationId(String appId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, appId);
    }
  }

  public InnerSourceApplication getByPackageUrl(PackageUrlIdentifier packageUrl) {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT entity FROM InnerSourceApplication entity WHERE entity.packageUrl=?1";
      return get(tx, sQuery, packageUrl.getPackageUrl());
    }
  }

  public InnerSourceApplication getByPackageUrlExcludingApplication(
      PackageUrlIdentifier packageUrl,
      String excludedApplicationId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT entity FROM InnerSourceApplication entity" +
          " WHERE entity.packageUrl=?1 and entity.applicationId<>?2";
      return get(tx, sQuery, packageUrl.getPackageUrl(), excludedApplicationId);
    }
  }

  public List<InnerSourceApplication> getByPackageUrls(Set<PackageUrlIdentifier> packageUrls) {
    String sQuery = "SELECT entity FROM InnerSourceApplication entity WHERE entity.packageUrl IN (?1)";
    return getListWithSqlInClause(packageUrls.stream().map(PackageUrlIdentifier::getPackageUrl).toList(),
        inClauseValuesPartition -> getList(sQuery, inClauseValuesPartition));
  }
}
