/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.generation;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomSummary;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.thirdparty.ThirdPartyResultHandlerFactory;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanContent;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanContext;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultHandler;
import com.sonatype.insight.scan.model.ItemContentType;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.cyclonedx.Version;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Derives a CycloneDX SBOM from a completed scan's evaluation results and persists it in SBOM Manager.
 * Used by CLI compliance scans where the scan stream itself contains no SBOM-typed items but the user
 * has supplied a {@code -av/--application-version}.
 */
@Named
@Singleton
public class ScanResultsSbomPersister
{
  private static final Logger log = LoggerFactory.getLogger(ScanResultsSbomPersister.class);

  private final ApiCycloneDxServiceV2 cycloneDxService;

  private final ThirdPartyPersistenceService persistenceService;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final ThirdPartySbomMetadataDAO sbomMetadataDAO;

  private final ThirdPartyResultHandlerFactory handlerFactory;

  @Inject
  public ScanResultsSbomPersister(
      ApiCycloneDxServiceV2 cycloneDxService,
      ThirdPartyPersistenceService persistenceService,
      ThirdPartyScanDAO thirdPartyScanDAO,
      ThirdPartySbomMetadataDAO sbomMetadataDAO,
      ThirdPartyResultHandlerFactory handlerFactory)
  {
    this.cycloneDxService = cycloneDxService;
    this.persistenceService = persistenceService;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.sbomMetadataDAO = sbomMetadataDAO;
    this.handlerFactory = handlerFactory;
  }

  /**
   * Builds a CycloneDX 1.6 SBOM from the scan's evaluation results and persists it via
   * {@link ThirdPartyPersistenceService#saveSbomManagerSbomFromScan}. Best-effort: any exception
   * is logged but does not propagate, because the user-visible scan succeeded and the SBOM
   * persistence is a downstream side effect.
   *
   * @param application application owning the scan
   * @param scanId scan id whose evaluation results have been persisted
   * @param applicationVersion the user-preferred SBOM version. May be {@code null} when {@code -av}
   *          was omitted on the CLI; in that case {@link ThirdPartyPersistenceService#saveSbomManagerSbomFromScan}'s
   *          {@code trySaveInLoop} fallback chain assigns a 17-digit timestamp version
   *          (yyyyMMddHHmmssSSS) instead.
   * @return the actual saved SBOM version (may differ from {@code applicationVersion} if a collision suffix
   *         was appended by the persistence service, or be a timestamp when {@code applicationVersion} was
   *         null), or {@code null} if persistence failed
   */
  public String persist(Application application, String scanId, String applicationVersion) {
    try {
      Bom bom = cycloneDxService.buildBom(application, scanId, Version.VERSION_16, null);
      String content = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
      String filename = "cli-derived-" + scanId + ".cdx.xml";

      SbomDetectionResult detection = new SbomDetectionResult();
      detection.isSbom = true;
      detection.isValid = true;
      detection.mimeType = MediaType.APPLICATION_XML;
      SbomSummary summary = new SbomSummary();
      summary.applicationName = application.getName();
      summary.applicationVersion = applicationVersion;
      summary.specification = SbomSpecification.CYCLONEDX.toString();
      summary.version = ExportSpecification.CYCLONEDX_16.getVersion();
      summary.format = MediaType.APPLICATION_XML;
      detection.summary = summary;

      ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> pair =
          persistenceService.saveSbomManagerSbomFromScan(content, filename, application.getId(),
              applicationVersion, detection);
      if (thirdPartyScanDAO.getByThirdPartyFileIdAndScanId(pair.getRight().getId(), scanId) == null) {
        persistenceService.associateWithScan(pair.getRight(), scanId);
        thirdPartyScanDAO.updateScanIdForScanRequest(scanId, scanId);
      }
      String actualVersion = pair.getLeft().getSbomVersion();
      // Invoke the SBOM content handler to populate file_coordinate from the derived CycloneDX.
      // Without this, the SBOM Manager UI's summary endpoints find zero components and render empty.
      // Mirrors the regular processor flow which calls this for every <item contentType="SBOM"> in
      // the scan stream (see ThirdPartyScanResultsProcessor.processItemElement → handleContent).
      try {
        ThirdPartyScanContext shimContext = new ThirdPartyScanContext(
            scanId,
            application.getId(),
            SbomScanType.SBOM,
            null, // scanEntity not consumed by SbomResultHandler.handleAndFilterContents
            ComplianceStageType.ID);
        shimContext.setApplicationVersion(actualVersion);
        ThirdPartyScanContent scanContent = new ThirdPartyScanContent(
            filename,
            ItemContentType.SBOM,
            null, // lastModified
            null, // hash
            content); // the CycloneDX XML string we just persisted
        ThirdPartyScanResultHandler handler =
            handlerFactory.newHandler(ItemContentType.SBOM, shimContext);
        handler.handleAndFilterContents(scanContent, pair.getRight());
      }
      catch (Exception e) {
        // Component extraction is best-effort; the SBOM is still persisted. Log so operators see this.
        log.warn("Component extraction failed for app {} scan {} version {}; SBOM Manager UI may render empty.",
            application.getId(), scanId, actualVersion, e);
      }
      // Transition UPLOADED → PENDING → ACTIVE. ScanPolicyEvaluator's makeSbomActiveIfExist
      // (line 1778) runs inside evaluate(...) which has already completed by the time this
      // persister runs, so we drive the same activation explicitly here.
      persistenceService.setSbomMetadataStatusToPending(pair.getLeft());
      sbomMetadataDAO.makeSbomActiveIfExist(scanId);
      log.debug("Persisted derived CycloneDX SBOM for app {} scan {} requested version {} actual version {}",
          application.getId(), scanId, applicationVersion, actualVersion);
      return actualVersion;
    }
    catch (Exception e) {
      // Best-effort: do not fail the user's scan because SBOM-Manager persistence failed.
      log.error("Failed to derive and persist SBOM for app {} scan {} version {}",
          application.getId(), scanId, applicationVersion, e);
      return null;
    }
  }
}
