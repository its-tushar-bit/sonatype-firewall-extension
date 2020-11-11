/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.experimental.dto.SCMRepositories;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.GitApiClientUtils;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderUtils.sanitizeUrl;
import static java.util.stream.Collectors.counting;

/**
 * This service supports bulk onboarding of Source Config Management repositories
 *
 * @since 1.98
 */
public class ApiScmOnboardingService
{
  private static final Logger log = LoggerFactory.getLogger(ApiScmOnboardingService.class);

  private final SourceControlDAO sourceControlDAO;

  private final ApiSourceControlService apiSourceControlService;

  private final GitApiClientFactory gitApiClientFactory;

  @Inject
  public ApiScmOnboardingService(final SourceControlDAO sourceControlDAO,
                                 final ApiSourceControlService apiSourceControlService,
                                 final GitApiClientFactory gitApiClientFactory)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.apiSourceControlService = apiSourceControlService;
    this.gitApiClientFactory = gitApiClientFactory;
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  public SCMRepositories loadRepositories(final String orgId, String hostUrl) throws IOException {
    log.debug("loadRepositories returning data for org {} and hostUrl {}", orgId, hostUrl);

    if (orgId == null) {
      throw new BadRequestException("No organization specified");
    }
    if (StringUtils.isEmpty(hostUrl) || "undefined".equalsIgnoreCase(hostUrl)) {
      throw new BadRequestException("No host URL defined");
    }

    SourceControl orgSourceControl = apiSourceControlService.getSourceControlByOwnerDecrypted(orgId);
    if (orgSourceControl == null) {
      log.debug("No source control entry defined for org {}, checking for entries at root", orgId);
      orgSourceControl = apiSourceControlService.getSourceControlByOwnerDecrypted(Organization.ROOT_ORGANIZATION_ID);
    }
    if (orgSourceControl == null) {
      log.error("Not able to retrieve source control entries at org {} or root, repository scan exiting",
          orgId);
      throw new BadRequestException("No source control entries found for organization ID " + orgId);
    }

    GitApiClientUtils gitUtils = gitApiClientFactory.getGitApiClientUtils(orgSourceControl.getProvider());
    String serverUrl = gitUtils.getBaseApiUrl(hostUrl);
    log.debug("Attempting to retrieving repositories using base url: {}", serverUrl);
    Configuration configuration = gitApiClientFactory.createConfiguration();
    configuration.setServerUrl(serverUrl);
    GeneralSCMApiClient generalClient = gitApiClientFactory
        .getGeneralSCMApiClient(orgSourceControl.getProvider(), configuration, orgSourceControl.getUsername(),
            orgSourceControl.getToken());
    List<SCMRepository> allRepositories = postProcess(generalClient.listAllRepositories());
    List<SCMRepository> availableRepositories = trimAlreadyConfigured(allRepositories);
    return new SCMRepositories(allRepositories.size(), availableRepositories);  
  }

  /**
   * calculates the default host URL for use in onboarding
   * @param orgId optional, if provided will attempt to use existing SCM repos in this org
   */
  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  public String getDefaultHostUrl(final String providerString, final String orgId) {
    if (StringUtils.isBlank(providerString)) {
      throw new BadRequestException("Provider has not been specified");
    }
    SourceControlProvider provider;
    try {
      provider = SourceControlProvider.fromString(providerString);
    }
    catch (IllegalArgumentException e ) {
      throw new BadRequestException("Invalid provider: " + providerString, e);
    }

    // if org is provided, try to gather URL from an app within the org
    String repoUrl = getMostCommonRepoBaseUrlForOrg(orgId);
    if (!StringUtils.isEmpty(repoUrl)) {
      return repoUrl;
    }

    switch (provider) {
      case GITHUB:
        return "https://github.com/";
      case GITLAB:
        return "https://gitlab.com/";
      case BITBUCKET:
        return "https://bitbucket.org/";
      default:
        return null;
    }
  }

  private String getMostCommonRepoBaseUrlForOrg(final String orgId) {
    List<SourceControl> sourceControls = Collections.emptyList();
    if (StringUtils.isNotEmpty(orgId)) {
      sourceControls =
          sourceControlDAO.getApplicationSourceControlsByOrganizationWithRepositories(orgId);
    }
    if (sourceControls.isEmpty()) {
      sourceControls = sourceControlDAO.getApplicationSourceControlsWithRepositories();
    }
    if (!sourceControls.isEmpty()) {
      Optional<Entry<String, Long>> maxEntry = sourceControls.stream()
          .map(SourceControl::getRepositoryUrl)
          .collect(Collectors.groupingBy(this::getBaseUrl, counting()))
          .entrySet().stream()
          .max(Entry.comparingByValue());
      return maxEntry.get().getKey();
    }
    return null;
  }

  private String getBaseUrl(String repoUrl) {
    try {
      URI url = new URI(repoUrl);
      return new URI(url.getScheme(), url.getUserInfo(), url.getHost(), url.getPort(), null, null, null).toString();
    }
    catch (URISyntaxException e) {
      log.info("Was not able to parse repo url {}, falling back to default for the provider", repoUrl, e);
      return "";
    }
  }

  /**
   * Trim the passed in list to remove any entries for already configured repositories.
   */
  private List<SCMRepository> trimAlreadyConfigured(final List<SCMRepository> allRepositories) {
    List<String> existing =
        sourceControlDAO.getAll().stream()
            .filter(sourceControl -> sourceControl.getRepositoryUrl() != null)
            .map(sourceControl -> sanitizeUrl(sourceControl.getRepositoryUrl()))
            .distinct()
            .collect(Collectors.toList());

    return allRepositories.stream()
        .filter(repo -> !existing.contains(repo.getHttpCloneUrl()))
        .collect(Collectors.toList());
  }

  /**
   * Post process the clone urls in this data structure to ensure they don't accidentally leak
   * user details that can be embedded in the urls.
   */
  private List<SCMRepository> postProcess(final List<SCMRepository> repositories) {
    for (SCMRepository repository : repositories) {
      repository.setHttpCloneUrl(sanitizeUrl(repository.getHttpCloneUrl()));
    }
    return repositories;
  }
}
