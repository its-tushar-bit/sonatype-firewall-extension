/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aideveloper;

import java.time.Instant;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.InsightConfig;

/**
 * Records and reports the AI Developer opt-in. Any authenticated user may opt in and the unlock then applies to
 * everyone on this server, so this resource carries no permission check beyond authentication. Reads and writes go
 * through {@link SystemConfigurationPropertyDAO}, which scopes them to the caller's tenant in MTIQ.
 * <p>
 * The resource deliberately declares no {@code ProductLicenseEnforcementPoint}: requiring AI Developer here would
 * lock away the call that unlocks it. It is also not an {@code UnlicensedPath}, so an invalid or expired license
 * still rejects the call.
 * </p>
 */
@Named
@Singleton
@Path(AiDeveloperOptInResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class AiDeveloperOptInResource
{
  public static final String RESOURCE_PATH = "api/v2/ai-developer/opt-in";

  private final SystemConfigurationPropertyDAO propertyDAO;

  private final CurrentUser currentUser;

  private final InsightConfig config;

  @Inject
  public AiDeveloperOptInResource(
      SystemConfigurationPropertyDAO propertyDAO,
      CurrentUser currentUser,
      InsightConfig config)
  {
    this.propertyDAO = propertyDAO;
    this.currentUser = currentUser;
    this.config = config;
  }

  @GET
  public AiDeveloperOptInStatus getOptIn() {
    return status(propertyDAO.get(SystemConfigurationProperty.AI_DEVELOPER_OPT_IN));
  }

  /**
   * Records the opt-in and returns the state in force. Opting in when already opted in keeps the original user and
   * instant, so the record of who unlocked AI Developer survives later calls.
   */
  @POST
  @Audited(AuditEvent.CONFIGURE_AI_DEVELOPER_OPT_IN)
  public AiDeveloperOptInStatus optIn() {
    String value = currentUser.getUsernameOrSystem() + "," + Instant.now();
    return status(propertyDAO.setIfAbsent(SystemConfigurationProperty.AI_DEVELOPER_OPT_IN, value));
  }

  private AiDeveloperOptInStatus status(String propertyValue) {
    return AiDeveloperOptInStatus.from(propertyValue, config.isDatabaseEmbedded());
  }
}
