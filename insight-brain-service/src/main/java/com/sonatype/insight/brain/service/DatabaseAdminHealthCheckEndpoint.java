/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.brain.operational.check.OdsDbOperationalCheck;

import com.codahale.metrics.health.HealthCheck.Result;
import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class DatabaseAdminHealthCheckEndpoint
    implements AdminHealthCheckEndpoint
{
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
    String message = getDatabaseInformation(OperationalDataStoreProvider.getDataSource());
    if (StringUtils.isBlank(message)) {
      message = getDatabaseInformation(DatamartProvider.getDataSource());
      if (StringUtils.isBlank(message)) {
        message = getDatabaseInformation(AggregationDataStoreProvider.getDataSource());
        if (StringUtils.isBlank(message)) {
          message = getDatabaseInformation(ThirdPartyScansProvider.getDataSource());
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
