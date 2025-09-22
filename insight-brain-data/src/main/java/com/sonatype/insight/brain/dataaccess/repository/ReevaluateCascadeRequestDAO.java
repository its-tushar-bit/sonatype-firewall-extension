/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data access object for managing cascade re-evaluation requests.
 * 
 * @since 1.196
 */
@Named
@Singleton
public class ReevaluateCascadeRequestDAO extends AbstractOperationalSqlDAO<ReevaluateCascadeRequest>
{
  private static final Logger log = LoggerFactory.getLogger(ReevaluateCascadeRequestDAO.class);

  @Inject
  public ReevaluateCascadeRequestDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Finds active cascade requests for the given component hash.
   * Used to prevent duplicate cascade operations.
   */
  public List<ReevaluateCascadeRequest> getByComponentHash(final String componentHash) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByComponentHash(tx, componentHash);
    }
  }

  /**
   * Finds active cascade requests for the given component hash.
   * Used to prevent duplicate cascade operations.
   */
  public List<ReevaluateCascadeRequest> getByComponentHash(final TransactionContext tx, final String componentHash) {
    String sQuery = "SELECT entity FROM ReevaluateCascadeRequest entity" + 
        " WHERE entity.componentReferenceHash=?1";
    return getList(tx, sQuery, componentHash);
  }
}
