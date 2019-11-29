/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.dataaccess.TransactionContext;

public class ThirdPartyCoordinateLicenseDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyCoordinateLicense>
{
  @Override
  public ThirdPartyCoordinateLicense getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateLicense entity" + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  public List<ThirdPartyCoordinateLicense> getByFileCoordinateId(TransactionContext tx, String coordinateFileId) {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateLicense entity" + //
        " WHERE entity.fileCoordinateId=?1";
    return getList(tx, sQuery, coordinateFileId);
  }

  public int deleteByFileCoordinateId(TransactionContext tx, String fileCoordinateId) {
    String sQuery = "DELETE from ThirdPartyCoordinateLicense entity WHERE entity.fileCoordinateId=?1";
    Query<ThirdPartyCoordinateLicense> query = createQuery(sQuery, fileCoordinateId);
    return query.executeUpdate(tx);
  }

  public List<ThirdPartyCoordinateLicense> getByFileCoordinateId(final String fileCoordinateId) {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateLicense entity" + //
        " WHERE entity.fileCoordinateId=?1";
    return getList(sQuery, fileCoordinateId);
  }
}
