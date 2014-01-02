/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.client.utils.ClientException;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import com.sonatype.insight.scan.cli.RestClientFactory.RestClient;

import org.apache.http.client.HttpResponseException;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class PolicyEvaluator
{

  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluator.class);

  private final Scanner scanner;

  private final RestClientFactory restClientFactory;

  @Inject
  public PolicyEvaluator(Scanner scanner, RestClientFactory restClientFactory) {
    this.scanner = scanner;
    this.restClientFactory = restClientFactory;
  }

  public void run(Parameters params) throws ExitException {
    RestClient restClient = restClientFactory.newRestClient(newHttpClientConfig(params));

    validateServerAccess(params, restClient);

    validateInputPaths(params.getFiles());

    File scanFile = doScan(params, getProprietaryConfiguration(params, restClient));

    evaluatePolicy(params, restClient, scanFile);
  }

  private Configuration newHttpClientConfig(Parameters params) {
    Configuration config = new Configuration();
    config.setServerUrl(params.getServerUrl());
    config.setProxy(params.getProxy());
    config.setProxyAuth(SimpleAuthentication.parse(params.getProxyUser()));
    return config;
  }

  private void validateServerAccess(Parameters params, RestClient restClient) throws ExitException {
    log.info("Validating application ID {} with CLM server {}...", params.getApplicationId(), params.getServerUrl());
    Collection<String> appIds;
    try {
      appIds = restClient.getApplications().keySet();
    }
    catch (Exception e) {
      if (e instanceof HttpResponseException) {
        HttpResponseException resp = (HttpResponseException) e;
        if (resp.getStatusCode() == 503) {
          log.error("The CLM Server is down for maintenance, please try again later.");
        }
        else if (resp.getStatusCode() == 407) {
          log.error("The proxy server {} requires authentication: {}", params.getProxy(), e.getMessage());
        }
        else {
          log.error("The CLM server {} could not be contacted: {} ({})", params.getServerUrl(), e.getMessage(),
              resp.getStatusCode());
        }
      }
      else {
        log.error("The CLM server {} could not be contacted: {}", params.getServerUrl(), e.getMessage());
        log.error("Error details below:", e);
      }
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    if (!(appIds.contains(params.getApplicationId()))) {
      log.error("The application ID {} is invalid.", params.getApplicationId());
      log.error("The following application IDs are registered on the CLM server: {} ", appIds);
      throw new ExitException(1, String.format("The application ID %s is invalid.", params.getApplicationId()));
    }
  }

  private ProprietaryConfig getProprietaryConfiguration(Parameters params, RestClient restClient) throws ExitException {
    log.debug("Retrieving configuration for proprietary components from CLM server...");
    try {
      return restClient.getProprietaryConfiguration();
    }
    catch (IOException e) {
      if (e instanceof HttpResponseException && ((HttpResponseException) e).getStatusCode() == 404) {
        log.warn("CLM server is outdated and does not provide configuration for proprietary components");
        return new ProprietaryConfig();
      }
      log.error("Could not retrieve configuration for proprietary components from CLM server", e);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
  }

  private void validateInputPaths(List<File> files) throws ExitException {
    if (files.isEmpty()) {
      log.error("The archives or directories to scan were not specified.");
      throw new ExitException(1, "The archives or directories to scan were not specified.");
    }
    for (File file : files) {
      if (!file.exists()) {
        log.error("The input path '{}' does not exist.", file.getAbsolutePath());
        throw new ExitException(1, String.format("The input path '%s' does not exist.", file.getAbsolutePath()));
      }
    }
  }

  private File doScan(Parameters params, ProprietaryConfig proprietaryConfig) throws ExitException {
    try {
      params.getOutputDirectory().mkdirs();
      File scanFile = File.createTempFile("scan-", ".xml.gz", params.getOutputDirectory());
      scanner.scan(scanFile, params.getFiles(), getScanConfiguration(params, proprietaryConfig));
      return scanFile;
    }
    catch (IOException e) {
      log.error("The scan could not be performed", e);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
  }

  private Properties getScanConfiguration(Parameters params, ProprietaryConfig proprietaryConfig) {
    Properties props = new Properties();
    if (proprietaryConfig != null) {
      props.put("proprietaryPackages", StringUtils.join(proprietaryConfig.getPackages().iterator(), ","));
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

  private PolicyEvaluationResult evaluatePolicy(Parameters params, RestClient restClient, File scanFile)
      throws ExitException
  {
    ScanReceipt receipt = uploadScan(params, restClient, scanFile);
    log.info("Fetching results of policy evaluation (ETA {}s)...", receipt.getTimeToReport());
    PolicyEvaluationResult eval;
    try {
      if (receipt.getTimeToReport() != null) {
        Thread.sleep(receipt.getTimeToReport() * 1000);
      }
      eval = restClient.evaluatePolicy(params.getApplicationId(), receipt.getScanId(), params.getStage()
          .getStageTypeId());
    }
    catch (HttpResponseException e) {
      log.error("The policy evaluation results could not be fetched from the CLM server: {} ({})", e.getMessage(),
          e.getStatusCode());
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    catch (ClientException e) {
      log.error("The policy evaluation results could not be fetched from the CLM server: {} ({})", e.getMessage(), e
          .getResult().status());
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    catch (IOException e) {
      log.error("The policy evaluation results could not be fetched from the CLM server", e);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    catch (InterruptedException e) {
      log.error("The process was interrupted");
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }

    log.info("");
    log.info("");
    log.info("");
    log.info("");

    PolicyAction outcome = PolicyAction.NONE;
    for (PolicyAlert alert : eval.getAlerts()) {
      PolicyFact trigger = alert.getTrigger();
      for (final Action action : alert.getActions()) {
        final String actionTypeId = action.getActionTypeId();
        if (Action.ID_FAIL.equals(actionTypeId)) {
          outcome = outcome.combine(PolicyAction.FAIL);
          log.error("Sonatype CLM reports policy failing due to " + trigger);
        }
        else if (Action.ID_WARN.equals(actionTypeId)) {
          outcome = outcome.combine(PolicyAction.WARN);
          log.warn("Sonatype CLM reports policy warning due to " + trigger);
        }
      }
    }

    String reportUrl = receipt.resolveReportUrl(params.getServerUrl());

    if (!PolicyAction.NONE.equals(outcome)) {
      log.info("");
      log.info("");
    }
    log.info("*********************************************************************************************");
    log.info("Policy Action: {}", outcome);
    log.info("Lifecycle stage: {}", params.getStage().getStageTypeId());
    log.info("Summary of policy violations: {} critical, {} severe, {} moderate", eval.getCriticalComponentCount(),
        eval.getSevereComponentCount(), eval.getModerateComponentCount());
    log.info("The detailed report can be viewed online at {}", reportUrl);
    log.info("*********************************************************************************************");

    if (outcome.equals(PolicyAction.FAIL)) {
      throw new ExitException(1, "Sonatype CLM reports policy failing.");
    }
    else if (outcome.equals(PolicyAction.WARN) && params.isFailOnPolicyWaring()) {
      throw new ExitException(1, "Sonatype CLM reports policy warning.");
    }

    return eval;
  }

  private ScanReceipt uploadScan(Parameters params, RestClient restClient, File scanFile) throws ExitException {
    log.info("Submitting scan to CLM server...");
    try {
      return restClient.uploadScan(params.getApplicationId(), scanFile);
    }
    catch (HttpResponseException e) {
      log.error("The scan could not be submitted to the CLM server: {} ({})", e.getMessage(), e.getStatusCode());
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
    catch (IOException e) {
      log.error("The scan could not be submitted to the CLM server", e);
      throw new ExitException(params.isIgnoreSystemErrors(), e);
    }
  }
}
