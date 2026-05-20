/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.insight.brain.api.v2.dto.githubapp.ApiGitHubAppManifestDTO;
import com.sonatype.insight.brain.api.v2.githubapp.ApiGitHubAppListDTO;
import com.sonatype.insight.brain.api.v2.dto.githubapp.Manifest;
import com.sonatype.insight.brain.api.v2.githubapp.ApiGitHubAppResource;
import com.sonatype.insight.brain.model.githubapp.GitHubAppRegistrationState;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.BaseUrl;
import java.io.StringReader;
import com.sonatype.nexus.scm.github.GitHubApiClient;
import com.sonatype.nexus.scm.github.GitHubAppOAuthClient;
import com.sonatype.insight.brain.api.v2.dto.ApiGitHubAppDTO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppInstallationStateDAO;
import com.sonatype.insight.brain.git.GitHubAppAuthStrategyCache;
import com.sonatype.insight.brain.git.GitHubManifestService;
import com.sonatype.insight.brain.model.githubapp.GitHubAppInstallationState;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.UriBuilder;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppRegistrationStateDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.nexus.scm.github.dto.GitHubAppCredentials;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.github.GitHubAppManagementClient;
import java.security.PrivateKey;
import java.util.Map;
import com.sonatype.nexus.scm.github.dto.GitHubUserInstallations;
import jakarta.ws.rs.InternalServerErrorException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApiGitHubAppService
{
  private static final Logger log = LoggerFactory.getLogger(ApiGitHubAppService.class);

  private static final String DEFAULT_GITHUB_API_BASE_URL = "https://api.github.com";

  private static final String DEFAULT_GITHUB_OAUTH_TOKEN_URL = "https://github.com";

  private static final long STATE_TOKEN_EXPIRATION_MS = 10 * 60 * 1000L;

  private static final int STATE_TOKEN_LENGTH = 32;

  private static final String MANIFEST_DESCRIPTION = "GitHub App for Sonatype IQ Server integration - " +
      "provides source control management, pull request automation, and security policy enforcement";

  private static final int RANDOM_SUFFIX_GENERATOR_LENGTH = 8;

  private static final String PERSONAL_ACCOUNT_MARKER = "(personal)";

  private final BaseUrl baseUrl;

  private final GitHubAppDAO gitHubAppDAO;

  private final GitHubAppInstallationStateDAO installationStateDAO;

  private final GitHubAppRegistrationStateDAO registrationStateDAO;

  private final SourceControlDAO sourceControlDAO;

  private final OwnerDAO ownerDAO;

  private final PasswordHandler passwordHandler;

  private final InsightProxy insightProxy;

  private final GitHubManifestService gitHubManifestService;

  private final GitHubAppAuthStrategyCache authStrategyCache;

  private final GitHubAppSelectionCache selectionCache;

  private final String githubApiBaseUrl;

  private final String githubOAuthTokenUrl;

  private GitHubAppDeletionService gitHubAppDeletionService;

  @Inject
  public ApiGitHubAppService(
      final GitHubAppDAO gitHubAppDAO,
      final GitHubAppInstallationStateDAO installationStateDAO,
      final GitHubAppRegistrationStateDAO registrationStateDAO,
      final SourceControlDAO sourceControlDAO,
      final OwnerDAO ownerDAO,
      final PasswordHandler passwordHandler,
      final InsightProxy insightProxy,
      final GitHubManifestService gitHubManifestService,
      final GitHubAppAuthStrategyCache authStrategyCache,
      final GitHubAppSelectionCache selectionCache,
      final GitHubAppDeletionService gitHubAppDeletionService,
      final BaseUrl baseUrl)
  {
    this(gitHubAppDAO, installationStateDAO, registrationStateDAO, sourceControlDAO, ownerDAO,
        passwordHandler, insightProxy, gitHubManifestService, authStrategyCache, selectionCache,
        gitHubAppDeletionService, DEFAULT_GITHUB_API_BASE_URL, DEFAULT_GITHUB_OAUTH_TOKEN_URL, baseUrl);
  }

  ApiGitHubAppService(
      final GitHubAppDAO gitHubAppDAO,
      final GitHubAppInstallationStateDAO installationStateDAO,
      final GitHubAppRegistrationStateDAO registrationStateDAO,
      final SourceControlDAO sourceControlDAO,
      final OwnerDAO ownerDAO,
      final PasswordHandler passwordHandler,
      final InsightProxy insightProxy,
      final GitHubManifestService gitHubManifestService,
      final GitHubAppAuthStrategyCache authStrategyCache,
      final GitHubAppSelectionCache selectionCache,
      final GitHubAppDeletionService gitHubAppDeletionService,
      final String githubApiBaseUrl,
      final String githubOAuthTokenUrl,
      final BaseUrl baseUrl)
  {
    this.gitHubAppDAO = gitHubAppDAO;
    this.installationStateDAO = installationStateDAO;
    this.registrationStateDAO = registrationStateDAO;
    this.sourceControlDAO = sourceControlDAO;
    this.ownerDAO = ownerDAO;
    this.passwordHandler = passwordHandler;
    this.insightProxy = insightProxy;
    this.gitHubManifestService = gitHubManifestService;
    this.authStrategyCache = authStrategyCache;
    this.selectionCache = selectionCache;
    this.gitHubAppDeletionService = gitHubAppDeletionService;
    this.githubApiBaseUrl = githubApiBaseUrl;
    this.githubOAuthTokenUrl = githubOAuthTokenUrl;
    this.baseUrl = baseUrl;
  }

  public List<ApiGitHubAppListDTO> listGitHubApps(final String ownerId) {
    Owner owner = ownerDAO.getByIdNotNull(ownerId);
    return listGitHubAppsAuthorized(owner);
  }

  @Authorize(permission = Permission.WRITE)
  List<ApiGitHubAppListDTO> listGitHubAppsAuthorized(@AuthzContext(Key.OWNER) final Owner owner) {
    List<GitHubApp> apps = gitHubAppDAO.getAllByOwnerId(owner.getId());
    return apps.stream().map(this::toListDTO).toList();
  }

  public void deleteGitHubApp(final String githubAppId, final String ownerId) {
    Owner owner = ownerDAO.getByIdNotNull(ownerId);
    deleteGitHubAppAuthorized(owner, githubAppId);
  }

  @Authorize(permission = Permission.WRITE)
  void deleteGitHubAppAuthorized(@AuthzContext(Key.OWNER) final Owner owner, final String githubAppId) {
    GitHubApp gitHubApp = gitHubAppDAO.getByGithubAppId(githubAppId);
    if (gitHubApp == null || !owner.getId().equals(gitHubApp.getOwnerId())) {
      throw new NotFoundException("GitHub App not found for owner");
    }
    gitHubAppDeletionService.delete(gitHubApp);
  }

  private ApiGitHubAppListDTO toListDTO(final GitHubApp app) {
    String installationUrl = buildInstallationUrl(app);
    return new ApiGitHubAppListDTO(
        app.getId(),
        app.getAppId(),
        app.getSlug(),
        app.getGithubOrganizationName(),
        app.getInstallationId(),
        app.isActive(),
        app.getLastUpdatedAt() != null ? app.getLastUpdatedAt().toInstant().toString() : null,
        installationUrl);
  }

  private String buildInstallationUrl(final GitHubApp app) {
    if (app.getInstallationId() == null) {
      return null;
    }
    String orgName = app.getGithubOrganizationName();
    boolean isPersonal = orgName == null || orgName.endsWith(PERSONAL_ACCOUNT_MARKER);
    if (isPersonal) {
      return "https://github.com/settings/installations/" + app.getInstallationId();
    }
    return "https://github.com/organizations/" + orgName + "/settings/installations/" + app.getInstallationId();
  }

  /**
   * Generates a GitHub App manifest for registration with GitHub's app creation flow.
   * This wrapper method validates owner exists, then delegates to the authorized method for permission checking.
   *
   * @param ownerId Owner ID
   * @param organizationName GitHub App organisation name
   * @return ApiGitHubAppManifestDTO containing state token and manifest JSON
   * @throws InternalServerErrorException if base URL is not configured or owner not found
   */
  public ApiGitHubAppManifestDTO generateManifest(final String ownerId, final String organizationName) {
    Owner owner = ownerDAO.getByIdNotNull(ownerId);
    return generateManifestAuthorized(owner, organizationName);
  }

  /**
   * Internal method that generates a GitHub App manifest after authorization check.
   * Creates a unique app name with random suffix, OAuth callback URLs with CSRF-protected
   * state token (expires in 10 minutes), and required source control permissions.
   * Uses WRITE permission on the owner, similar to ApiSourceControlService.addSourceControlByOwner.
   *
   * @param owner Owner for authorization check
   * @param organizationName GitHub App organisation name
   * @return ApiGitHubAppManifestDTO containing state token and manifest JSON
   * @throws InternalServerErrorException if base URL is not configured or state token creation fails
   */
  @Authorize(permission = Permission.WRITE)
  ApiGitHubAppManifestDTO generateManifestAuthorized(
      @AuthzContext(Key.OWNER) final Owner owner,
      final String organizationName)
  {
    final String baseUrlForManifest = baseUrl.get();

    if (StringUtils.isBlank(baseUrlForManifest)) {
      log.error("Cannot generate GitHub App manifest: base URL is not configured");
      throw new InternalServerErrorException("IQ Server base URL must be configured to register GitHub Apps");
    }

    final String stateToken = generateStateToken();

    final String redirectPath = UriBuilder.fromResource(ApiGitHubAppResource.class)
        .path("redirect")
        .build()
        .getPath();

    final String setupPath = UriBuilder.fromResource(ApiGitHubAppResource.class)
        .path(ApiGitHubAppResource.class, "handleInstallationSetup")
        .build()
        .getPath();

    final GitHubAppRegistrationState stateRecord = new GitHubAppRegistrationState();
    stateRecord.setStateToken(stateToken);
    stateRecord.setOwnerId(owner.getId());
    stateRecord.setGithubOrganizationName(
        StringUtils.isBlank(organizationName) ? PERSONAL_ACCOUNT_MARKER : organizationName);
    stateRecord.setExpiresAt(new Date(System.currentTimeMillis() + STATE_TOKEN_EXPIRATION_MS));
    stateRecord.setCreatedAt(new Date());

    registrationStateDAO.insert(stateRecord);

    final String randomSuffix = generateRandomSuffix(RANDOM_SUFFIX_GENERATOR_LENGTH);
    final String appName = "Sonatype IQ Server " + randomSuffix;

    final String redirectUrl = baseUrlForManifest + redirectPath;
    final String setupUrl = baseUrlForManifest + setupPath;
    final List<String> callbackUrls = List.of(setupUrl);

    final Map<String, String> permissions = createDefaultPermissions();

    log.debug("Generated manifest for app '{}' with redirect URL including state token", appName);

    return new ApiGitHubAppManifestDTO(
        stateToken,
        new Manifest(
            appName,
            baseUrlForManifest,
            redirectUrl,
            setupUrl,
            callbackUrls,
            true,
            MANIFEST_DESCRIPTION,
            false,
            permissions,
            true));
  }

  /**
   * Handle GitHub App installation setup callback after OAuth authorization.
   * This wrapper method validates the state token, extracts owner information,
   * and delegates to the authorized method for permission checking.
   *
   * @param installationId GitHub App installation ID
   * @param state State token for CSRF protection
   * @param oauthCode OAuth authorization code
   * @return GitHubApp object (used to build redirect URL with githubAppId in Resource layer)
   * @throws BadRequestException if validation fails or configuration errors occur
   */
  public GitHubApp handleInstallationSetupCallback(
      final Long installationId,
      final String state,
      final String oauthCode) throws IOException
  {
    log.info("Configuring GitHub App installation {} with state: {}", installationId, state);

    GitHubAppInstallationState installationState = installationStateDAO.findAndDeleteByStateToken(state);

    if (installationState == null || installationState.isExpired()) {
      log.warn("GitHub App OAuth callback received invalid or expired state token - "
          + "possible replay attack or expired session: {}", state);
      throw new BadRequestException("Invalid or expired state parameter");
    }

    String githubAppId = installationState.getGithubAppId();

    GitHubApp gitHubApp = gitHubAppDAO.getById(githubAppId);
    if (gitHubApp == null) {
      throw new InternalServerErrorException("No GitHub App found: " + githubAppId);
    }

    String ownerId = gitHubApp.getOwnerId();
    Owner owner = ownerDAO.getByIdNotNull(ownerId);

    handleInstallationSetupCallbackAuthorized(owner, installationId, gitHubApp, oauthCode);
    return gitHubApp;
  }

  /**
   * Internal method that performs the actual installation setup after authorization check.
   * Uses WRITE permission on the owner, similar to ApiSourceControlService.addSourceControlByOwner.
   * Exchanges OAuth code for access token, verifies user owns the installation, and configures installation ID.
   *
   * @param owner Owner for authorization check
   * @param installationId GitHub App installation ID
   * @param gitHubApp Pre-fetched GitHub App entity
   * @param oauthCode OAuth authorization code
   * @throws BadRequestException if validation fails or configuration errors occur
   */
  @Authorize(permission = Permission.WRITE)
  void handleInstallationSetupCallbackAuthorized(
      @AuthzContext(Key.OWNER) final Owner owner,
      final Long installationId,
      final GitHubApp gitHubApp,
      final String oauthCode) throws IOException
  {
    String userAccessToken = getUserAccessToken(oauthCode, gitHubApp);

    GitHubUserInstallations installations = getUserInstallations(userAccessToken);

    boolean hasAccess = installations != null
        && installations.getInstallations() != null
        && installations.getInstallations()
            .stream()
            .anyMatch(installation -> installationId.equals(installation.getId()));

    if (!hasAccess) {
      log.warn("User does not own installation {}", installationId);
      throw new BadRequestException("User does not have permission to install this GitHub App");
    }

    String accountName = installations.getInstallations()
        .stream()
        .filter(installation -> installationId.equals(installation.getId()))
        .findFirst()
        .map(installation -> installation.getAccount().getLogin())
        .get();

    log.info("OAuth validation successful for installation {} and owner {}", installationId, owner.getId());

    configureInstallation(gitHubApp, owner.getId(), installationId, accountName);
  }

  /**
   * Handle GitHub App manifest conversion and registration.
   * This wrapper method validates the state token, extracts owner information,
   * and delegates to the authorized method for permission checking.
   *
   * @param code Temporary manifest conversion code from GitHub
   * @param state OAuth state token for CSRF protection
   * @return Installation URL for GitHub App
   * @throws Exception if validation fails or registration errors occur
   */
  public String handleManifestConversionAndRegistration(
      final String code,
      final String state) throws Exception
  {
    GitHubAppRegistrationState registrationState = findVerifyAndDeleteStateToken(state);

    String ownerId = registrationState.getOwnerId();
    Owner owner = ownerDAO.getByIdNotNull(ownerId);

    return handleManifestConversionAndRegistrationAuthorized(owner, code, registrationState);
  }

  /**
   * Internal method that performs the actual manifest conversion and registration after authorization check.
   * Uses WRITE permission on the owner, similar to ApiSourceControlService.addSourceControlByOwner.
   *
   * @param owner Owner for authorization check
   * @param code Temporary manifest conversion code from GitHub
   * @param registrationState Pre-fetched registration state
   * @return Installation URL for GitHub App
   * @throws Exception if registration errors occur
   */
  @Authorize(permission = Permission.WRITE)
  String handleManifestConversionAndRegistrationAuthorized(
      @AuthzContext(Key.OWNER) final Owner owner,
      final String code,
      final GitHubAppRegistrationState registrationState) throws Exception
  {
    final ApiGitHubAppDTO gitHubApp = createGitHubAppFromManifest(code, registrationState);

    GitHubAppInstallationState installationState =
        initiateInstallation(gitHubApp.id());

    return UriBuilder.fromUri("https://github.com")
        .path("apps")
        .path(gitHubApp.slug())
        .path("installations")
        .path("new")
        .queryParam("state", installationState.getStateToken())
        .build()
        .toString();
  }

  @Authorize(permission = Permission.WRITE)
  public ApiGitHubAppDTO createGitHubAppFromManifest(
      final String code,
      GitHubAppRegistrationState registrationState) throws Exception
  {
    if (code == null || code.trim().isEmpty()) {
      throw new BadRequestException("GitHub manifest conversion code is required");
    }
    GitHubAppManagementClient client = createGitHubAppManagementClient(githubApiBaseUrl);

    final GitHubAppCredentials githubResponse =
        gitHubManifestService.convertManifestCode(code, client);

    validateGitHubManifestResponse(githubResponse);

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      tx.begin();

      final GitHubApp gitHubApp = createGitHubAppFromManifestResponse(githubResponse, registrationState);
      gitHubAppDAO.insert(tx, gitHubApp);
      tx.commit();

      selectionCache.invalidateAll();
      authStrategyCache.invalidate(gitHubApp.getId());

      return toGitHubAppDTO(gitHubApp);
    }
  }

  public GitHubAppRegistrationState findVerifyAndDeleteStateToken(String state) {
    GitHubAppRegistrationState stateToken = registrationStateDAO.findAndDeleteByStateToken(state);

    if (stateToken == null || stateToken.isExpired()) {
      log.warn("GitHub App OAuth callback received invalid or expired state token - " +
          "possible replay attack or expired session: {}", state);
      throw new BadRequestException("Invalid or expired state parameter");
    }
    return stateToken;
  }

  private GitHubApp createGitHubAppFromManifestResponse(
      final GitHubAppCredentials response,
      final GitHubAppRegistrationState registrationState) throws Exception
  {
    final String encryptedClientSecret = passwordHandler.encryptPassword(response.getClientSecret());
    final String encryptedPrivateKey = processAndEncryptPrivateKey(response.getPem());

    final GitHubApp gitHubApp = new GitHubApp();
    gitHubApp.setId(UUID.randomUUID().toString());
    gitHubApp.setAppId(response.getId());
    gitHubApp.setSlug(response.getSlug());
    gitHubApp.setClientId(response.getClientId());
    gitHubApp.setClientSecret(encryptedClientSecret);
    gitHubApp.setPrivateKey(encryptedPrivateKey);

    gitHubApp.setOwnerId(registrationState.getOwnerId());
    gitHubApp.setGithubOrganizationName(registrationState.getGithubOrganizationName());
    gitHubApp.setLastUpdatedAt(new Date());

    gitHubApp.setInstallationId(null);

    return gitHubApp;
  }

  private String processAndEncryptPrivateKey(final String pemKey) throws Exception {
    final PrivateKey privateKey = parsePemPrivateKey(pemKey);

    final byte[] pkcs8Bytes = privateKey.getEncoded();
    final String base64Key = Base64.getEncoder().encodeToString(pkcs8Bytes);

    return passwordHandler.encryptPassword(base64Key);
  }

  private PrivateKey parsePemPrivateKey(final String pemKey) throws Exception {
    try (StringReader stringReader = new StringReader(pemKey);
        PEMParser pemParser = new PEMParser(stringReader))
    {
      final Object pemObject = pemParser.readObject();

      if (pemObject instanceof PEMKeyPair pemKeyPair) {
        final JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
      }
      else if (pemObject instanceof PrivateKeyInfo) {
        final JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        return converter.getPrivateKey((PrivateKeyInfo) pemObject);
      }
      else {
        throw new IllegalArgumentException("Unsupported PEM object type: " +
            (pemObject != null ? pemObject.getClass().getName() : "null"));
      }
    }
  }

  private ApiGitHubAppDTO toGitHubAppDTO(final GitHubApp gitHubApp) {
    return new ApiGitHubAppDTO(
        gitHubApp.getId(),
        gitHubApp.getAppId(),
        gitHubApp.getSlug(),
        gitHubApp.getClientId(),
        gitHubApp.getInstallationId(),
        gitHubApp.getOwnerId());
  }

  private void validateGitHubManifestResponse(final GitHubAppCredentials response) {
    if (response.getId() == null) {
      log.error("GitHub API response missing app ID");
      throw new InternalServerErrorException("Invalid response from GitHub: missing app ID");
    }

    if (response.getClientId() == null || response.getClientId().trim().isEmpty()) {
      log.error("GitHub API response missing client_id");
      throw new InternalServerErrorException("Invalid response from GitHub: missing client_id");
    }

    if (response.getClientSecret() == null || response.getClientSecret().trim().isEmpty()) {
      log.error("GitHub API response missing client_secret");
      throw new InternalServerErrorException("Invalid response from GitHub: missing client_secret");
    }

    if (response.getPem() == null || response.getPem().trim().isEmpty()) {
      log.error("GitHub API response missing private key (pem)");
      throw new InternalServerErrorException("Invalid response from GitHub: missing private key");
    }
  }

  private String getUserAccessToken(String oauthCode, GitHubApp gitHubApp) throws IOException {
    String clientSecret = passwordHandler.decryptPassword(gitHubApp.getClientSecret());
    GitHubAppOAuthClient client = createGitHubAppOAuthClient(githubOAuthTokenUrl);
    return client.exchangeOAuthCode(gitHubApp.getClientId(), clientSecret, oauthCode);
  }

  private GitHubAppOAuthClient createGitHubAppOAuthClient(String serverUrl) {
    HttpClientUtils.Configuration config = new HttpClientUtils.Configuration();
    config.setServerUrl(serverUrl);
    return new GitHubAppOAuthClient(config);
  }

  private GitHubAppManagementClient createGitHubAppManagementClient(String serverUrl) {
    HttpClientUtils.Configuration config = new HttpClientUtils.Configuration();
    config.setServerUrl(serverUrl);
    insightProxy.contextualize(config, serverUrl);
    return new GitHubAppManagementClient(config);
  }

  private GitHubUserInstallations getUserInstallations(String userAccessToken) throws IOException {
    HttpClientUtils.Configuration config = new HttpClientUtils.Configuration();
    config.setServerUrl(githubApiBaseUrl);
    insightProxy.contextualize(config, githubApiBaseUrl);
    GitHubApiClient client = new GitHubApiClient(config, githubOAuthTokenUrl, userAccessToken);
    return client.getUserInstallations();
  }

  private void configureInstallation(GitHubApp gitHubApp, String ownerId, Long installationId, String accountName) {
    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      tx.begin();

      gitHubApp.setOwnerId(ownerId);
      gitHubApp.setInstallationId(installationId);
      gitHubApp.setActive(true);
      if (PERSONAL_ACCOUNT_MARKER.equals(gitHubApp.getGithubOrganizationName())) {
        gitHubApp.setGithubOrganizationName(accountName + PERSONAL_ACCOUNT_MARKER);
      }
      gitHubAppDAO.update(tx, gitHubApp);
      log.info("Updated GitHub App {} with installation {} for owner: {} and account: {}", gitHubApp.getId(),
          installationId, ownerId, accountName);

      tx.commit();

      // Invalidate caches after GitHub App configuration update
      selectionCache.invalidateAll();
      authStrategyCache.invalidate(gitHubApp.getId());
    }
  }

  private String generateStateToken() {
    return RandomStringUtils.secure().nextAlphanumeric(STATE_TOKEN_LENGTH);
  }

  private Map<String, String> createDefaultPermissions() {
    return Map.of(
        "contents", "write",
        "pull_requests", "write",
        "administration", "read",
        "statuses", "write",
        "deployments", "read",
        "metadata", "read");
  }

  private String generateRandomSuffix(final int length) {
    return RandomStringUtils.secure().next(length, true, true);
  }

  public GitHubAppInstallationState initiateInstallation(final String githubAppId) {

    GitHubApp gitHubApp = gitHubAppDAO.getByGithubAppId(githubAppId);
    if (gitHubApp == null) {
      throw new InternalServerErrorException("GitHub App not found: " + githubAppId);
    }

    final String stateToken = RandomStringUtils.secure().nextAlphanumeric(32);

    final GitHubAppInstallationState installationState = new GitHubAppInstallationState();
    installationState.setId(UUID.randomUUID().toString());
    installationState.setStateToken(stateToken);
    installationState.setGithubAppId(gitHubApp.getId());
    installationState.setCreatedAt(new Date());
    installationState.setExpiresAt(new Date(System.currentTimeMillis() + 10 * 60 * 1000L));

    installationStateDAO.insert(installationState);

    return installationState;
  }
}
