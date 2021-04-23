/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.innersource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;

/**
 * @since 1.98
 */
public class InnerSourceComponentDAO
    extends AbstractOperationalSqlDAO<InnerSourceComponent>
{
  public static final String SELECT_ENTITY_FROM_INNER_SOURCE_COMPONENT =
      "SELECT entity FROM InnerSourceComponent entity";

  @Override
  public InnerSourceComponent getById(TransactionContext tx, String id) {
    String sQuery = SELECT_ENTITY_FROM_INNER_SOURCE_COMPONENT + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<InnerSourceComponent> getByApplicationId(TransactionContext tx, String appId) {
    String sQuery = SELECT_ENTITY_FROM_INNER_SOURCE_COMPONENT + //
        " WHERE entity.applicationId=?1";
    return getList(tx, sQuery, appId);
  }

  public List<InnerSourceComponent> getByApplicationId(String appId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, appId);
    }
  }

  public InnerSourceComponent getByPackageUrl(PackageUrlIdentifier packageUrl) {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = SELECT_ENTITY_FROM_INNER_SOURCE_COMPONENT + //
          " WHERE entity.packageUrl=?1";
      return get(tx, sQuery, packageUrl.getPackageUrl());
    }
  }

  public List<InnerSourceComponent> getByPackageUrls(Set<PackageUrlIdentifier> packageUrls) {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = SELECT_ENTITY_FROM_INNER_SOURCE_COMPONENT + //
          " WHERE entity.packageUrl IN (?1)";
      return getList(tx, sQuery,
          packageUrls.stream().map(PackageUrlIdentifier::getPackageUrl).collect(Collectors.toList()));
    }
  }
}
