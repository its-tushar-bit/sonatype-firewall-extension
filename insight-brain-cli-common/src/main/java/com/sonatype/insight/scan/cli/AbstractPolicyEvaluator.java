/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.callflowanalysis.ApiCallFlowAnalysisConfigDTO;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.signature.ComponentWithSignaturesList;
import com.sonatype.clm.dto.model.signature.VulnerabilitySignatureAnalysisDTO;
import com.sonatype.insight.brain.client.PolicyAction;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.brain.client.UnsupportedServerVersionException;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import com.sonatype.insight.client.utils.UserAgentUtils;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.ScanMetadata;
import com.sonatype.insight.scanner.call.flow.analyzer.CallFlowAnalysisConfig;
import com.sonatype.insight.scanner.call.flow.analyzer.CallFlowGraphExtractor;
import com.sonatype.nexus.git.utils.Environment.AzureDevOpsCI;
import com.sonatype.nexus.git.utils.Environment.GitLabCI;
import com.sonatype.nexus.git.utils.commit.CommitHashFinderBuilder;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPolicyEvaluator<P extends AbstractParameters>
{
  public static final String MINIMAL_SERVER_VERSION_REQUIRED = "1.69.0";

  private static final Logger log = LoggerFactory.getLogger(AbstractPolicyEvaluator.class);

  private static final List<String> DEFAULT_MODULE_INCLUDES =
      Collections.unmodifiableList(Arrays.asList("**/sonatype-clm/module.xml", "**/nexus-iq/module.xml"));

  private static final String[] MODULE_INDICES_SUFFIXES = {
      "/sonatype-clm/module.xml",
      "/nexus-iq/module.xml"
  };

  protected final RestClientFactory restClientFactory;

  private final Scanner scanner;

  protected AbstractPolicyEvaluator(Scanner scanner,
                                    RestClientFactory restClientFactory)
  {
    this.scanner = scanner;
    this.restClientFactory = restClientFactory;
  }

  void run(P params) throws ExitException {
    logCliAndEnvironmentInfo();

    RestClient restClient = createClient(params);

    validate(params, restClient);

    CliScanResult cliScanResult = scan(params, getProprietaryConfiguration(params, restClient),
        getLicensedFeatures(params, restClient), restClient);

    evaluatePolicy(params, restClient, cliScanResult, getClientScanType());

    if (cliScanResult.hasScanningErrors() && !params.isIgnoreScanningErrors()) {
      saveErrorData(params, CLIError.forScanningError("Scanning errors encountered"), restClient);
      throw new ExitException(2);
    }
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
    String nonProxyHostsSystemProperty = System.getProperty("http.nonProxyHosts");
    if (nonProxyHostsSystemProperty != null && nonProxyHostsSystemProperty.length() != 0) {
      List<String> nonProxyHosts = Arrays.asList(nonProxyHostsSystemProperty.split(","));
      config.setProxyExcludeHosts(nonProxyHosts);
    }
    config.setServerAuth(SimpleAuthentication.parse(params.getServerUser()));
    return config;
  }

  private void validateServerAccess(P params, RestClient restClient) throws ExitException {
    log.info("Validating application ID {} with the IQ Server {}...", params.getApplicationId(), params.getServerUrl());
    boolean isApplicationAllowed;
    try {
      isApplicationAllowed =
          restClient.verifyOrCreateApplication(params.getApplicationId(), params.getOrganizationId());
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
      String message =
          StringUtils.isBlank(params.getOrganizationId()) ? String.format("The application ID %s is invalid.",
              params.getApplicationId()) : String.format("The application ID %s is invalid for organization ID %s.",
              params.getApplicationId(), params.getOrganizationId());
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
      if (isContainerTargetSoSkipFileExistsCheck(scanTarget)) {
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

  protected CliScanResult scan(
      P params,
      ProprietaryConfig proprietaryConfig,
      Set<String> licensedFeatures,
      RestClient restClient) throws ExitException
  {
    ScanMetadata scanMetadata = verifyAndPopulateMetadata(params);
    try {
      Files.createDirectories(params.getOutputDirectory().toPath());
      File scanFile = Files.createTempFile(params.getOutputDirectory().toPath(), "scan-", ".xml.gz").toFile();
      if (!params.isKeepScanFile()) {
        scanFile.deleteOnExit();
      }

      List<File> files = new ArrayList<>();
      for (String scanTarget : params.getScanTargets()) {
        files.add(new File(scanTarget));
      }
      List<File> moduleIndices = getModuleIndices(params.getBaseDir(), files, params.getModuleExcludes());

      log.debug("Saving scan file to {}", scanFile.getAbsolutePath());

      return scanner.scan(scanFile, params.getBaseDir(), files, moduleIndices,
          getScanConfiguration(params.getProperties(), proprietaryConfig), scanMetadata, licensedFeatures);
    }
    catch (IOException e) {
      log.error("The scan could not be performed", e);
      saveErrorData(params, CLIError.forSystemError("The scan could not be performed: " + e.getMessage()), restClient);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
  }

  // Visible for testing
  List<File> getModuleIndices(File baseDirectory, List<File> targets, List<String> moduleExcludes) {
    List<File> moduleIndices = new ArrayList<>();

    for (File target : targets) {
      if (target.getPath().startsWith("container:") || target.getPath().startsWith("iac:") ) {
        continue;
      }
      if (target.isFile()) {
        if (fileIsModule(target)) {
          moduleIndices.add(target);
        }
      }
      else
      {
        if (baseDirectory != null && !target.isAbsolute()) {
          target = baseDirectory.toPath().resolve(target.getPath()).toFile();
        }
        moduleIndices.addAll(getModuleIndices(target.getAbsoluteFile(), moduleExcludes));
      }
    }
    return moduleIndices;
  }

  private boolean fileIsModule(File file) {
    for (String suffix : MODULE_INDICES_SUFFIXES) {
      String path = FilenameUtils.separatorsToUnix(file.getAbsolutePath());
      if (path.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  // Visible for testing
  List<File> getModuleIndices(File baseDirectory, List<String> moduleExcludes) {
    DirectoryScanner directoryScanner = new DirectoryScanner();
    directoryScanner.setBasedir(baseDirectory);
    directoryScanner.setIncludes(DEFAULT_MODULE_INCLUDES.toArray(new String[0]));
    if (moduleExcludes != null) {
      directoryScanner.setExcludes(moduleExcludes.toArray(new String[0]));
    }
    directoryScanner.addDefaultExcludes();
    directoryScanner.scan();
    return Arrays.stream(directoryScanner.getIncludedFiles())
        .map(f -> new File(baseDirectory, f))
        .sorted()
        .collect(Collectors.toList());
  }

  protected Properties getScanConfiguration(List<String> properties, ProprietaryConfig proprietaryConfig) {
    Properties props = new Properties();
    if (proprietaryConfig != null) {
      props.put("proprietaryPackages", StringUtils.join(proprietaryConfig.getPackages().iterator(), ","));
      props.put("proprietaryRegexes", StringUtils.join(proprietaryConfig.getRegexes().iterator(), ":::"));
    }
    for (String property : properties) {
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
    PolicyEvaluationResult policyEvaluationResult = eval.getResult();
    ApiCallFlowAnalysisConfigDTO iqCallFlowParams = fetchCallFlowAnalysisConfig(params, restClient);
    if (shouldRunCallFlowAnalysis(iqCallFlowParams, params)) {
      policyEvaluationResult = runCallFlowAnalysis(restClient, eval, params, iqCallFlowParams);
    }

    log.info("");
    log.info("");
    log.info("");
    log.info("");

    PolicyAction outcome = PolicyAction.NONE;
    for (PolicyAlert alert : policyEvaluationResult.getAlerts()) {
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

    processResults(params, eval.getScanReceipt(), policyEvaluationResult, outcome, restClient);
  }

  private ApiCallFlowAnalysisConfigDTO fetchCallFlowAnalysisConfig(P params, RestClient restClient) {
    try {
      return restClient.getCallFlowAnalysisConfig("application", params.getApplicationId());
    }
    catch (HttpResponseException e) {
      if (e.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
        log.error("Could not fetch IQ params for application {}", params.getApplicationId(), e);
      }
    }
    catch (IOException e) {
      log.error("The call flow analysis configuration for application ID {} could not be fetched from the IQ Server",
          params.getApplicationId());
    }
    return null;
  }

  private boolean shouldRunCallFlowAnalysis(ApiCallFlowAnalysisConfigDTO iqCallFlowParams, P params) {
    return (iqCallFlowParams != null && iqCallFlowParams.enabled) ||
        params.isRunCallFlowAnalysis() || params.getCallFlowAnalysisNamespaces() != null;
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
        .withEnvironmentVariableNamed(AzureDevOpsCI.COMMIT_HASH_ENV_VARIABLE_AUTO_TRIGGER)
        .withEnvironmentVariableNamed(AzureDevOpsCI.COMMIT_HASH_ENV_VARIABLE_MANUAL_TRIGGER)
        .withEnvironmentVariableNamed(GitLabCI.COMMIT_HASH_ENV_VARIABLE)
        .withEnvironmentVariableDefault()
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

  private PolicyEvaluationResult runCallFlowAnalysis(
      RestClient restClient,
      PolicyEvaluationPollingResult policyEvaluationResult,
      P params,
      ApiCallFlowAnalysisConfigDTO iqCallFlowParams) throws ExitException
  {
    log.info("Running call flow analysis...");
    StopWatch stopWatch = StopWatch.createStarted();

    List<String> namespaces = prepareNamespaces(params, iqCallFlowParams);

    PolicyEvaluationResult result = policyEvaluationResult.getResult();
    String scanId = policyEvaluationResult.getScanReceipt().getScanId();
    try {
      ComponentWithSignaturesList vulnerableComponentsSignatures =
          restClient.getVulnerableComponentsWithSignatures(params.getApplicationId(), scanId);

      if (vulnerableComponentsSignatures != null && !vulnerableComponentsSignatures.getComponents().isEmpty()) {
        CallFlowAnalysisConfig config = new CallFlowAnalysisConfig(
            params.getScanTargets(),
            namespaces,
            // Pass down parameters entered by the user
            getScanConfiguration(params.getProperties(), null),
            iqCallFlowParams
        );

        CallFlowGraphExtractor extractor = CallFlowGraphExtractor.newInstance(log, config);

        VulnerabilitySignatureAnalysisDTO analysisDto = extractor
            .buildCallFlowGraph()
            .buildVulnerabilitySignatureAnalysis(vulnerableComponentsSignatures);

        result = restClient.importReachabilityAnalysis(params.getApplicationId(), scanId, analysisDto);

        stopWatch.stop();
        log.info("Call flow analysis completed in {} seconds.", stopWatch.getTime(TimeUnit.SECONDS));
      }
      else {
        stopWatch.stop();
        log.info("Call flow analysis skipped due to not finding vulnerable signatures.");
      }
    }
    catch (Exception e) {
      String message = String.format("Call flow analysis for application ID %s and scan ID %s failed: %s",
          params.getApplicationId(), scanId, e.getMessage());
      log.error(message, e);
      saveErrorData(params, CLIError.forSystemError(message), restClient);
    }
    return result;
  }

  private List<String> prepareNamespaces(P params, ApiCallFlowAnalysisConfigDTO iqCallFlowParams) {
    if (params.getCallFlowAnalysisNamespaces() != null) {
      return new ArrayList<>(params.getCallFlowAnalysisNamespaces());
    }
    else if (iqCallFlowParams != null && iqCallFlowParams.namespaces != null) {
      return new ArrayList<>(iqCallFlowParams.namespaces);
    }
    return null;
  }

  private void logCliAndEnvironmentInfo() {
    logCliVersion();
    logJavaVersionAndHome();
    logLocaleAndEncoding();
    logOsInformation();
  }

  private void logCliVersion() {
    try {
      String name = "Sonatype IQ CLI";
      String clientName = UserAgentUtils.getClientName();
      if ("Docker_Nexus_IQ_CLI".equals(clientName)) {
        name += " (Docker)";
      }
      else if ("Sonatype_CLM_CLI_NATIVE".equals(clientName)) {
        name += " (Native)";
      }
      String version = UserAgentUtils.getClientVersion();
      if (StringUtils.isNotEmpty(version)) {
        String jarSha1 = getJarSha1();
        if (StringUtils.isNotEmpty(jarSha1)) {
          log.info("{} version: {} ({})", name, version, jarSha1);
        }
        else {
          log.info("{} version: {}", name, version);
        }
      }
    }
    catch (Exception e) {
      log.info("Could not log iq cli version and sha1.", e);
    }
  }

  private void logJavaVersionAndHome() {
    try {
      String javaVersion = System.getProperty("java.version");
      String javaHome = System.getProperty("java.home");
      if (StringUtils.isAnyEmpty(javaVersion, javaHome)) {
        log.debug("Could not log java version and java home information.");
      }
      else {
        log.info("Java version: {}, Java Home: {}", javaVersion, javaHome);
      }
    }
    catch (Exception e) {
      log.debug("Could not log java version and java home information.", e);
    }
  }

  private void logLocaleAndEncoding() {
    try {
      String locale = Locale.getDefault().toString();
      String fileEncoding = System.getProperty("file.encoding");
      if (StringUtils.isAnyEmpty(locale, fileEncoding)) {
        log.debug("Could not log locale and file encoding.");
      }
      else {
        log.info("Default locale: {}, platform encoding: {}", locale, fileEncoding);
      }
    }
    catch (Exception e) {
      log.debug("Could not log locale and file encoding.", e);
    }
  }

  private void logOsInformation() {
    try {
      String osName = System.getProperty("os.name");
      String osVersion = System.getProperty("os.version");
      String osArch = System.getProperty("os.arch");
      if (StringUtils.isAnyEmpty(osName, osVersion, osArch)) {
        log.debug("Could not log OS information");
      }
      else {
        log.info("OS name: {}, version: {}, arch: {}", osName, osVersion, osArch);
      }
    }
    catch (Exception e) {
      log.debug("Could not log OS information", e);
    }
  }

  private String getJarSha1() {
    try {
      String filePath = Scanner.class.getProtectionDomain().getCodeSource().getLocation().getPath();
      StringBuilder hexString = new StringBuilder();
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      try (FileInputStream fis = new FileInputStream(filePath);
           DigestInputStream dis = new DigestInputStream(fis, md)) {
        byte[] buffer = new byte[8192];
        while (dis.read(buffer) != -1) {
        }
        byte[] digest = md.digest();
        for (byte b : digest) {
          hexString.append(String.format("%02x", b));
        }
      }
      return hexString.toString();
    }
    catch (Exception e) {
      log.debug("Could not extract jar sha-1.", e);
      return "";
    }
  }
}
