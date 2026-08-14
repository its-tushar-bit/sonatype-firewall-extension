/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryIdentifiedComponentCacheTest
    extends AbstractMultiTenantDatabaseTest
{
  private RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache;

  private RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO;

  @BeforeEach
  public void before() {
    repositoryIdentifiedComponentDAO = daoFactory.createRepositoryIdentifiedComponentDAO();

    RepositoryIdentifiedComponentCacheLoader repositoryIdentifiedComponentCacheLoader =
        new RepositoryIdentifiedComponentCacheLoader(repositoryIdentifiedComponentDAO);
    repositoryIdentifiedComponentCache =
        new RepositoryIdentifiedComponentCache(repositoryIdentifiedComponentCacheLoader,
            repositoryIdentifiedComponentDAO);
  }

  @Test
  public void testLoadingCache_PerTenant() {
    String hash1 = "hash1";
    ComponentIdentifier componentIdentifier1 =
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String hash2 = "hash2";
    ComponentIdentifier componentIdentifier2 =
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");

    Tenant tenant1 = testAsNewTenant(t1 -> testPut(hash1, componentIdentifier1));

    Tenant tenant2 = testAsNewTenant(t2 -> testPut(hash2, componentIdentifier2));

    TenantTestHelper.testAsTenantAndInvalidate(tenant1.tenantSlug, t1 -> {
      assertThat(repositoryIdentifiedComponentCache.get(hash1)).isNotNull();
      assertThat(repositoryIdentifiedComponentDAO.getByHash(hash1)).isNotNull();
      assertThat(repositoryIdentifiedComponentCache.get(hash2)).isNull();
      assertThat(repositoryIdentifiedComponentDAO.getByHash(hash2)).isNull();
    });

    TenantTestHelper.testAsTenantAndInvalidate(tenant2.tenantSlug, t2 -> {
      assertThat(repositoryIdentifiedComponentCache.get(hash1)).isNull();
      assertThat(repositoryIdentifiedComponentDAO.getByHash(hash1)).isNull();
      assertThat(repositoryIdentifiedComponentCache.get(hash2)).isNotNull();
      assertThat(repositoryIdentifiedComponentDAO.getByHash(hash2)).isNotNull();
    });
  }

  private void testPut(String hash, ComponentIdentifier componentIdentifier) {
    assertThat(repositoryIdentifiedComponentCache.get(hash)).isNull();
    assertThat(repositoryIdentifiedComponentDAO.getByHash(hash)).isNull();
    Date date = new Date();

    repositoryIdentifiedComponentCache.put(hash, componentIdentifier);

    assertThat(repositoryIdentifiedComponentCache.get(hash)).isEqualTo(componentIdentifier);
    RepositoryIdentifiedComponent repositoryIdentifiedComponent = repositoryIdentifiedComponentDAO.getByHash(hash);
    assertThat(repositoryIdentifiedComponent).isNotNull();
    assertThat(repositoryIdentifiedComponent.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(repositoryIdentifiedComponent.getCreateTime()).isAfterOrEqualTo(date);
    assertThat(repositoryIdentifiedComponent.getLastAccessTime()).isEqualTo(
        repositoryIdentifiedComponent.getCreateTime());
  }
}
