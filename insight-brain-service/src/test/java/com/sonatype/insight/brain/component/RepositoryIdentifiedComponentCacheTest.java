/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryIdentifiedComponentCacheTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryIdentifiedComponentCacheLoader repositoryIdentifiedComponentCacheLoader;

  @Inject
  private RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache;

  @Inject
  private RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO;

  @After
  public void after() {
    repositoryIdentifiedComponentDAO.getAll().forEach(repositoryIdentifiedComponentDAO::delete);
    repositoryIdentifiedComponentCache.getLoadingCache().invalidateAll();
  }

  @Test
  public void testCreateLoadingCache() {
    RepositoryIdentifiedComponentCache spyRepositoryIdentifiedComponentCache = spy(repositoryIdentifiedComponentCache);
    CacheBuilder<Object, Object> spyCacheBuilder = spy(CacheBuilder.newBuilder());
    when(spyRepositoryIdentifiedComponentCache.newCacheBuilder()).thenReturn(spyCacheBuilder);

    LoadingCache<String, ComponentIdentifier> loadingCache = spyRepositoryIdentifiedComponentCache.createLoadingCache();

    assertThat(loadingCache).isNotNull();
    verify(spyCacheBuilder).expireAfterWrite(RepositoryIdentifiedComponentCache.MAX_AGE.toMillis(),
        TimeUnit.MILLISECONDS);
    verify(spyCacheBuilder).maximumSize(RepositoryIdentifiedComponentCache.MAXIMUM_SIZE);
    verify(spyCacheBuilder).build(repositoryIdentifiedComponentCacheLoader);
  }

  @Test
  public void testGet_DoesNotExist() {
    assertThat(repositoryIdentifiedComponentCache.get("unknown")).isNull();
  }

  @Test
  public void testGet() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent =
        tempEntity.newRepositoryIdentifiedComponent("hash",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), new Date(0), new Date(1));

    assertThat(repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent.getHash())).isEqualTo(
        repositoryIdentifiedComponent.getComponentIdentifier());
  }

  @Test
  public void testPut() {
    String hash = "hash";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
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

  @Test
  public void testPut_AlreadyExists() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent =
        tempEntity.newRepositoryIdentifiedComponent("hash",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), new Date(0), new Date(1));
    assertThat(repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent.getHash())).isEqualTo(
        repositoryIdentifiedComponent.getComponentIdentifier());
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    Date date = new Date();

    repositoryIdentifiedComponentCache.put(repositoryIdentifiedComponent.getHash(), componentIdentifier);

    assertThat(repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent.getHash())).isEqualTo(
        componentIdentifier);
    repositoryIdentifiedComponent =
        repositoryIdentifiedComponentDAO.getByHash(repositoryIdentifiedComponent.getHash());
    assertThat(repositoryIdentifiedComponent).isNotNull();
    assertThat(repositoryIdentifiedComponent.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(repositoryIdentifiedComponent.getCreateTime()).isAfterOrEqualTo(date);
    assertThat(repositoryIdentifiedComponent.getLastAccessTime()).isEqualTo(
        repositoryIdentifiedComponent.getCreateTime());
  }

  @Test
  public void testRemoveByHash() {
    repositoryIdentifiedComponentCache.getLoadingCache()
        .put("hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    repositoryIdentifiedComponentCache.getLoadingCache()
        .put("hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"));

    repositoryIdentifiedComponentCache.removeByHash("hash1");

    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().keySet()).containsExactly("hash2");
  }

  @Test
  public void testRemoveByComponentIdentifier() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    repositoryIdentifiedComponentCache.getLoadingCache().put("hash1", componentIdentifier1);
    repositoryIdentifiedComponentCache.getLoadingCache().put("hash2", componentIdentifier1);
    repositoryIdentifiedComponentCache.getLoadingCache().put("hash3", componentIdentifier2);

    repositoryIdentifiedComponentCache.removeByComponentIdentifier(componentIdentifier1);

    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().values()).containsExactly(
        componentIdentifier2);
  }
}
