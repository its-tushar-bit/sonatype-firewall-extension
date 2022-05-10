/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.RepositoryIdentifiedComponentCache;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.model.component.RepositoryIdentifiedComponent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiRepositoryIdentifiedComponentServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiRepositoryIdentifiedComponentService repositoryIdentifiedComponentService;

  @Inject
  private RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO;

  @Inject
  private RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache;

  @After
  public void after() {
    repositoryIdentifiedComponentCache.getLoadingCache().invalidateAll();
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_TwoOrMoreNotNull() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent("hash",
                componentIdentifier, PackageUrlIdentifier.toPackageUrl(componentIdentifier)))
        .withMessageContaining("Only one of either hash or componentIdentifier or packageUrl must be specified.");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent("hash",
                componentIdentifier, null))
        .withMessageContaining("Only one of either hash or componentIdentifier or packageUrl must be specified.");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null,
                componentIdentifier, PackageUrlIdentifier.toPackageUrl(componentIdentifier)))
        .withMessageContaining("Only one of either hash or componentIdentifier or packageUrl must be specified.");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent("hash",
                null, PackageUrlIdentifier.toPackageUrl(componentIdentifier)))
        .withMessageContaining("Only one of either hash or componentIdentifier or packageUrl must be specified.");
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_AllNull() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null, null))
        .withMessageContaining("You must specify one of either hash or componentIdentifier or packageUrl.");
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Hash_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent("doesNotExist", null, null))
        .withMessageContaining("Repository identified component with hash doesNotExist was not found.");
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Hash_OnlyInDatabase() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    RepositoryIdentifiedComponent repositoryIdentifiedComponent2 = tempEntity.newRepositoryIdentifiedComponent();

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(repositoryIdentifiedComponent1.getHash(),
        null, null);

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getHash)
        .containsExactly(repositoryIdentifiedComponent2.getHash());
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Hash_OnlyInMemory() {
    repositoryIdentifiedComponentCache.getLoadingCache().asMap()
        .put("hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));
    repositoryIdentifiedComponentCache.getLoadingCache().asMap()
        .put("hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"));

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent("hash1", null, null);

    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().keySet()).containsExactly("hash2");
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Hash() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    RepositoryIdentifiedComponent repositoryIdentifiedComponent2 = tempEntity.newRepositoryIdentifiedComponent();
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent1.getHash());
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent2.getHash());

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(repositoryIdentifiedComponent1.getHash(),
        null, null);

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getHash)
        .containsExactly(repositoryIdentifiedComponent2.getHash());
    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().keySet()).containsExactly(
        repositoryIdentifiedComponent2.getHash());
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_ComponentIdentifier_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null,
                ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), null))
        .withMessageContaining("Repository identified components with component identifier " +
            "maven: {artifactId=a, classifier=c, extension=e, groupId=g, version=v} were not found.");
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_ComponentIdentifier_OnlyInDatabase() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    tempEntity.newRepositoryIdentifiedComponent(repositoryIdentifiedComponent1.getComponentIdentifier());
    RepositoryIdentifiedComponent repositoryIdentifiedComponent3 = tempEntity.newRepositoryIdentifiedComponent();

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null,
        repositoryIdentifiedComponent1.getComponentIdentifier(), null);

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getComponentIdentifier)
        .containsExactly(repositoryIdentifiedComponent3.getComponentIdentifier());
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_ComponentIdentifier_OnlyInMemory() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    repositoryIdentifiedComponentCache.getLoadingCache().asMap().put("hash1", componentIdentifier1);
    repositoryIdentifiedComponentCache.getLoadingCache().asMap().put("hash2", componentIdentifier1);
    repositoryIdentifiedComponentCache.getLoadingCache().asMap().put("hash3", componentIdentifier2);

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, componentIdentifier1, null);

    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().values()).containsExactly(
        componentIdentifier2);
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_ComponentIdentifier() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    RepositoryIdentifiedComponent repositoryIdentifiedComponent2 =
        tempEntity.newRepositoryIdentifiedComponent(repositoryIdentifiedComponent1.getComponentIdentifier());
    RepositoryIdentifiedComponent repositoryIdentifiedComponent3 = tempEntity.newRepositoryIdentifiedComponent();
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent1.getHash());
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent2.getHash());
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent3.getHash());

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null,
        repositoryIdentifiedComponent1.getComponentIdentifier(), null);

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getComponentIdentifier)
        .containsExactly(repositoryIdentifiedComponent3.getComponentIdentifier());
    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().values()).containsExactly(
        repositoryIdentifiedComponent3.getComponentIdentifier());
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Purl_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null,
                PackageUrlIdentifier.toPackageUrl(ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"))))
        .withMessageContaining("Repository identified components with component identifier " +
            "maven: {artifactId=a, classifier=c, extension=e, groupId=g, version=v} were not found.");
  }

  @Test
  public void testBadPurl() {
    assertThatExceptionOfType(InvalidPackageURLException.class).isThrownBy(
        () -> repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null, "badPurl"));
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Purl_OnlyInDatabase() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    tempEntity.newRepositoryIdentifiedComponent(repositoryIdentifiedComponent1.getComponentIdentifier());
    RepositoryIdentifiedComponent repositoryIdentifiedComponent3 = tempEntity.newRepositoryIdentifiedComponent();

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null,
        PackageUrlIdentifier.toPackageUrl(repositoryIdentifiedComponent1.getComponentIdentifier()));

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getComponentIdentifier)
        .containsExactly(repositoryIdentifiedComponent3.getComponentIdentifier());
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Purl_OnlyInMemory() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    repositoryIdentifiedComponentCache.getLoadingCache().asMap().put("hash1", componentIdentifier1);
    repositoryIdentifiedComponentCache.getLoadingCache().asMap().put("hash2", componentIdentifier1);
    repositoryIdentifiedComponentCache.getLoadingCache().asMap().put("hash3", componentIdentifier2);

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null,
        PackageUrlIdentifier.toPackageUrl(componentIdentifier1));

    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().values()).containsExactly(
        componentIdentifier2);
  }

  @Test
  public void testDeleteRepositoryIdentifiedComponent_Purl() {
    RepositoryIdentifiedComponent repositoryIdentifiedComponent1 = tempEntity.newRepositoryIdentifiedComponent();
    RepositoryIdentifiedComponent repositoryIdentifiedComponent2 =
        tempEntity.newRepositoryIdentifiedComponent(repositoryIdentifiedComponent1.getComponentIdentifier());
    RepositoryIdentifiedComponent repositoryIdentifiedComponent3 = tempEntity.newRepositoryIdentifiedComponent();
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent1.getHash());
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent2.getHash());
    repositoryIdentifiedComponentCache.get(repositoryIdentifiedComponent3.getHash());

    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null,
        null, PackageUrlIdentifier.toPackageUrl(repositoryIdentifiedComponent1.getComponentIdentifier()));

    assertThat(repositoryIdentifiedComponentDAO.getAll()).map(RepositoryIdentifiedComponent::getComponentIdentifier)
        .containsExactly(repositoryIdentifiedComponent3.getComponentIdentifier());
    assertThat(repositoryIdentifiedComponentCache.getLoadingCache().asMap().values()).containsExactly(
        repositoryIdentifiedComponent3.getComponentIdentifier());
  }
}
