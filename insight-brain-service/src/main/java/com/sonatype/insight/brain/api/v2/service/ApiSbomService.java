/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryDTO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SbomAction;
import com.sonatype.insight.brain.thirdparty.SbomStatus;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.file.ThirdPartyUtils.SbomFormat;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApiSbomService
{
  private static final Logger log = LoggerFactory.getLogger(ApiSbomService.class);

  private static final String cannotFindVersionError = "Cannot find version %s for application with ID %s.";

  public static final String SBOM_STATE_CURRENT = "current";

  public static final String SBOM_STATE_ORIGINAL = "original";

  private final ThirdPartySbomMetadataDAO dao;

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final InsightWork insightWork;

  private final ApplicationDAO applicationDAO;

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  public ApiSbomService(
      final ThirdPartySbomMetadataDAO dao, final ThirdPartyFileDAO thirdPartyFileDAO, final InsightWork insightWork,
      final ApplicationDAO applicationDAO, final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO)
  {
    this.dao = dao;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.insightWork = insightWork;
    this.applicationDAO = applicationDAO;
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteSbomVersion(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sbomVersion) throws IOException
  {
    final ThirdPartySbomMetadata thirdPartySbomMetadata = getThirdPartySbomMetadataNotNull(applicationId, sbomVersion);

    AuditData.get().setSbomVersion(thirdPartySbomMetadata, SbomAction.DELETE);

    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      thirdPartyFileDAO.delete(tx, thirdPartySbomMetadata.getThirdPartyFileId());
      Files.delete(new File(insightWork.getSbomDir(applicationId), thirdPartySbomMetadata.getFilename()).toPath());
      tx.commit();
    }
  }

  @Authorize(permission = Permission.READ)
  public Response getSbomVersion(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sbomVersion,
      String sbomState)
  {
    if (!sbomState.equals(SBOM_STATE_CURRENT) && !sbomState.equals(SBOM_STATE_ORIGINAL)) {
      throw new BadRequestException("Invalid sbom state " + sbomState);
    }

    if (sbomState.equals(SBOM_STATE_CURRENT)) {
      throw new BadRequestException("Retrieving the current state of the sbom is not supported yet.");
    }

    final ThirdPartySbomMetadata thirdPartySbomMetadata =
        dao.getByApplicationIdAndSbomVersionAndStatus(applicationId, sbomVersion, SbomStatus.ACTIVE.name());
    if (thirdPartySbomMetadata == null) {
      throw new NotFoundException(String.format(cannotFindVersionError, sbomVersion, applicationId));
    }

    MediaType type;
    String fileName =
        applicationDAO.getById(applicationId).getName() + "_" + sbomVersion + "." +
            thirdPartySbomMetadata.getSpecFormat();
    if (thirdPartySbomMetadata.getSpecFormat().equals(SbomFormat.JSON.toString().toLowerCase())) {
      type = MediaType.APPLICATION_JSON_TYPE;
    }
    else {
      type = MediaType.APPLICATION_XML_TYPE;
    }

    File sbomDir = insightWork.getSbomDir(applicationId);

    try (FileInputStream fileInputStream = new FileInputStream(
        new File(sbomDir, thirdPartySbomMetadata.getFilename()))) {
      GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fileInputStream);
      return Response.ok(IOUtils.toByteArray(gzipInputStream), type)
          .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(fileName))
          .build();
    }
    catch (IOException e) {
      log.error("File not found for sbom metadata with application id {}, version {}, filename {}", applicationId,
          sbomVersion, thirdPartySbomMetadata.getFilename(), e);
      throw new InternalServerException(
          String.format("Internal server error trying to retrieve the %s sbom for application %s version %s", sbomState,
              applicationId, sbomVersion));
    }
  }

  @Authorize(permission = Permission.READ)
  public List<ThirdPartySbomMetadataSummaryDTO> getSbomListForAppId(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId, String sortByDate, int limit, int offset)
  {
    if (offset < 0) {
      throw new BadRequestException("Offset index must not be less than zero!");
    }

    if (limit < 1) {
      throw new BadRequestException("Limit must not be less than one!");
    }
    return thirdPartyFileCoordinateDAO.getSbomApplicationVulnerabilities(applicationId, sortByDate, limit, offset);
  }

  @Authorize(permission = Permission.READ)
  public List<SbomComponentDTO> getSbomComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sbomVersion)
  {
    ThirdPartySbomMetadata thirdPartySbomMetadata = getThirdPartySbomMetadataNotNull(applicationId, sbomVersion);
    return thirdPartyFileCoordinateDAO
        .getSbomComponentsByThirdPartyFileId(thirdPartySbomMetadata.getThirdPartyFileId());
  }

  private ThirdPartySbomMetadata getThirdPartySbomMetadataNotNull(String applicationId, String sbomVersion) {
    ThirdPartySbomMetadata thirdPartySbomMetadata = dao.getByApplicationIdAndSbomVersion(applicationId, sbomVersion);
    if (thirdPartySbomMetadata == null) {
      throw new NotFoundException(String.format(cannotFindVersionError, sbomVersion, applicationId));
    }
    return thirdPartySbomMetadata;
  }
}

