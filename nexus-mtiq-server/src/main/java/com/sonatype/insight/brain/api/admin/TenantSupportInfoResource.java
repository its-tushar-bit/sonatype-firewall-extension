/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.service.TenantSupportInfoService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.support.SupportInfo;
import com.sonatype.insight.brain.support.SupportZipInProgressException;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_SUPPORT_INFO_PATH)
public class TenantSupportInfoResource
{
  private static final Logger log = LoggerFactory.getLogger(TenantSupportInfoResource.class);

  private final TenantSupportInfoService tenantSupportInfoService;

  @Inject
  public TenantSupportInfoResource(TenantSupportInfoService tenantSupportInfoService) {
    this.tenantSupportInfoService = tenantSupportInfoService;
  }

  @GET
  @Produces("application/zip")
  @Audited(AuditEvent.GENERATE_TENANT_SUPPORT_INFO)
  public Response getSupportZip(
      @PathParam("tenantSlug") String tenantSlug) throws IOException
  {
    final SupportInfo supportInfo;
    try {
      supportInfo = tenantSupportInfoService.getSupportInfo(tenantSlug);
    }
    catch (SupportZipInProgressException e) {
      // Another request is already generating a bundle for this tenant. 429 is more accurate than
      // a 5xx and matches the on-prem SupportResource behavior.
      return Response.status(Status.TOO_MANY_REQUESTS).entity(e.getMessage()).build();
    }

    final File zipFile = supportInfo.getSupportInfoFile();
    boolean ownershipTransferredToStream = false;
    try {
      final StreamingOutput streamingOutput = output -> {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(zipFile.toPath()))) {
          IOUtils.copy(in, output);
        }
        finally {
          deleteQuietly(zipFile);
        }
      };

      final ResponseBuilder response = Response.ok();
      response.entity(streamingOutput);
      response.header(HttpHeaders.CONTENT_DISPOSITION,
          HttpHeaderUtils.buildContentDispositionHeaderValue(supportInfo.getSupportInfoName()));

      Response built = response.build();
      ownershipTransferredToStream = true;
      return built;
    }
    finally {
      // If we never got as far as returning the built Response, Jersey will not invoke the
      // StreamingOutput and its in-lambda delete never runs. Delete here so we don't leak the
      // zip on this exit path. (Bundles that Jersey silently drops after Response.build() are
      // caught by the stale-file sweep on the next generation — see SupportInfoUtil.)
      if (!ownershipTransferredToStream) {
        deleteQuietly(zipFile);
      }
    }
  }

  private static void deleteQuietly(File file) {
    if (file == null) {
      return;
    }
    try {
      Files.deleteIfExists(file.toPath());
    }
    catch (IOException e) {
      log.warn("Failed to delete support bundle file: {}", file.getAbsolutePath(), e);
    }
  }
}
