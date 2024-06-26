/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * Accepts uploads of application binaries for the purpose of scanning them.
 *
 * @since 1.8
 */
@Named
@Timed
@Path(ScanResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_EVALUATION)
public class ScanResource
{
  public static final String RESOURCE_PATH = "rest/scan/{applicationPublicId}";

  private final ScanService scanService;

  private final ErrorResponseGenerator errorResponseGenerator;

  private final AntiCsrfFilter antiCsrfFilter;

  @Inject
  public ScanResource(ScanService scanService,
                      ErrorResponseGenerator errorResponseGenerator,
                      AntiCsrfFilter antiCsrfFilter)
  {
    this.scanService = scanService;
    this.errorResponseGenerator = errorResponseGenerator;
    this.antiCsrfFilter = antiCsrfFilter;
  }

  @POST
  @Produces({MediaType.APPLICATION_JSON, ErrorResponse.CONTENT_TYPE})
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Audited(AuditEvent.EVALUATE_APPLICATION)
  public Response uploadBinary(@PathParam("applicationPublicId") String appPublicId,
                               @FormDataParam("file") InputStream is,
                               @FormDataParam("file") FormDataContentDisposition fileDetail,
                               @FormDataParam("filename") String filename,
                               @FormDataParam(AntiCsrfFilter.CSRF_HEADER_NAME) String csrfToken,
                               @Context HttpHeaders headers,
                               @QueryParam("stageId") String stageId,
                               @QueryParam("sendNotifications") boolean sendNotifications,
                               @QueryParam("noFormData") boolean noFormData,
                               @Context HttpServletRequest request) throws Exception
  {
    try {
      antiCsrfFilter.validate(csrfToken, headers);
      // Browsers submit the Content-Disposition header with an UTF-8 encoded filename, Jersey however decodes that
      // header using Latin-1, messing up non-ASCII filenames.
      // We therefore transmit the filename in the body part of a separate form parameter for modern browsers ...
      if (filename == null) {
        // ... and fallback to the broken header in case of IE9
        filename = fileDetail.getFileName();
      }
      ScanTicket result = scanService.scanBinary(appPublicId, is, filename, new Stage(stageId), sendNotifications,
          HdsClient.getClientUserAgent(request), "ui");
      if (noFormData) {
        return Response.ok(JsonUtils.generate(result), ErrorResponse.CONTENT_TYPE).build();
      }
      else {
        return Response.ok(result, MediaType.APPLICATION_JSON).build();
      }
    }
    catch (Exception e) {
      if (noFormData) {
        AuditData.get().setException(e);
        String msg = errorResponseGenerator.mapExceptionAndLog(e).getMessageBody();
        return Response.ok(msg, ErrorResponse.CONTENT_TYPE).build();
      }
      throw e;
    }
  }

  @Path("/{ticketId}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ScanTicket getTicket(@PathParam("applicationPublicId") String appPublicId,
                              @PathParam("ticketId") String ticketId) throws Exception
  {
    return scanService.getTicket(appPublicId, ticketId);
  }
}
