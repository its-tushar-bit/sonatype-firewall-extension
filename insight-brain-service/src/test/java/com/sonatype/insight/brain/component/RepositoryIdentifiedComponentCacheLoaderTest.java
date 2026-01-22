/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Date;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@Category(SlowTest.class)
public class RepositoryIdentifiedComponentCacheLoaderTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryIdentifiedComponentCacheLoader repositoryIdentifiedComponentCacheLoader;

  private RepositoryIdentifiedComponentDAO spyRepositoryIdentifiedComponentDAO;

  @Override
  public void configure(Binder binder) {
    spyRepositoryIdentifiedComponentDAO = spy(daoFactory.createRepositoryIdentifiedComponentDAO());
    binder.bind(RepositoryIdentifiedComponentDAO.class).toInstance(spyRepositoryIdentifiedComponentDAO);
    super.configure(binder);
  }

  @Test
  public void testLoad_DoesNotExist() {
    String hash = "unknown";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> repositoryIdentifiedComponentCacheLoader.load(hash))
        .withMessageContaining("RepositoryIdentifiedComponent with hash " + hash + " does not exist.");
  }

  @Test
  public void testLoad() throws Exception {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent =
        tempEntity.newRepositoryIdentifiedComponent("hash",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), new Date(0), new Date(1));
    tempEntity.newRepositoryIdentifiedComponent("otherHash",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"), new Date(2), new Date(3));

    ComponentIdentifier componentIdentifier =
        repositoryIdentifiedComponentCacheLoader.load(repositoryIdentifiedComponent.getHash());

    assertThat(componentIdentifier).isEqualTo(repositoryIdentifiedComponent.getComponentIdentifier());
    verify(spyRepositoryIdentifiedComponentDAO).getByHashNotNullAndUpdateLastAccessTime(
        repositoryIdentifiedComponent.getHash());
  }
}
