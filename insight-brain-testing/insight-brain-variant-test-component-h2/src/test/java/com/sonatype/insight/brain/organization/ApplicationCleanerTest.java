/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@ComponentH2Test
public class ApplicationCleanerTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApplicationCleaner appCleaner;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private InsightWork work;

  @Inject
  private GitHubAppDAO gitHubAppDAO;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Test
  public void testDelete_DeleteDirectories() throws Exception {
    // Given
    Application app = tempEntity.newApplicationWithParent();
    File iconDir = new File(work.getApplicationIconDir(), app.getId());
    iconDir.mkdirs();
    new File(iconDir, "icon.png").createNewFile();

    File scanDir = work.getScanDir(app.getId());
    scanDir.mkdirs();
    new File(scanDir, "scanFile").createNewFile();
    File auditDir = work.getAuditDir(app.getId());
    auditDir.mkdirs();
    new File(auditDir, "auditFile").createNewFile();
    File reportDir = work.getReportDir(app.getId());
    reportDir.mkdirs();
    new File(auditDir, "reportFile").createNewFile();
    File sourceControlDir = work.getSourceControlDir(app.getId());
    sourceControlDir.mkdirs();
    new File(auditDir, "sourceControlFile").createNewFile();
    File sbomDir = work.getSbomDir(app.getId());
    sbomDir.mkdirs();
    new File(sbomDir, "sbomFile").createNewFile();

    assertThat(scanDir).exists();
    assertThat(auditDir).exists();
    assertThat(reportDir).exists();
    assertThat(sourceControlDir).exists();
    assertThat(sbomDir).exists();

    // When
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      appCleaner.delete(tx, app);
      tx.commit();
    }

    // Then
    assertThat(iconDir).doesNotExist();
    assertThat(scanDir).doesNotExist();
    assertThat(auditDir).doesNotExist();
    assertThat(reportDir).doesNotExist();
    assertThat(sourceControlDir).doesNotExist();
    assertThat(sbomDir).doesNotExist();

    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    final OwnerMaintenanceTelemetry ownerMaintenanceTelemetry =
        new OwnerMaintenanceTelemetry(
            app.getId(),
            app.getName(),
            app.getParentOwnerId(),
            app.getType().toString(),
            OwnerMaintenanceTelemetry.TYPE_DELETE);
    final Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY, ownerMaintenanceTelemetry);

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REAL_OWNER_IDS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertAttributes(expectedAttributes, telemetryData.getAttributes());
  }

  private void assertAttributes(
      final Map<String, Object> expectedAttributes,
      final Map<String, Object> actualAttributes)
  {
    assertThat(actualAttributes).containsKey(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY);
    assertTelemetryData(
        (OwnerMaintenanceTelemetry) expectedAttributes.get(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY),
        (OwnerMaintenanceTelemetry) actualAttributes.get(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY));
  }

  private void assertTelemetryData(final OwnerMaintenanceTelemetry expected, final OwnerMaintenanceTelemetry actual) {
    assertThat(actual.getOwnerId()).isEqualTo(expected.getOwnerId());
    assertThat(actual.getOwnerName()).isEqualTo(expected.getOwnerName());
    assertThat(actual.getParentOwnerId()).isEqualTo(expected.getParentOwnerId());
    assertThat(actual.getOwnerType()).isEqualTo(expected.getOwnerType());
    assertThat(actual.getOwnerMaintenanceType()).isEqualTo(expected.getOwnerMaintenanceType());
  }

  @Test
  public void testDelete_DeactivatesGitHubApps() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    GitHubApp gitHubApp1 = tempEntity.newGitHubApp(application.getId());
    gitHubApp1.setActive(true);
    gitHubAppDAO.update(gitHubApp1);

    GitHubApp gitHubApp2 = tempEntity.newGitHubApp(application.getId());
    gitHubApp2.setActive(false);
    gitHubAppDAO.update(gitHubApp2);

    assertThat(gitHubAppDAO.getById(gitHubApp1.getId())).isNotNull();
    assertThat(gitHubAppDAO.getById(gitHubApp2.getId())).isNotNull();
    assertThat(gitHubAppDAO.getById(gitHubApp1.getId()).isActive()).isTrue();

    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      appCleaner.delete(tx, application);
      tx.commit();
    }

    assertThat(applicationDAO.getById(application.getId())).isNull();
    assertThat(gitHubAppDAO.getById(gitHubApp1.getId()).isActive()).isFalse();
    assertThat(gitHubAppDAO.getById(gitHubApp2.getId()).isActive()).isFalse();
  }

  @Test
  public void testDelete_NoGitHubApps_StillSucceeds() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    assertThat(gitHubAppDAO.getByOwnerId(application.getId())).isEmpty();

    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      appCleaner.delete(tx, application);
      tx.commit();
    }

    assertThat(applicationDAO.getById(application.getId())).isNull();
  }

  @Test
  public void testDelete_WithMultipleGitHubApps_RollsBackAllDeactivationsOnFailure() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    GitHubApp gitHubApp1 = tempEntity.newGitHubApp(application.getId());
    gitHubApp1.setActive(true);
    gitHubAppDAO.update(gitHubApp1);

    GitHubApp gitHubApp2 = tempEntity.newGitHubApp(application.getId());
    gitHubApp2.setActive(false);
    gitHubAppDAO.update(gitHubApp2);

    GitHubApp gitHubApp3 = tempEntity.newGitHubApp(application.getId());
    gitHubApp3.setActive(true);
    gitHubAppDAO.update(gitHubApp3);

    assertThat(gitHubAppDAO.getById(gitHubApp1.getId()).isActive()).isTrue();
    assertThat(gitHubAppDAO.getById(gitHubApp2.getId()).isActive()).isFalse();
    assertThat(gitHubAppDAO.getById(gitHubApp3.getId()).isActive()).isTrue();

    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      appCleaner.delete(tx, application);
      tx.rollback();
    }

    assertThat(gitHubAppDAO.getById(gitHubApp1.getId()).isActive()).isTrue();
    assertThat(gitHubAppDAO.getById(gitHubApp2.getId()).isActive()).isFalse();
    assertThat(gitHubAppDAO.getById(gitHubApp3.getId()).isActive()).isTrue();
    assertThat(applicationDAO.getById(application.getId())).isNotNull();
  }
}
