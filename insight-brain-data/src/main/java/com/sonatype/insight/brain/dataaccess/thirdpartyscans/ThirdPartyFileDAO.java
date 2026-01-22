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
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class ThirdPartyFileDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyFile>
{
  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyUnknownComponentDAO thirdPartyUnknownComponentDAO;

  @Inject
  public ThirdPartyFileDAO(
      final ThirdPartyScansDataStore thirdPartyScansDataStore,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyUnknownComponentDAO thirdPartyUnknownComponentDAO)
  {
    super(thirdPartyScansDataStore);
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyUnknownComponentDAO = thirdPartyUnknownComponentDAO;
  }

  public List<ThirdPartyFile> getAll() {
    return getList("SELECT entity FROM ThirdPartyFile entity");
  }

  public List<ThirdPartyFile> getByScanId(String scanId) {
    String sQuery = "SELECT TPF FROM ThirdPartyFile TPF," + //
        " ThirdPartyScan TPS" + //
        " WHERE TPS.thirdPartyFileId=TPF.id AND TPS.scanId=?1";
    return getList(sQuery, scanId);
  }

  public void deleteByScanId(String scanId) {
    getByScanId(scanId).forEach(this::delete);
  }

  @Override
  public ThirdPartyFile getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartyFile entity" + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  // PMD incorrectly thinks this method needs an @Override annotation. Adding one does
  // not compile
  @SuppressWarnings("PMD")
  public void delete(TransactionContext tx, String thirdPartyFileId) {
    delete(tx, getById(thirdPartyFileId));
  }

  @Override
  public void delete(TransactionContext tx, ThirdPartyFile thirdPartyFile) {
    // cascade delete file coordinates
    thirdPartyFileCoordinateDAO.deleteByThirdPartyFileId(tx, thirdPartyFile.getId());

    // cascade delete sbom metadata
    thirdPartySbomMetadataDAO.deleteByThirdPartyFileId(tx, thirdPartyFile.getId());

    // cascade delete scanned files
    thirdPartyScanDAO.deleteByThirdPartyFileId(tx, thirdPartyFile.getId());

    // cascade delete unknown components
    thirdPartyUnknownComponentDAO.deleteByThirdPartyFileId(tx, thirdPartyFile.getId());

    // lastly delete this entity
    super.delete(tx, thirdPartyFile);
  }
}
