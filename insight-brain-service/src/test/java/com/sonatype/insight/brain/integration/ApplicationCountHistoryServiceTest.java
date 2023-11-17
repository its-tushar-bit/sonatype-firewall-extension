/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.integration;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationCountHistory;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ApplicationCountHistoryServiceTest
    extends AbstractComponentTest
{
  @Mock
  private DateTimeService dateTimeService;

  @Inject
  private PlexusCipher plexusCipher;

  @Inject
  private ApplicationCountHistoryService applicationCountHistoryService;

  private static final String ROOT_TOKEN = "root-token";

  private static final String ENC = "CMMDwoV";

  private Organization givenOrganization;

  @Override
  public void configure(Binder binder) {
    binder.bind(DateTimeService.class).toInstance(dateTimeService);
    super.configure(binder);
  }

  @Before
  public void setup() throws PlexusCipherException {
    givenOrganization = tempEntity.newOrganization();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipher.encrypt(ROOT_TOKEN, ENC),
        SourceControlProvider.GITHUB);
  }

  @Test
  public void testRecordApplicationCount_shouldCorrectlyRecordingNumberOfApplications() {
    // === Given ===
    final Date firstRecording = Date.from(Instant.now().plus(Duration.ofDays(1)));
    final Date secondRecording = Date.from(Instant.now().plus(Duration.ofDays(2)));

    // === Then Initial State ===
    final List<ApplicationCountHistory> initialRows = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(initialRows.size()).isEqualTo(1);
    assertThat(initialRows.get(0).getId()).isEqualTo("initialization");

    // === Then First Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(firstRecording);
    tempEntity.createApplications(10, givenOrganization);
    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording.size()).isEqualTo(2);
    assertApplicationHistoryCountEqual(
        rowsAfterFirstRecording.get(1),
        new ApplicationCountHistory(firstRecording, 10, 0)
    );

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);
    tempEntity.createApplications(34, givenOrganization);
    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterSecondRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterSecondRecording.size()).isEqualTo(3);
    assertApplicationHistoryCountEqual(
        rowsAfterSecondRecording.get(2),
        new ApplicationCountHistory(secondRecording, 44, 0)
    );
  }

  @Test
  public void testRecordApplicationCount_shouldCorrectlyRecordNumberAppsWithScmFeedbackEnabled() {
    // === Given ===
    final Date firstRecording = Date.from(Instant.now().plus(Duration.ofDays(1)));
    final Date secondRecording = Date.from(Instant.now().plus(Duration.ofDays(2)));

    // === Then First Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(firstRecording);
    final List<Application> applications = tempEntity.createApplications(7, givenOrganization);
    enableScmFeedBackForApp(applications.get(0));
    enableScmFeedBackForApp(applications.get(2));

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterFirstRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterFirstRecording.size()).isEqualTo(2);
    assertApplicationHistoryCountEqual(
        rowsAfterFirstRecording.get(1),
        new ApplicationCountHistory(firstRecording, 7, 2)
    );

    // === Then Second Recording ===
    when(dateTimeService.getCurrentDate()).thenReturn(secondRecording);
    enableScmFeedBackForApp(applications.get(3));
    enableScmFeedBackForApp(applications.get(4));
    enableScmFeedBackForApp(applications.get(5));

    applicationCountHistoryService.recordApplicationCount();

    final List<ApplicationCountHistory> rowsAfterSecondRecording = tempEntity.getAllApplicationHistoryCountRows();
    assertThat(rowsAfterSecondRecording.size()).isEqualTo(3);
    assertApplicationHistoryCountEqual(
        rowsAfterSecondRecording.get(2),
        new ApplicationCountHistory(secondRecording, 7, 5)
    );
  }

  // ignore id equality
  private void assertApplicationHistoryCountEqual(
      final ApplicationCountHistory actual,
      final ApplicationCountHistory expected
  )
  {
    assertThat(actual.getApplicationCount()).isEqualTo(expected.getApplicationCount());
    assertThat(actual.getScmFeedbackEnabledCount()).isEqualTo(expected.getScmFeedbackEnabledCount());
    assertThat(actual.getUpdatedDate()).isEqualTo(expected.getUpdatedDate());
  }

  private void enableScmFeedBackForApp(final Application application) {
    final String anyRepoUrl = "https://example.com/organization/" + UUID.randomUUID();
    final String appId = application.getId();

    tempEntity.newSourceControl(
        appId,
        anyRepoUrl,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        null,
        true,
        true,
        "/target/*",
        true,
        true
    );
  }
}
