/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public API for exporting policy configurations.
 * <p>
 * <b>Authorization model:</b> Hierarchical permissions are used where parent permissions grant child access.
 * Direct export requires READ permission on the requested owner. Inherited export also requires READ permission
 * only on the requested owner; parent entities in the hierarchy are included automatically based on IQ Server's
 * hierarchical permission model.
 * <p>
 * <b>Important:</b> Permission on an organization automatically grants access to all applications under that
 * organization (application hierarchy includes the parent organization). However, repositories have a separate
 * hierarchy through RepositoryManager and RepositoryContainer, so organization permission does NOT automatically
 * grant repository access unless the repository's hierarchy includes that organization.
 * <p>
 * <b>Hierarchy structure:</b>
 * <ul>
 * <li><b>Application hierarchy:</b> Application → parent Organization(s) → Global
 * <br>
 * <i>Permission inheritance:</i> Org permission grants app access ✓</li>
 * <li><b>Repository hierarchy:</b> Repository → RepositoryManager → RepositoryContainer → ROOT Organization → Global
 * <br>
 * <i>Permission inheritance:</i> Org permission grants repo access only if repo's hierarchy includes that org
 * <br>
 * <i>Policy export:</i> All levels in the hierarchy are included in inherited export</li>
 * <li><b>Organization hierarchy:</b> Organization → parent Organization(s) → Global</li>
 * </ul>
 */
@Named
@Timed
@Path(PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_MANAGEMENT)
@io.swagger.v3.oas.annotations.tags.Tag(name = "Policy Export",
    description = "Export policy configurations for organizations, applications, and repositories")
public class ApiPolicyExportResourceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiPolicyExportResourceV2.class);

  private final PolicyImportExport policyImportExport;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final RepositoryDAO repositoryDAO;

  @Inject
  public ApiPolicyExportResourceV2(
      final PolicyImportExport policyImportExport,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final RepositoryDAO repositoryDAO)
  {
    this.policyImportExport = policyImportExport;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.repositoryDAO = repositoryDAO;
  }

  @GET
  @Path("export")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Export policy configuration",
      description = """
          Exports policy configurations including policy rules, conditions, labels, license threat groups,
          and tags for the specified owner (organization, application, or repository).
          Use the `includeInherited` parameter to include policies from parent levels in the hierarchy.

          **Permissions required**: View IQ Elements (READ permission on the specified owner)

          **Export scope**:
          - Policy rules and conditions
          - Policy assignments (via ownerId on each policy)
          - Component labels
          - License threat groups
          - Application categories (tags) for organizations

          **Note**: Waivers are not included in the export.""")
  @ApiResponse(
      responseCode = "200",
      description = "Policy configuration exported successfully. The response includes policies, labels, " +
          "license threat groups, and (for organizations) tags. When includeInherited=true, policies from " +
          "parent levels are included with their original ownerId preserved.",
      useReturnTypeSchema = true)
  @ApiResponse(
      responseCode = "403",
      description = "User does not have READ permission on the specified owner")
  @ApiResponse(
      responseCode = "404",
      description = "Organization, application, or repository not found")
  public PolicyExportResult exportPolicies(
      @Parameter(description = "Type of owner (organization, application, or repository)",
          required = true) @PathParam("ownerType") @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @Parameter(description = "Internal ID of the owner",
          required = true) @PathParam("ownerId") @AuthzContext(AuthzContext.Key.INTERNAL_ID) final String ownerId,
      @Parameter(description = "If true, include policies from parent levels in the hierarchy. " +
          "For repositories, includes policies from the repository and parent organization " +
          "(RepositoryManager and RepositoryContainer levels are skipped as they do not define policies). " +
          "For applications, includes policies from the application and parent organization(s). " +
          "For organizations, includes policies from the organization and any parent organizations. " +
          "Default: false (direct policies only)") @QueryParam("includeInherited") @DefaultValue("false") final boolean includeInherited)
  {
    log.debug("Exporting policies for {}/{} (includeInherited={})", ownerType, ownerId, includeInherited);

    if (includeInherited) {
      return policyImportExport.exportWithInheritance(ownerType, ownerId);
    }
    else {
      return exportDirect(ownerType, ownerId);
    }
  }

  /**
   * Export policies defined directly at the specified owner level only.
   */
  private PolicyExportResult exportDirect(final OwnerType ownerType, final String internalOwnerId) {
    switch (ownerType) {
      case ORGANIZATION:
        final Organization organization = organizationDAO.getByIdNotNull(internalOwnerId);
        return policyImportExport.exportOrganization(organization);

      case APPLICATION:
        final Application application = applicationDAO.getByIdNotNull(internalOwnerId);
        return policyImportExport.exportApplication(application);

      case REPOSITORY:
        final Repository repository = repositoryDAO.getByIdNotNull(internalOwnerId);
        return policyImportExport.exportRepository(repository);

      default:
        // Unreachable: JAX-RS path regex restricts ownerType to organization|application|repository
        throw new IllegalStateException(
            "Unexpected owner type: " + ownerType + " - path validation should prevent this");
    }
  }

}
