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
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.ExperimentalFeature;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.time.DateUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.125
 */
@Named
public class DbQuarantinedComponentAccessManager
    implements QuarantinedComponentAccessManager
{
  private static final Logger log = LoggerFactory.getLogger(DbQuarantinedComponentAccessManager.class);

  private static final int EXPIRATION_TIME_IN_DAYS = 2;

  private final InsightConfig insightConfig;

  private final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  @Inject
  public DbQuarantinedComponentAccessManager(
      final InsightConfig insightConfig,
      final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO)
  {
    this.insightConfig = insightConfig;
    this.quarantinedComponentAccessDAO = quarantinedComponentAccessDAO;
  }

  /**
   * Creates a token which provides read access to the quarantined component report and associated API's for the given
   * repository component id. The token is the generated UUID value for the quarantined_component_access database entry.
   * The token is valid for the default / configured validity time from the time the token is generated.
   *
   * @param repositoryComponent The repository component for which the token should provide read access
   * @return the base64 url encoded token
   */
  @Override
  public String createToken(final RepositoryComponent repositoryComponent) {
    checkFeatureFlag();
    final QuarantinedComponentAccess quarantinedComponentAccess =
        new QuarantinedComponentAccess(repositoryComponent.getRepositoryId(), repositoryComponent.getId(), new Date());
    quarantinedComponentAccessDAO.insert(quarantinedComponentAccess);
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Retrieves the repository component id for the given token. The supplied token is decoded and used as the id for the
   * entry that needs to be retrieved from the quarantined_component_access table. The current time will be checked
   * against the time the token was generated plus the default/configured validity time. If outside the validity window,
   * if the token cannot be decoded, or if the entry does not exist, a NotFoundException will be thrown.
   *
   * @param token The base64 url encoded token
   * @return The repository component id that is associated with the supplied token.
   */
  @Override
  public String getRepositoryComponentIdFromToken(final String token) {
    checkFeatureFlag();
    byte[] decodedBytes;
    try {
      decodedBytes = Base64.getUrlDecoder().decode(token);
    }
    catch (final IllegalArgumentException e) {
      final String logToken = token.replaceAll("[\n\r\t]", "_");
      log.error("Invalid supplied encoded token '{}' cannot be decoded", logToken);
      throw new BadRequestException(String.format("Invalid supplied encoded token '%s' cannot be decoded", token));
    }

    final String decodedInput = new String(decodedBytes);
    final QuarantinedComponentAccess quarantinedComponentAccess = quarantinedComponentAccessDAO.getById(decodedInput);
    if (quarantinedComponentAccess == null) {
      log.error("Could not find quarantined component report access entry for id: {}", decodedInput);
      throw new NotFoundException(String.format("Component report with identifier %s could not be found", token));
    }

    if (DateUtils.addDays(quarantinedComponentAccess.getGenerateTime(), EXPIRATION_TIME_IN_DAYS)
        .before(new Date())) {
      log.error("Access to quarantined component report entry with expired id: {} was attempted", decodedInput);
      throw new NotFoundException(String.format("Component report with identifier %s is expired", token));
    }

    if (quarantinedComponentAccess.getGenerateTime().after(new Date())) {
      log.error("Access to quarantined component report entry with future id: {} was attempted", decodedInput);
      throw new NotFoundException(String.format("Component report with identifier %s is not available yet", token));
    }

    return quarantinedComponentAccess.getRepositoryComponentId();
  }

  private void checkFeatureFlag() {
    if (!insightConfig.isExperimentalFeatureEnabled(ExperimentalFeature.ANONYMOUS_QUARANTINED_COMPONENT_VIEW)) {
      throw new UnauthorizedException(
          ExperimentalFeature.ANONYMOUS_QUARANTINED_COMPONENT_VIEW.getFlag() + " feature is disabled.");
    }
  }
}
