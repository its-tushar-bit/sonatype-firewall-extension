/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.dao;

import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.model.security.TenantMetadata;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantMetadataDAOTest
    extends AbstractMultiTenantDatabaseTest
{
  private TenantMetadataDAO underTest;

  @Before
  public void before() {
    underTest = new TenantMetadataDAO(databaseRule.getOperationalDataStore());
  }

  @Test
  public void tenant_canWriteUniqueMetadata() {
    testAsNewTenant(tenant1 -> {
      TenantMetadata expected1 =
          new TenantMetadata("appId1", "appName1", "connId1", "connName1", "encKeyName", "orgId", "orgName");

      underTest.insert(expected1);

      testAsNewTenant(tenant2 -> {
        TenantMetadata expected2 =
            new TenantMetadata("appId2", "appName2", "connId2", "connName2", "encKeyName", "orgId", "orgName");

        underTest.insert(expected2);
        TenantMetadata actual2 = underTest.get();
        assertThat(actual2.getApplicationId()).isEqualTo("appId2");
      });

      TenantMetadata actual1 = underTest.get();

      assertThat(actual1.getApplicationId()).isEqualTo("appId1");
    });
  }
}
