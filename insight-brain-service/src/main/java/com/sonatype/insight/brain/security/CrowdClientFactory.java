/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;

import com.atlassian.crowd.integration.rest.service.factory.RestCrowdClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class CrowdClientFactory
{
  private static final Logger log = LoggerFactory.getLogger(CrowdClientFactory.class);

  private final CrowdConfigurationDAO crowdConfigurationDAO;

  private final PasswordHandler passwordHandler;

  private final RestCrowdClientFactory restCrowdClientFactory;

  @Inject
  public CrowdClientFactory(CrowdConfigurationDAO crowdConfigurationDAO, PasswordHandler passwordHandler) {
    this.crowdConfigurationDAO = crowdConfigurationDAO;
    this.passwordHandler = passwordHandler;
    restCrowdClientFactory = new RestCrowdClientFactory();
  }

  public CrowdClient createCrowdClient() {
    if (!SystemConfigurationPropertyFeature.CROWD_INTEGRATION.isEnabled()) {
      return null;
    }

    CrowdConfiguration crowdConfiguration = crowdConfigurationDAO.get();
    if (crowdConfiguration == null) {
      return null;
    }

    try {
      return createCrowdClient(crowdConfiguration);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  public CrowdClient createCrowdClient(CrowdConfiguration crowdConfiguration) {
    try {
      return new CrowdClient(restCrowdClientFactory.newInstance(crowdConfiguration.getServerUrl(),
          crowdConfiguration.getApplicationName(),
          new String(passwordHandler.decryptPassword(crowdConfiguration.getApplicationPassword()))));
    }
    catch (Exception e) {
      throw new RuntimeException(String.format(
          "Failed to create a Crowd REST client for serverUrl '%s', applicationName '%s', " +
              "and applicationPassword '****'. Your Crowd configuration may be invalid.",
          crowdConfiguration.getServerUrl(), crowdConfiguration.getApplicationName()), e);
    }
  }
}
