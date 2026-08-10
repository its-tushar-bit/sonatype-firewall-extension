/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import java.util.Date;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class RepositoryIdentifiedComponentCacheLoaderTest
    extends AbstractComponentH2Test
{
  @Inject
  private RepositoryIdentifiedComponentCacheLoader repositoryIdentifiedComponentCacheLoader;

  private RepositoryIdentifiedComponentDAO spyRepositoryIdentifiedComponentDAO;

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
