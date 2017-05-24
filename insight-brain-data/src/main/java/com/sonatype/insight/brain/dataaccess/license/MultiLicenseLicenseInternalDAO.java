/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.model.license.MultiLicenseLicenseInternal;
import com.sonatype.insight.dataaccess.TransactionContext;

public class MultiLicenseLicenseInternalDAO
    extends AbstractDatamartSqlDAO<MultiLicenseLicenseInternal>
{
  public List<MultiLicenseLicenseInternal> getByMultiLicenseId(TransactionContext tx, String multiLicenseId) {
    String sQuery = "SELECT entity FROM MultiLicenseLicenseInternal entity" + //
        " WHERE entity.multiLicenseId=?1";
    return getList(tx, sQuery, multiLicenseId);
  }
}
