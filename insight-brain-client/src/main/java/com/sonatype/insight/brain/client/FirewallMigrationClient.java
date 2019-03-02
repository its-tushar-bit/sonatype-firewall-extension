/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.clm.dto.model.repository.migration.MigrationDetails;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;

/**
 * @since 1.30
 */
public class FirewallMigrationClient
    extends AbstractRequestClient
{
  private static final String RESOURCE_PATH = "rest/integration/repositories/migration";

  private static final String SUPPORTED_PATH = "supported";

  private static final String HISTORY_PATH = "history";

  public FirewallMigrationClient(final Configuration config) {
    super(config);
  }

  public void verifyMigrationSupport(final String protocolVersion) throws IOException {
    verifyStatusCode(path(RESOURCE_PATH, SUPPORTED_PATH, protocolVersion).post(null));
  }

  public void migrateRepositoryHistory(String sourceRepositoryManagerInstanceId,
                                       String sourceRepositoryPublicId,
                                       String targetRepositoryManagerInstanceId,
                                       String targetRepositoryPublicId) throws IOException
  {
    Result result = path(RESOURCE_PATH, HISTORY_PATH, targetRepositoryManagerInstanceId, targetRepositoryPublicId)
        .query("sourceRepositoryManagerInstanceId", sourceRepositoryManagerInstanceId, "sourceRepositoryPublicId",
            sourceRepositoryPublicId)
        .post(null);
    verifyStatusCode(result);
  }

  public MigrationDetails getRepositoryMigrationState(String targetRepositoryManagerInstanceId,
                                                      String targetRepositoryPublicId) throws IOException
  {
    Result result =
        path(RESOURCE_PATH, HISTORY_PATH, targetRepositoryManagerInstanceId, targetRepositoryPublicId).get();
    return parseResult(result, MigrationDetails.class);
  }
}
