/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.service.BaseUrlProvider;
import com.sonatype.insight.brain.service.CveAffectedComponentSearchService;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.CsvMediaType.TEXT_CSV;
import static com.sonatype.insight.brain.utils.HttpHeaderUtils.buildContentDispositionHeaderValue;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;

/**
 * REST API for searching applications by components.
 */
@Named
@Timed
@Path(ApiComponentSearchResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Component Search", description = "Search applications containing specific components")
public class ApiComponentSearchResource
{
  private static final Logger log = LoggerFactory.getLogger(ApiComponentSearchResource.class);

  public static final String RESOURCE_PATH = "api/v2/componentSearch";

  private static final String REACT2SHELL_CVE_ID = "CVE-2025-55182";

  private static final String REPORT_FILE_PREFIX = "react2shell-vulnerability-report";

  private static final String CSV_HEADER =
      "Application Name,Application ID,Component Name,Component Version," +
          "Vulnerability ID,Recommended Action,Recommended Version,Last Evaluation," +
          "Active Waiver,Implicated Files,Evaluation";

  private static final int STREAM_BUFFER_SIZE = 65536; // 64KB

  private static final String TIMESTAMP_FORMAT = "yyyyMMdd-HHmmss";

  private static final int CACHE_MAX_AGE_SECONDS = 300; // 5 minutes

  private final CveAffectedComponentSearchService cveAffectedComponentSearchService;

  private final BaseUrlProvider baseUrlProvider;

  @Inject
  public ApiComponentSearchResource(
      final CveAffectedComponentSearchService cveAffectedComponentSearchService,
      final BaseUrlProvider baseUrlProvider)
  {
    this.cveAffectedComponentSearchService = cveAffectedComponentSearchService;
    this.baseUrlProvider = baseUrlProvider;
  }

  @GET
  @Path("/downloadComponentSearchReport")
  @Produces(TEXT_CSV)
  @Operation(
      description = "Export component search results as CSV (streaming). " +
          "Identifies applications containing components affected by CVE-2025-55182. " +
          "Results are streamed to avoid memory issues with large datasets. " +
          "<p>" +
          "Permissions Required: View IQ Elements"
  )
  @ApiResponse(responseCode = "200", description = "CSV file containing the component search results")
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public Response exportComponentSearchReport() {
    String filename = generateCsvFilename(REPORT_FILE_PREFIX);

    return Response.ok()
        .type(TEXT_CSV)
        .header(CONTENT_DISPOSITION, buildContentDispositionHeaderValue(filename))
        .header(CACHE_CONTROL, "public, max-age=" + CACHE_MAX_AGE_SECONDS)
        .entity(createStreamingOutput())
        .build();
  }

  private String generateCsvFilename(String prefix) {
    String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
    return prefix + "-" + timestamp + ".csv";
  }

  private StreamingOutput createStreamingOutput() {
    return output -> {
      try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, UTF_8), STREAM_BUFFER_SIZE)) {

        writer.write(CSV_HEADER);
        writer.write("\r\n");

        String baseUrl = baseUrlProvider.getBaseUrl();

        cveAffectedComponentSearchService
            .find(REACT2SHELL_CVE_ID, baseUrl)
            .forEach(applicationComponentMatchDTO -> {
              try {
                writer.write(applicationComponentMatchDTO.toCsvLine());
                writer.write("\r\n");
              }
              catch (IOException e) {
                log.error("Error while writing CSV line", e);
              }
            });

        writer.flush();
      }
      catch (Exception e) {
        log.error("Error streaming component search results", e);
        throw new WebApplicationException("Failed to generate CSV report: " + e.getMessage(), e);
      }
    };
  }
}
