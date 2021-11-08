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
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
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
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.GitApiClient;
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

  private static final String SOURCE_CONTROL_EVALUATION_STAGE = Stage.ID_SOURCE;

  public static final int MAX_PUBLICID_RENAME_ATTEMPTS = 5;

  public static final int INITIAL_RENAME_POSTFIX = 2;

  private final SourceControlDAO sourceControlDAO;

  private SourceControlEventPublisher sourceControlEventPublisher;

  private final ApplicationDAO appDAO;

  private final OrganizationDAO orgDAO;

  private final ApplicationHelper applicationHelper;

  private final ApiSourceControlService apiSourceControlService;

  private final OrganizationService organizationService;

  private final ApiCompositeSourceControlService apiCompositeSourceControlService;

  private final GitClientFactory gitClientFactory;

  private final TelemetrySender telemetrySender;

  private final ScmApplicationNameConverter applicationNameConverter;

  private final IqForScmLicenseChecker licenseChecker;

  private SourceControlUtils sourceControlUtils;

  @Inject
  public ScmOnboardingService(
      final SourceControlDAO sourceControlDAO,
      final SourceControlEventPublisher sourceControlEventPublisher,
      final ApplicationDAO appDAO,
      final OrganizationDAO orgDAO,
      final ApplicationHelper applicationHelper,
      final ApiSourceControlService apiSourceControlService,
      final ApiCompositeSourceControlService apiCompositeSourceControlService,
      final OrganizationService organizationService,
      final GitClientFactory gitClientFactory,
      final TelemetrySender telemetrySender,
      final ScmApplicationNameConverter applicationNameConverter,
      final IqForScmLicenseChecker licenseChecker,
      final SourceControlUtils sourceControlUtils)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.appDAO = appDAO;
    this.orgDAO = orgDAO;
    this.applicationHelper = applicationHelper;
    this.apiSourceControlService = apiSourceControlService;
    this.apiCompositeSourceControlService = apiCompositeSourceControlService;
    this.organizationService = organizationService;
    this.gitClientFactory = gitClientFactory;
    this.telemetrySender = telemetrySender;
    this.applicationNameConverter = applicationNameConverter;
    this.licenseChecker = licenseChecker;
    this.sourceControlUtils = sourceControlUtils;
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

    String providerString = sourceControlDTO.provider.value != null ?
        sourceControlDTO.provider.value :
        sourceControlDTO.provider.parentValue;

    if (StringUtils.isEmpty(providerString)) {
      throw new BadRequestException("No provider configured");
    }

    SourceControlProvider provider = SourceControlProvider.fromString(providerString);

    String username = sourceControlDTO.username.value != null ?
        sourceControlDTO.username.value :
        sourceControlDTO.username.parentValue;
    String token = sourceControlDTO.token.value != null ?
        sourceControlDTO.token.value :
        sourceControlDTO.token.parentValue;

    log.debug("Attempting to retrieve repositories using given host url: {}", hostUrl);
    GeneralSCMApiClient generalClient = gitClientFactory.createGeneralApiClient(provider, hostUrl, username, token);
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
   *
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
    catch (IllegalArgumentException e) {
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
    if (sourceControls.isEmpty() && orgUsesRootTokenAndProvider(orgId)) {
      // no apps found within this org, look for apps in other orgs
      sourceControls = sourceControlDAO.getApplicationSourceControlsWithInheritedCredentials();
    }
    return sourceControls.stream()
        .map(SourceControl::getRepositoryUrl)
        .collect(Collectors.groupingBy(repoUrl -> getBaseUrl(repoUrl, provider), counting()))
        .entrySet().stream()
        .max(Entry.comparingByValue())
        .map(Entry::getKey)
        .orElse("");
  }

  private boolean orgUsesRootTokenAndProvider(final String orgId) {
    ApiCompositeSourceControlDTO sourceControlDTO =
        apiCompositeSourceControlService.getCompositeSourceControlByOwnerDecrypted(OwnerType.ORGANIZATION, orgId);

    return sourceControlDTO.token.value == null && sourceControlDTO.provider.value == null;
  }

  private String getBaseUrl(String repoUrl, SourceControlProvider provider) {
    try {
      return gitClientFactory.getClientUtils(provider).getBaseUrlFromRepo(repoUrl);
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
            .map(SourceControl::getNormalizedRepositoryUrl)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

    return allRepositories.stream()
        .filter(repo -> !existing.contains(SourceControl.normalizeRepositoryUrl(repo.getHttpCloneUrl())))
        .collect(Collectors.toList());
  }

  /**
   * Post process the clone urls in this data structure to ensure they don't accidentally leak
   * user details that can be embedded in the urls.
   */
  private List<SCMRepository> postProcess(final List<SCMRepository> repositories) {
    for (SCMRepository repository : repositories) {
      repository.setHttpCloneUrl(sanitizeUrl(repository.getHttpCloneUrl()));
      // this will ensure we return a standard value to the UI
      // when the default branch is null or empty
      if (StringUtils.isEmpty(repository.getDefaultBranch())) {
        repository.setDefaultBranch(GitApiClient.DEFAULT_BRANCH_NOT_DEFINED);
      }
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
        SCMRepository importResult = importRepositoryAndInitiatePolicyEvaluation(orgId, scmRepository);
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

  private SCMRepository importRepositoryAndInitiatePolicyEvaluation(
      final String orgId,
      final SCMRepository scmRepository)
  {
    String publicId = applicationNameConverter.buildPublicId(scmRepository);
    String name = applicationNameConverter.buildName(scmRepository);
    Application app = appDAO.getByPublicId(publicId);
    if (app == null) {
      log.debug("Creating Application entry, name: [{}], publicId: [{}]", name, publicId);
      app = new Application(publicId, name, orgId);
      applicationHelper.addApplication(app);
    }
    else {
      // app with this ID exists! This may happen because two repos share project/app ID across different
      // providers/hosts, or because the app ID contains special characters, or because we have
      // previously imported this repo in another session

      List<SourceControl> reposWithMatchingUrl = sourceControlDAO.getByRepositoryUrl(scmRepository.getHttpCloneUrl());
      if (reposWithMatchingUrl.isEmpty()) {
        // no existing SC entry matches this URL, free to create a new app with a postfix to avoid the
        // accidental collision
        app = createApplicationWithPostfix(orgId, scmRepository);
      }
      else {
        // existing SC entry exists already, no need to create a new app
        app = appDAO.getById(reposWithMatchingUrl.get(0).getOwnerId());
      }
    }

    // get default branch value and updates SCM repository value with result
    String defaultBranch = getAndSetDefaultBranch(scmRepository, orgId);
    ApiSourceControlDTO apiSourceControlDTO = apiSourceControlService.addOrUpdateSourceControl(app.getPublicId(),
        scmRepository.getHttpCloneUrl(), scmRepository.getSshCloneUrl(), defaultBranch);

    if (licenseChecker.isIqForScmSupported()) {
      GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(app.getId());
      Boolean sourceControlEvaluationsEnabled = gitRepositoryInfo.getSourceControlEvaluationsEnabled();
      if (sourceControlEvaluationsEnabled != null && sourceControlEvaluationsEnabled.booleanValue()) {
        initiateSourceControlEvaluation(apiSourceControlDTO);
      }
    }

    return scmRepository;
  }

  private String getAndSetDefaultBranch(
      final SCMRepository scmRepository,
      final String orgId)
  {
    String defaultBranch = scmRepository.getDefaultBranch();

    if (isAValidDefaultBranch(defaultBranch)) {
      defaultBranch = normalizeDefaultBranch(defaultBranch);
    }
    else {
      // if is invalid we try to get it from SCM
      defaultBranch = getDefaultBranchFromSCM(scmRepository, orgId);
    }

    scmRepository.setDefaultBranch(defaultBranch);

    return defaultBranch;
  }

  private String getDefaultBranchFromSCM(final SCMRepository scmRepository, final String orgId) {
    String defaultBranch = null;

    try {
      GitRepositoryInfo repositoryInfo = getGitRepositoryInfo(scmRepository, orgId);
      GitApiClient gitApiClient = gitClientFactory.createApiClient(repositoryInfo);
      defaultBranch = gitApiClient.getDefaultBranch();
    }
    catch (IOException e) {
      // not need to stop the process. Making default branch not defined
      log.debug("Error getting default branch found for: {}", scmRepository.getHttpCloneUrl(),  e);
    }

    return normalizeDefaultBranch(defaultBranch);
  }

  private boolean isAValidDefaultBranch(final String defaultBranch) {
    // Git Client sets the branch to UNKNOWN_DEFAULT_BRANCH when the SCM doesn't return the branch
    // with the list of all repositories
    return !GeneralSCMApiClient.UNKNOWN_DEFAULT_BRANCH.equals(defaultBranch);
  }

  private String normalizeDefaultBranch(String defaultBranch) {
    // if branch is not defined, we set the default branch to null
    // with this we will leverage org/root-org configuration
    if (StringUtils.isEmpty(defaultBranch)) {
      return null;
    }
    return defaultBranch;
  }

  private GitRepositoryInfo getGitRepositoryInfo(
      SCMRepository scmRepository,
      String orgId)
  {
    return sourceControlUtils.getGitRepositoryInfoForRepository(
        orgId,
        sanitizeUrl(scmRepository.getHttpCloneUrl()),
        scmRepository.getSourceControlProvider());
  }

  private void initiateSourceControlEvaluation(ApiSourceControlDTO sourceControlDTO) {
    try {
      String statusId = UUID.randomUUID().toString().replace("-", "");

      if (null == sourceControlDTO.baseBranch) {
        ApiCompositeSourceControlDTO dto = apiCompositeSourceControlService
            .getCompositeSourceControlByOwner(OwnerType.APPLICATION, sourceControlDTO.ownerId);
        if (null != dto) {
          sourceControlDTO.baseBranch =
              dto.baseBranch.value != null ? dto.baseBranch.value : dto.baseBranch.parentValue;
        }
      }

      SourceControlEvent sourceControlEvent = new SourceControlEvent() //
          .forSourceControlEvaluation() //
          .setApplicationId(sourceControlDTO.ownerId) //
          .setStageTypeId(SOURCE_CONTROL_EVALUATION_STAGE) //
          .setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING) //
          .setStatusId(statusId) //
          .setBranchName(sourceControlDTO.baseBranch);

      log.debug("Initiating a source control evaluation for application {}, stage {} and branch {} with status ID {}.",
          sourceControlEvent.getApplicationId(), sourceControlEvent.getStageTypeId(),
          sourceControlEvent.getBranchName(),
          sourceControlEvent.getStatusId());

      sourceControlEventPublisher.publishEvent(sourceControlEvent);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
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
