/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.report.ApplicationReport.ReportFile;
import com.sonatype.insight.brain.report.ApplicationReport.ReportFileLocationType;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceProvider;
import com.sonatype.insight.brain.report.ReportEntity;
import com.sonatype.insight.brain.report.ReportPdfEntity;
import com.sonatype.insight.brain.sbom.datastore.SbomEntity;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceServiceProvider;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceProvider;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class CopyStorageService
{
  private static final Logger log = LoggerFactory.getLogger(CopyStorageService.class);

  private static final String COPY_MARKER = "copyInProgress";

  private static final int DEFAULT_PAGE_SIZE = 10000;

  private static final int DEFAULT_APPS_PER_LOG = 100;

  private static final Set<DataStoreType> SUPPORTED = Set.of(DataStoreType.FILE, DataStoreType.S3);

  private final InsightConfig insightConfig;

  private final Provider<ScanPersistenceServiceProvider> scanPersistanceProvider;

  private final Provider<ApplicationReportPersistenceServiceProvider> reportPersistenceProvider;

  private final Provider<SbomPersistenceServiceProvider> sbomPersistenceProvider;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ClusterLockManager clusterLockManager;

  private final ExecutorService zipExecutorService;

  @Inject
  public CopyStorageService(
      final InsightConfig insightConfig,
      final Provider<ScanPersistenceServiceProvider> scanPersistanceProvider,
      final Provider<ApplicationReportPersistenceServiceProvider> reportPersistenceProvider,
      final Provider<SbomPersistenceServiceProvider> sbomPersistenceProvider,
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ClusterLockManager clusterLockManager)
  {
    this.insightConfig = insightConfig;
    this.scanPersistanceProvider = scanPersistanceProvider;
    this.reportPersistenceProvider = reportPersistenceProvider;
    this.sbomPersistenceProvider = sbomPersistenceProvider;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.clusterLockManager = clusterLockManager;
    zipExecutorService = Executors.newSingleThreadExecutor();
  }

  public void execute(final DataStoreType from, final DataStoreType to) {
    checkSupported(from);
    checkSupported(to);
    checkPrimaryStorageIsTarget(to);
    checkFromAndToAreDifferent(from, to);

    // From persistence services
    ScanPersistenceService fromScan = scanPersistanceProvider.get().get(from);
    ApplicationReportPersistenceService fromReport = reportPersistenceProvider.get().get(from);
    SbomPersistenceService fromSbom = sbomPersistenceProvider.get().get(from);

    // To persistence services
    ScanPersistenceService toScan = scanPersistanceProvider.get().get(to);
    ApplicationReportPersistenceService toReport = reportPersistenceProvider.get().get(to);
    SbomPersistenceService toSbom = sbomPersistenceProvider.get().get(to);

    Iterator<Application> apps = createApplicationIterator();
    log.info("Starting copy of scans, reports, and SBOMs from '{}' to '{}'.", from, to);
    long start = System.currentTimeMillis();
    int count = 0;
    while (apps.hasNext()) {
      if (count > 0 && count % DEFAULT_APPS_PER_LOG == 0) {
        log.info("Copy of scans, reports, and SBOMs from '{}' to '{}' completed for {} app(s).", from, to, count);
      }

      Application app = apps.next();

      // Scans
      log.trace("Starting copy of scans from '{}' to '{}' for app '{}' with id '{}'.", from, to, app.getName(),
          app.getId());
      copyScans(app, fromScan, toScan);
      log.trace("Finished copy of scans from '{}' to '{}' for app '{}' with id '{}'.", from, to, app.getName(),
          app.getId());

      // Reports
      log.trace("Starting copy of reports from '{}' to '{}' for app '{}' with id '{}'.", from, to, app.getName(),
          app.getId());
      copyReports(app, fromReport, toReport);
      log.trace("Finished copy of reports from '{}' to '{}' for app '{}' with id '{}'.", from, to, app.getName(),
          app.getId());

      // SBOMs
      log.trace("Starting copy of SBOMs from '{}' to '{}' for app '{}' with id '{}'.", from, to, app.getName(),
          app.getId());
      copySboms(app, fromSbom, toSbom);
      log.trace("Finished copy of SBOMs from '{}' to '{}' for app '{}' with id '{}'.", from, to, app.getName(),
          app.getId());

      count++;
    }
    long elapsed = System.currentTimeMillis() - start;
    log.info("Finished copy of scans, reports, and SBOMs from '{}' to '{}' for {} app(s) in '{}' ms.", from, to, count,
        elapsed);
  }

  public void checkSupported(final DataStoreType dataStoreType) {
    if (!SUPPORTED.contains(dataStoreType)) {
      throw new IllegalArgumentException("Storage '%s' is unsupported.".formatted(dataStoreType));
    }
  }

  public void checkPrimaryStorageIsTarget(final DataStoreType to) {
    StorageConfig storage = insightConfig.getStorage();
    DataStoreType primaryStorage = storage.getType();
    if (DataStoreType.HYBRID == primaryStorage) {
      primaryStorage = storage.getHybridConfig().getTypes().iterator().next();
    }
    if (primaryStorage != to) {
      throw new BadRequestException(("Primary storage type is '%s' but copy is targeting '%s'," +
          " scans, reports, and/or SBOMs written during copy may be missed.").formatted(primaryStorage, to));
    }
  }

  public void checkFromAndToAreDifferent(final DataStoreType from, final DataStoreType to) {
    if (from == to) {
      throw new BadRequestException("Not copying from '%s' to '%s', these should be different.".formatted(from, to));
    }
  }

  private void copyScans(final Application app, final ScanPersistenceService from, final ScanPersistenceService to) {
    from.allScanFilesFor(app.getId()).forEach(sourceScan -> {
      String appId = sourceScan.getAppId();
      String scanId = sourceScan.getScanId();

      if (!sourceScan.exists()) {
        log.trace("Skipping scan copying for app id '{}' scan id '{}' since it does not exist.", appId, scanId);
        return;
      }

      ScanEntity targetScan = to.getScan(appId, scanId);
      if (targetScan.exists()) {
        log.trace("Skipping scan copying for app id '{}' scan id '{}' since it is already done.", appId, scanId);
        return;
      }

      String fromLocation = sourceScan.getLocation();
      String toLocation = targetScan.getLocation();

      ScanEntity targetTempScan = null;
      Exception exception = null;
      try {
        try {
          log.trace("Copying scan from '{}' to '{}'.", fromLocation, toLocation);
          targetTempScan = to.createTempScan(appId);
          try (InputStream inputStream = sourceScan.getInputStream();
               OutputStream outputStream = targetTempScan.getOutputStream()) {
            inputStream.transferTo(outputStream);
          }
          to.moveTempScan(targetTempScan, appId, scanId);
          log.trace("Copied scan from '{}' to '{}'.", fromLocation, toLocation);
        }
        catch (Exception e) {
          exception = e;
          throw exception;
        }
        finally {
          try {
            if (targetTempScan != null && targetTempScan.exists()) {
              to.deleteScan(targetTempScan);
            }
          }
          catch (Exception e) {
            if (exception != null) {
              exception.addSuppressed(e);
            }
            else {
              exception = e;
            }
          }
        }
        if (exception != null) {
          throw exception;
        }
      }
      catch (Exception e) {
        log.error("Failed to copy scan from '{}' to '{}'.", fromLocation, toLocation, e);
      }
    });
  }

  private void copyReports(
      final Application app,
      final ApplicationReportPersistenceService from,
      final ApplicationReportPersistenceService to)
  {
    Iterator<PolicyEvaluation> evals = createPolicyEvaluationIterator(app.getId());
    while (evals.hasNext()) {
      PolicyEvaluation eval = evals.next();
      String appId = eval.getApplicationId();
      String scanId = eval.getScanId();

      String fromLocation = from.getReportLocation(appId, scanId);
      String toLocation = to.getReportLocation(appId, scanId);

      try (ClusterLock clusterLock = clusterLockManager.createForPolicyEvaluation(app, scanId)) {
        ReportEntity copyMarker;
        if (to.reportExists(appId, scanId)) {
          copyMarker = to.getReportEntity(eval.getApplicationId(), eval.getScanId(), COPY_MARKER);
          if (!copyMarker.exists()) {
            log.trace("Skipping report copying for app id '{}' scan id '{}' since it is already done.", appId, scanId);
            continue;
          }
        }
        else {
          to.saveAdditionalReportFile(appId, scanId, COPY_MARKER, new ByteArrayInputStream(new byte[0]));
          copyMarker = to.getReportEntity(eval.getApplicationId(), eval.getScanId(), COPY_MARKER);
        }
        // Once we're sure we want to copy this report, lock it so it can't be changed
        clusterLock.lock();
        if (!from.reportExists(appId, scanId)) {
          log.trace("Skipping report copying for app id '{}' scan id '{}' since it does not exist.", appId, scanId);
          continue;
        }
        log.trace("Copying report from '{}' to '{}'.", fromLocation, toLocation);

        try (InputStream inputStream = createInputStream(from.getOriginalReportEntities(appId, scanId))) {
          to.saveOriginalReport(appId, scanId, inputStream);
        }
        from.getAllReportEntities(appId, scanId).forEach(sourceReportEntity -> {
          try {
            ReportFile reportFile = ReportFile.fromName(sourceReportEntity.getName());
            if (reportFile == null || reportFile.getLocationTypes().contains(ReportFileLocationType.ADDITIONAL)) {
              try (InputStream inputStream = sourceReportEntity.getInputStream()) {
                to.saveAdditionalReportFile(appId, scanId, sourceReportEntity.getName(), inputStream);
              }
            }
            else {
              try (InputStream inputStream = sourceReportEntity.getInputStream()) {
                to.saveReportFile(appId, scanId, sourceReportEntity.getName(), inputStream);
              }
            }
          }
          catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
        ReportPdfEntity sourceReportPdfEntity = from.getPdfEntity(appId, scanId);
        ReportPdfEntity targetReportPdfEntity = to.getPdfEntity(appId, scanId);
        if (sourceReportPdfEntity.exists()) {
          try (InputStream inputStream = sourceReportPdfEntity.getInputStream();
               OutputStream outputStream = targetReportPdfEntity.getOutputStream()) {
            inputStream.transferTo(outputStream);
          }
        }
        to.deleteReportEntity(copyMarker);
        log.trace("Copied report from '{}' to '{}'.", fromLocation, toLocation);
      }
      catch (Exception e) {
        log.error("Failed to copy report from '{}' to '{}'.", fromLocation, toLocation, e);
      }
    }
  }

  private void copySboms(final Application app, final SbomPersistenceService from, final SbomPersistenceService to) {
    Iterator<ThirdPartySbomMetadata> sboms = createThirdPartySbomMetadataIterator(app.getId());
    while (sboms.hasNext()) {
      ThirdPartySbomMetadata sbomMetadata = sboms.next();
      String appId = sbomMetadata.getApplicationId();
      String fileName = sbomMetadata.getFilename();

      SbomEntity sourceSbom = from.getPermanentSbom(appId, fileName);
      SbomEntity targetSbom = to.getPermanentSbom(appId, fileName);

      String fromLocation = sourceSbom.getLocation();
      String toLocation = targetSbom.getLocation();

      try {
        if (!sourceSbom.exists()) {
          log.trace("Skipping sbom copying for app id '{}' file name '{}' since it does not exist.", appId, fileName);
          continue;
        }

        if (targetSbom.exists()) {
          log.trace("Skipping sbom copying for app id '{}' file name '{}' since it is already done.", appId, fileName);
          continue;
        }

        SbomEntity targetTempSbom = null;
        Exception exception = null;
        try {
          targetTempSbom = to.getTransientSbom(fileName);
          log.trace("Copying sbom from '{}' to '{}'.", fromLocation, toLocation);
          try (
              InputStream inputStream = sourceSbom.getInputStream();
              OutputStream outputStream = targetTempSbom.getOutputStream()
          ) {
            inputStream.transferTo(outputStream);
          }
          to.moveSbomEntity(targetTempSbom, targetSbom);
          log.trace("Copied sbom from '{}' to '{}'.", fromLocation, toLocation);
        }
        catch (Exception e) {
          exception = e;
          throw exception;
        }
        finally {
          try {
            if (targetTempSbom != null && targetTempSbom.exists()) {
              to.deleteSbom(targetTempSbom);
            }
          }
          catch (Exception e) {
            if (exception != null) {
              exception.addSuppressed(e);
            }
            else {
              exception = e;
            }
          }
        }
        if (exception != null) {
          throw exception;
        }
      }
      catch (Exception e) {
        log.error("Failed to copy sbom from '{}' to '{}'.", fromLocation, toLocation, e);
      }
    }
  }

  private Iterator<Application> createApplicationIterator() {
    return new PageIterator<>(1, DEFAULT_PAGE_SIZE, applicationDAO::getAll);
  }

  private Iterator<PolicyEvaluation> createPolicyEvaluationIterator(final String applicationId) {
    return new PageIterator<>(1, DEFAULT_PAGE_SIZE,
        (page, pageSize) -> policyEvaluationDAO.getByApplicationId(applicationId, page, pageSize));
  }

  private Iterator<ThirdPartySbomMetadata> createThirdPartySbomMetadataIterator(final String applicationId) {
    return new PageIterator<>(1, DEFAULT_PAGE_SIZE,
        (page, pageSize) -> thirdPartySbomMetadataDAO.getActiveByApplicationId(applicationId, page, pageSize));
  }

  private static class InputStreamWithAsyncWriter
      extends FilterInputStream
  {
    private final Future<?> asyncWriting;

    InputStreamWithAsyncWriter(final InputStream inputStream, final Future<?> asyncWriting) {
      super(inputStream);
      this.asyncWriting = asyncWriting;
    }

    @Override
    public void close() throws IOException {
      try {
        asyncWriting.get();
      }
      catch (ExecutionException e) {
        if (e.getCause() instanceof IOException ioException) {
          throw ioException;
        }
        throw new IOException(e);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException(e);
      }
      finally {
        super.close();
      }
    }
  }

  private InputStreamWithAsyncWriter createInputStream(final Stream<ReportEntity> reportEntities)
      throws IOException
  {
    PipedInputStream pipedInputStream = new PipedInputStream();
    PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
    Future<?> future = zipExecutorService.submit(() -> {
      try (ZipOutputStream zip = new ZipOutputStream(pipedOutputStream)) {
        reportEntities.forEach(reportEntity -> {
          try (InputStream inputStream = reportEntity.getInputStream()) {
            zip.putNextEntry(new ZipEntry(reportEntity.getName()));
            inputStream.transferTo(zip);
            zip.closeEntry();
          }
          catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new UncheckedIOException(e);
          }
        });
      }
      catch (IOException e) {
        log.error(e.getMessage(), e);
        throw new UncheckedIOException(e);
      }
      finally {
        reportEntities.close();
      }
    });
    return new InputStreamWithAsyncWriter(pipedInputStream, future);
  }
}
