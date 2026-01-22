/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class ApplicationCleanerTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationCleaner appCleaner;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private InsightWork work;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
  }

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
            OwnerMaintenanceTelemetry.TYPE_DELETE
        );
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
}
