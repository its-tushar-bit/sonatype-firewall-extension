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
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class ThirdPartySbomMetadataDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartySbomMetadata>
{
  @Inject
  public ThirdPartySbomMetadataDAO(ThirdPartyScansDataStore thirdPartyScansDataStore) {
    super(thirdPartyScansDataStore);
  }

  @Override
  public ThirdPartySbomMetadata getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  public List<ThirdPartySbomMetadata> getAll() {
    return getList("SELECT entity FROM ThirdPartySbomMetadata entity");
  }

  public ThirdPartySbomMetadata getByThirdPartyFileId(String thirdPartyFileId) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.thirdPartyFileId=?1";
    return get(sQuery, thirdPartyFileId);
  }

  public void deleteByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    String sQuery = "DELETE FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.thirdPartyFileId=?1";
    createQuery(sQuery, thirdPartyFileId).executeUpdate(tx);
  }

  public List<ThirdPartySbomMetadata> getByApplicationId(String applicationId) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.applicationId=?1";
    return getList(sQuery, applicationId);
  }

  public void deleteByApplicationId(TransactionContext tx, String applicationId) {
    String sQuery = "DELETE FROM ThirdPartySbomMetadata entity " //
        + " WHERE entity.applicationId=?1";
    createQuery(sQuery, applicationId).executeUpdate(tx);
  }
}
