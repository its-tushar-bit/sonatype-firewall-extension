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
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class DbQuarantinedComponentAccessManagerTest
    extends AbstractComponentTest
{
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

  @Test
  public void testGetQuarantinedComponentAccessFromToken() {
    // Setup
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    QuarantinedComponentAccess result =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(encodedToken);
    assertThat(result).isNotNull();
    assertThat(result.getRepositoryComponentId()).isEqualTo(repositoryComponent.getId());
  }

  @Test(expected = BadRequestException.class)
  public void testGetQuarantinedComponentAccessFromToken_featureNotEnabled() {
    quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken("token");
  }

  @Test
  public void testGetRepositoryComponentIdFromToken_tokenDoesNotExist() {
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("fakeToken".getBytes(StandardCharsets.UTF_8));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(encodedToken));
  }

  @Test
  public void testGetQuarantinedComponentAccessFromToken_tokenExpired() {
    // Setup
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(),
            DateUtils.addHours(new Date(), -13));
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(encodedToken));
  }

  @Test
  public void testGetQuarantinedComponentAccessFromToken_invalidToken() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken("token"));
  }
}
