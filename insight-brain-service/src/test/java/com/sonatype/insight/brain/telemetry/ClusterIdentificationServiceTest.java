/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.telemetry.ClusterIdentificationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.telemetry.ClusterIdentification;
import com.sonatype.insight.brain.organization.SampleDataCreator;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrlProvider;
import com.sonatype.insight.brain.telemetry.ClusterIdentificationService.IdResolutionResult;
import com.sonatype.insight.brain.telemetry.ClusterIdentificationService.ResolutionOutcome;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.hash.Hashing;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.sonatype.insight.brain.telemetry.ClusterIdentificationService.CORRUPTED_TELEMETRY_PREFIX;
import static com.sonatype.insight.brain.telemetry.ClusterIdentificationService.DEFAULT_CLUSTER_IDENTIFICATION_ID;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClusterIdentificationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ClusterIdentificationDAO clusterIdentificationDAO;

  @Mock
  private ApplicationDAO mockApplicationDAO;

  @Mock
  private BaseUrlProvider mockBaseUrlProvider;

  @Mock
  private TelemetryQueue mockTelemetryQueue;

  private ClusterIdentificationService testSubject;

  private Date testStartTime;

  private Date updateTime;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    testSubject = new ClusterIdentificationService(
        mockApplicationDAO,
        mockBaseUrlProvider,
        clusterIdentificationDAO,
        mockTelemetryQueue
    );
    testStartTime = new Date();
    updateTime = null;
  }

  @After
  public void cleanup() {
    clusterIdentificationDAO.delete(clusterIdentificationDAO.getById(DEFAULT_CLUSTER_IDENTIFICATION_ID));
  }

  @Test
  public void testResolveClusterIdentity_initializeNewInstance() {
    // given: no applications defined yet and the calculated IDs
    instanceHasNoApplicationsYet();
    final var baseUrl = "http://some.host.net";

    final var calculatedClusterId = createHash("some.db.config");
    final var generatedTelemetryId = "abc23-456de";

    // when:
    var result = testSubject.resolveClusterIdentity(calculatedClusterId, generatedTelemetryId);

    // then: should have initialized as a new instance
    assertThat(result.outcome()).isEqualTo(INITIALIZED_AS_NEW_INSTANCE);
    validateCalculatedIdsReplaced(result, calculatedClusterId, generatedTelemetryId);

    // the persisted cluster identification record matches what we expect
    validatePersistedClusterIdentification(result, baseUrl, calculatedClusterId);

    validateTelemetry(INITIALIZED_AS_NEW_INSTANCE);
  }

  @Test
  public void testResolveClusterIdentity_initializeNewInstanceWithSampleData() {
    // given: new instance initialized with sample data
    instanceHasSampleApplication();
    final var baseUrl = "http://some.host.net";
    withBaseUrl(baseUrl);

    final var calculatedClusterId = createHash("some.db.config");
    final var generatedTelemetryId = "abc23-456de";

    // when:
    var result = testSubject.resolveClusterIdentity(calculatedClusterId, generatedTelemetryId);

    // then: should have initialized as a new instance
    assertThat(result.outcome()).isEqualTo(INITIALIZED_AS_NEW_INSTANCE);
    validateCalculatedIdsReplaced(result, calculatedClusterId, generatedTelemetryId);

    // the persisted cluster identification record matches what we expect
    validatePersistedClusterIdentification(result, baseUrl, calculatedClusterId);
    validateTelemetry(INITIALIZED_AS_NEW_INSTANCE);
  }

  @Test
  public void testResolveClusterIdentity_initializeExistingInstance() {
    // given: there are applications defined
    instanceHasApplications();
    final var baseUrl = "http://some.host.net";
    withBaseUrl(baseUrl);

    final var calculatedClusterId = createHash("some.db.config");
    final var generatedTelemetryId = "abc23-456de";

    // when:
    var result = testSubject.resolveClusterIdentity(calculatedClusterId, generatedTelemetryId);

    // then: should have initialized with the calculated IDs
    assertThat(result.outcome()).isEqualTo(INITIALIZED);
    validateProvidedIdsPreserved(result, calculatedClusterId, generatedTelemetryId);

    // and the persisted cluster identification record matches what we expect
    validatePersistedClusterIdentification(result, baseUrl, calculatedClusterId);
    validateTelemetry(INITIALIZED);
  }

  @Test
  public void testResolveClusterIdentity_initializeWithCorruptedTelemetryHost() {
    // given: there are applications defined
    instanceHasApplications();
    final var baseUrl = "http://some.host.net";
    withBaseUrl(baseUrl);

    final var calculatedClusterId = createHash("some.db.config");
    final var corruptedTelemetryId = CORRUPTED_TELEMETRY_PREFIX + "456de";

    // when:
    var result = testSubject.resolveClusterIdentity(calculatedClusterId, corruptedTelemetryId);

    // then: should have initialized with the calculated IDs
    assertThat(result.outcome()).isEqualTo(INITIALIZED_WITH_HOST_CORRECTION);
    validateCorruptedTelemetryIdFixed(result, calculatedClusterId, corruptedTelemetryId);

    // and the persisted cluster identification record matches what we expect
    validatePersistedClusterIdentification(result, baseUrl, calculatedClusterId);
    validateTelemetry(INITIALIZED_WITH_HOST_CORRECTION);
  }

  @Test
  public void testResolveClusterIdentity_nullCalculatedClusterIdNewInstance() {
    // given: no applications defined yet and the calculated IDs
    instanceHasNoApplicationsYet();
    final var baseUrl = "http://some.host.net";
    withBaseUrl(baseUrl);

    final String calculatedClusterId = null;
    final var generatedTelemetryId = "abc23-456de";

    // when:
    var result = testSubject.resolveClusterIdentity(calculatedClusterId, generatedTelemetryId);

    // then: should have initialized as a new instance
    assertThat(result.outcome()).isEqualTo(INITIALIZED_AS_NEW_INSTANCE);
    validateCalculatedIdsReplaced(result, calculatedClusterId, generatedTelemetryId);

    // the persisted cluster identification record matches what we expect
    validatePersistedClusterIdentification(result, baseUrl, calculatedClusterId);
    validateTelemetry(INITIALIZED_AS_NEW_INSTANCE);
  }

  @Test
  public void testResolveClusterIdentity_nullCalculatedClusterIdExistingInstance() {
    // given: there are applications defined and we only change the calculated cluster ID and we do it with a null value
    instanceHasApplications();
    final var baseUrl = "http://some.host.net";
    withBaseUrl(baseUrl);

    final var assignedClusterId = createHash("some.db.config");
    final var assignedTelemetryId = "abc23-456de";
    final var generatedTelemetryId = assignedTelemetryId;

    initializeClusterIdentificationWithApps(assignedClusterId, assignedTelemetryId);

    // when: only the calculated cluster ID changes and it's null
    final String nullClusterId = null;
    var result = testSubject.resolveClusterIdentity(nullClusterId, generatedTelemetryId);

    // then: should have initialized with the calculated IDs
    assertThat(result.outcome()).isEqualTo(DB_CONNECTION_INFO_CHANGED);
    validateAssignedIdsPreserved(result, assignedClusterId, assignedTelemetryId);

    // and the persisted cluster identification record matches what we expect
    validatePersistedClusterIdentification(result, baseUrl, nullClusterId);
    validateTelemetry(DB_CONNECTION_INFO_CHANGED);
  }

  @Test
  public void testResolveClusterIdentity_baseUrlChanged() {
    // given: cluster identification initialized with a given base URL
    final var calculatedClusterId = createHash("some.db.config");
    final var generatedTelemetryId = "abc23-456de";
    withBaseUrl("some.host.net");

    final var ogClusterIdentification =
        initializeClusterIdentificationWithApps(calculatedClusterId, generatedTelemetryId);
    final var assignedClusterId = ogClusterIdentification.getAssignedClusterId();
    final var assignedTelemetryId = ogClusterIdentification.getAssignedTelemetryId();

    // when: the base URL changes
    final var newBaseUrl = "http://some.other.host.net";
    withBaseUrl(newBaseUrl);
    var result = testSubject.resolveClusterIdentity(calculatedClusterId, generatedTelemetryId);

    // then: the calculated IDs and tamper code weren't changed and the base URL hash was updated
    assertThat(result.outcome()).isEqualTo(BASE_URL_CHANGED);
    validateAssignedIdsPreserved(result, assignedClusterId, assignedTelemetryId);
    validatePersistedClusterIdentification(result, newBaseUrl, calculatedClusterId);
    validateTamperCodeUnchanged(ogClusterIdentification);
    validateBaseUrlHashUpdated(ogClusterIdentification);
    validateTelemetry(BASE_URL_CHANGED);
  }

  @Test
  public void testResolveClusterIdentity_calculatedClusterIdChanged() {
    // given: cluster identification initialized with a given base URL
    final var calculatedClusterId = createHash("some.db.config");
    final var generatedTelemetryId = "abc23-456de";
    final var baseUrl = "http://some.host.net";
    withBaseUrl(baseUrl);
    final var ogClusterIdentification =
        initializeClusterIdentificationWithApps(calculatedClusterId, generatedTelemetryId);

    // when: the calculated cluster ID changes
    final var newCalculatedClusterId = createHash("some.other.db.config");
    var result = testSubject.resolveClusterIdentity(newCalculatedClusterId, generatedTelemetryId);

    // then: the calculated IDs and tamper code weren't changed and the last calculated cluster ID was updated
    assertThat(result.outcome()).isEqualTo(DB_CONNECTION_INFO_CHANGED);
    validateProvidedIdsPreserved(result, calculatedClusterId, generatedTelemetryId);
    validatePersistedClusterIdentification(result, baseUrl, newCalculatedClusterId);
    validateTamperCodeUnchanged(ogClusterIdentification);
    validateTelemetry(DB_CONNECTION_INFO_CHANGED);
  }

  @Test
  public void testResolveClusterIdentity_newInstanceDetected() {
    // given: cluster identification initialized
    final var calculatedClusterId = createHash("some.db.config");
    final var generatedTelemetryId = "abc12-345de";
    withBaseUrl("http://some.host.net");
    final var ogClusterIdentification =
        initializeClusterIdentificationWithoutApps(calculatedClusterId, generatedTelemetryId);
    final var ogAssignedClusterId = ogClusterIdentification.getAssignedClusterId();
    final var ogAssignedTelemetryId = ogClusterIdentification.getAssignedTelemetryId();

    // when: the base URL and calculated cluster ID both change
    final var newBaseUrl = "http://some.other.host.net";
    withBaseUrl(newBaseUrl);
    final var newCalculatedClusterId = createHash("some.other.db.config");
    var result = testSubject.resolveClusterIdentity(newCalculatedClusterId, generatedTelemetryId);

    // then: new ids were generated previous IDs were sent in telemetry
    assertThat(result.outcome()).isEqualTo(NEW_INSTANCE_DETECTED);
    validateNewIdsCreated(result, ogAssignedClusterId, ogAssignedTelemetryId);
    validatePersistedClusterIdentification(result, newBaseUrl, newCalculatedClusterId);
    validateTamperCodeUpdated();
    validateBaseUrlHashUpdated(ogClusterIdentification);
    validateTelemetry(NEW_INSTANCE_DETECTED);
  }

  @Test
  public void testResolveClusterIdentity_noChangeFromNewInstanceInit() {
    // given: cluster identification initialized with new IDs
    final var calculatedClusterId = createHash("some.db.config");
    final var generatedTelemetryId = "abc12-345de";
    final var baseUrl = "http://some.host.net";
    withBaseUrl(baseUrl);
    final var ogClusterIdentification =
        initializeClusterIdentificationWithoutApps(calculatedClusterId, generatedTelemetryId);
    final var ogAssignedClusterId = ogClusterIdentification.getAssignedClusterId();
    final var ogAssignedTelemetryId = ogClusterIdentification.getAssignedTelemetryId();
    updateTime = null;

    // when: resolve identity with same values
    var result = testSubject.resolveClusterIdentity(calculatedClusterId, generatedTelemetryId);

    // then: the calculated IDs and tamper code weren't changed and the last calculated cluster ID was updated
    assertThat(result.outcome()).isEqualTo(NO_CHANGE);
    validateAssignedIdsPreserved(result, ogAssignedClusterId, ogAssignedTelemetryId);
    validatePersistedClusterIdentification(result, baseUrl, calculatedClusterId);
    validateTamperCodeUnchanged(ogClusterIdentification);
    validateTelemetry(NO_CHANGE);
  }

  @Test
  public void testResolveClusterIdentity_noChangeFromExistingInstanceInit() {
    // given: cluster identification initialized with preserved IDs
    final var calculatedClusterId = createHash("some.db.config");
    final var generatedTelemetryId = "abc12-345de";
    final var baseUrl = "http://some.host.net";
    withBaseUrl(baseUrl);
    final var ogClusterIdentification =
        initializeClusterIdentificationWithApps(calculatedClusterId, generatedTelemetryId);
    final var ogAssignedClusterId = ogClusterIdentification.getAssignedClusterId();
    final var ogAssignedTelemetryId = ogClusterIdentification.getAssignedTelemetryId();
    updateTime = null;

    // when: resolve identity with same values
    var result = testSubject.resolveClusterIdentity(calculatedClusterId, generatedTelemetryId);

    // then: the calculated IDs and tamper code weren't changed and the last calculated cluster ID was updated
    assertThat(result.outcome()).isEqualTo(NO_CHANGE);
    validateAssignedIdsPreserved(result, ogAssignedClusterId, ogAssignedTelemetryId);
    validatePersistedClusterIdentification(result, baseUrl, calculatedClusterId);
    validateTamperCodeUnchanged(ogClusterIdentification);
    validateTelemetry(NO_CHANGE);
  }

  @Test
  public void testClusterIdentity_tamperedWithUsingValidIds() {
    // given: cluster identification initialized with new IDs
    final var calculatedClusterId = createHash("some.db.config");
    final var generatedTelemetryId = "abc12-345de";
    final var baseUrl = "http://some.host.net";
    withBaseUrl(baseUrl);
    final var ogClusterIdentification =
        initializeClusterIdentificationWithoutApps(calculatedClusterId, generatedTelemetryId);

    // when: tamper with the assigned IDs and try to resolve with actual calculated values
    final var validTamperedClusterId = createHash("some.other.db.config");
    final var validTamperedTelemetryId = "789ab-456cd";
    tamperWithAssignedIdentifiers(ogClusterIdentification, validTamperedClusterId, validTamperedTelemetryId);
    var result = testSubject.resolveClusterIdentity(calculatedClusterId, generatedTelemetryId);

    // then: the tampered IDs were preserved and the last calculated cluster ID was updated
    assertThat(result.outcome()).isEqualTo(TAMPERING_DETECTED);
    validateAssignedIdsPreserved(result, validTamperedClusterId, validTamperedTelemetryId);
    validatePersistedClusterIdentification(result, baseUrl, calculatedClusterId);
    validateTelemetry(TAMPERING_DETECTED);
  }

  @Test
  public void testClusterIdentity_tamperedWithUsingInvalidValidIds() {
    // given: cluster identification initialized with new IDs
    final var calculatedClusterId = createHash("some.db.config");
    final var generatedTelemetryId = "abc12-345de";
    final var baseUrl = "http://some.host.net";
    withBaseUrl(baseUrl);
    final var ogClusterIdentification =
        initializeClusterIdentificationWithoutApps(calculatedClusterId, generatedTelemetryId);

    // when: tamper with the assigned IDs and try to resolve with new calculated values
    final var tamperedTelemetryId = CORRUPTED_TELEMETRY_PREFIX + "345de";
    tamperWithAssignedIdentifiers(ogClusterIdentification, "tampered-cluster-id", tamperedTelemetryId);
    final var newCalculatedClusterId = createHash("some.other.db.config");
    final var newGeneratedTelemetryId = "789ab-456cd";
    var result = testSubject.resolveClusterIdentity(newCalculatedClusterId, newGeneratedTelemetryId);

    // then: the invalid IDs were changed to the new calculated values
    assertThat(result.outcome()).isEqualTo(TAMPERING_DETECTED_AND_CORRECTED);
    assertThat(result.assignedClusterId()).isEqualTo(newCalculatedClusterId);
    assertThat(result.assignedTelemetryId()).isEqualTo(newGeneratedTelemetryId);
    validatePersistedClusterIdentification(result, baseUrl, newCalculatedClusterId);
    validateTelemetry(TAMPERING_DETECTED_AND_CORRECTED);
  }

  private void tamperWithAssignedIdentifiers(
      ClusterIdentification clusterIdentification,
      String tamperedClusterId,
      String tamperedTelemetryId)
  {
    clusterIdentification.setAssignedClusterId(tamperedClusterId);
    clusterIdentification.setAssignedTelemetryId(tamperedTelemetryId);
    clusterIdentificationDAO.update(clusterIdentification);
  }

  private void assertIsUUID(String value) {
    assertThat(ClusterIdentificationService.CLUSTER_ID_UUID_PATTERN.matcher(value).matches()).isTrue();
  }

  private String createHash(String value) {
    return Hashing.sha512().newHasher().putString(value, StandardCharsets.UTF_8).hash().toString();
  }

  private void instanceHasApplications() {
    final var someApplications = 2L;
    when(mockApplicationDAO.getCount()).thenReturn(someApplications);
  }

  private void instanceHasNoApplicationsYet() {
    final var noApplications = 0L;
    when(mockApplicationDAO.getCount()).thenReturn(noApplications);
  }

  private void instanceHasSampleApplication() {
    final var oneSampleApplication = 1L;
    when(mockApplicationDAO.getCount()).thenReturn(oneSampleApplication);
    when(mockApplicationDAO.getByPublicId(SampleDataCreator.SAMPLE_APPLICATION_PUBLIC_ID)).thenReturn(
        new Application(SampleDataCreator.SAMPLE_APPLICATION_PUBLIC_ID, "sample-application-name", "orgId")
    );
  }

  private ClusterIdentification initializeClusterIdentificationWithApps(
      String calculatedClusterId,
      String generatedTelemetryId)
  {
    instanceHasApplications();
    return initializeClusterIdentification(calculatedClusterId, generatedTelemetryId);
  }

  private ClusterIdentification initializeClusterIdentificationWithoutApps(
      String calculatedClusterId,
      String generatedTelemetryId)
  {
    instanceHasNoApplicationsYet();
    return initializeClusterIdentification(calculatedClusterId, generatedTelemetryId);
  }

  private ClusterIdentification initializeClusterIdentification(
      String calculatedClusterId,
      String generatedTelemetryId)
  {
    testSubject.resolveClusterIdentity(calculatedClusterId, generatedTelemetryId);
    updateTime = new Date(); // update clock starts now
    reset(mockTelemetryQueue);
    return clusterIdentificationDAO.getById(DEFAULT_CLUSTER_IDENTIFICATION_ID);
  }

  private void validateBaseUrlHashUpdated(ClusterIdentification ogClusterIdentification) {
    var currentClusterIdentification = clusterIdentificationDAO.getById(DEFAULT_CLUSTER_IDENTIFICATION_ID);
    assertThat(currentClusterIdentification.getBaseUrlHash()).isNotNull();
    assertThat(currentClusterIdentification.getBaseUrlHash()).isNotEqualTo(ogClusterIdentification.getBaseUrlHash());
  }

  private void validateTamperCodeUnchanged(ClusterIdentification ogClusterIdentification) {
    var currentClusterIdentification = clusterIdentificationDAO.getById(DEFAULT_CLUSTER_IDENTIFICATION_ID);
    assertThat(currentClusterIdentification.getTamperCode()).isNotNull();
    assertThat(currentClusterIdentification.getTamperCode()).isEqualTo(ogClusterIdentification.getTamperCode());
  }

  private void validateTamperCodeUpdated() {
    var currentClusterIdentification = clusterIdentificationDAO.getById(DEFAULT_CLUSTER_IDENTIFICATION_ID);
    var expectedTamperCode = testSubject.calculateTamperCode(currentClusterIdentification);
    assertThat(currentClusterIdentification.getTamperCode()).isEqualTo(expectedTamperCode);
  }

  private void validateCalculatedIdsReplaced(
      IdResolutionResult idResolutionResult,
      String calculatedClusterId,
      String generatedTelemetryId)
  {
    // we are generating a new cluster ID as a random UUID
    assertThat(idResolutionResult.assignedClusterId()).isNotEqualTo(calculatedClusterId);
    assertIsUUID(idResolutionResult.assignedClusterId());

    var expectedTelemetryId = createExpectedTelemetryId(idResolutionResult.assignedClusterId(), generatedTelemetryId);
    assertThat(idResolutionResult.assignedTelemetryId()).isEqualTo(expectedTelemetryId);
  }

  /**
   * when we generate a new telemetry ID we use:
   * - the first 5 chars of the new cluster ID (random UUID)
   * - and the last 5 chars of the generated telemetry ID, which is a computed hash of hardware info
   */
  private String createExpectedTelemetryId(String assignedClusterId, String generatedTelemetryId) {
    final var expectedTelemetryIdPrefix = assignedClusterId.substring(0, 5);
    final var expectedTelemetryIdSuffix = generatedTelemetryId.substring(generatedTelemetryId.indexOf('-') + 1);
    return format("%s-%s", expectedTelemetryIdPrefix, expectedTelemetryIdSuffix);
  }

  private void validateAssignedIdsPreserved(
      IdResolutionResult idResolutionResult,
      String assignedClusterId,
      String assignedTelemetryId)
  {
    validateIdsPreserved(idResolutionResult, assignedClusterId, assignedTelemetryId);
  }

  private void validateProvidedIdsPreserved(
      IdResolutionResult idResolutionResult,
      String calculatedClusterId,
      String generatedTelemetryId)
  {
    validateIdsPreserved(idResolutionResult, calculatedClusterId, generatedTelemetryId);
  }

  private void validateIdsPreserved(
      IdResolutionResult idResolutionResult,
      String expectedAssignedClusterId,
      String expectedAssignedTelemetryId)
  {
    assertThat(idResolutionResult.assignedClusterId()).isEqualTo(expectedAssignedClusterId);
    assertThat(idResolutionResult.assignedTelemetryId()).isEqualTo(expectedAssignedTelemetryId);
  }

  private void validateCorruptedTelemetryIdFixed(
      IdResolutionResult idResolutionResult,
      String expectedAssignedClusterId,
      String corruptedTelemetryId)
  {
    assertThat(idResolutionResult.assignedClusterId()).isEqualTo(expectedAssignedClusterId);
    // the fixed ID will take the first 5 chars of the cluster ID and append the last 5 chars of the corrupted ID
    var expectedAssignedTelemetryId = createExpectedTelemetryId(expectedAssignedClusterId, corruptedTelemetryId);
    assertThat(idResolutionResult.assignedTelemetryId()).isEqualTo(expectedAssignedTelemetryId);
  }

  private void validateNewIdsCreated(
      IdResolutionResult idResolutionResult,
      String ogAssignedClusterId,
      String ogAssignedTelemetryId)
  {
    assertThat(idResolutionResult.assignedClusterId()).isNotEqualTo(ogAssignedClusterId);
    assertIsUUID(idResolutionResult.assignedClusterId());

    // the new telemetry ID will take the first 5 chars of the new cluster ID and append the last 5 chars of the old ID
    var expectedAssignedTelemetryId =
        createExpectedTelemetryId(idResolutionResult.assignedClusterId(), ogAssignedTelemetryId);
    assertThat(idResolutionResult.assignedTelemetryId()).isEqualTo(expectedAssignedTelemetryId);
  }

  private void validatePersistedClusterIdentification(
      IdResolutionResult idResolutionResult,
      String actualBaseUrl,
      String expectedLastCalculatedClusterId)
  {
    final var expectedAssignedClusterId = idResolutionResult.assignedClusterId();
    final var expectedAssignedTelemetryId = idResolutionResult.assignedTelemetryId();
    final var clusterIdentification = clusterIdentificationDAO.getById(DEFAULT_CLUSTER_IDENTIFICATION_ID);

    assertThat(clusterIdentification.getAssignedClusterId()).isEqualTo(expectedAssignedClusterId);
    assertThat(clusterIdentification.getAssignedTelemetryId()).isEqualTo(expectedAssignedTelemetryId);
    assertThat(clusterIdentification.getLastCalculatedClusterId()).isEqualTo(expectedLastCalculatedClusterId);
    assertThat(clusterIdentification.getCreated()).isAfterOrEqualTo(testStartTime);

    if (null == updateTime) {
      assertThat(clusterIdentification.getLastUpdated()).isNull();
    }
    else {
      assertThat(clusterIdentification.getLastUpdated()).isAfterOrEqualTo(updateTime);
    }

    validateTamperCodeUpdated();
    assertThat(clusterIdentification.getTamperCode()).isNotNull();
    if (null != expectedAssignedClusterId) {
      assertThat(clusterIdentification.getTamperCode()).doesNotContainIgnoringCase(expectedAssignedClusterId);
    }
    assertThat(clusterIdentification.getTamperCode()).doesNotContainIgnoringCase(expectedAssignedTelemetryId);

    assertThat(clusterIdentification.getBaseUrlHash()).isNotNull();
    assertThat(clusterIdentification.getBaseUrlHash()).isNotEqualTo(actualBaseUrl);
  }

  private void validateTelemetry(ResolutionOutcome expectedOutcome) {
    ArgumentCaptor<TelemetryData> captor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetryQueue).add(captor.capture());

    TelemetryData capturedData = captor.getValue();
    assertThat(capturedData.getPurpose()).isEqualTo(TelemetryPurpose.CLUSTER_IDENTITY);
    assertThat(capturedData.getAttributes())
        .containsEntry(ClusterIdentificationService.RESOLUTION_OUTCOME, expectedOutcome.name());

    verify(mockTelemetryQueue, never()).flush();

    // when: send the telemetry
    testSubject.sendTelemetry();

    // then:
    verify(mockTelemetryQueue, times(1)).flush();
  }

  private void withBaseUrl(String baseUrl) {
    reset(mockBaseUrlProvider);
    when(mockBaseUrlProvider.getBaseUrl()).thenReturn(baseUrl);
  }
}
