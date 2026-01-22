/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.iq.location.discovery.LocationDiscoveryExecutor;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryRequest;
import com.sonatype.nexus.iq.location.dto.LocationDiscoveryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PullRequestLocationDiscoveryService
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestLocationDiscoveryService.class);

  private final GitApiFactory gitApiFactory;

  private final LocationDiscoveryExecutor locationDiscoveryExecutor;

  private final SourceControlUtils sourceControlUtils;

  private final ApplicationDAO applicationDAO;

  private final SourceControlSshService sourceControlSshService;

  @Inject
  public PullRequestLocationDiscoveryService(
      final GitApiFactory gitApiFactory,
      final ApplicationDAO applicationDAO,
      final LocationDiscoveryExecutor locationDiscoveryExecutor,
      final SourceControlUtils sourceControlUtils,
      final SourceControlSshService sourceControlSshService)
  {
    this.gitApiFactory = gitApiFactory;
    this.applicationDAO = applicationDAO;
    this.locationDiscoveryExecutor = locationDiscoveryExecutor;
    this.sourceControlUtils = sourceControlUtils;
    this.sourceControlSshService = sourceControlSshService;
  }

  /**
   * Given a repository, branch name and a policy violation list retrieve all potential location to comment on.
   * <p>
   * The ecosystem specific location collection steps are executed only if there is at least one component in
   * policy evaluation diff that matches that ecosystem.
   * <p>
   * The output of this step is a map between components (ComponentIdentifier) and a list of
   * potential locations to comment on (RankedSourceLocation).
   */
  public LocationDiscoveryResult doLocationDiscovery(final List<PolicyViolation> violationList,
      final GitRepositoryInfo gitRepositoryInfo,
      final String branch,
      final String applicationId)
  {
    sourceControlSshService.verifySshUrlAndUpdateIfNeeded(applicationId);

    LocationDiscoveryResult result = null;

    List<ComponentIdentifier> componentIdentifierSet = violationList.stream()
        .filter(pv -> pv.getComponentIdentifier() != null)
        .map(PolicyViolation::getComponentIdentifier)
        .filter(ci -> ci.getFormat().equalsIgnoreCase(ComponentIdentifier.FORMAT_MAVEN) ||
                      ci.getFormat().equalsIgnoreCase(ComponentIdentifier.FORMAT_NPM) ||
                      ci.getFormat().equalsIgnoreCase(ComponentIdentifier.FORMAT_GOLANG))
        .distinct()
        .collect(Collectors.toList());

    if (!componentIdentifierSet.isEmpty()) {
      File checkoutDir = null;
      Application app = applicationDAO.getById(applicationId);
      try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
        log.debug("Pull request location discovery initiated for application '{}'", applicationId);

        checkoutDir = sourceControlUtils.getCheckoutDirectory(app);

        GitApi gitApi = gitApiFactory.createGitApi(gitRepositoryInfo);
        LocationDiscoveryRequest request =
            new LocationDiscoveryRequest(componentIdentifierSet, gitApi, branch, checkoutDir);
        result = locationDiscoveryExecutor.execute(request);

        log.debug("Pull request location discovery completed for application '{}': {} components found",
            applicationId, result.getLocationMap().size());
      }
      catch (Exception e) {
        log.error("Failed to execute pull request location discovery", e);
        sourceControlUtils.deleteCheckoutDirectory(app);
      }
    }
    return result;
  }
}
