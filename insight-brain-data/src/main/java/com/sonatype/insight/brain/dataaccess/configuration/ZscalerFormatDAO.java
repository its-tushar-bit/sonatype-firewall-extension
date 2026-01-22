/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.ZscalerFormat;

@Named
@Singleton
public class ZscalerFormatDAO
    extends AbstractOperationalSqlDAO<ZscalerFormat>
{
  @Inject
  public ZscalerFormatDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public List<ZscalerFormat> getAll() {
    String sQuery = "SELECT entity FROM ZscalerFormat entity";
    return getList(sQuery);
  }
}
