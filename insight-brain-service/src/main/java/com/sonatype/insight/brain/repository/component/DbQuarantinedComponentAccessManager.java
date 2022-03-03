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

/**
 * @since 1.125
 */
@Named
public class DbQuarantinedComponentAccessManager
    implements QuarantinedComponentAccessManager
{
  private static final int EXPIRATION_TIME_IN_HOURS = 12;

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
      throw new BadRequestException(
          "The quarantined component view cannot be retrieved because the URL contains invalid characters.");
    }

    final String decodedInput = new String(decodedBytes);
    final QuarantinedComponentAccess quarantinedComponentAccess = quarantinedComponentAccessDAO.getById(decodedInput);
    if (quarantinedComponentAccess == null) {
      throw new NotFoundException(
          "The quarantined component view for the blocked component you are trying to view could not be found.");
    }

    Date expirationDate = DateUtils.addHours(quarantinedComponentAccess.getGenerateTime(), EXPIRATION_TIME_IN_HOURS);

    if (expirationDate.before(new Date())) {
      throw new NotFoundException("This report expired on " + expirationDate +
          ". You may generate a new report by requesting the blocked component again.");
    }

    if (quarantinedComponentAccess.getGenerateTime().after(new Date())) {
      throw new NotFoundException("The quarantined component view you are trying to access is not available yet.");
    }

    return quarantinedComponentAccess.getRepositoryComponentId();
  }

  private void checkFeatureFlag() {
    if (!insightConfig.isExperimentalFeatureEnabled(ExperimentalFeature.ANONYMOUS_QUARANTINED_COMPONENT_VIEW)) {
      throw new BadRequestException(
          ExperimentalFeature.ANONYMOUS_QUARANTINED_COMPONENT_VIEW.getFlag() + " feature is disabled.");
    }
  }
}
