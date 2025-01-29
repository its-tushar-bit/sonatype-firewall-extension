/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.artifactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.dataaccess.RotatableSecretsDAO;
import com.sonatype.insight.brain.security.RotatableSecrets;

@Named
@Singleton
public class ArtifactoryConnectionDAO
    extends RotatableSecretsDAO<ArtifactoryConnection>
    implements RotatableSecrets
{
  @Inject
  public ArtifactoryConnectionDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  private static final String SELECT_ENTITY = "SELECT entity FROM ArtifactoryConnection entity ";

  public ArtifactoryConnection getByOwnerId(String ownerId) {
    String sQuery = SELECT_ENTITY + "WHERE entity.ownerId=?1";
    return get(sQuery, ownerId);
  }

  public ArtifactoryConnection getByIdAndOwnerId(String artifactoryConnectionId, String ownerId) {
    String sQuery = SELECT_ENTITY + "WHERE entity.id=?1 AND entity.ownerId=?2";
    return get(sQuery, artifactoryConnectionId, ownerId);
  }
}
