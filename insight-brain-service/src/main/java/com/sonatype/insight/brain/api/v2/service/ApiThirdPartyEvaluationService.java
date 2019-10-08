/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.cyclonedx.CycloneDxSchemaValidator;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXParseException;

/**
 * @since 1.75
 */
@Named
@Singleton
public class ApiThirdPartyEvaluationService
{
  private final CycloneDxSchemaValidator schemaValidator;

  @Inject
  public ApiThirdPartyEvaluationService(final CycloneDxSchemaValidator schemaValidator) {
    this.schemaValidator = schemaValidator;
  }

  @Authorize(permission = Permission.READ)
  public ApiThirdPartyScanTicketDTO scanComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final String source,
      final String stageId,
      final String sbom)
  {
    validateRequest(sbom);
    
    // This will be replace with the real status Id, after the scan.xml is created and sent
    String scanRequestId = UUID.randomUUID().toString().replace("-", "");
    ApiThirdPartyScanTicketDTO evaluationTicketDTO = createScanTicket(applicationId, scanRequestId);
    return evaluationTicketDTO;
  }

  private void validateRequest(final String sbom) {
    if (StringUtils.isBlank(sbom)) {
      throw new BadRequestException("sbom is null or empty");
    }

    List<SAXParseException> validationErrors = schemaValidator.validate(sbom);
    if (!validationErrors.isEmpty()) {
      List<String> errorMessages =
          validationErrors.stream().map(SAXParseException::getMessage).collect(Collectors.toList());
      throw new WebApplicationException(
          Response.status(Status.BAD_REQUEST).entity(new GenericEntity<List<String>>(errorMessages) {}).build());
    }
  }

  private ApiThirdPartyScanTicketDTO createScanTicket(final String applicationId, final String scanRequestId) {
    ApiThirdPartyScanTicketDTO scanTicketDTO = new ApiThirdPartyScanTicketDTO();
    scanTicketDTO.statusUrl = PublicApiPaths.THIRD_PARTY_SCAN_PATH + "/" + applicationId + "/status/" + scanRequestId;
    return scanTicketDTO;
  }
}
