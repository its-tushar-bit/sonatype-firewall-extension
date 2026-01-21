/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallQuarantinedComponentDto;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationResultList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryContainerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerListDTO;
import com.sonatype.insight.brain.api.v2.dto.PaginationResponseBuilder;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField.FirewallFilterableField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField;
import com.sonatype.insight.brain.integration.ApplicationSummaryService;
import com.sonatype.insight.brain.integration.Goal;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.error.exception.BadRequestException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.106.0
 */
@Named
@Path(PublicApiPaths.FIREWALL_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = ApiFirewallResource.SWAGGER_UI_API_LABEL,
    description = "Use this REST API for managing and monitoring firewall features, including metrics, " +
        "repository management, quarantine operations, and namespace confusion prevention.")
public class ApiFirewallResource
{
  static final String CONFIGURATION_PATH = "configuration";

  static final String RELEASE_QUARANTINE = "releaseQuarantine";

  static final String SUMMARY_PATH = "summary";

  static final String RELEASE_QUARANTINE_SUMMARY_PATH = RELEASE_QUARANTINE + "/" + SUMMARY_PATH;

  static final String RELEASE_QUARANTINE_CONFIGURATION_PATH = RELEASE_QUARANTINE + "/" + CONFIGURATION_PATH;

  static final String QUARANTINE_PATH = "quarantine";

  static final String QUARANTINE_SUMMARY_PATH = QUARANTINE_PATH + "/summary";

  static final String COMPONENTS_PATH = "/components";

  static final String UNQUARANTINE_PATH = COMPONENTS_PATH + "/autoReleasedFromQuarantine";

  static final String QUARANTINED_PATH = COMPONENTS_PATH + "/quarantined";

  static final String QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS =
      "quarantinedComponentView/configuration/anonymousAccess";

  static final String QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS_SET =
      QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS + "/{enabled: true|false}";

  static final String REPOSITORY_MANAGERS_PATH = "repositoryManagers";

  static final String SWAGGER_UI_API_LABEL = "Firewall";

  private static final String REPOSITORIES_PATH = "repositories";

  private static final String REPOSITORY_MANAGER_ID_PATH = "{repositoryManagerId}";

  private static final String REPOSITORY_ID_PATH = "{repositoryId}";

  static final String REPOSITORY_MANAGER_PATH = REPOSITORY_MANAGERS_PATH + "/" + REPOSITORY_MANAGER_ID_PATH;

  // Visible for testing
  static final String REPOSITORIES_CONFIGURATION_PATH =
      REPOSITORIES_PATH + "/" + CONFIGURATION_PATH + "/" + REPOSITORY_MANAGER_ID_PATH;

  static final String EVALUATE_COMPONENTS_PATH =
      COMPONENTS_PATH + "/" + REPOSITORY_MANAGER_ID_PATH + "/" + REPOSITORY_ID_PATH + "/evaluate";

  static final String REPOSITORY_CONTAINER_PATH = "repositoryContainer";

  static final String CONNECTION_VERIFY_PATH = "connection/verify";

  private final ApiFirewallService apiFirewallService;

  private final ApplicationSummaryService applicationSummaryService;

  @Inject
  public ApiFirewallResource(
      final ApiFirewallService apiFirewallService,
      final ApplicationSummaryService applicationSummaryService)
  {
    this.apiFirewallService = apiFirewallService;
    this.applicationSummaryService = applicationSummaryService;
  }

  @GET
  @Path(RELEASE_QUARANTINE_SUMMARY_PATH)
  @Operation(description = "Use this method to track how many components have been automatically released from " +
      "quarantine over different time periods." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains:" +
                  "<ul>" +
                  "<li>`autoReleaseQuarantineCountMTD` is the number of auto-released quarantine components " +
                  "from the start of the current month to the current date.</li>" +
                  "<li>`autoReleaseQuarantineCountYTD` is the number of auto-released quarantine components " +
                  "from the start of the current year to the current date.</li>" +
                  "</ul>",
              useReturnTypeSchema = true)
      })
  public ApiFirewallReleaseQuarantineSummaryDTO getFirewallUnquarantineSummary() {
    return apiFirewallService.getReleaseQuarantineSummary();
  }

  @GET
  @Operation(description = "Use this method to retrieve the configuration settings for auto-release from quarantine " +
      "for repositories." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains a list of repositories and the corresponding configuration for " +
                  "auto-release from quarantine.",
              useReturnTypeSchema = true)
      })
  @Path(RELEASE_QUARANTINE_CONFIGURATION_PATH)
  public List<ApiFirewallReleaseQuarantineConfigDTO> getFirewallAutoUnquarantineConfig() {
    return apiFirewallService.getReleaseQuarantineConfig();
  }

  @PUT
  @Path(RELEASE_QUARANTINE_CONFIGURATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING)
  @Operation(description = "Use this method to set the configurations for auto-release from quarantine for a " +
      "list of repositories." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains each updated `autoReleaseQuarantineEnabled` status for the " +
                  "repositories requested.",
              useReturnTypeSchema = true)
      })
  public List<ApiFirewallReleaseQuarantineConfigDTO> setFirewallAutoUnquarantineConfig(
      @RequestBody(description = "Enter value for each repository and the required status for auto-release as `true` " +
          "or `false`.", required = true)
      final List<ApiFirewallReleaseQuarantineConfigDTO> apiFirewallReleaseQuarantineConfigDTOS)
  {
    return apiFirewallService.setReleaseQuarantineConfig(apiFirewallReleaseQuarantineConfigDTOS);
  }

  @GET
  @Operation(description = "Use this method to request a summary of quarantined components." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response contains:" +
                  "<ul>" +
                  "<li>`repositoryCount` is the total number of repositories.</li>" +
                  "<li>`quarantineEnabledRepositoryCount` is the total number of repositories with quarantine  " +
                  "capability enabled.</li>" +
                  "<li>`quarantinedEnabled` indicates if any repository has the quarantine capability enabled.</li>" +
                  "<li>`totalComponentCount` is the total number of components across all repositories.</li>" +
                  "<li>`quarantinedComponentCount` is the total number of quarantined components.</li>" +
                  "</ul>",
              useReturnTypeSchema = true
          )
      })
  @Path(QUARANTINE_SUMMARY_PATH)
  public ApiFirewallQuarantineSummaryDTO getQuarantineSummary() {
    return apiFirewallService.getQuarantineSummary();
  }

  private class ApiFirewallComponentDTOResult
      extends ApiPageResult<ApiFirewallComponentDTO>
  {
  }

  @GET
  @Path(UNQUARANTINE_PATH)
  @Operation(description = "Use this method to retrieve the details of components that are auto-released " +
      "from quarantine." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response includes:" +
                  "<ul>" +
                  "<li>`total` is the total number of records this request can return across all pages.</li>" +
                  "<li>`page` is the page number specified in the request.</li>" +
                  "<li>`pageSize` is the page size specified in the request.</li>" +
                  "<li>`pageCount` is the total number of pages this request can return.</li>" +
                  "</ul>" +
                  "The `results` section contains details of each component that has been auto-released. It includes:" +
                  "<ul>" +
                  "<li>`displayName` is the name and version of the component.</li>" +
                  "<li>`repository` indicates the repository name where the component is stored.</li>" +
                  "<li>`quarantineDate` is the date and time when the component was quarantined.</li>" +
                  "<li>`dateCleared` is the date and time when the component was auto-released from quarantine.</li>" +
                  "<li>`quarantinePolicyViolations` will be empty for components that are auto-released.</li>" +
                  "<li>`componentIdentifier` is the format and coordinates for the component.</li>" +
                  "<li>`pathname` indicates the component path in the repository.</li>" +
                  "<li>`hash` is the hash of the component.</li>" +
                  "<li>`matchState` indicates the whether the component is an `EXACT` or `SIMILAR` match to the " +
                  "known  components or is `UNKNOWN`.</li>" +
                  "<li>`repositoryId` is the ID of the repository where the component is stored.</li>" +
                  "<li>`quarantined` indicates whether the component is quarantined.</li>" +
                  "</ul>",
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = ApiFirewallComponentDTOResult.class)
              ),
              headers = {
                  @Header(name = "Link",
                      description = "Pagination links (first, last, next, prev)",
                      schema = @Schema(type = "string"))
              }
          )
      })
  public Response getUnquarantineList(
      @Context UriInfo uriInfo,
      @Parameter(description = "Enter the page number.")
      @DefaultValue("1") @QueryParam("page") int page,
      @Parameter(description = "Enter the number of results to be returned for a page.")
      @DefaultValue("10") @QueryParam("pageSize") int pageSize,
      @Parameter(description = "Enter the `policyId`. When provided, the results will include the components that " +
          "have a policy violation for the policyId.")
      @QueryParam("policyId") String policyId,
      @Parameter(description = "Enter the component name. When provided, the results will include components with " +
          "display names (case-insensitive) that match the given name.")
      @QueryParam("componentName") String componentName,
      @Parameter(description = "Enter the sort criteria `releaseQuarantineTime` or `quarantineTime`.")
      @QueryParam("sortBy") String sortBy,
      @Parameter(description = "Select `true` to set the sort order to ascending.")
      @DefaultValue("true") @QueryParam("asc") boolean asc
  )
  {
    HashMap<FirewallFilterableField, Object> filterFieldsMap = new HashMap<>();
    filterFieldsMap.put(FirewallFilterableField.POLICY_ID, Optional.ofNullable(policyId).map(Set::of).orElse(null));
    filterFieldsMap.put(FirewallFilterableField.COMPONENT_NAME, componentName);

    List<FirewallFilterField> filterFields = buildFilterFieldsList(filterFieldsMap);

    FirewallSortableField sortableField =
        initializeSortField(sortBy, FirewallSortableField.RELEASE_QUARANTINE_TIME);

    return getComponents(uriInfo, page, pageSize, asc, filterFields, sortableField,
        FirewallComponentFilterState.UNQUARANTINE_AUTO);
  }

  private class ApiFirewallQuarantinedComponentDtoResult
      extends ApiPageResult<ApiFirewallQuarantinedComponentDto>
  {
  }

  @GET
  @Path(QUARANTINED_PATH)
  @Operation(description = "Use this method to request a list of quarantined components." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response includes:" +
                  "<ul>" +
                  "<li>`total` is the total number of records this request can return across all pages.</li>" +
                  "<li>`page` is the page number specified in the request.</li>" +
                  "<li>`pageSize` is the page size specified in the request.</li>" +
                  "<li>`pageCount` is the total number of pages this request can return.</li>" +
                  "</ul>" +
                  "The `results` section contains details of each component that has been auto-released. It includes:" +
                  "<ul>" +
                  "<li>`threatLevel` is the threat level of the policy violation.</li>" +
                  "<li>`policyName` is the name of the violated policy.</li>" +
                  "<li>`quarantined` indicates whether the component is quarantined.</li>" +
                  "<li>`quarantineDate` is the date and time when the component was quarantined.</li>" +
                  "<li>`componentIdentifier` is the format and coordinates for the component.</li>" +
                  "<li>`pathname` indicates the component path in the repository.</li>" +
                  "<li>`displayName` is the name and version of the component.</li>" +
                  "<li>`repositoryId` is the ID of the repository where the component is stored.</li>" +
                  "<li>`repositoryName` indicates the repository name where the component is stored.</li>" +
                  "<li>`hash` is the hash of the component.</li>" +
                  "<li>`matchState` indicates the whether the component is an `EXACT` or `SIMILAR` match to " +
                  "the known components or is `UNKNOWN`.</li>" +
                  "</ul>",
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = ApiFirewallQuarantinedComponentDtoResult.class)
              ),
              headers = {
                  @Header(name = "Link",
                      description = "Pagination links (first, last, next, prev)",
                      schema = @Schema(type = "string"))
              }
          )
      })
  public Response getQuarantineList(
      @Context UriInfo uriInfo,
      @Parameter(description = "Enter the starting page number for the response.")
      @DefaultValue("1") @QueryParam("page") int page,
      @Parameter(description = "Enter the page size for the response.")
      @DefaultValue("10") @QueryParam("pageSize") int pageSize,
      @Parameter(description = "Enter the list of policy IDs causing the quarantine.")
      @QueryParam("policyId") Set<String> policyIds,
      @Parameter(description = "Enter the component name.")
      @QueryParam("componentName") String componentName,
      @Parameter(description = "Enter the repository public ID of the quarantined component.")
      @QueryParam("repositoryPublicId") String repositoryPublicId,
      @Parameter(description = "Enter the quarantine time of the component.")
      @QueryParam("quarantineTime") Integer quarantineDays,
      @Parameter(description = "Enter `quarantineTime` to sort the results by quarantine time.")
      @QueryParam("sortBy") String sortBy,
      @Parameter(description = "Select the sort order.")
      @DefaultValue("false") @QueryParam("asc") boolean asc
  )
  {
    HashMap<FirewallFilterableField, Object> filterFieldsMap = new HashMap<>();
    filterFieldsMap.put(FirewallFilterableField.POLICY_ID, policyIds);
    filterFieldsMap.put(FirewallFilterableField.COMPONENT_NAME, componentName);
    filterFieldsMap.put(FirewallFilterableField.REPOSITORY_PUBLIC_ID, repositoryPublicId);
    filterFieldsMap.put(FirewallFilterableField.QUARANTINE_TIME, quarantineDays);

    List<FirewallFilterField> filterFields = buildFilterFieldsList(filterFieldsMap);

    final FirewallSortableField sortableField =
        initializeSortField(sortBy, FirewallSortableField.QUARANTINE_TIME);

    return getComponents(uriInfo, page, pageSize, asc, filterFields, sortableField,
        FirewallComponentFilterState.QUARANTINE);
  }

  /**
   * Enables/disables anonymous access to the Quarantined Component view
   *
   * @since 1.136
   */
  @PUT
  @Path(QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS_SET)
  @Audited(AuditEvent.CONFIGURE_SECURITY_QUARANTINED_COMPONENT_VIEW_ANON_ACCESS)
  @Operation(description = "Use this method to enable/disable anonymous access to view the quarantined components." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "Successfully updated anonymous access.")
      })
  public void setQuarantinedComponentViewAnonymousAccess(
      @Parameter(description = "Select `true` or `false` to enable or disable anonymous access.")
      @PathParam("enabled") boolean enabled)
  {
    apiFirewallService.setQuarantinedComponentViewAnonymousAccess(enabled);
  }

  /**
   * Returns whether Quarantine Component View can be accessed without authentication or not
   *
   * @since 1.136
   */
  @GET
  @Path(QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS)
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(description = "Use this method to determine if the quarantined component(s) details can be accessed " +
      "anonymously." +
      "\n" +
      "\n" +
      "Permissions required: None",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response returns `true` if anonymous access to quarantined components is enabled.",
              content = @Content(
                  mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "boolean")
              )
          )
      }
  )
  public Response getQuarantinedComponentViewAnonymousAccess() {
    return Response.ok(apiFirewallService.getQuarantinedComponentViewAnonymousAccess()).build();
  }

  @GET
  @Path(REPOSITORY_MANAGERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve all configured repository managers." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains details of configured repository managers.",
              useReturnTypeSchema = true)
      }
  )
  public ApiRepositoryManagerListDTO getRepositoryManagers() {
    return apiFirewallService.getRepositoryManagers();
  }

  @GET
  @Path(REPOSITORY_MANAGER_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve details of an existing repository manager." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response contains the details of the repository manager requested.",
              useReturnTypeSchema = true
          )
      })
  public ApiRepositoryManagerDTO getRepositoryManager(
      @Parameter(description = "Enter the repository manager ID.")
      @PathParam("repositoryManagerId") String repositoryManagerId)
  {
    return apiFirewallService.getRepositoryManager(repositoryManagerId);
  }

  @DELETE
  @Path(REPOSITORY_MANAGER_PATH)
  @Audited(AuditEvent.DELETE_REPOSITORY_MANAGER)
  @Operation(description = "Use this method to delete an existing repository manager." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "The requested repository manager has been deleted.")
      })
  public void deleteRepositoryManager(
      @Parameter(description = "Enter the repository manager ID.")
      @PathParam("repositoryManagerId") String repositoryManagerId)
  {
    apiFirewallService.deleteRepositoryManager(repositoryManagerId);
  }

  @POST
  @Path(REPOSITORY_MANAGERS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_REPOSITORY_MANAGER)
  @Operation(description = "Use this method to add a new repository manager." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the details of the new repository manager.",
              useReturnTypeSchema = true)
      }
  )
  public ApiRepositoryManagerDTO addRepositoryManager(
      @RequestBody(description = "Enter values for the new repository manager.",
          required = true, useParameterTypeSchema = true)
      ApiRepositoryManagerDTO apiRepositoryManagerDTO)
  {
    return apiFirewallService.addRepositoryManager(apiRepositoryManagerDTO);
  }

  @GET
  @Path(REPOSITORIES_CONFIGURATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the configuration details of an existing repository manager." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the configuration details of the requested repository manager.",
              useReturnTypeSchema = true)
      })
  public ApiRepositoryListDTO getConfiguredRepositories(
      @Parameter(description = "Enter the repository manager ID.")
      @PathParam("repositoryManagerId") String repositoryManagerId,
      @Parameter(description = "Enter the epoch time in milliseconds when the repository was last updated.")
      @QueryParam("sinceUtcTimestamp") Long sinceUtcTimestamp)
  {
    return apiFirewallService.getConfiguredRepositories(repositoryManagerId, sinceUtcTimestamp);
  }

  @POST
  @Path(REPOSITORIES_CONFIGURATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_REPOSITORY)
  @Operation(description = "Use this method to update the repositories for an existing repository manager." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "Repositories updated successfully."
          )
      })
  public void configureRepositories(
      @Parameter(description = "Enter the repository manager ID.")
      @PathParam("repositoryManagerId") String repositoryManagerId,
      @RequestBody(description = "Enter values for the repository configuration properties to be updated.",
          required = true, useParameterTypeSchema = true)
      ApiRepositoryListDTO dto)
  {
    apiFirewallService.configureRepositories(repositoryManagerId, dto);
  }

  @POST
  @Path(EVALUATE_COMPONENTS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_REPOSITORY)
  @Operation(description = "Use this method to evaluate components (max. 100)." +
      "\n" +
      "\n" +
      "Permissions required: Evaluate Individual Components",
      responses = @ApiResponse(
          responseCode = "200",
          description =
              "The response contains the evaluation results.",
          useReturnTypeSchema = true
      ))
  public ApiRepositoryComponentEvaluationResultList evaluateComponents(
      @Parameter(description = "Enter the repository manager ID.")
      @PathParam("repositoryManagerId") final String repositoryManagerId,
      @Parameter(description = "Enter the repository ID.")
      @PathParam("repositoryId") final String repositoryId,
      @RequestBody(description = "Provide the array of the component identifiers to be evaluated, using the " +
          "component hash and the (packageUrl or pathname). A maximum of 100 components can be evaluated " +
          "in one request.",
          required = true, useParameterTypeSchema = true)
      final ApiRepositoryComponentEvaluationRequestList apiRepositoryComponentEvaluationRequestList)
  {
    return apiFirewallService.evaluateComponents(repositoryManagerId, repositoryId,
        apiRepositoryComponentEvaluationRequestList);
  }

  @GET
  @Path(REPOSITORY_CONTAINER_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the ID and name for the repository container." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the ID and name for the repository container.",
              useReturnTypeSchema = true)
      })
  public ApiRepositoryContainerDTO getRepositoryContainer() {
    return apiFirewallService.getRepositoryContainer();
  }

  private Response getComponents(
      final UriInfo uriInfo,
      final int page,
      final int pageSize,
      final boolean isAscendingSort,
      final List<FirewallFilterField> filterFields,
      final FirewallSortableField sortableField,
      final FirewallComponentFilterState firewallComponentFilterState)
  {
    final FirewallRepositoryComponentFilter firewallFilter =
        new FirewallRepositoryComponentFilter(page, pageSize, firewallComponentFilterState, sortableField,
            isAscendingSort, filterFields);

    if (firewallComponentFilterState.equals(FirewallComponentFilterState.QUARANTINE)) {
      return new PaginationResponseBuilder<>(uriInfo.getAbsolutePath().getPath(), page, pageSize,
          apiFirewallService.getQuarantinedComponents(firewallFilter))
          .queryParameters(uriInfo.getQueryParameters())
          .build();
    }

    final ApiPageResult<ApiFirewallComponentDTO> result = apiFirewallService.getComponents(firewallFilter);

    return new PaginationResponseBuilder<>(uriInfo.getAbsolutePath().getPath(), page, pageSize, result)
        .queryParameters(uriInfo.getQueryParameters())
        .build();
  }

  private FirewallSortableField initializeSortField(
      final String sortBy,
      final FirewallSortableField defaultSortableField)
  {
    if (StringUtils.isEmpty(sortBy)) {
      return defaultSortableField;
    }

    final FirewallSortableField sortableField;
    try {
      sortableField = FirewallSortableField.getByLabel(sortBy);
    }
    catch (IllegalArgumentException exception) {
      throw new BadRequestException("sortBy field is invalid");
    }
    return sortableField;
  }

  private List<FirewallFilterField> buildFilterFieldsList(final Map<FirewallFilterableField, Object> filterFieldsMap) {
    return filterFieldsMap.entrySet().stream()
        .filter(this::filterFieldFn)
        .map(this::mapFieldFn)
        .toList();
  }

  private boolean filterFieldFn(Entry<FirewallFilterableField, Object> entry) {
    FirewallFilterableField key = entry.getKey();
    Object value = entry.getValue();

    return Optional.ofNullable(value)
        .filter(ObjectUtils::isNotEmpty)
        .filter(v -> !key.equals(FirewallFilterableField.QUARANTINE_TIME) || (Integer) v > 0)
        .isPresent();
  }

  private FirewallFilterField mapFieldFn(Entry<FirewallFilterableField, Object> entry) {
    FirewallFilterableField key = entry.getKey();
    Object value = entry.getValue();

    if (key.equals(FirewallFilterableField.COMPONENT_NAME) ||
        key.equals(FirewallFilterableField.REPOSITORY_PUBLIC_ID)) {
      String lowerCaseValue = ((String) value).toLowerCase();
      return new FirewallFilterField(key, lowerCaseValue);
    }

    if (key.equals(FirewallFilterableField.QUARANTINE_TIME)) {
      String quarantineTime = quarantineDaysToQuarantineTime((Integer) value);
      return new FirewallFilterField(key, quarantineTime);
    }

    return new FirewallFilterField(key, value);
  }

  private String quarantineDaysToQuarantineTime(Integer quarantineDays) {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
        LocalDateTime.ofInstant(Instant.now().minus(quarantineDays, ChronoUnit.DAYS), ZoneOffset.UTC));
  }

  @GET
  @Path(CONNECTION_VERIFY_PATH)
  @Operation(
      description = "Use this method to verify that the authenticated user has required permissions for " +
          "firewall operations and retrieve accessible applications." +
          "\n" +
          "\n" +
          "Permissions required: Evaluate Individual Components",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Connection verified successfully. Returns list of applications accessible to the user.",
              useReturnTypeSchema = true
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Authentication required. User is not authenticated."
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is authenticated but does not have 'Evaluate Individual Components' permission."
          )
      }
  )
  public ApplicationSummaryList verifyConnectionAndGetApplications() {
    apiFirewallService.checkEvaluateComponentPermission(RepositoryContainer.SINGLETON);
    // Get all applications (no organization or application ID filtering) accessible for component evaluation
    return applicationSummaryService.getApplications(Goal.EVALUATE_COMPONENT, null, Collections.emptySet());
  }
}
