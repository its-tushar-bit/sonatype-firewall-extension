/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;

public class ThirdPartyFileCoordinateDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyFileCoordinate>
{
  @Override
  public ThirdPartyFileCoordinate getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  public ThirdPartyFileCoordinate getBySourceFormatNameVersionAndThirdPartyFileId(
      String source,
      String format,
      String name,
      String version,
      String thirdPartyFileId)
  {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.source=?1 AND entity.format=?2 AND entity.name=?3" + //
        " AND entity.version=?4 AND entity.thirdPartyFileId=?5";
    return get(sQuery, source, format, name, version, thirdPartyFileId);
  }

  public List<ThirdPartyFileCoordinate> getAll() {
    return getList("SELECT entity FROM ThirdPartyFileCoordinate entity");
  }

  public List<ThirdPartyFileCoordinate> getByThirdPartyFileId(String thirdPartyFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByThirdPartyFileId(tx, thirdPartyFileId);
    }
  }

  public List<ThirdPartyFileCoordinate> getByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.thirdPartyFileId=?1";
    return getList(tx, sQuery, thirdPartyFileId);
  }

  public void deleteByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    List<ThirdPartyFileCoordinate> coordinateFiles = getByThirdPartyFileId(tx, thirdPartyFileId);
    coordinateFiles.forEach(entity -> delete(tx, entity));
  }

  @Override
  public void delete(TransactionContext tx, ThirdPartyFileCoordinate fileCoordinate) {
    new ThirdPartyCoordinateSecurityDAO().deleteByFileCoordinateId(tx, fileCoordinate.getId());
    super.delete(tx, fileCoordinate);
  }
}
