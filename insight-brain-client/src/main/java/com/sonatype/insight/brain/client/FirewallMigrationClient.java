/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;

/**
 * @since 1.29
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
    verifyStatusCode(postRequest(path(RESOURCE_PATH, SUPPORTED_PATH, protocolVersion), null));
  }

  public void migrateRepositoryHistory(String repositoryManagerInstanceId,
                                       String repositoryPublicId,
                                       String sourceRepositoryManagerInstanceId,
                                       String sourceRepositoryPublicId,
                                       String lastMigratedPathname) throws IOException
  {
    Result result = postRequest(
        path(RESOURCE_PATH, HISTORY_PATH, repositoryManagerInstanceId, repositoryPublicId)
            .query("sourceRepositoryManagerInstanceId", sourceRepositoryManagerInstanceId, "sourceRepositoryPublicId",
                sourceRepositoryPublicId, "lastMigratedPathname", lastMigratedPathname), null);
    verifyStatusCode(result);
  }


  public MigrationState getRepositoryMigrationState(String repositoryManagerInstanceId, String repositoryPublicId)
      throws IOException
  {
    Result result = getRequest(
        path(RESOURCE_PATH, HISTORY_PATH, repositoryManagerInstanceId, repositoryPublicId));
    return parseResult(result, MigrationState.class);
  }
}
