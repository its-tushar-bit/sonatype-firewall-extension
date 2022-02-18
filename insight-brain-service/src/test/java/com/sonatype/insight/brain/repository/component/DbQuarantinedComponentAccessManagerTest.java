/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DbQuarantinedComponentAccessManagerTest
    extends AbstractComponentTest
{
  @Inject
  private InsightConfig insightConfig;

  @Inject
  private QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  @Inject
  private DbQuarantinedComponentAccessManager quarantinedComponentAccessManager;

  @Test
  public void testCreateToken() {
    // Setup
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());

    final String token = quarantinedComponentAccessManager.createToken(repositoryComponent);

    assertThat(token).isNotNull().isNotEmpty();

    final byte[] decodedBytes = Base64.getUrlDecoder().decode(token);
    final String decodedInput = new String(decodedBytes);

    final QuarantinedComponentAccess quarantinedComponentAccess = quarantinedComponentAccessDAO.getById(decodedInput);
    assertThat(quarantinedComponentAccess).isNotNull();
    assertThat(quarantinedComponentAccess.getRepositoryComponentId()).isEqualTo(repositoryComponent.getId());
  }

  @Test(expected = BadRequestException.class)
  public void testCreateToken_featureNotEnabled() {
    insightConfig.setFeatures(ImmutableMap.of(Feature.ANONYMOUS_QUARANTINED_COMPONENT_VIEW.getFlag(), false));
    quarantinedComponentAccessManager.createToken(new RepositoryComponent());
  }

  @Test
  public void testGetRepositoryComponentIdFromToken() {
    // Setup
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    assertThat(quarantinedComponentAccessManager.getRepositoryComponentIdFromToken(encodedToken))
        .isEqualTo(repositoryComponent.getId());
  }

  @Test(expected = BadRequestException.class)
  public void testGetRepositoryComponentIdFromToken_featureNotEnabled() {
    insightConfig.setFeatures(ImmutableMap.of(Feature.ANONYMOUS_QUARANTINED_COMPONENT_VIEW.getFlag(), false));
    quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token");
  }

  @Test
  public void testGetRepositoryComponentIdFromToken_tokenDoesNotExist() {
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("fakeToken".getBytes(StandardCharsets.UTF_8));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> quarantinedComponentAccessManager.getRepositoryComponentIdFromToken(encodedToken));
  }

  @Test
  public void testGetRepositoryComponentIdFromToken_tokenExpired() {
    // Setup
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(),
            DateUtils.addHours(new Date(), -13));
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> quarantinedComponentAccessManager.getRepositoryComponentIdFromToken(encodedToken));
  }

  @Test
  public void testGetRepositoryComponentIdFromToken_invalidToken() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> quarantinedComponentAccessManager.getRepositoryComponentIdFromToken("token"));
  }
}
