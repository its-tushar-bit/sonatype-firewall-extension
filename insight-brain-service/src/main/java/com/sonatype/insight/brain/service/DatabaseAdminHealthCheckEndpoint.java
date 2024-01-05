/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.operational.check.OdsDbOperationalCheck;

import com.codahale.metrics.health.HealthCheck.Result;
import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class DatabaseAdminHealthCheckEndpoint
    implements AdminHealthCheckEndpoint
{
  private final OperationalDataStore operationalDataStore;

  private final DataMartDataStore dataMartDataStore;

  private final AggregationDataStore aggregationDataStore;

  private final ThirdPartyScansDataStore thirdPartyScansDataStore;

  @Inject
  public DatabaseAdminHealthCheckEndpoint(
      final OperationalDataStore operationalDataStore,
      final DataMartDataStore dataMartDataStore,
      final AggregationDataStore aggregationDataStore,
      final ThirdPartyScansDataStore thirdPartyScansDataStore)
  {
    this.operationalDataStore = operationalDataStore;
    this.dataMartDataStore = dataMartDataStore;
    this.aggregationDataStore = aggregationDataStore;
    this.thirdPartyScansDataStore = thirdPartyScansDataStore;
  }

  @Override
  public String getName() {
    return "Database";
  }

  @Override
  public String getPath() {
    return "/healthcheck/database";
  }

  @Override
  public HealthCheckResponse getHealthCheckResponse() {
    String message = getDatabaseInformation(operationalDataStore.getDataSource());
    if (StringUtils.isBlank(message)) {
      message = getDatabaseInformation(dataMartDataStore.getDataSource());
      if (StringUtils.isBlank(message)) {
        message = getDatabaseInformation(aggregationDataStore.getDataSource());
        if (StringUtils.isBlank(message)) {
          message = getDatabaseInformation(thirdPartyScansDataStore.getDataSource());
          if (StringUtils.isBlank(message)) {
            return new HealthCheckResponse(true);
          }
        }
      }
    }
    return new HealthCheckResponse(false, message);
  }

  private String getDatabaseInformation(DataSource dataSource) {
    Result result = OdsDbOperationalCheck.checkConnection(dataSource);
    if (!result.isHealthy()) {
      return result.getMessage();
    }
    return null;
  }
}
