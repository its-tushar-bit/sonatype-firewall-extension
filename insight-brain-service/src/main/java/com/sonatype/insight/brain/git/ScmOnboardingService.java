/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiCompositeSourceControlService;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.dto.ImportFailure;
import com.sonatype.insight.brain.git.dto.ImportRepositoriesRequest;
import com.sonatype.insight.brain.git.dto.ImportResults;
import com.sonatype.insight.brain.git.dto.OnboardingOrganization;
import com.sonatype.insight.brain.git.dto.SCMRepositories;
import com.sonatype.insight.brain.git.dto.ValidationResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.GitApiClientUtils;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.git.ScmResultStatus.SCM_AUTHN_FAILURE;
import static com.sonatype.insight.brain.git.ScmResultStatus.SCM_AUTHZ_FAILURE;
import static com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderUtils.sanitizeUrl;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.counting;

/**
 * This service supports bulk onboarding of Source Config Management repositories
 *
 * @since 1.98
 */
public class ScmOnboardingService
{
  private static final Logger log = LoggerFactory.getLogger(ScmOnboardingService.class);

  public static final int MAX_PUBLICID_RENAME_ATTEMPTS = 5;

  public static final int INITIAL_RENAME_POSTFIX = 2;

  private final SourceControlDAO sourceControlDAO;

  private final ApplicationDAO appDAO;

  private final OrganizationDAO orgDAO;

  private final ApplicationHelper applicationHelper;

  private final ApiSourceControlService apiSourceControlService;

  private final OrganizationService organizationService;

  private final ApiCompositeSourceControlService apiCompositeSourceControlService;

  private final GitApiClientFactory gitApiClientFactory;

  private final TelemetrySender telemetrySender;

  private final ScmApplicationNameConverter applicationNameConverter;

  @Inject
  public ScmOnboardingService(final SourceControlDAO sourceControlDAO,
                              final ApplicationDAO appDAO,
                              final OrganizationDAO orgDAO,
                              final ApplicationHelper applicationHelper,
                              final ApiSourceControlService apiSourceControlService,
                              final ApiCompositeSourceControlService apiCompositeSourceControlService,
                              OrganizationService organizationService,
                              final GitApiClientFactory gitApiClientFactory,
                              final TelemetrySender telemetrySender,
                              final ScmApplicationNameConverter applicationNameConverter)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.appDAO = appDAO;
    this.orgDAO = orgDAO;
    this.applicationHelper = applicationHelper;
    this.apiSourceControlService = apiSourceControlService;
    this.apiCompositeSourceControlService = apiCompositeSourceControlService;
    this.organizationService = organizationService;
    this.gitApiClientFactory = gitApiClientFactory;
    this.telemetrySender = telemetrySender;
    this.applicationNameConverter = applicationNameConverter;
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  public SCMRepositories loadRepositories(final String orgId, final String hostUrl) throws IOException {
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
    try {
      List<SCMRepository> allRepositories = postProcess(generalClient.listAllRepositories());
      List<SCMRepository> availableRepositories = trimAlreadyConfigured(allRepositories);
      return new SCMRepositories(allRepositories.size(), availableRepositories);
    }
    catch (HttpResponseException e) {
      switch (e.getStatusCode()) {
        case HttpStatus.SC_UNAUTHORIZED:
          return new SCMRepositories(SCM_AUTHN_FAILURE);
        case HttpStatus.SC_FORBIDDEN:
          return new SCMRepositories(SCM_AUTHZ_FAILURE);
        default:
          throw e;
      }
    }
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
    return getMostCommonRepoBaseUrlForOrg(orgId, provider);
  }

  private String getMostCommonRepoBaseUrlForOrg(final String orgId, SourceControlProvider provider) {
    List<SourceControl> sourceControls = emptyList();
    if (StringUtils.isNotEmpty(orgId)) {
      sourceControls =
          sourceControlDAO.getApplicationSourceControlsByOrganizationWithRepositories(orgId);
    }
    if (sourceControls.isEmpty() && orgUsesRootToken(orgId)) {
      // no apps found within this org, look for apps in other orgs
      sourceControls = sourceControlDAO.getApplicationSourceControlsWithRepositoriesAndDefaultToken();
    }
    return sourceControls.stream()
        .map(SourceControl::getRepositoryUrl)
        .collect(Collectors.groupingBy(repoUrl -> getBaseUrl(repoUrl, provider), counting()))
        .entrySet().stream()
        .max(Entry.comparingByValue())
        .map(Entry::getKey)
        .orElse("");
  }

  private boolean orgUsesRootToken(final String orgId) {
    ApiCompositeSourceControlDTO sourceControlDTO =
        apiCompositeSourceControlService.getCompositeSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, orgId);

    return sourceControlDTO.token.value == null;
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
   * @param importReposRequest the repositories to import with associated telemetry data
   * @return list of all imported repositories
   */
  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  public ImportResults importRepositories(final String orgId, final ImportRepositoriesRequest importReposRequest) {
    log.debug("importing repositories into org {}, using: {}", orgId, importReposRequest);

    // validate org ID
    if (orgDAO.getById(orgId) == null) {
      throw new NotFoundException("No organization found for ID " + orgId);
    }

    if (importReposRequest.scmRepositories == null) {
      throw new BadRequestException("SCM Repositories must not be null");
    }

    ArrayList<SCMRepository> importedRepos = new ArrayList<>();
    ArrayList<ImportFailure> failedRepos = new ArrayList<>();
    for (SCMRepository scmRepository : importReposRequest.scmRepositories) {
      try {
        SCMRepository importResult = importRepository(orgId, scmRepository);
        importedRepos.add(importResult);
      }
      catch (Exception e) {
        log.error("Unable to import repository {}", scmRepository, e);
        failedRepos.add(new ImportFailure(scmRepository, e.getMessage()));
      }
    }
    sendImportTelemetry(importReposRequest);
    return new ImportResults(importedRepos, failedRepos);
  }

  private void sendImportTelemetry(final ImportRepositoriesRequest importReposRequest) {
    final int importedSize = importReposRequest.scmRepositories.size();

    Map<String, Object> attributes = new HashMap<>();
    attributes.put("onboarding_batch_count", importedSize);

    if (importReposRequest.totalRepoCount == 0) {
      // log at debug because telemetry errors are our bug and not something customers should care about
      log.debug("importRepositories failed preconditions: totalRepoCount == 0");
    }
    else {
      attributes
          .put("onboarding_batch_percent", (int) Math.round(importedSize * 100.0 / importReposRequest.totalRepoCount));
      attributes.put("onboarding_total_percent", (int) Math
          .round((importedSize + importReposRequest.prevImportedCount) * 100.0 / importReposRequest.totalRepoCount));
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_ONBOARDING);
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
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

  private SCMRepository importRepository(final String orgId, final SCMRepository scmRepository) {
    String publicId = applicationNameConverter.buildPublicId(scmRepository);
    String name = applicationNameConverter.buildName(scmRepository);
    String cloneUrl = sanitizeUrl(scmRepository.getHttpCloneUrl());
    Application app = appDAO.getByPublicId(publicId);
    if (app == null) {
      log.debug("Creating Application entry, name: [{}], publicId: [{}]", name, publicId);
      app = new Application(publicId, name, orgId);
      applicationHelper.addApplication(app);
    }
    else {
      if (applicationNameConverter.doesPublicIdRequireModification(scmRepository)) {
        // this branch handles newly imported applications (already de-duped during load) but a name with disallowed
        // characters which have been removed and as a consequence of that introduced a name conflict
        app = createApplicationWithPostfix(orgId, scmRepository);
      }
    }
    apiSourceControlService.addOrUpdateSourceControl(app.getPublicId(), cloneUrl);
    return scmRepository;
  }

  private Application createApplicationWithPostfix(
      final String orgId,
      final SCMRepository scmRepository)
  {
    int postfix = findNextPublicIdPostfix(scmRepository);
    String publicIdWithPostfix = applicationNameConverter.buildPublicIdWithPostfix(scmRepository, postfix);
    String nameWithPostfix = applicationNameConverter.buildNameWithPostfix(scmRepository, postfix);
    log.debug("Creating Application entry, name: [{}], publicId: [{}]", nameWithPostfix, publicIdWithPostfix);
    Application applicationWithPostfix = new Application(publicIdWithPostfix, nameWithPostfix, orgId);
    applicationHelper.addApplication(applicationWithPostfix);
    return applicationWithPostfix;
  }

  private int findNextPublicIdPostfix(final SCMRepository scmRepository) {
    return IntStream.range(INITIAL_RENAME_POSTFIX, MAX_PUBLICID_RENAME_ATTEMPTS + INITIAL_RENAME_POSTFIX)
        .filter(i -> appDAO.getByPublicId(applicationNameConverter.buildPublicIdWithPostfix(scmRepository, i)) == null)
        .findFirst()
        .orElseThrow(() -> new BadRequestException("Could not find unique name for publicId: [" +
            applicationNameConverter.buildPublicId(scmRepository) + "]"));
  }

  // Delegate auth checks to the organizationService and apiCompositeSourceControlService
  public List<OnboardingOrganization> getOrgsForOnboarding() {
    return organizationService.getAll().stream()
        .map(organization -> new OnboardingOrganization(organization, apiCompositeSourceControlService
            .getCompositeSourceControlByOwner(OwnerType.ORGANIZATION, organization.getId())))
        .collect(Collectors.toList());
  }
}
