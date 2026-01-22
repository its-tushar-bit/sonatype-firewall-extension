/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMatchingResultDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiCompositeSourceControlService;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlOrganizationImportEventDAO;
import com.sonatype.insight.brain.git.dto.ImportFailure;
import com.sonatype.insight.brain.git.dto.ImportFailures;
import com.sonatype.insight.brain.git.dto.ImportRepositoriesRequest;
import com.sonatype.insight.brain.git.dto.ImportResults;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationStatus;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationTicket;
import com.sonatype.insight.brain.git.dto.OnboardingOrganization;
import com.sonatype.insight.brain.git.dto.SCMRepositories;
import com.sonatype.insight.brain.git.dto.ValidationResponse;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent.ImportStatus;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.InvalidRepositoryUrlException;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import io.dropwizard.lifecycle.Managed;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.git.ScmResultStatus.SCM_AUTHN_FAILURE;
import static com.sonatype.insight.brain.git.ScmResultStatus.SCM_AUTHZ_FAILURE;
import static com.sonatype.insight.brain.git.ScmResultStatus.SCM_UNKNOWN_HOST_FAILURE;
import static com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderUtils.sanitizeUrl;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.counting;

/**
 * This service supports bulk onboarding of Source Config Management repositories
 *
 * @since 1.98
 */
@Named
@Singleton
public class ScmOnboardingService
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(ScmOnboardingService.class);

  private static final String SOURCE_CONTROL_EVALUATION_STAGE = Stage.ID_SOURCE;

  public static final int MAX_PUBLICID_RENAME_ATTEMPTS = 5;

  public static final int INITIAL_RENAME_POSTFIX = 2;

  private static final int SCM_IMPORT_BATCH_SIZE = 100;

  private static int scmParallelImportThreshold = 100;

  private static int scmParallelImportMaxRepositoriesPerBatch = 25;

  private static int importEventStatusUpdateThreshold = 20;

  private final SourceControlDAO sourceControlDAO;

  private SourceControlEventPublisher sourceControlEventPublisher;

  private final ApplicationDAO appDAO;

  private final OrganizationDAO orgDAO;

  private final SourceControlOrganizationImportEventDAO sourceControlOrganizationImportEventDAO;

  private final ApplicationHelper applicationHelper;

  private final ApiSourceControlService apiSourceControlService;

  private final OrganizationService organizationService;

  private final ApiCompositeSourceControlService apiCompositeSourceControlService;

  private final GitClientFactory gitClientFactory;

  private final TelemetrySender telemetrySender;

  private final ScmApplicationNameConverter applicationNameConverter;

  private final IqForScmLicenseChecker licenseChecker;

  private SourceControlUtils sourceControlUtils;

  private final SourceControlImportThreadPoolExecutor executor;

  private final InsightProxy insightProxy;

  private final ScmUserMatchingService userMatchingService;

  private final ScmUserMappingService scmUserMappingService;

  @Inject
  public ScmOnboardingService(
      final SourceControlDAO sourceControlDAO,
      final SourceControlEventPublisher sourceControlEventPublisher,
      final ApplicationDAO appDAO,
      final OrganizationDAO orgDAO,
      final SourceControlOrganizationImportEventDAO sourceControlOrganizationImportEventDAO,
      final ApplicationHelper applicationHelper,
      final ApiSourceControlService apiSourceControlService,
      final ApiCompositeSourceControlService apiCompositeSourceControlService,
      final OrganizationService organizationService,
      final GitClientFactory gitClientFactory,
      final TelemetrySender telemetrySender,
      final ScmApplicationNameConverter applicationNameConverter,
      final IqForScmLicenseChecker licenseChecker,
      final SourceControlUtils sourceControlUtils,
      final InsightProxy insightProxy,
      final Configuration configuration,
      final ShutdownHandler shutdownHandler,
      final ScmUserMatchingService userMatchingService,
      final ScmUserMappingService scmUserMappingService)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.appDAO = appDAO;
    this.orgDAO = orgDAO;
    this.sourceControlOrganizationImportEventDAO = sourceControlOrganizationImportEventDAO;
    this.applicationHelper = applicationHelper;
    this.apiSourceControlService = apiSourceControlService;
    this.apiCompositeSourceControlService = apiCompositeSourceControlService;
    this.organizationService = organizationService;
    this.gitClientFactory = gitClientFactory;
    this.telemetrySender = telemetrySender;
    this.applicationNameConverter = applicationNameConverter;
    this.licenseChecker = licenseChecker;
    this.sourceControlUtils = sourceControlUtils;
    this.insightProxy = insightProxy;
    this.executor = new SourceControlImportThreadPoolExecutor(configuration.getSourceControlImportPoolSize());
    this.userMatchingService = userMatchingService;
    this.scmUserMappingService = scmUserMappingService;
    shutdownHandler.add(executor);
  }

  // Visible for testing
  SourceControlImportThreadPoolExecutor getExecutor() {
    return executor;
  }

  @Override
  public void start() throws Exception {
    //no-op
  }

  @Override
  public void stop() throws Exception {
    executor.shutdown();
  }

  @Authorize(permission = Permission.ADD_APPLICATION)
  public SCMRepositories loadRepositories(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) final String orgId,
      final String hostUrl) throws IOException
  {
    log.debug("loadRepositories returning data for org {} and hostUrl {}", orgId, hostUrl);
    if (StringUtils.isEmpty(hostUrl)) {
      throw new BadRequestException("No host URL defined");
    }
    return loadScmRepositories(orgId, hostUrl);
  }

  @NotNull
  private SCMRepositories loadScmRepositories(final String orgId, final String hostUrl) throws IOException {
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
    catch (InvalidRepositoryUrlException e) {
      throw new BadRequestException(e);
    }
    catch (UnknownHostException e) {
      return new SCMRepositories(SCM_UNKNOWN_HOST_FAILURE);
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
  @Authorize(permission = Permission.ADD_APPLICATION)
  public String getDefaultHostUrl(
      final String providerString,
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) final String orgId)
  {
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
      HttpClientUtils.Configuration configuration = new HttpClientUtils.Configuration();
      insightProxy.contextualize(configuration, repoUrl);
      return gitClientFactory.getClientUtils(provider, configuration).getBaseUrlFromRepo(repoUrl);
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
            .map(String::toLowerCase)
            .distinct()
            .collect(Collectors.toList());

    return allRepositories.stream()
        .filter(repo -> !existing.contains(SourceControl.normalizeRepositoryUrl(repo.getHttpCloneUrl()).toLowerCase()))
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
  @Authorize(permission = Permission.ADD_APPLICATION)
  public ImportResults importRepositories(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) final String orgId,
      final ImportRepositoriesRequest importReposRequest)
  {
    log.debug("importing repositories into org {}, using: {}", orgId, importReposRequest);

    // validate org ID
    if (orgDAO.getById(orgId) == null) {
      throw new NotFoundException("No organization found for ID " + orgId);
    }

    if (importReposRequest.scmRepositories == null) {
      throw new BadRequestException("SCM Repositories must not be null");
    }

    return doImportRepositories(orgId, importReposRequest, null);
  }

  private ImportResults doImportRepositories(
      final String orgId,
      final ImportRepositoriesRequest importReposRequest,
      final SourceControlOrganizationImportEvent importEvent)
  {
    ArrayList<SCMRepository> importedRepos = new ArrayList<>();
    ArrayList<ImportFailure> failedRepos = new ArrayList<>();
    MutableInt successCounter = new MutableInt(0);
    MutableInt failureCounter = new MutableInt(0);
    for (SCMRepository scmRepository : importReposRequest.scmRepositories) {
      try {
        SCMRepository importResult = importRepositoryAndInitiatePolicyEvaluation(orgId, scmRepository);
        importedRepos.add(importResult);
        successCounter.add(1);
      }
      catch (Exception e) {
        log.error("Unable to import repository {}", scmRepository, e);
        failedRepos.add(new ImportFailure(scmRepository, e.getMessage()));
        failureCounter.add(1);
      }
      //periodically update the event in the case of a large SCM import
      updateImportEventIntermediateState(importEvent, successCounter, failureCounter);
    }
    if (importEvent != null && (successCounter.getValue() > 0 || failureCounter.getValue() > 0)) {
      updateImportEventIntermediateState(importEvent, successCounter.getValue(), failureCounter.getValue());
    }
    sendImportTelemetry(importReposRequest);
    return new ImportResults(importedRepos, failedRepos);
  }

  private void updateImportEventIntermediateState(
      final SourceControlOrganizationImportEvent importEvent,
      final MutableInt successCount,
      final MutableInt failureCount)
  {
    if (importEvent != null &&
        (successCount.getValue() + failureCount.getValue()) >= importEventStatusUpdateThreshold) {
      updateImportEventIntermediateState(importEvent, successCount.getValue(), failureCount.getValue());
      successCount.setValue(0);
      failureCount.setValue(0);
    }
  }

  private synchronized void updateImportEventIntermediateState(
      SourceControlOrganizationImportEvent event,
      int successCount,
      int failedCount)
  {
    event.setLastUpdatedTime(new Date())
        .setImportSuccessCount(event.getImportSuccessCount() + successCount)
        .setImportFailureCount(event.getImportFailureCount() + failedCount);
    sourceControlOrganizationImportEventDAO.update(event);
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
      app = createNewApplication(orgId, publicId, name);
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

    importUserRolesBasedOnSCMContributors(app);

    if (licenseChecker.isIqForScmSupported()) {
      GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(app.getId());
      Boolean sourceControlEvaluationsEnabled = gitRepositoryInfo.getSourceControlEvaluationsEnabled();
      if (sourceControlEvaluationsEnabled != null && sourceControlEvaluationsEnabled.booleanValue()) {
        initiateSourceControlEvaluation(apiSourceControlDTO);
      }
    }

    return scmRepository;
  }

  //visible for testing
  Application createNewApplication(final String orgId, final String publicId, final String name) {
    log.debug("Creating Application entry, name: [{}], publicId: [{}]", name, publicId);
    Application app = new Application(publicId, name, orgId);
    try {
      applicationHelper.addApplication(app);
    }
    catch (InvalidNameException e) {
      //closely named repos potentially results in duplicate names in `name_lowercase_no_whitespace` column causing
      // a duplicate name exception (due to normalization and stripping off special characters, etc.)
      // Adding a randomization to improve uniqueness (just once)
      log.debug("Resulted app name {} conflicts with an existing app. Randomizing name and retrying", name);
      String newName = String.format("%s-%s", name, RandomStringUtils.secure().nextAlphabetic(5));
      app = new Application(publicId, newName, orgId);
      applicationHelper.addApplication(app);
    }
    return app;
  }

  // only supported for Github in general, although, the mapping SCM_USER to IQ_USER should work
  // more broadly
  private void importUserRolesBasedOnSCMContributors(final Application app) {
    final SCMUserMappingsResponseDTO mappings =
        scmUserMappingService.getUserMappingsByOwnerNoAuthz(OwnerType.APPLICATION, app.getId());

    if (nonNull(mappings)) {
      log.info("performing automatic role assignment for app {}", app.getPublicId());

      try {
        final SCMUserMatchingResultDTO results =
            userMatchingService.automaticRoleAssignmentByMappingNoAuthz(app, mappings.userMapping());

        if (!results.matchedUsers().isEmpty()) {
          log.info("{} user(s) imported from SCM for app {}", results.matchedUsers().size(), app.getPublicId());
          log.info("The successful mapping strategy was {}", results.successfulMapping());
        }
        else
        {
          log.info("no users imported for {}", app.getPublicId());
        }
      }
      catch (RuntimeException  ex) {
        log.warn("Could not import users for app {}: {}", app.getPublicId(), ex.getMessage());
      }
    }
    else {
      log.info("skipping automatic role assignment -- no scm mapping strategies configured");
    }
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

  @Authorize(permission = Permission.ADD_APPLICATION)
  public ImportScmOrganizationStatus getImportScmOrganizationStatus(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) final String orgId,
      final String eventId)
  {
    SourceControlOrganizationImportEvent event =
        sourceControlOrganizationImportEventDAO.getByOrganizationAndEventId(orgId, eventId);
    if (event == null) {
      throw new NotFoundException("import event not found");
    }
    return createImportScmOrganizationStatusFrom(event);
  }

  private ImportScmOrganizationStatus createImportScmOrganizationStatusFrom(
      final SourceControlOrganizationImportEvent event)
  {
    ImportScmOrganizationRequest request =
        new ImportScmOrganizationRequest(event.getScmHostUrl(), event.getImportLimit(),
            event.getDesiredSubOrganizationCount());
    ImportScmOrganizationStatus status =
        new ImportScmOrganizationStatus(request, event.getImportStatus().toString(), event.getImportSuccessCount(),
            event.getImportFailureCount());
    status.updateStartTime(event.getStartTime());
    status.updateLastUpdatedTime(event.getLastUpdatedTime());
    if (ImportStatus.COMPLETE.equals(event.getImportStatus()) || ImportStatus.ERROR.equals(event.getImportStatus())) {
      status.errors = event.getImportErrors();
    }
    return status;
  }

  @Authorize(permission = Permission.ADD_APPLICATION)
  public ImportScmOrganizationTicket importScmOrganization(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) final String orgId,
      final ImportScmOrganizationRequest importRequest)
  {
    AuditData.get().setScmImportEvent(importRequest);
    validateImportRequest(importRequest);
    SourceControlOrganizationImportEvent importEvent = new SourceControlOrganizationImportEvent()
        .setOrganizationId(orgId)
        .setScmHostUrl(importRequest.scmHostUrl)
        .setImportLimit(importRequest.importLimit)
        .setLastUpdatedTime(new Date())
        .setDesiredSubOrganizationCount(importRequest.desiredSubOrganizationCount);

    sourceControlOrganizationImportEventDAO.insert(importEvent);
    ImportScmOrganizationTicket ticket = createImportTicketFor(importEvent);
    AuditData.get().continueAsync(importTask(importEvent), executor::submit);
    return ticket;
  }

  private ImportScmOrganizationTicket createImportTicketFor(final SourceControlOrganizationImportEvent importEvent) {
    ImportScmOrganizationTicket ticket = new ImportScmOrganizationTicket();
    ticket.statusUrl = PublicApiPaths.EXPERIMENTAL_ONBOARDING_RESOURCE_PATH + "/" +
        ApiScmOnboardingResource.IMPORT_REPO_STATUS_PATH
            .replace("{organizationId}", importEvent.getOrganizationId())
            .replace("{eventId}", importEvent.getId());
    return ticket;
  }

  private static void validateImportRequest(final ImportScmOrganizationRequest importRequest) {
    if (importRequest == null || StringUtils.isEmpty(importRequest.scmHostUrl)) {
      throw new BadRequestException("No host URL defined");
    }
    if (importRequest.importLimit == 0) {
      throw new BadRequestException("repository import limit must not be 0");
    }
    if (importRequest.desiredSubOrganizationCount < 0) {
      throw new BadRequestException("desiredSubOrganizationCount must be a positive integer");
    }
  }

  private Runnable importTask(final SourceControlOrganizationImportEvent event) {
    return () -> doScmOrganizationImport(event);
  }

  //visible for testing
  void doScmOrganizationImport(final SourceControlOrganizationImportEvent event)
      throws RuntimeException
  {
    String limit = event.getImportLimit() > 0 ? String.valueOf(event.getImportLimit()) : "all";
    log.debug("Onboarding {} scm repositories for org {} and hostUrl {}",
        limit,
        event.getOrganizationId(),
        event.getScmHostUrl());
    SCMRepositories scmRepositories;
    try {
      scmRepositories = loadScmRepositories(event.getOrganizationId(), event.getScmHostUrl());
    }
    catch (IOException e) {
      event.setImportStatus(ImportStatus.ERROR);
      event.setImportErrors(ExceptionUtils.getStackTrace(e));
      sourceControlOrganizationImportEventDAO.update(event);
      throw new RuntimeException(e);
    }
    int numberOfReposToImport = determineNumberOfReposToImport(event, scmRepositories);
    if (numberOfReposToImport == 0) {
      log.debug("No available scm repositories to onboard from {} total repositories",
          scmRepositories.totalRepositories);
      doZeroRepositoriesImport(event);
    }
    else if (numberOfReposToImport < scmParallelImportThreshold) {
      log.debug("{} repositories importable from {} available repositories",
          scmRepositories.availableRepositories.size(),
          scmRepositories.totalRepositories);
      doSequentialImport(event, scmRepositories, numberOfReposToImport);
    }
    else {
      log.debug("{} repositories importable from {} available repositories. Initiating parallel import",
          scmRepositories.availableRepositories.size(),
          scmRepositories.totalRepositories);
      doParallelImport(event, scmRepositories, numberOfReposToImport);
    }
  }

  private void doZeroRepositoriesImport(final SourceControlOrganizationImportEvent event) {
    ImportResults results = new ImportResults(emptyList(), emptyList());
    completeImportEventWithResults(event, results);
  }

  private void doSequentialImport(final SourceControlOrganizationImportEvent event,
                                   final SCMRepositories scmRepositories,
                                   final int numberOfReposToImport)
  {
    List<SCMRepository>  selectedRepos = scmRepositories.getAvailableRepositories()
        .stream()
        .limit(numberOfReposToImport)
        .collect(Collectors.toList());
    ImportResults results = event.getDesiredSubOrganizationCount() == 0 ?
        importRepositoriesWithoutSubOrganizations(event, scmRepositories, selectedRepos) :
        importRepositoriesWithNewSubOrganizations(event, selectedRepos);
    completeImportEventWithResults(event, results);
  }

  private ImportResults importRepositoriesWithoutSubOrganizations(final SourceControlOrganizationImportEvent event,
                                                                  final SCMRepositories scmRepositories,
                                                                  final List<SCMRepository> selectedReposToImport)
  {
    ImportRepositoriesRequest importRepoRequest =
        new ImportRepositoriesRequest(selectedReposToImport, scmRepositories.totalRepositories, 0);
    return doImportRepositories(event.getOrganizationId(), importRepoRequest, event);
  }

  private void doParallelImport(final SourceControlOrganizationImportEvent event,
                                            final SCMRepositories scmRepositories,
                                            final int numberOfReposToImport)
  {
    List<SCMRepository> selectedReposToImport = scmRepositories.getAvailableRepositories()
        .stream()
        .limit(numberOfReposToImport)
        .collect(Collectors.toList());
    boolean subOrganizationsDesired = event.getDesiredSubOrganizationCount() > 0;
    int numberOfBatches =
        subOrganizationsDesired ?
            event.getDesiredSubOrganizationCount() :
            determineRequiredNumberOfBatches(selectedReposToImport.size(), SCM_IMPORT_BATCH_SIZE);
    log.debug("Creating {} batches for import event {}", numberOfBatches, event.getId());
    List<List<SCMRepository>> batches = partition(selectedReposToImport, numberOfBatches);
    List<ImportFailure> importFailures = Collections.synchronizedList(new ArrayList<>());
    List<CompletableFuture<Void>> taskList = new ArrayList<>(batches.size());
    for (List<SCMRepository> repositories : batches) {
      String organizationId = subOrganizationsDesired ?
          newChildOrganization(event.getOrganizationId()).getId() :
          event.getOrganizationId();
      if (repositories.size() > scmParallelImportMaxRepositoriesPerBatch) {
        int numberOfSubBatches = repositories.size() / scmParallelImportMaxRepositoriesPerBatch;
        List<List<SCMRepository>> partitionedBatches = partition(repositories, numberOfSubBatches);
        log.debug("Creating {} import sub-batches for organization ID {}", numberOfSubBatches, organizationId);
        for (List<SCMRepository> batch : partitionedBatches) {
          log.debug("Submitting sub-batch for organization ID {} with size {}", organizationId, batch.size());
          submitBatch(event, batch, importFailures, organizationId, taskList);
        }
      }
      else {
        log.debug("Submitting batch for organization ID {} with size {}", organizationId, repositories.size());
        submitBatch(event, repositories, importFailures, organizationId, taskList);
      }
    }
    CompletableFuture.allOf(taskList.toArray(new CompletableFuture[0])).join();
    finalizeParallelImport(event, importFailures);
  }

  private void submitBatch(final SourceControlOrganizationImportEvent event,
                           final List<SCMRepository> batch,
                           final List<ImportFailure> importFailures,
                           String organizationId,
                           List<CompletableFuture<Void>> taskList)
  {
    RepositoryBatchImportTask importTask = new RepositoryBatchImportTask(event,
        batch,
        importFailures,
        organizationId);
    taskList.add(CompletableFuture.runAsync(importTask, executor));
  }

  int determineRequiredNumberOfBatches(final int numberOfReposToImport, final int eachBatchSize) {
    return (numberOfReposToImport + eachBatchSize - 1) / eachBatchSize;
  }

  private synchronized void finalizeParallelImport(
      final SourceControlOrganizationImportEvent event,
      final List<ImportFailure> importFailures)
  {
    event.setLastUpdatedTime(new Date());
    long diff = Math.abs(event.getLastUpdatedTime().getTime() - event.getStartTime().getTime());
    long diffMinutes = TimeUnit.MINUTES.convert(diff, TimeUnit.MILLISECONDS);
    event.setImportStatus(ImportStatus.COMPLETE);
    if (CollectionUtils.isNotEmpty(importFailures)) {
      event.setImportErrors(JsonUtils.writeUnformatted(new ImportFailures(importFailures)));
    }
    sourceControlOrganizationImportEventDAO.update(event);
    log.debug("Completed import event {} in {} minutes", event.getId(), diffMinutes);
  }

  private void completeImportEventWithResults(
      final SourceControlOrganizationImportEvent event,
      final ImportResults results)
  {
    event.setImportStatus(ImportStatus.COMPLETE);
    event.setLastUpdatedTime(new Date());
    if (results != null) {
      event.setImportErrors(JsonUtils.writeUnformatted(new ImportFailures(results.getFailedRepositories())));
    }
    sourceControlOrganizationImportEventDAO.update(event);
  }

  ImportResults importRepositoriesWithNewSubOrganizations(
      final SourceControlOrganizationImportEvent event,
      List<SCMRepository> selectedReposToImport)
  {
    int totalRepoCount = selectedReposToImport.size();
    List<List<SCMRepository>> repoBatches = partition(selectedReposToImport, event.getDesiredSubOrganizationCount());
    log.debug("importing {} repositories in to {} child organizations within organization [{}]", totalRepoCount,
        repoBatches.size(), event.getOrganizationId());

    Organization parentOrg = orgDAO.getByIdNotNull(event.getOrganizationId());
    MutableInt prevImportedCount = new MutableInt(0);
    List<SCMRepository> allImportedRepositories = new ArrayList<>();
    List<ImportFailure> allFailedRepositories = new ArrayList<>();
    for (int i = 0; i < repoBatches.size(); i++) {
      importRepositoryBatch(event,
          repoBatches,
          parentOrg,
          prevImportedCount,
          totalRepoCount,
          allImportedRepositories,
          allFailedRepositories,
          i);
    }
    return new ImportResults(allImportedRepositories, allFailedRepositories);
  }

  private ImportResults importRepositoryBatch(
      final SourceControlOrganizationImportEvent event,
      final List<List<SCMRepository>> repoGroups,
      final Organization parentOrg,
      final MutableInt prevImportedCount,
      final int totalRepoCount,
      final List<SCMRepository> allImportedRepositories,
      final List<ImportFailure> allFailedRepositories,
      final int batchIndex)
  {
    List<SCMRepository> repoGroup = repoGroups.get(batchIndex);

    Organization childOrg = newChildOrganization(parentOrg.getId());
    ImportRepositoriesRequest importRepositoriesRequest =
        new ImportRepositoriesRequest(repoGroup, totalRepoCount, prevImportedCount.getAndAdd(repoGroup.size()));
    ImportResults importResults = doImportRepositories(childOrg.getId(), importRepositoriesRequest, event);
    allImportedRepositories.addAll(importResults.getImportedRepositories());
    allFailedRepositories.addAll(importResults.getFailedRepositories());
    return importResults;
  }

  private static int determineNumberOfReposToImport(
      final SourceControlOrganizationImportEvent event,
      final SCMRepositories scmRepositories)
  {
    return Integer.min(scmRepositories.availableRepositories.size(),
        event.getImportLimit() < 0 ? scmRepositories.availableRepositories.size() : event.getImportLimit());
  }

  private <T> List<List<T>> partition(List<T> items, final int noOfChunks) {
    return new ArrayList<>(IntStream.range(0, items.size()).boxed()
        .collect(Collectors.groupingBy(e -> e % noOfChunks, Collectors.mapping(items::get, Collectors.toList())))
        .values());
  }

  class RepositoryBatchImportTask
      implements Runnable
  {
    private SourceControlOrganizationImportEvent event;

    private final List<SCMRepository> repoBatch;

    private final List<ImportFailure> importFailures;

    private final String organizationId;

    public RepositoryBatchImportTask(
        final SourceControlOrganizationImportEvent event,
        final List<SCMRepository> repoBatch,
        final List<ImportFailure> importFailures,
        String organizationId)
    {
      this.event = event;
      this.repoBatch = repoBatch;
      this.importFailures = importFailures;
      this.organizationId = organizationId;
    }

    @Override
    public void run() {
      log.debug("Event {} : importing batch of {} repositories in to organization {}",
          event.getId(), repoBatch.size(), organizationId);
      ImportRepositoriesRequest importRepoRequest =
          new ImportRepositoriesRequest(repoBatch, repoBatch.size(), 0);
      ImportResults results =
          doImportRepositories(organizationId, importRepoRequest, event);
      importFailures.addAll(results.getFailedRepositories());
    }
  }

  private Organization newChildOrganization(final String parentOrgId) {
    Organization parentOrg = orgDAO.getByIdNotNull(parentOrgId);
    String childName = parentOrg.getName() + "-" + RandomStringUtils.secure().nextAlphabetic(8);
    Organization child = new Organization(childName);
    child.setParentOrganizationId(parentOrg.getId());
    try {
      orgDAO.insert(child);
    }
    catch (InvalidNameException e) {
      log.debug("Sub-organization {} already exists. Fetching existing sub-organization", child.getName());
      return orgDAO.getByName(childName);
    }
    return child;
  }

  // visible for testing
  static void setScmParallelImportThreshold(int threshold) {
    scmParallelImportThreshold = threshold;
  }

  // visible for testing
  static void setImportEventStatusUpdateThreshold(int threshold) {
    importEventStatusUpdateThreshold = threshold;
  }

  // visible for testing
  static void setScmParallelImportMaxRepositoriesPerBatch(int maxRepositoriesPerBatch) {
    scmParallelImportMaxRepositoriesPerBatch = maxRepositoriesPerBatch;
  }
}
