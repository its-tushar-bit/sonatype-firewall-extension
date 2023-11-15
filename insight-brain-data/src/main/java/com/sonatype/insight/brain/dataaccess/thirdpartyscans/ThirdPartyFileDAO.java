/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.dataaccess.TransactionContext;

public class ThirdPartyFileDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyFile>
{
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

  @Override
  public void delete(TransactionContext tx, ThirdPartyFile thirdPartyFile) {
    // cascade delete file coordinates
    new ThirdPartyFileCoordinateDAO().deleteByThirdPartyFileId(tx, thirdPartyFile.getId());

    // cascade delete scanned files
    new ThirdPartyScanDAO().deleteByThirdPartyFileId(tx, thirdPartyFile.getId());

    // lastly delete this entity
    super.delete(tx, thirdPartyFile);
  }
}
