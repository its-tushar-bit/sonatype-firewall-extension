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
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class ThirdPartyCoordinateSecurityDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyCoordinateSecurity>
{
  private final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  @Inject
  public ThirdPartyCoordinateSecurityDAO(
      final ThirdPartyScansDataStore thirdPartyScansDataStore,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO)
  {
    super(thirdPartyScansDataStore);
    this.thirdPartyVulnerabilityExploitabilityExchangeDAO = thirdPartyVulnerabilityExploitabilityExchangeDAO;
  }

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
        " WHERE entity.fileCoordinateId=?1 AND UPPER(entity.refId)=?2";
    return get(sQuery, coordinateFileId, refId.toUpperCase());
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

  public void deleteByFileCoordinateId(TransactionContext tx, String fileCoordinateId) {
    List<ThirdPartyCoordinateSecurity> coordinateSecurityFiles = getByFileCoordinateId(tx, fileCoordinateId);
    coordinateSecurityFiles.forEach(entity -> delete(tx, entity));
  }

  public List<ThirdPartyCoordinateSecurity> getByFileCoordinateId(final String fileCoordinateId) {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateSecurity entity" + //
        " WHERE entity.fileCoordinateId=?1";
    return getList(sQuery, fileCoordinateId);
  }

  @Override
  public void delete(TransactionContext tx, ThirdPartyCoordinateSecurity coordinateSecurity) {
    // cascade delete vulnerability exploitability exchanges records
    thirdPartyVulnerabilityExploitabilityExchangeDAO.deleteByCoordinateSecurityId(tx, coordinateSecurity.getId());

    // lastly delete this entity
    super.delete(tx, coordinateSecurity);
  }
}
