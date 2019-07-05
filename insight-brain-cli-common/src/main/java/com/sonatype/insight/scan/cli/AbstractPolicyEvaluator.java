/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.brain.client.UnsupportedServerVersionException;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import com.sonatype.insight.scan.model.ClientScanType;

import org.apache.http.client.HttpResponseException;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPolicyEvaluator<P extends AbstractParameters>
{
  private static final Logger log = LoggerFactory.getLogger(AbstractPolicyEvaluator.class);

  public static final String MINIMAL_SERVER_VERSION_REQUIRED = "1.69.0";

  private final Scanner scanner;

  protected final RestClientFactory restClientFactory;

  protected AbstractPolicyEvaluator(Scanner scanner, RestClientFactory restClientFactory) {
    this.scanner = scanner;
    this.restClientFactory = restClientFactory;
  }

  void run(P params) throws ExitException {
    RestClient restClient = createClient(newHttpClientConfig(params));

    validateServerVersion(params, restClient);

    validateServerAccess(params, restClient);

    validateScanTargets(params.getScanTargets());

    File scanFile = scan(params, getProprietaryConfiguration(params, restClient));

    evaluatePolicy(params, restClient, scanFile, getClientScanType());
  }

  protected abstract ClientScanType getClientScanType();

  private Configuration newHttpClientConfig(P params) {
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
      throw handleHttpResponseException(params, e);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    if (!isApplicationAllowed) {
      log.error("The application ID {} is invalid.", params.getApplicationId());
      throw new ExitException(1, String.format("The application ID %s is invalid.", params.getApplicationId()));
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

  protected void validateScanTargets(List<String> scanTargets) throws ExitException {
    if (scanTargets.isEmpty()) {
      log.error("The archives or directories to scan were not specified.");
      throw new ExitException(1, "The archives or directories to scan were not specified.");
    }
    for (String scanTarget : scanTargets) {
      File file = new File(scanTarget);
      if (!file.exists()) {
        log.error("The input path '{}' does not exist.", file.getAbsolutePath());
        throw new ExitException(1, String.format("The input path '%s' does not exist.", file.getAbsolutePath()));
      }
    }
  }

  protected File scan(P params, ProprietaryConfig proprietaryConfig) throws ExitException {
    try {
      params.getOutputDirectory().mkdirs();
      File scanFile = File.createTempFile("scan-", ".xml.gz", params.getOutputDirectory());
      List<File> files = new ArrayList<>();
      for (String scanTarget : params.getScanTargets()) {
        files.add(new File(scanTarget));
      }
      scanner.scan(scanFile, files, getScanConfiguration(params, proprietaryConfig), params.getScanMetadata());
      return scanFile;
    }
    catch (IOException e) {
      log.error("The scan could not be performed", e);
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

  protected void evaluatePolicy(P params, RestClient restClient, File scanFile, ClientScanType clientScanType)
      throws ExitException
  {

    PolicyEvaluationPollingResult eval;
    try {
      eval = restClient
          .evaluatePolicy(params.getApplicationId(), params.getStage().getStageTypeId(), scanFile, clientScanType);
    }
    catch (HttpResponseException e) {
      log.error("The policy evaluation results for app ID {} could not be fetched from the IQ Server: {} ({})",
          params.getApplicationId(), e.getMessage(), e.getStatusCode());
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    catch (IOException e) {
      log.error("The policy evaluation results for application ID {} could not be fetched from the IQ Server",
          params.getApplicationId(), e);
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
      throw handleHttpResponseException(params, e);
    }
    catch (UnsupportedServerVersionException e) {
      log.error(e.getMessage());
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    catch (Exception e) {
      log.error("The IQ Server {} could not be contacted: {}", params.getServerUrl(), e.getMessage());
      log.error("Error details below:", e);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
  }

  private ExitException handleHttpResponseException(P params, HttpResponseException e) {
    if (e.getStatusCode() == 503) {
      log.error("The IQ Server is down for maintenance, please try again later.");
    }
    else if (e.getStatusCode() == 407) {
      log.error("The proxy server {} requires authentication: {}", params.getProxy(), e.getMessage());
    }
    else if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
      log.error("The IQ Server {} rejected the supplied credentials.", params.getServerUrl());
    }
    else {
      log.error("The IQ Server {} could not be contacted: {} ({})", params.getServerUrl(), e.getMessage(),
          e.getStatusCode());
    }
    return new ExitException(params.isIgnoreSystemErrors(), e);
  }
}
