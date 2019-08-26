/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.dataaccess.TransactionContext;

public class ThirdPartyScanDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyScan>
{
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

  public List<ThirdPartyScan> getByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    String sQuery = "SELECT entity FROM ThirdPartyScan entity" + //
        " WHERE entity.thirdPartyFileId=?1";
    return getList(tx, sQuery, thirdPartyFileId);
  }

  public int deleteByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    String sQuery = "DELETE from ThirdPartyScan entity WHERE entity.thirdPartyFileId=?1";
    Query<ThirdPartyScan> query = createQuery(sQuery, thirdPartyFileId);
    return query.executeUpdate(tx);
  }
}
