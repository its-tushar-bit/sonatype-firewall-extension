/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DbQuarantinedComponentAccessManagerTest
    extends AbstractComponentTest
{
  @Inject
  private QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  @Inject
  private DbQuarantinedComponentAccessManager quarantinedComponentAccessManager;

  @Inject
  private Configuration configuration;

  @Test
  public void testCreateToken() {
    // Setup
    final Repository repository = tempEntity.newRepository();
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());

    Date before = new Date();
    final String token = quarantinedComponentAccessManager.createToken(repositoryComponent);
    Date after = new Date();

    assertThat(token).isNotNull().isNotEmpty();

    final byte[] decodedBytes = Base64.getUrlDecoder().decode(token);
    final String decodedInput = new String(decodedBytes);

    final QuarantinedComponentAccess quarantinedComponentAccess = quarantinedComponentAccessDAO.getById(decodedInput);
    assertThat(quarantinedComponentAccess.getRepositoryId()).isEqualTo(repository.getId());
    assertThat(quarantinedComponentAccess.getRepositoryComponentId()).isEqualTo(repositoryComponent.getId());
    assertThat(quarantinedComponentAccess.getGenerateTime()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
  }

  @Test
  public void testGetQuarantinedComponentAccessFromToken() {
    // Setup
    final Repository repository = tempEntity.newRepository();
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    final String encodedToken = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    QuarantinedComponentAccess result =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(encodedToken);
    assertThat(result.getId()).isEqualTo(quarantinedComponentAccess.getId());
    assertThat(result.getRepositoryId()).isEqualTo(quarantinedComponentAccess.getRepositoryId());
    assertThat(result.getRepositoryComponentId()).isEqualTo(quarantinedComponentAccess.getRepositoryComponentId());
    assertThat(quarantinedComponentAccess.getGenerateTime()).isEqualTo(quarantinedComponentAccess.getGenerateTime());
  }

  @Test
  public void testGetQuarantinedComponentAccessFromToken_tokenDoesNotExist() {
    final String encodedToken = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString("fakeToken".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(encodedToken))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(
            "The quarantined component view for the blocked component you are trying to view could not be found.");
  }

  @Test
  public void testGetQuarantinedComponentAccessFromToken_tokenExpired() {
    // Setup
    final Repository repository = tempEntity.newRepository();
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(),
            DateUtils.addHours(new Date(), -13));
    final String encodedToken = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(encodedToken))
        .isInstanceOf(NotFoundException.class)
        .hasMessageStartingWith("This report expired on ")
        .hasMessageEndingWith("You may generate a new report by requesting the blocked component again.");
  }

  @Test
  public void testGetQuarantinedComponentAccessFromToken_invalidToken() {
    // The token is not base64 encoded
    assertThatThrownBy(() -> quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken("token"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("The quarantined component view cannot be retrieved because the URL contains invalid characters.");
  }

  @Test
  public void testGetTokenExpiryTime_Default() {
    Date date = new Date();

    assertThat(quarantinedComponentAccessManager.getTokenExpiryTime(date)).isEqualTo(
        new Date(date.getTime() + configuration.getQuarantinedComponentReportExpirationTimeInHours() * 3600000));
  }

  @Test
  public void testGetTokenExpiryTime_Modified() {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationProperty.QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS, "24");

    Date date = new Date();

    assertThat(quarantinedComponentAccessManager.getTokenExpiryTime(date)).isEqualTo(
        new Date(date.getTime() + configuration.getQuarantinedComponentReportExpirationTimeInHours() * 3600000));
  }
}
