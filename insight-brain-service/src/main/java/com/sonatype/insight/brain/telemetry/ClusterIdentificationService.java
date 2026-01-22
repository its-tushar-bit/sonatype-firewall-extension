/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.telemetry.ClusterIdentificationDAO;
import com.sonatype.insight.brain.model.telemetry.ClusterIdentification;
import com.sonatype.insight.brain.organization.SampleDataCreator;
import com.sonatype.insight.brain.service.BaseUrlProvider;
import com.sonatype.insight.scan.util.HashUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.telemetry.ClusterIdentificationService.ResolutionOutcome.BASE_URL_CHANGED;
import static com.sonatype.insight.brain.telemetry.ClusterIdentificationService.ResolutionOutcome.DB_CONNECTION_INFO_CHANGED;
import static com.sonatype.insight.brain.telemetry.ClusterIdentificationService.ResolutionOutcome.INITIALIZED;
import static com.sonatype.insight.brain.telemetry.ClusterIdentificationService.ResolutionOutcome.INITIALIZED_AS_NEW_INSTANCE;
import static com.sonatype.insight.brain.telemetry.ClusterIdentificationService.ResolutionOutcome.INITIALIZED_WITH_HOST_CORRECTION;
import static com.sonatype.insight.brain.telemetry.ClusterIdentificationService.ResolutionOutcome.NEW_INSTANCE_DETECTED;
import static com.sonatype.insight.brain.telemetry.ClusterIdentificationService.ResolutionOutcome.NO_CHANGE;
import static com.sonatype.insight.brain.telemetry.ClusterIdentificationService.ResolutionOutcome.TAMPERING_DETECTED;
import static com.sonatype.insight.brain.telemetry.ClusterIdentificationService.ResolutionOutcome.TAMPERING_DETECTED_AND_CORRECTED;
import static java.lang.String.format;

@Named
@Singleton
public class ClusterIdentificationService
{
  private static final Logger log = LoggerFactory.getLogger(ClusterIdentificationService.class);

  // this hard-coded value is not used for encryption or security - we just use it to make it harder for someone
  // to manually tamper with the data without being detected
  private static final String HASH_SALT = "ce8432b0-a89f-4871-9400-02ae3fb6a094";

  public static final String RESOLUTION_OUTCOME = "outcome";

  static final Pattern CLUSTER_ID_HASH_PATTERN = Pattern.compile("^[0-9a-f]{128}$");

  static final Pattern CLUSTER_ID_UUID_PATTERN = Pattern.compile(format("^%1$s{8}-(%1$s{4}-){3}%1$s{12}$", "[0-9a-f]"));

  static final Pattern TELEMETRY_ID_PATTERN = Pattern.compile("^[0-9a-f]{5}-[0-9a-f]{5}$");

  static final String CORRUPTED_TELEMETRY_PREFIX = "****-";

  static final TelemetryPurpose TELEMETRY_PURPOSE = TelemetryPurpose.CLUSTER_IDENTITY;

  static final String CLUSTER_ID_CORRECTED = "cluster_id_corrected";

  static final String DEFAULT_CLUSTER_IDENTIFICATION_ID = "DEFAULT_CLUSTER_IDENTIFICATION_ID";

  static final String PREVIOUS_CLUSTER_ID = "previous_cluster_id";

  static final String PREVIOUS_TELEMETRY_ID = "previous_telemetry_id";

  static final String TELEMETRY_HOST_CORRECTED = "telemetry_host_corrected";

  public enum ResolutionOutcome
  {
    BASE_URL_CHANGED,
    DB_CONNECTION_INFO_CHANGED,
    INITIALIZED,
    INITIALIZED_AS_NEW_INSTANCE,
    INITIALIZED_WITH_HOST_CORRECTION,
    NEW_INSTANCE_DETECTED,
    NO_CHANGE,
    TAMPERING_DETECTED,
    TAMPERING_DETECTED_AND_CORRECTED
  }

  private final ApplicationDAO applicationDAO;

  private final BaseUrlProvider baseUrlProvider;

  private final ClusterIdentificationDAO clusterIdentificationDAO;

  private final TelemetryQueue telemetryQueue;

  @Inject
  public ClusterIdentificationService(
      ApplicationDAO applicationDAO,
      BaseUrlProvider baseUrlProvider,
      ClusterIdentificationDAO clusterIdentificationDAO,
      TelemetryQueue telemetryQueue)
  {
    this.applicationDAO = applicationDAO;
    this.baseUrlProvider = baseUrlProvider;
    this.clusterIdentificationDAO = clusterIdentificationDAO;
    this.telemetryQueue = telemetryQueue;
  }

  /**
   * Resolve the cluster identity (cluster ID and telemetry ID) for this instance. We look for changes to either the
   * base URL or the calculated cluster ID (hash of DB connection info), or both, to determine whether we need to
   * generate new cluster and telemetry IDs.
   *
   * We determine those changes by comparing the passed in calculated cluster ID to the one we previously stored in the
   * `last_calculated_cluster_id` field and similarly for the base URL.
   *
   * If there were no changes or only one tracked value changed we consider this the same instance and don't make
   * any changes to the cluster identity.
   *
   * If both the base URL and calculated cluster ID changed, we consider this a new environment and generate new cluster
   * and telemetry IDs.
   *
   * @param calculatedClusterId  the cluster ID dynamically calculated by the system
   * @param generatedTelemetryId the telemetry ID dynamically generated by the system
   * @return the assigned cluster and telemetry IDs
   */
  public IdResolutionResult resolveClusterIdentity(
      String calculatedClusterId,
      String generatedTelemetryId)
  {
    var baseUrl = Objects.requireNonNullElse(baseUrlProvider.getBaseUrl(), "");

    var clusterIdentification = clusterIdentificationDAO.getById(DEFAULT_CLUSTER_IDENTIFICATION_ID);

    if (null == clusterIdentification) {
      return initializeClusterIdentity(calculatedClusterId, generatedTelemetryId, baseUrl);
    }

    final var calculatedClusterIdChanged =
        !Objects.equals(calculatedClusterId, clusterIdentification.getLastCalculatedClusterId());
    final var baseUrlChanged = !hashWithSalt(baseUrl).equals(clusterIdentification.getBaseUrlHash());

    if (calculatedClusterIdChanged && baseUrlChanged) {
      return handleNewInstanceDetected(clusterIdentification, calculatedClusterId, generatedTelemetryId, baseUrl);
    }

    if (wasTamperedWith(clusterIdentification)) {
      return handleAssignedIdentityWasManuallyChanged(
          clusterIdentification,
          calculatedClusterId,
          generatedTelemetryId,
          baseUrl
      );
    }

    if (calculatedClusterIdChanged) {
      return handleOnlyDbConnectionInfoChanged(clusterIdentification, calculatedClusterId);
    }

    if (baseUrlChanged) {
      return handleOnlyBaseUrlChanged(clusterIdentification, baseUrl);
    }

    // otherwise, no changes, return the existing cluster identity
    log.info("Cluster identity loaded");
    addTelemetryForNoChange();

    return new IdResolutionResult(
        clusterIdentification.getAssignedClusterId(),
        clusterIdentification.getAssignedTelemetryId(),
        NO_CHANGE);
  }

  public void sendTelemetry() {
    telemetryQueue.flush();
  }

  @VisibleForTesting
  String calculateTamperCode(ClusterIdentification clusterIdentification) {
    return hashWithSalt(
        clusterIdentification.getAssignedClusterId() + clusterIdentification.getAssignedTelemetryId());
  }

  private TelemetryData createTelemetryData(ResolutionOutcome outcome) {
    return new TelemetryData(TELEMETRY_PURPOSE).put(RESOLUTION_OUTCOME, outcome.name());
  }

  private boolean correctTelemetryHostIfNeeded(ClusterIdentification clusterIdentification) {
    boolean telemetryHostWasUpdated = false;

    if (clusterIdentification.getAssignedTelemetryId().startsWith(CORRUPTED_TELEMETRY_PREFIX)) {
      final var newTelemetryId = createNewTelemetryId(
          clusterIdentification.getAssignedClusterId(),
          clusterIdentification.getAssignedTelemetryId()
      );
      clusterIdentification.setAssignedTelemetryId(newTelemetryId);
      telemetryHostWasUpdated = true;
    }

    return telemetryHostWasUpdated;
  }

  private IdResolutionResult handleAssignedIdentityWasManuallyChanged(
      ClusterIdentification clusterIdentification,
      String calculatedClusterId,
      String calculatedTelemetryId,
      String baseUrl)
  {
    boolean wasTelemetryHostCorrected = false;
    boolean wasClusterIdCorrected = false;

    // if the cluster ID is invalid default it to the calculated value; otherwise ignore the change
    if (!ClusterIdentityValidator.isValidClusterId(clusterIdentification.getAssignedClusterId())) {
      clusterIdentification.setAssignedClusterId(calculatedClusterId);
      wasClusterIdCorrected = true;
    }

    if (!ClusterIdentityValidator.isValidTelemetryId(clusterIdentification.getAssignedTelemetryId())) {
      clusterIdentification.setAssignedTelemetryId(calculatedTelemetryId);
      wasTelemetryHostCorrected = true;
    }

    clusterIdentification.setBaseUrlHash(hashWithSalt(baseUrl));
    clusterIdentification.setLastCalculatedClusterId(calculatedClusterId);
    clusterIdentification.setLastUpdated(new Date());

    clusterIdentification.setTamperCode(calculateTamperCode(clusterIdentification));
    clusterIdentificationDAO.update(clusterIdentification);

    log.warn("Cluster identity tampering detected, cluster ID reset = {}, host corrected = {}", wasClusterIdCorrected,
        wasTelemetryHostCorrected);

    addTelemetryForTampering(wasClusterIdCorrected, wasTelemetryHostCorrected);

    return new IdResolutionResult(
        clusterIdentification.getAssignedClusterId(),
        clusterIdentification.getAssignedTelemetryId(),
        wasClusterIdCorrected || wasTelemetryHostCorrected ? TAMPERING_DETECTED_AND_CORRECTED : TAMPERING_DETECTED);
  }

  private IdResolutionResult handleOnlyBaseUrlChanged(
      ClusterIdentification clusterIdentification,
      String baseUrl)
  {
    clusterIdentification.setBaseUrlHash(hashWithSalt(baseUrl));
    clusterIdentification.setLastUpdated(new Date());
    // reset tampering
    clusterIdentification.setTamperCode(calculateTamperCode(clusterIdentification));
    clusterIdentificationDAO.update(clusterIdentification);

    log.info("Cluster identity base URL changed");
    addTelemetryForBaseUrlChange();

    return new IdResolutionResult(
        clusterIdentification.getAssignedClusterId(),
        clusterIdentification.getAssignedTelemetryId(),
        BASE_URL_CHANGED);
  }

  private IdResolutionResult handleOnlyDbConnectionInfoChanged(
      ClusterIdentification clusterIdentification,
      String calculatedClusterId)
  {
    clusterIdentification.setLastCalculatedClusterId(calculatedClusterId);
    clusterIdentification.setLastUpdated(new Date());
    // ignoring/resetting tampering
    clusterIdentification.setTamperCode(calculateTamperCode(clusterIdentification));
    clusterIdentificationDAO.update(clusterIdentification);

    log.info("Cluster identity DB connection info changed");
    addTelemetryForDbConnectionInfoChange();

    return new IdResolutionResult(
        clusterIdentification.getAssignedClusterId(),
        clusterIdentification.getAssignedTelemetryId(),
        DB_CONNECTION_INFO_CHANGED);
  }

  /**
   * since both the base URL and cluster ID changed, we will consider this a new environment and therefore
   * will generate new cluster and telemetry IDs
   */
  private IdResolutionResult handleNewInstanceDetected(
      ClusterIdentification clusterIdentification,
      String calculatedClusterId,
      String calculatedTelemetryId,
      String baseUrl)
  {
    var newClusterId = createNewClusterId();
    var newTelemetryId = createNewTelemetryId(newClusterId, calculatedTelemetryId);
    var previousClusterId = clusterIdentification.getAssignedClusterId();
    var previousTelemetryId = clusterIdentification.getAssignedTelemetryId();
    clusterIdentification.setAssignedClusterId(newClusterId);
    clusterIdentification.setAssignedTelemetryId(newTelemetryId);
    clusterIdentification.setLastCalculatedClusterId(calculatedClusterId);
    clusterIdentification.setBaseUrlHash(hashWithSalt(baseUrl));
    clusterIdentification.setLastUpdated(new Date());
    clusterIdentification.setTamperCode(calculateTamperCode(clusterIdentification));
    clusterIdentificationDAO.update(clusterIdentification);

    log.info("Cluster identity generated for new instance");
    addTelemetryForNewInstance(previousClusterId, previousTelemetryId);

    return new IdResolutionResult(newClusterId, newTelemetryId, NEW_INSTANCE_DETECTED);
  }

  private static String hashWithSalt(String value) {
    return HashUtils.hash(value + HASH_SALT, HashUtils.SHA1);
  }

  private IdResolutionResult initializeClusterIdentity(
      String calculatedClusterId,
      String generatedTelemetryId,
      String baseUrl)
  {
    var clusterIdentification = new ClusterIdentification();
    var outcome = INITIALIZED;

    clusterIdentification.setId(DEFAULT_CLUSTER_IDENTIFICATION_ID);

    var telemetryHostWasCorrected = false;
    if (isNewInstance()) {
      // create new identifiers
      var newClusterId = createNewClusterId();
      var newTelemetryId = createNewTelemetryId(newClusterId, generatedTelemetryId);
      clusterIdentification.setAssignedClusterId(newClusterId);
      clusterIdentification.setAssignedTelemetryId(newTelemetryId);
      outcome = INITIALIZED_AS_NEW_INSTANCE;
    }
    else {
      // seed with the existing identifiers so we don't break anything (i.e. cause the instance to become disconnected
      // from its historical data in databricks)
      clusterIdentification.setAssignedClusterId(calculatedClusterId);
      clusterIdentification.setAssignedTelemetryId(generatedTelemetryId);
      telemetryHostWasCorrected = correctTelemetryHostIfNeeded(clusterIdentification);
      if (telemetryHostWasCorrected) {
        outcome = INITIALIZED_WITH_HOST_CORRECTION;
      }
    }
    clusterIdentification.setLastCalculatedClusterId(calculatedClusterId);
    clusterIdentification.setBaseUrlHash(hashWithSalt(baseUrl));
    clusterIdentification.setCreated(new Date());
    clusterIdentification.setTamperCode(calculateTamperCode(clusterIdentification));

    clusterIdentificationDAO.insert(clusterIdentification);

    log.info("Cluster identity initialized for the first time");
    addTelemetryForInitialClusterIdentification(outcome);

    return new IdResolutionResult(
        clusterIdentification.getAssignedClusterId(),
        clusterIdentification.getAssignedTelemetryId(),
        outcome);
  }

  private boolean isNewInstance() {
    final var appCount = applicationDAO.getCount();
    return 0 == appCount
        || (1 == appCount && null != applicationDAO.getByPublicId(SampleDataCreator.SAMPLE_APPLICATION_PUBLIC_ID));
  }

  private String createNewClusterId() {
    return UUID.randomUUID().toString();
  }

  private void addTelemetryForBaseUrlChange() {
    telemetryQueue.add(createTelemetryData(ResolutionOutcome.BASE_URL_CHANGED));
  }

  private void addTelemetryForDbConnectionInfoChange() {
    telemetryQueue.add(createTelemetryData(ResolutionOutcome.DB_CONNECTION_INFO_CHANGED));
  }

  private void addTelemetryForInitialClusterIdentification(ResolutionOutcome outcome) {
    telemetryQueue.add(createTelemetryData(outcome));
  }

  private void addTelemetryForNewInstance(String previousClusterId, String previousTelemetryId) {
    var telemetryData = createTelemetryData(ResolutionOutcome.NEW_INSTANCE_DETECTED)
        .put(PREVIOUS_CLUSTER_ID, previousClusterId)
        .put(PREVIOUS_TELEMETRY_ID, previousTelemetryId);
    telemetryQueue.add(telemetryData);
  }

  private void addTelemetryForNoChange() {
    telemetryQueue.add(createTelemetryData(ResolutionOutcome.NO_CHANGE));
  }

  private void addTelemetryForTampering(boolean wasClusterIdCorrected, boolean wasTelemetryHostCorrected) {
    final var corrected = wasClusterIdCorrected || wasTelemetryHostCorrected;
    telemetryQueue.add(
        createTelemetryData(corrected ? TAMPERING_DETECTED_AND_CORRECTED : TAMPERING_DETECTED)
            .put(CLUSTER_ID_CORRECTED, wasClusterIdCorrected)
            .put(TELEMETRY_HOST_CORRECTED, wasTelemetryHostCorrected)
    );
  }

  /**
   * the telemetry ID is calculated externally.  The prefix consists of 5 chars from a UUID.  The suffix is a hash of
   * some hardware/network info, which we should probably preserve.  Thus, when we need to generate a new telemetry ID,
   * we'll generate a new prefix and we'll base it on the uuid that was created for the cluster ID.
   */
  private String createNewTelemetryId(String newClusterId, String generatedTelemetryId) {
    var telemetryIdPrefix = newClusterId.substring(0, 5);
    return telemetryIdPrefix + generatedTelemetryId.substring(generatedTelemetryId.indexOf('-'));
  }

  private boolean wasTamperedWith(ClusterIdentification clusterIdentification) {
    return !calculateTamperCode(clusterIdentification).equals(clusterIdentification.getTamperCode());
  }

  public static class ClusterIdentityValidator
  {
    public static boolean isValidClusterId(String clusterId) {
      return null != clusterId &&
          (CLUSTER_ID_HASH_PATTERN.matcher(clusterId).matches() ||
              CLUSTER_ID_UUID_PATTERN.matcher(clusterId).matches());
    }

    public static boolean isValidTelemetryId(String telemetryId) {
      return null != telemetryId && TELEMETRY_ID_PATTERN.matcher(telemetryId).matches();
    }
  }

  public record IdResolutionResult(String assignedClusterId, String assignedTelemetryId, ResolutionOutcome outcome)
  {
  }
}
