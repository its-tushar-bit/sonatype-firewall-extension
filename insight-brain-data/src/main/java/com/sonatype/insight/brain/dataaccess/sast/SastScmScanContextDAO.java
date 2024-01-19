/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess.sast;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sast.SastScmScanContext;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class SastScmScanContextDAO
    extends AbstractOperationalSqlDAO<SastScmScanContext>
{
  @Inject
  public SastScmScanContextDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public void update(final TransactionContext tx, final SastScmScanContext entity) {
    throw new UnsupportedOperationException("The SastScmScanContext table does not support update operations");
  }
}
