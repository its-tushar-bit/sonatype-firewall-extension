/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.githubapp;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.security.RotatableSecrets;

/**
 * @since 1.201
 */
@Named
@Singleton
public class GitHubAppDAO
    extends AbstractOperationalSqlDAO<GitHubApp>
    implements RotatableSecrets
{
  @Inject
  public GitHubAppDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }
}
