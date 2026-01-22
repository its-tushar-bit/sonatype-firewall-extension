/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.model.license.MultiLicenseLicenseInternal;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class MultiLicenseLicenseInternalDAO
    extends AbstractDatamartSqlDAO<MultiLicenseLicenseInternal>
{
  @Inject
  public MultiLicenseLicenseInternalDAO(final DataMartDataStore dataMartDataStore) {
    super(dataMartDataStore);
  }

  public List<MultiLicenseLicenseInternal> getByMultiLicenseId(TransactionContext tx, String multiLicenseId) {
    String sQuery = "SELECT entity FROM MultiLicenseLicenseInternal entity" + //
        " WHERE entity.multiLicenseId=?1";
    return getList(tx, sQuery, multiLicenseId);
  }
}
