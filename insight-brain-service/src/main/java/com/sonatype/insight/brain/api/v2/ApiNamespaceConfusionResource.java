/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.integration.repository.RepositoryService;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
@Timed
@Path(ApiNamespaceConfusionResource.NAMESPACE_CONFUSION_PATH)
@Tag(name = ApiFirewallResource.SWAGGER_UI_API_LABEL)
public class ApiNamespaceConfusionResource
{
  public static final String NAMESPACE_CONFUSION_ROOT = "namespace_confusion";

  public static final String NAMESPACE_CONFUSION_PATH = PublicApiPaths.FIREWALL_RESOURCE_PATH + "/"
      + NAMESPACE_CONFUSION_ROOT + "/{format}";

  protected static final String NAMESPACE_CONFUSION_PREFIX = "nsc_";

  private final RepositoryService repositoryService;

  @Inject
  public ApiNamespaceConfusionResource(final RepositoryService repositoryService) {
    this.repositoryService = checkNotNull(repositoryService);
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.ADD_PROPRIETARY_COMPONENT_NAMES)
  @Operation(
      description = "Adds a list of proprietary component namespaces for the specified format "
          + "to prevent namespace confusion attacks." +
          "\n" +
          "\n" +
          "Permissions required: Evaluate Individual Components",
      responses = {
          @ApiResponse(responseCode = "204", description = "Namespaces successfully added."),
      }
  )
  public void addProprietaryComponentNames(
      @Parameter(
          description = "Format for which the proprietary namespaces are being added.",
          required = true,
          example = "maven"
      )
      @PathParam("format") String format,
      @Parameter(
          description = "List of namespaces to register as proprietary for this format.",
          required = true
      )
      List<String> namespaces)
  {
    repositoryService.addProprietaryNamespaceNames(NAMESPACE_CONFUSION_ROOT,
        NAMESPACE_CONFUSION_PREFIX + format, format, namespaces);
  }

  @DELETE
  @Audited(AuditEvent.REMOVE_PROPRIETARY_COMPONENT_NAMES)
  @Operation(
      description = "Removes proprietary component namespaces for the specified format." +
          "\n" +
          "\n" +
          "Permissions required: Evaluate Individual Components",
      responses = {
          @ApiResponse(responseCode = "204", description = "Namespaces successfully removed."),
      }
  )
  public void removeProprietaryComponentNames(
      @Parameter(
          description = "Format for which the proprietary namespaces are being removed.",
          required = true,
          example = "maven"
      )
      @PathParam("format") String format)
  {
    repositoryService.removeProprietaryNamespaceNames(NAMESPACE_CONFUSION_ROOT,
        NAMESPACE_CONFUSION_PREFIX + format);
  }
}
