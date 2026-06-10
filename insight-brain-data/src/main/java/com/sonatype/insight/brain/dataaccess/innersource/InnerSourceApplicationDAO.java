/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.innersource;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import com.google.common.collect.Lists;
import org.jooq.Table;
import static java.util.stream.Collectors.toList;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.InnerSourceApplication.INNER_SOURCE_APPLICATION;

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
    return tx.dsl()
        .selectFrom(INNER_SOURCE_APPLICATION)
        .where(INNER_SOURCE_APPLICATION.APPLICATION_ID.eq(appId))
        .fetch(this::toEntity);
  }

  public List<InnerSourceApplication> getByApplicationId(String appId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, appId);
    }
  }

  public InnerSourceApplication getByPackageUrl(PackageUrlIdentifier packageUrl) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(INNER_SOURCE_APPLICATION)
          .where(INNER_SOURCE_APPLICATION.PACKAGE_URL.eq(packageUrl.getPackageUrl()))
          .fetchOne());
    }
  }

  public InnerSourceApplication getByPackageUrlExcludingApplication(
      PackageUrlIdentifier packageUrl,
      String excludedApplicationId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(INNER_SOURCE_APPLICATION)
          .where(INNER_SOURCE_APPLICATION.PACKAGE_URL.eq(packageUrl.getPackageUrl()))
          .and(INNER_SOURCE_APPLICATION.APPLICATION_ID.ne(excludedApplicationId))
          .fetchOne());
    }
  }

  public List<InnerSourceApplication> getByPackageUrls(Set<PackageUrlIdentifier> packageUrls) {
    List<String> urls = packageUrls.stream().map(PackageUrlIdentifier::getPackageUrl).toList();
    List<List<String>> partitions = Lists.partition(urls, getInOperatorThreshold());

    try (TransactionContext tx = createTransactionContext()) {
      return partitions.stream()
          .map(partition -> tx.dsl()
              .selectFrom(INNER_SOURCE_APPLICATION)
              .where(INNER_SOURCE_APPLICATION.PACKAGE_URL.in(partition))
              .fetch(this::toEntity))
          .flatMap(Collection::stream)
          .collect(toList());
    }
  }

  public void deleteByApplicationId(TransactionContext tx, String applicationId) {
    tx.dsl()
        .deleteFrom(INNER_SOURCE_APPLICATION)
        .where(INNER_SOURCE_APPLICATION.APPLICATION_ID.eq(applicationId))
        .execute();
  }

  public void deleteByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationId(tx, applicationId);
      tx.commit();
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return INNER_SOURCE_APPLICATION;
  }

  @Override
  public Class<InnerSourceApplication> getEntityClass() {
    return InnerSourceApplication.class;
  }
}
