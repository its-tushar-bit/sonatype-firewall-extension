/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import com.sonatype.insight.brain.model.Owner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LifecycleReportTest
{
  @Mock
  private LifecycleReportPersistenceService persistenceService;

  @Test
  public void getOwner_returnsConstructorOwner() {
    Owner owner = mock(Owner.class);

    LifecycleReport report = new LifecycleReport(persistenceService, owner, "scan-id-42");

    assertThat(report.getOwner()).isSameAs(owner);
    assertThat(report.getScanId()).isEqualTo("scan-id-42");
  }

  @Test
  public void embedOwnerPublicId_readsPublicIdFromOwner() throws IOException {
    // Proves embedOwnerPublicId reads owner.getPublicId(), not application.getPublicId().
    // Uses a raw Owner mock (NOT an Application) — the widening is not cosmetic.
    Owner owner = mock(Owner.class);
    when(owner.getId()).thenReturn("owner-uuid-999");
    when(owner.getPublicId()).thenReturn("public-id-for-owner");

    // A concrete persistence service that serves a cached index.html and captures saved content.
    // We use a concrete subclass because saveReportFile/getReportEntity are final on the abstract class
    // and cannot be reliably stubbed via Mockito doAnswer for the stream-content assertion.
    StringBuilder savedContent = new StringBuilder();
    byte[] html = "<html><script>var applicationId = '';</script></html>".getBytes(StandardCharsets.UTF_8);

    LifecycleReportPersistenceService concretePersistence = new LifecycleReportPersistenceService()
    {
      @Override
      protected ReportEntity doGetReportEntity(String ownerId, String scanId, String name) throws IOException {
        ReportEntity entity = mock(ReportEntity.class);
        when(entity.exists(MetadataSource.CACHED)).thenReturn(true);
        when(entity.getTime(MetadataSource.CACHED)).thenReturn(0L);
        when(entity.getInputStream()).thenReturn(new ByteArrayInputStream(html));
        return entity;
      }

      @Override
      protected void doSaveReportFile(
          String ownerId,
          String scanId,
          String name,
          InputStream contents) throws IOException
      {
        savedContent.append(new String(contents.readAllBytes(), StandardCharsets.UTF_8));
      }

      @Override
      public Stream<ReportEntity> getAllReportEntities(String ownerId, String scanId) {
        return Stream.empty();
      }

      @Override
      public Stream<ReportEntity> getOriginalReportEntities(String ownerId, String scanId) {
        return Stream.empty();
      }

      @Override
      public void saveOriginalReport(String ownerId, String scanId, InputStream reportZipContents) {
        // no-op
      }

      @Override
      public void saveOriginalReportEntities(
          String ownerId,
          String scanId,
          Stream<ReportEntity> originalReportEntities)
      {
        // no-op
      }

      @Override
      public void moveReport(String appId, String sourceScanId, String destinationScanId) {
        // no-op
      }

      @Override
      protected void doSaveAdditionalReportFile(
          String ownerId,
          String scanId,
          String name,
          InputStream contents)
      {
        // no-op
      }

      @Override
      public ReportPdfEntity getPdfEntity(String ownerId, String scanId) {
        return null;
      }

      @Override
      public BaseReportEntity getVulnerabilitySignaturesEntity(String ownerId, String scanId) {
        return null;
      }

      @Override
      public String getReportLocation(String ownerId, String scanId) {
        return null;
      }

      @Override
      public boolean reportExists(String ownerId, String scanId) {
        return true;
      }

      @Override
      public void deleteReport(String ownerId, String scanId) {
        // no-op
      }

      @Override
      public void deleteReports(String ownerId) {
        // no-op
      }

      @Override
      public void deleteReportEntity(ReportEntity reportEntity) {
        // no-op
      }

      @Override
      public Class<? extends ReportEntity> getReportEntityClass() {
        return ReportEntity.class;
      }
    };

    LifecycleReport report = new LifecycleReport(concretePersistence, owner, "scan-id-1");

    // Invoke the method under test.
    report.embedOwnerPublicId();

    // Verify: owner.getPublicId() was called — the widening from Application to Owner is real.
    verify(owner).getPublicId();

    // Verify: the HTML placeholder was replaced with the owner's public ID.
    assertThat(savedContent.toString()).contains("applicationId = 'public-id-for-owner'");
  }
}
