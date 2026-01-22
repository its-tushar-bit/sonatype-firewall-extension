/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess.sast;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sast.SastScmScanContext;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.git.utils.GitBranchNameValidator;
import com.sonatype.nexus.git.utils.InvalidBranchNameException;

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
  public void insert(TransactionContext tx, SastScmScanContext entity) {
    try {
      GitBranchNameValidator.validate(entity.getBranchName());
    }
    catch (InvalidBranchNameException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
    super.insert(tx, entity);
  }

  @Override
  public void update(final TransactionContext tx, final SastScmScanContext entity) {
    throw new UnsupportedOperationException("The SastScmScanContext table does not support update operations");
  }
}
