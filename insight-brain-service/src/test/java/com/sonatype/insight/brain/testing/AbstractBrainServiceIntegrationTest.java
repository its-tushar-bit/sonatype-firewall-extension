/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;

import org.junit.Rule;

/**
 * Base integration test class for regular single-tenant IQ. {@link TemporaryEntity} resides here to manipulate data for
 * the single tenant.
 */
public abstract class AbstractBrainServiceIntegrationTest
    extends AbstractBaseIntegrationTest
{
  @Rule(order = 2)
  public TemporaryEntity tempEntity = new TemporaryEntity(databaseContainerRule)
  {
    @Override
    public void after() {
      super.after();
      afterDatabaseReset();
    }
  };

  @Override
  public void setUpTestLicenseThreatGroups() {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
  }
}
