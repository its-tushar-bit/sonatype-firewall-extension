/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.iq.location.discovery.LocationDiscoveryExecutor;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryRequest;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clone or update a repository, and find all potential location to comment on, which will be later used by
 * the PR commenting feature.
 */
public class PullRequestLocationDiscoveryTask
    extends GitRepositoryTask
    implements Callable<LocationDiscoveryResult>
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestLocationDiscoveryTask.class);

  private final GitApiFactory gitApiFactory;

  private final ApplicationDAO applicationDAO;

  private LocationDiscoveryExecutor locationDiscoveryExecutor;

  private List<ComponentIdentifier> componentIdentifiers;

  private GitRepositoryInfo gitRepositoryInfo;

  private String applicationId;

  private String branch;

  @Inject
  public PullRequestLocationDiscoveryTask(
      final ApplicationDAO applicationDAO,
      final GitApiFactory gitApiFactory,
      final InsightConfig insightConfig,
      final FileCleaner fileCleaner)
  {
    super(insightConfig, fileCleaner);
    this.applicationDAO = applicationDAO;
    this.gitApiFactory = gitApiFactory;
  }

  public void init(
      final LocationDiscoveryExecutor locationDiscoveryExecutor,
      final List<ComponentIdentifier> componentIdentifiers,
      final GitRepositoryInfo gitRepositoryInfo,
      final String branch,
      final String applicationId)
  {
    this.locationDiscoveryExecutor = locationDiscoveryExecutor;
    this.componentIdentifiers = componentIdentifiers;
    this.gitRepositoryInfo = gitRepositoryInfo;
    this.branch = branch;
    this.applicationId = applicationId;
  }

  @Override
  public LocationDiscoveryResult call() {
    LocationDiscoveryResult result = null;

    boolean initialized = isNotNull(locationDiscoveryExecutor, "locationDiscoveryExecutor") &&
        isNotNull(componentIdentifiers, "componentIdentifiers") && isNotNull(branch, "branch") &&
        isNotNull(gitRepositoryInfo, "gitRepositoryInfo") && isNotNull(applicationId, "applicationId");

    if (initialized) {
      File checkoutDir = null;
      try {
        log.debug("Pull request location discovery task initiated for application '{}'", applicationId);

        String applicationPublicId = applicationDAO.getById(applicationId).getPublicId();

        checkoutDir = getCheckoutDirectory(applicationPublicId, applicationId, gitRepositoryInfo);

        LocationDiscoveryRequest request =
            new LocationDiscoveryRequest(componentIdentifiers, gitApiFactory.createGitApi(gitRepositoryInfo), branch,
                checkoutDir);
        result = locationDiscoveryExecutor.execute(request);

        log.debug("Pull request location discovery task completed for application '{}': {}", applicationId, result);
      }
      catch (Exception e) {
        log.error("Failed to execute pull request location discovery task, cleaning pull request directory", e);
        cleanDirectory(checkoutDir);
      }
      catch (Throwable t) {
        // Try to log to stderr before trying the standard logging because the standard logging may not be operational
        // at this point.
        t.printStackTrace();
        log.error(t.getMessage(), t);
        System.exit(1);
      }
    }
    return result;
  }

  private boolean isNotNull(final Object object, final String name) {
    if (object == null) {
      log.error("Missing required {}", name);
    }
    return object != null;
  }
}

