/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.ComponentChangeDetectionEvent;

/**
 * @since 1.188.0
 */
@Named
@Singleton
public class ComponentChangeDetectionEventDAO
    extends AbstractOperationalSqlDAO<ComponentChangeDetectionEvent>
{
  @Inject
  protected ComponentChangeDetectionEventDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public void deleteEntriesOlderThan(Date time) {
    String sQuery = "DELETE FROM ComponentChangeDetectionEvent entity WHERE entity.addedTime < ?1";
    createQuery(sQuery, time).executeUpdate();
  }
}
