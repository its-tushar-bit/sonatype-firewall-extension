/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.client.PolicyAction;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.brain.client.UnsupportedServerVersionException;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ScanMetadata;
import com.sonatype.nexus.git.utils.Environment.GitLabCI;
import com.sonatype.nexus.git.utils.commit.CommitHashFinderBuilder;

import org.apache.http.client.HttpResponseException;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPolicyEvaluator<P extends AbstractParameters>
{
  public static final String MINIMAL_SERVER_VERSION_REQUIRED = "1.69.0";

  private static final Logger log = LoggerFactory.getLogger(AbstractPolicyEvaluator.class);

  protected final RestClientFactory restClientFactory;

  private final Scanner scanner;

  protected AbstractPolicyEvaluator(Scanner scanner,
                                    RestClientFactory restClientFactory)
  {
    this.scanner = scanner;
    this.restClientFactory = restClientFactory;
  }

  void run(P params) throws ExitException {
    RestClient restClient = createClient(params);

    validate(params, restClient);

    ClientScanResult clientScanResult = scan(params, getProprietaryConfiguration(params, restClient),
        getLicensedFeatures(params, restClient), restClient);

    evaluatePolicy(params, restClient, clientScanResult, getClientScanType());
  }

  protected abstract ClientScanType getClientScanType();

  /**
   * Validate the given {@link AbstractParameters} (params) with a given {@link RestClient}
   *
   * @param params     - {@link AbstractParameters}
   * @param restClient - {@link RestClient}
   * @throws ExitException when validation fails
   * @since 1.101
   */
  protected void validate(final P params, final RestClient restClient) throws ExitException {
    validateServerVersion(params, restClient);

    validateServerAccess(params, restClient);

    validateScanTargets(params, restClient);
  }

  protected Configuration newHttpClientConfig(P params) {
    Configuration config = new Configuration();
    config.setServerUrl(params.getServerUrl());
    config.setProxy(params.getProxy());
    config.setProxyAuth(SimpleAuthentication.parse(params.getProxyUser()));
    config.setServerAuth(SimpleAuthentication.parse(params.getServerUser()));
    return config;
  }

  private void validateServerAccess(P params, RestClient restClient) throws ExitException {
    log.info("Validating application ID {} with the IQ Server {}...", params.getApplicationId(), params.getServerUrl());
    boolean isApplicationAllowed;
    try {
      isApplicationAllowed = restClient.verifyOrCreateApplication(params.getApplicationId());
    }
    catch (HttpResponseException e) {
      throw handleHttpResponseException(params, e, restClient);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      saveErrorData(params, CLIError.forSystemError(e.getMessage()), restClient);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    if (!isApplicationAllowed) {
      String message = String.format("The application ID %s is invalid.", params.getApplicationId());
      log.error(message);
      saveErrorData(params, CLIError.forConfigurationError(message), restClient);
      throw new ExitException(1, message);
    }
  }

  @SuppressWarnings("unused")
  protected void saveErrorData(
      @SuppressWarnings("unused") P params,
      @SuppressWarnings("unused") CLIError error,
      @SuppressWarnings("unused") RestClient restClient) throws ExitException
  {
  }

  protected Set<String> getLicensedFeatures(P params, RestClient restClient) throws ExitException {
    log.debug("Retrieving licensed features from the IQ Server...");
    try {
      return restClient.getLicensedFeatures();
    }
    catch (Exception e) {
      log.error("Could not retrieve licensed features from the IQ Server", e);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
  }

  protected ProprietaryConfig getProprietaryConfiguration(P params, RestClient restClient) throws ExitException {
    log.debug("Retrieving configuration for proprietary components from the IQ Server...");
    try {
      return restClient.getProprietaryConfigForApplicationEvaluation(params.getApplicationId());
    }
    catch (Exception e) {
      log.error("Could not retrieve configuration for proprietary components from the IQ Server", e);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
  }

  protected void validateScanTargets(P params, RestClient restClient) throws ExitException {
    if (params.getScanTargets().isEmpty()) {
      String message = "The archives or directories to scan were not specified.";
      log.error(message);
      saveErrorData(params, CLIError.forConfigurationError(message), restClient);
      throw new ExitException(1, message);
    }
    for (String scanTarget : params.getScanTargets()) {
      if (isContainerTargetSoSkipFileExistsCheck(scanTarget) || isIacTargetSoSkipFileExistsCheck(scanTarget)) {
        continue;
      }

      File file = new File(scanTarget);
      if (!file.exists()) {
        String message = String.format("The input path '%s' does not exist.", file.getAbsolutePath());
        log.error(message);
        saveErrorData(params, CLIError.forConfigurationError(message), restClient);
        throw new ExitException(1, message);
      }
    }
  }

  private boolean isContainerTargetSoSkipFileExistsCheck(final String scanTarget) {
    return scanTarget.startsWith("container:");
  }

  private boolean isIacTargetSoSkipFileExistsCheck(final String scanTarget) {
    return scanTarget.startsWith("iac:");
  }

  protected ClientScanResult scan(
      P params,
      ProprietaryConfig proprietaryConfig,
      Set<String> licensedFeatures,
      RestClient restClient) throws ExitException
  {
    ScanMetadata scanMetadata = verifyAndPopulateMetadata(params);
    try {
      Files.createDirectories(params.getOutputDirectory().toPath());
      File scanFile = Files.createTempFile(params.getOutputDirectory().toPath(), "scan-", ".xml.gz").toFile();
      List<File> files = new ArrayList<>();
      for (String scanTarget : params.getScanTargets()) {
        files.add(new File(scanTarget));
      }

      log.debug("Saving scan file to {}", scanFile.getAbsolutePath());

      return scanner.scan(scanFile, params.getBaseDir(), files,
          getScanConfiguration(params, proprietaryConfig), scanMetadata, licensedFeatures);
    }
    catch (IOException e) {
      log.error("The scan could not be performed", e);
      saveErrorData(params, CLIError.forSystemError("The scan could not be performed: " + e.getMessage()), restClient);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
  }

  protected Properties getScanConfiguration(P params, ProprietaryConfig proprietaryConfig) {
    Properties props = new Properties();
    if (proprietaryConfig != null) {
      props.put("proprietaryPackages", StringUtils.join(proprietaryConfig.getPackages().iterator(), ","));
      props.put("proprietaryRegexes", StringUtils.join(proprietaryConfig.getRegexes().iterator(), ":::"));
    }
    for (String property : params.getProperties()) {
      int eq = property.indexOf('=');
      if (eq < 0) {
        props.setProperty(property, "true");
      }
      else {
        String key = property.substring(0, eq);
        String val = property.substring(eq + 1);
        props.setProperty(key, val);
      }
    }
    return props;
  }

  protected void evaluatePolicy(P params,
                                RestClient restClient,
                                ClientScanResult clientScanResult,
                                ClientScanType clientScanType) throws ExitException
  {

    PolicyEvaluationPollingResult eval;
    try {
      eval = restClient
          .evaluatePolicy(params.getApplicationId(), params.getStage().getStageTypeId(), clientScanResult,
              clientScanType);
    }
    catch (HttpResponseException e) {
      String message = String.format("The policy evaluation results for app ID %s could not " +
          "be fetched from the IQ Server: %s (%s)", params.getApplicationId(), e.getMessage(), e.getStatusCode());
      log.error(message);
      saveErrorData(params, CLIError.forSystemError(message), restClient);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    catch (IOException e) {
      String message = String.format("The policy evaluation results for application ID %s could not " +
          "be fetched from the IQ Server", params.getApplicationId());
      log.error(message);
      saveErrorData(params, CLIError.forSystemError(message), restClient);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }

    log.info("");
    log.info("");
    log.info("");
    log.info("");

    PolicyAction outcome = PolicyAction.NONE;
    for (PolicyAlert alert : eval.getResult().getAlerts()) {
      PolicyFact trigger = alert.getTrigger();
      for (final Action action : alert.getActions()) {
        final String actionTypeId = action.getActionTypeId();
        if (Action.ID_FAIL.equals(actionTypeId)) {
          outcome = outcome.combine(PolicyAction.FAIL);
          log.error("The IQ Server reports policy failing due to {}", trigger);
        }
        else if (Action.ID_WARN.equals(actionTypeId)) {
          outcome = outcome.combine(PolicyAction.WARN);
          log.warn("The IQ Server reports policy warning due to {}", trigger);
        }
      }
    }

    processResults(params, eval.getScanReceipt(), eval.getResult(), outcome, restClient);
  }

  protected RestClient createClient(Configuration configuration) {
    return restClientFactory.newRestCIClient(configuration);
  }

  protected RestClient createClient(P params) {
    return createClient(newHttpClientConfig(params));
  }

  protected abstract void processResults(P params,
                                         ScanReceipt receipt,
                                         PolicyEvaluationResult eval,
                                         PolicyAction outcome,
                                         RestClient restClient) throws ExitException;

  private void validateServerVersion(P params, RestClient restClient) throws ExitException {
    log.info("Validating IQ Server version {}...", params.getServerUrl());

    try {
      restClient.validateServerVersion(MINIMAL_SERVER_VERSION_REQUIRED);
    }
    catch (HttpResponseException e) {
      throw handleHttpResponseException(params, e, restClient);
    }
    catch (UnsupportedServerVersionException e) {
      log.error(e.getMessage());
      saveErrorData(params, CLIError.forSystemError(e.getMessage()), restClient);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    catch (Exception e) {
      String message = String.format("The IQ Server %s could not be contacted: %s",
          params.getServerUrl(), e.getMessage());
      log.error(message);
      log.error("Error details below:", e);
      saveErrorData(params, CLIError.forSystemError(message), restClient);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
  }

  private ExitException handleHttpResponseException(P params,
                                                    HttpResponseException e,
                                                    RestClient restClient) throws ExitException
  {
    if (e.getStatusCode() == 503) {
      String message = "The IQ Server is down for maintenance, please try again later.";
      log.error(message);
      saveErrorData(params, CLIError.forSystemError(message), restClient);
    }
    else if (e.getStatusCode() == 407) {
      String message = String.format("The proxy server %s requires authentication: %s",
          params.getProxy(), e.getMessage());
      log.error(message);
      saveErrorData(params, CLIError.forSystemError(message), restClient);
    }
    else if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
      String message = String.format("The IQ Server %s rejected the supplied credentials.", params.getServerUrl());
      log.error(message);
      saveErrorData(params, CLIError.forConfigurationError(message), restClient);
    }
    else {
      String message = String.format("The IQ Server %s could not be contacted: %s (%s)",
          params.getServerUrl(), e.getMessage(), e.getStatusCode());
      log.error(message);
      saveErrorData(params, CLIError.forSystemError(message), restClient);
    }
    return new ExitException(params.isIgnoreSystemErrors(), e);
  }

  private ScanMetadata verifyAndPopulateMetadata(P params) {
    ScanMetadata scanMetadata = params.getScanMetadata() == null ? new ScanMetadata() : params.getScanMetadata();
    Optional<String> optional = new CommitHashFinderBuilder()
        .withEnvironmentVariableDefault()
        .withEnvironmentVariableNamed(GitLabCI.COMMIT_HASH_ENV_VARIABLE)
        .withGitRepo()
        .withFallBack(scanMetadata.getCommitHash())
        .build()
        .tryGetCommitHash();
    if (optional.isPresent()) {
      scanMetadata.setCommitHash(optional.get());
    }
    else {
      log.debug("Commit hash for application with id: {} could not be found.", params.getApplicationId());
    }
    return scanMetadata;
  }
}
