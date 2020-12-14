/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.experimental.dto.SCMRepositories;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiCompositeSourceControlService;
import com.sonatype.insight.brain.api.experimental.dto.ValidationResponse;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.GitApiClientUtils;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderUtils.sanitizeUrl;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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

  private final ApplicationDAO appDAO;

  private final OrganizationDAO orgDAO;

  private final ApplicationHelper applicationHelper;

  private final ApiSourceControlService apiSourceControlService;

  private final ApiCompositeSourceControlService apiCompositeSourceControlService;

  private final GitApiClientFactory gitApiClientFactory;

  @Inject
  public ApiScmOnboardingService(final SourceControlDAO sourceControlDAO,
                                 final ApplicationDAO appDAO,
                                 final OrganizationDAO orgDAO,
                                 final ApplicationHelper applicationHelper,
                                 final ApiSourceControlService apiSourceControlService,
                                 final ApiCompositeSourceControlService apiCompositeSourceControlService,
                                 final GitApiClientFactory gitApiClientFactory)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.appDAO = appDAO;
    this.orgDAO = orgDAO;
    this.applicationHelper = applicationHelper;
    this.apiSourceControlService = apiSourceControlService;
    this.apiCompositeSourceControlService = apiCompositeSourceControlService;
    this.gitApiClientFactory = gitApiClientFactory;
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  public SCMRepositories loadRepositories(final String orgId, String hostUrl) throws IOException {
    log.debug("loadRepositories returning data for org {} and hostUrl {}", orgId, hostUrl);

    if (orgId == null) {
      throw new BadRequestException("No organization specified");
    }
    orgDAO.getByIdNotNull(orgId);
    if (StringUtils.isEmpty(hostUrl) || "undefined".equalsIgnoreCase(hostUrl)) {
      throw new BadRequestException("No host URL defined");
    }

    ApiCompositeSourceControlDTO sourceControlDTO =
        apiCompositeSourceControlService.getCompositeSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, orgId);

    if (StringUtils.isEmpty(sourceControlDTO.provider)) {
      throw new BadRequestException("No provider configured");
    }

    SourceControlProvider provider = SourceControlProvider.fromString(sourceControlDTO.provider);

    String username = sourceControlDTO.username.value != null ?
        sourceControlDTO.username.value :
        sourceControlDTO.username.parentValue;
    String token = sourceControlDTO.token.value != null ?
        sourceControlDTO.token.value :
        sourceControlDTO.token.parentValue;

    GitApiClientUtils gitUtils = gitApiClientFactory.getGitApiClientUtils(provider);
    String serverUrl = gitUtils.getBaseApiUrl(hostUrl);
    log.debug("Attempting to retrieve repositories using base url: {}", serverUrl);
    Configuration configuration = gitApiClientFactory.createConfiguration();
    configuration.setServerUrl(serverUrl);
    GeneralSCMApiClient generalClient = gitApiClientFactory
        .getGeneralSCMApiClient(provider, configuration, username, token);
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
    String repoUrl = getMostCommonRepoBaseUrlForOrg(orgId, provider);
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

  private String getMostCommonRepoBaseUrlForOrg(final String orgId, SourceControlProvider provider) {
    List<SourceControl> sourceControls = emptyList();
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
          .collect(Collectors.groupingBy(repoUrl -> getBaseUrl(repoUrl, provider), counting()))
          .entrySet().stream()
          .max(Entry.comparingByValue());
      return maxEntry.get().getKey();
    }
    return null;
  }

  private String getBaseUrl(String repoUrl, SourceControlProvider provider) {
    try {
      return gitApiClientFactory.getGitApiClientUtils(provider).getBaseUrlFromRepo(repoUrl);
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
    
  /**
   * Import the selected repositories into the given organization
   * @param orgId the org in which to import the repos
   * @param scmRepositories the list of repositories to import
   * @return list of all imported repositories
   */
  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  public ImportResults importRepositories(final String orgId, final List<SCMRepository> scmRepositories) {
    log.debug("importing repositories into org {}, using: {}", orgId, scmRepositories);

    // validate org ID
    if (orgDAO.getById(orgId) == null) {
      throw new NotFoundException("No organization found for ID " + orgId);
    }

    ArrayList<SCMRepository> importedRepos = new ArrayList<>();
    int failedImportCount = 0;
    for (SCMRepository scmRepository : scmRepositories) {
      try {
        importRepository(orgId, scmRepository);
        importedRepos.add(scmRepository);
      }
      catch (Exception e) {
        log.error("Unable to import repository {}", scmRepository, e);
        ++failedImportCount;
      }
    }
    return new ImportResults(importedRepos, failedImportCount);
  }

  @Authorize(permission = Permission.READ)
  public ValidationResponse validateScmHostUrl(final String scmProvider, final String scmHostUrl) {
    try {
      return new ValidationResponse(checkScmUrl(SourceControlProvider.valueOf(scmProvider.toUpperCase()), scmHostUrl));
    }
    catch (IllegalArgumentException e) {
      return new ValidationResponse(singletonList("Invalid SCM provider."));
    }
  }

  private List<String> checkScmUrl(final SourceControlProvider provider, final String scmUrl) {
    try {
      new GitApiClientFactory().getGitApiClientUtils(provider).getBaseApiUrl(scmUrl);
      return emptyList();
    }
    catch (IllegalArgumentException e) {
      return singletonList(e.getMessage());
    }
  }

  private void importRepository(final String orgId, final SCMRepository scmRepository) {
    String publicId = buildPublicId(scmRepository);
    String name = buildName(scmRepository);
    String cloneUrl = sanitizeUrl(scmRepository.getHttpCloneUrl());
    Application app = appDAO.getByPublicId(publicId);
    if (app == null) {
      log.debug("Creating Application entry, name: [{}], publicId: [{}]", name, publicId);
      app = new Application(publicId, name, orgId);
      applicationHelper.addApplication(app);
    }
    apiSourceControlService.addOrUpdateSourceControl(app.getPublicId(), cloneUrl);
  }

  private String buildPublicId(SCMRepository scmRepository) {
    return scmRepository.getProject() + "__" + scmRepository.getNamespace();
  }

  private String buildName(SCMRepository scmRepository) {
    return toReadableName(scmRepository.getProject()) +
        " - " + toReadableName(scmRepository.getNamespace());
  }

  private String toReadableName(String name) {
    return WordUtils.capitalizeFully(name.replaceAll("[^\\w\\d]+", " ").trim());
  }
}
