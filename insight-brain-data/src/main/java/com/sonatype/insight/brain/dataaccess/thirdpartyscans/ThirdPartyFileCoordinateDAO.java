/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class ThirdPartyFileCoordinateDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyFileCoordinate>
{
  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Inject
  public ThirdPartyFileCoordinateDAO(
      final ThirdPartyScansDataStore thirdPartyScansDataStore,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO)
  {
    super(thirdPartyScansDataStore);
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartyCoordinateLicenseDAO = thirdPartyCoordinateLicenseDAO;
  }

  @Override
  public ThirdPartyFileCoordinate getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  public List<ThirdPartyFileCoordinate> getBySourceFormatNameVersionAndThirdPartyFileId(
      String source,
      String format,
      String name,
      String version,
      String thirdPartyFileId)
  {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.source=?1 AND entity.format=?2 AND entity.name=?3" + //
        " AND entity.version=?4 AND entity.thirdPartyFileId=?5";
    return getList(sQuery, source, format, name, version, thirdPartyFileId);
  }

  public List<ThirdPartyFileCoordinate> getAll() {
    return getList("SELECT entity FROM ThirdPartyFileCoordinate entity");
  }

  public List<ThirdPartyFileCoordinate> getByThirdPartyFileId(String thirdPartyFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByThirdPartyFileId(tx, thirdPartyFileId);
    }
  }

  public List<ThirdPartyFileCoordinate> getByHashAndScanId(String hash, String scanId) {
    String sQuery = "SELECT TPF FROM ThirdPartyScan TPS," + //
        " ThirdPartyFileCoordinate TPF" + //
        " WHERE TPS.thirdPartyFileId=TPF.thirdPartyFileId AND TPF.hash=?1 AND TPS.scanId=?2";
    return getList(sQuery, hash, scanId);
  }

  public List<ThirdPartyFileCoordinate> getByScanId(String scanId) {
    String sQuery = "SELECT TPF FROM ThirdPartyScan TPS," + //
        " ThirdPartyFileCoordinate TPF" + //
        " WHERE TPS.thirdPartyFileId=TPF.thirdPartyFileId AND TPS.scanId=?1";
    return getList(sQuery, scanId);
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
    // cascade delete coordinate security records
    thirdPartyCoordinateSecurityDAO.deleteByFileCoordinateId(tx, fileCoordinate.getId());

    // cascade delete coordinate license records
    thirdPartyCoordinateLicenseDAO.deleteByFileCoordinateId(tx, fileCoordinate.getId());

    super.delete(tx, fileCoordinate);
  }
}
