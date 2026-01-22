/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.telemetry;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.telemetry.ClusterIdentification;

@Named
@Singleton
public class ClusterIdentificationDAO
    extends AbstractOperationalSqlDAO<ClusterIdentification>
{
  @Inject
  public ClusterIdentificationDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }
}
