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
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyUnknownComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class ThirdPartyUnknownComponentDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyUnknownComponent>
{
  @Inject
  public ThirdPartyUnknownComponentDAO(ThirdPartyScansDataStore thirdPartyScansDataStore) {
    super(thirdPartyScansDataStore);
  }

  @Override
  public ThirdPartyUnknownComponent getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartyUnknownComponent entity " + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  public List<ThirdPartyUnknownComponent> getByThirdPartyFileId(String thirdPartyFileId) {
    String sQuery = "SELECT entity FROM ThirdPartyUnknownComponent entity " + //
        " WHERE entity.thirdPartyFileId = ?1";
    return getList(sQuery, thirdPartyFileId);
  }

  public int deleteByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    String sQuery = "DELETE from ThirdPartyUnknownComponent entity WHERE entity.thirdPartyFileId=?1";
    Query<ThirdPartyUnknownComponent> query = createQuery(sQuery, thirdPartyFileId);
    return query.executeUpdate(tx);
  }
}
