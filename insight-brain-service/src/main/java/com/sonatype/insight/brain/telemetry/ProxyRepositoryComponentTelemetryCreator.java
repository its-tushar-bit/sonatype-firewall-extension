/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ProxyRepositoryComponentTelemetryCreator
{
  private static final Logger log = LoggerFactory.getLogger(ProxyRepositoryComponentTelemetryCreator.class);

  // Wire-format value is "repository_component" for historical reasons: this string is emitted
  // as a JSON attribute key on TelemetryData records that flow to the downstream telemetry
  // schema (Segment / Databricks). Renaming the value in isolation would break the schema
  // contract on those consumers. TODO (CLM-43588): rename this constant to
  // PROXY_REPOSITORY_COMPONENT_TELEMETRY in unison with a coordinated update to the downstream
  // telemetry schema in HDS.
  public static final String REPOSITORY_COMPONENT_TELEMETRY = "repository_component";

  public static final String POLICY_VIOLATION_TELEMETRY = "policy_violations";

  private final TelemetrySender telemetrySender;

  private final PendoCache pendoCache;

  private final RepositoryDAO repositoryDAO;

  private final TelemetryDataObfuscator telemetryDataObfuscator;

  @Inject
  public ProxyRepositoryComponentTelemetryCreator(
      final TelemetrySender telemetrySender,
      final PendoCache pendoCache,
      final RepositoryDAO repositoryDAO,
      final TelemetryDataObfuscator telemetryDataObfuscator)
  {
    this.telemetrySender = telemetrySender;
    this.pendoCache = pendoCache;
    this.repositoryDAO = repositoryDAO;
    this.telemetryDataObfuscator = telemetryDataObfuscator;
  }

  public void sendRepositoryComponentTelemetry(
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final List<ProxyRepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType)
  {
    sendRepositoryComponentTelemetry(proxyRepositoryComponent, policyViolations, repositoryManagerId,
        repositoryComponentTelemetryEventType, null, null, Collections.emptyList());
  }

  public void sendRepositoryComponentTelemetry(
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final List<ProxyRepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final List<PolicyNotification> policyNotifications)
  {
    sendRepositoryComponentTelemetryInternal(proxyRepositoryComponent, policyViolations, repositoryManagerId, null,
        repositoryComponentTelemetryEventType, null, null, policyNotifications, null);
  }

  public void sendRepositoryComponentTelemetry(
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final List<ProxyRepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final List<PolicyNotification> policyNotifications,
      final Component component)
  {
    sendRepositoryComponentTelemetryInternal(proxyRepositoryComponent, policyViolations, repositoryManagerId, null,
        repositoryComponentTelemetryEventType, null, null, policyNotifications, component);
  }

  public void sendRepositoryComponentTelemetry(
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final List<ProxyRepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final String repositoryName,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final List<PolicyNotification> policyNotifications,
      final Component component)
  {
    sendRepositoryComponentTelemetryInternal(proxyRepositoryComponent, policyViolations, repositoryManagerId,
        repositoryName,
        repositoryComponentTelemetryEventType, null, null, policyNotifications, component);
  }

  public void sendRepositoryComponentTelemetry(
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final List<ProxyRepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final ReleaseQuarantineType releaseQuarantineType)
  {
    sendRepositoryComponentTelemetry(proxyRepositoryComponent, policyViolations, repositoryManagerId,
        repositoryComponentTelemetryEventType, releaseQuarantineType, null, Collections.emptyList());
  }

  public void sendRepositoryComponentTelemetry(
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final List<ProxyRepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final String releaseReason,
      final List<PolicyNotification> policyNotifications)
  {
    sendRepositoryComponentTelemetryInternal(proxyRepositoryComponent, policyViolations, repositoryManagerId, null,
        repositoryComponentTelemetryEventType, releaseQuarantineType, releaseReason, policyNotifications, null);
  }

  public void sendRepositoryComponentTelemetry(
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final List<ProxyRepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      final String repositoryName,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final String releaseReason,
      final List<PolicyNotification> policyNotifications)
  {
    sendRepositoryComponentTelemetryInternal(proxyRepositoryComponent, policyViolations, repositoryManagerId,
        repositoryName,
        repositoryComponentTelemetryEventType, releaseQuarantineType, releaseReason, policyNotifications, null);
  }

  public void sendRepositoryComponentTelemetry(TelemetryData proxyRepositoryComponentTelemetry) {
    if (proxyRepositoryComponentTelemetry == null ||
        !proxyRepositoryComponentTelemetry.getPurpose().equals(TelemetryPurpose.REPOSITORY_COMPONENT))
    {
      log.debug("TelemetryData is not for REPOSITORY_COMPONENT purpose. Skipping telemetry send.");
      return;
    }
    telemetrySender.send(proxyRepositoryComponentTelemetry);
  }

  private void sendRepositoryComponentTelemetryInternal(
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final List<ProxyRepositoryPolicyViolation> policyViolations,
      final String repositoryManagerId,
      String repositoryName,
      final RepositoryComponentTelemetryEventType repositoryComponentTelemetryEventType,
      final ReleaseQuarantineType releaseQuarantineType,
      final String releaseReason,
      final List<PolicyNotification> policyNotifications,
      final Component component)
  {
    final String accountId = getAccountId();

    // Lookup repository to extract repositoryType for telemetry.
    // Note: This lookup is always performed to obtain repositoryType, even when repositoryName is provided.
    // If repositoryName is not provided, the lookup also serves to retrieve the repository name.
    final Repository repository = Optional.ofNullable(proxyRepositoryComponent)
        .map(ProxyRepositoryComponent::getRepositoryId)
        .map(this::lookupRepository)
        .orElse(null);

    // Use repositoryName from caller if provided, otherwise get it from the repository lookup above
    if (repositoryName == null && repository != null) {
      log.debug("repositoryName not provided for repositoryId {}. Using repository lookup result. " +
          "Performance tip: Caller should pass repositoryName explicitly to avoid database lookup.",
          proxyRepositoryComponent.getRepositoryId());
      repositoryName = repository.getPublicId();
    }

    final String repositoryType = Optional.ofNullable(repository)
        .map(Repository::getRepositoryType)
        .map(Enum::name)
        .orElse(null);

    // Create policy violation telemetry with Component data to fill missing CVE fields
    final List<PolicyViolationTelemetry> policyViolationTelemetries =
        policyViolations.stream()
            .map(pv -> PolicyViolationTelemetry.createWithComponent(pv, component))
            .collect(Collectors.toList());

    final ProxyRepositoryComponentTelemetry proxyRepositoryComponentTelemetry =
        ProxyRepositoryComponentTelemetry.builder()
            .accountId(accountId)
            .repositoryManagerId(repositoryManagerId)
            .repositoryName(repositoryName)
            .repositoryType(repositoryType)
            .telemetryDataObfuscator(telemetryDataObfuscator)
            .fromRepositoryComponent(proxyRepositoryComponent)
            .eventType(repositoryComponentTelemetryEventType)
            .releaseQuarantineType(releaseQuarantineType)
            .releaseReason(releaseReason)
            .policyNotifications(policyNotifications)
            .build();

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REPOSITORY_COMPONENT);
    telemetryData.getAttributes().put(POLICY_VIOLATION_TELEMETRY, policyViolationTelemetries);
    telemetryData.getAttributes().put(REPOSITORY_COMPONENT_TELEMETRY, proxyRepositoryComponentTelemetry);

    telemetrySender.send(telemetryData);
  }

  /**
   * Looks up the repository from the database given a repository ID.
   * Returns the full Repository object to avoid multiple database lookups.
   *
   * @param repositoryId The internal repository ID
   * @return The Repository entity, or null if not found
   */
  private Repository lookupRepository(final String repositoryId) {
    try {
      List<Repository> repositories = repositoryDAO.getByIds(Set.of(repositoryId));
      if (repositories != null && !repositories.isEmpty()) {
        return repositories.get(0);
      }
    }
    catch (Exception e) {
      log.debug("Failed to lookup repository for repositoryId: {}", repositoryId, e);
    }
    return null;
  }

  /**
   * Retrieves the Salesforce Account ID from HDS telemetry properties.
   * The accountId is tenant-specific and cached per-tenant by PendoCache.
   *
   * @return Salesforce Account ID (e.g., "001QO00000sRp3lYAC") or null if not available
   */
  private String getAccountId() {
    try {
      CustomerTelemetryProperties telemetryProperties = pendoCache.getCustomerTelemetryProperties();
      if (telemetryProperties != null && telemetryProperties.segmentAttributes != null) {
        Object accountIdObj = telemetryProperties.segmentAttributes.get("iq_accountId");
        if (accountIdObj != null && !accountIdObj.toString().startsWith("UNKNOWN-")) {
          return accountIdObj.toString();
        }
      }
    }
    catch (Exception e) {
      log.debug("Failed to retrieve Salesforce accountId for telemetry", e);
    }
    return null;
  }
}
