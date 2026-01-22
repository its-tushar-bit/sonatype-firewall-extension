/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class ThirdPartyScanDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyScan>
{
  @Inject
  public ThirdPartyScanDAO(final ThirdPartyScansDataStore thirdPartyScansDataStore) {
    super(thirdPartyScansDataStore);
  }

  @Override
  public ThirdPartyScan getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartyScan entity" + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  public List<ThirdPartyScan> getAll() {
    return getList("SELECT entity FROM ThirdPartyScan entity");
  }

  public ThirdPartyScan getByThirdPartyFileIdAndScanId(String thirdPartyFileId, String scanId) {
    String sQuery = "SELECT entity FROM ThirdPartyScan entity" + //
        " WHERE entity.thirdPartyFileId=?1 AND entity.scanId=?2";
    return get(sQuery, thirdPartyFileId, scanId);
  }

  public List<ThirdPartyScan> getByScanId(String scanId) {
    String sQuery = "SELECT entity FROM ThirdPartyScan entity WHERE entity.scanId=?1";
    return getList(sQuery, scanId);
  }

  public ThirdPartyScan getByThirdPartyFileId(String thirdPartyFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT entity FROM ThirdPartyScan entity WHERE entity.thirdPartyFileId=?1";
      return get(tx, sQuery, thirdPartyFileId);
    }
  }

  public List<ThirdPartyScan> getByScanRequestId(String scanRequestId) {
    String sQuery = "SELECT entity FROM ThirdPartyScan entity WHERE entity.scanRequestId=?1";
    return getList(sQuery, scanRequestId);
  }

  public ThirdPartyScan getSingleByScanRequestId(String scanRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      Query<ThirdPartyScan> query =
          createQuery("SELECT entity FROM ThirdPartyScan entity WHERE entity.scanRequestId=?1", scanRequestId);
      query.setMaxResults(1);
      return query.get(tx);
    }
  }

  public void updateScanIdForScanRequest(String scanRequestId, String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      String sQuery = "UPDATE ThirdPartyScan entity SET entity.scanId=?1 WHERE entity.scanRequestId=?2";
      Query<ThirdPartyScan> query = createQuery(sQuery, scanId, scanRequestId);
      query.executeUpdate(tx);
      tx.commit();
    }
  }

  public int deleteByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    String sQuery = "DELETE from ThirdPartyScan entity WHERE entity.thirdPartyFileId=?1";
    Query<ThirdPartyScan> query = createQuery(sQuery, thirdPartyFileId);
    return query.executeUpdate(tx);
  }
}
