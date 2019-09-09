/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.dataaccess.TransactionContext;

public class ThirdPartyCoordinateSecurityDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyCoordinateSecurity>
{
  @Override
  public ThirdPartyCoordinateSecurity getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateSecurity entity" + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  public List<ThirdPartyCoordinateSecurity> getAll() {
    return getList("SELECT entity FROM ThirdPartyCoordinateSecurity entity");
  }

  public ThirdPartyCoordinateSecurity getByCoordinateFileIdAndRefId(String coordinateFileId, String refId) {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateSecurity entity" + //
        " WHERE entity.fileCoordinateId=?1 AND entity.refId=?2";
    return get(sQuery, coordinateFileId, refId);
  }

  public List<ThirdPartyCoordinateSecurity> getByFileCoordinateIds(List<String> fileCoordinateIdList) {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateSecurity entity" + //
        " WHERE entity.fileCoordinateId IN ?1";
    return getList(sQuery, fileCoordinateIdList);
  }

  public List<ThirdPartyCoordinateSecurity> getByFileCoordinateId(TransactionContext tx, String coordinateFileId) {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateSecurity entity" + //
        " WHERE entity.fileCoordinateId=?1";
    return getList(tx, sQuery, coordinateFileId);
  }

  public int deleteByFileCoordinateId(TransactionContext tx, String fileCoordinateId) {
    String sQuery = "DELETE from ThirdPartyCoordinateSecurity entity WHERE entity.fileCoordinateId=?1";
    Query<ThirdPartyCoordinateSecurity> query = createQuery(sQuery, fileCoordinateId);
    return query.executeUpdate(tx);
  }
}
