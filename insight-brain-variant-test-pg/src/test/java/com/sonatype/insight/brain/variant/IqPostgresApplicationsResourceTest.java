/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomApplicationListSummaryDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomApplicationSummaryDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomApplicationsSortableField;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.applications.ApplicationsResource;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
public class IqPostgresApplicationsResourceTest
{
  private IqTestContext ctx;

  private Application app;

  private Organization org;

  private Policy policy;

  @BeforeEach
  public void before() throws Exception {
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplicationWithParent(org);
    policy = ctx.tempEntity().newPolicy(org);
    ctx.setFeatures(LicensedFeature.SBOM_MANAGER);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(ApplicationsResource.RESOURCE_BASE_PATH);
  }

  @Test
  public void testGetApplicationDetails_InvalidSortBy_ReturnsBadRequest() throws Exception {
    HttpResponse response = restRequest()
        .path(ApplicationsResource.SBOMS_APPLICATIONS_PATH)
        .query("sortBy", "invalidField")
        .get();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  public void testGetApplicationDetails() throws Exception {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(ctx.daoFactory())
        .withApplicationId(app.getId())
        .withCreatedAt(new Date())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = ctx.tempEntity()
        .newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
            "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(),
            packageUrlIdentifier1.getVersion(),
            "h1", packageUrlIdentifier1.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity1 = ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(coordinate1,
            "cve-2", sbomMetadata.getId(), "description2", "link2", CvssV3Severity.CRITICAL.getEndScoreRange(),
            CvssV3Severity.CRITICAL.getDisplayName(), "fix1");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity1);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = ctx.tempEntity()
        .newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
            "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(),
            packageUrlIdentifier2.getVersion(),
            "h2", packageUrlIdentifier2.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity2 = ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(coordinate2,
            "cve-2", sbomMetadata.getId(), "description2", "link2", CvssV3Severity.NONE.getStartScoreRange(),
            CvssV3Severity.NONE.getDisplayName(), "fix2");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(coordinate2, "cve-3", sbomMetadata.getId(), "description3", "link3",
            CvssV3Severity.LOW.getStartScoreRange() + 0.2f, CvssV3Severity.LOW.getDisplayName(), "fix3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(coordinate2, "cve-4", sbomMetadata.getId(), "description4", "link4",
            CvssV3Severity.MEDIUM.getStartScoreRange() + 1f, CvssV3Severity.MEDIUM.getDisplayName(), "fix4");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(coordinate2, "cve-5", sbomMetadata.getId(), "description5", "link5",
            CvssV3Severity.HIGH.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix5");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(coordinate2, "cve-6", sbomMetadata.getId(), "description6", "link6",
            CvssV3Severity.HIGH.getEndScoreRange() - 0.1f, CvssV3Severity.HIGH.getDisplayName(), "fix6");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(coordinate2, "cve-7", sbomMetadata.getId(), "description7", "link7",
            CvssV3Severity.CRITICAL.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix7");

    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity2);

    PolicyEvaluation policyEvaluation = ctx.tempEntity()
        .newPolicyEvaluation(app.getId(),
            ComplianceStageType.ID, "scanId1App1");
    ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, "g1",
            "a1", "v1", "h1", "r1");

    HttpResponse response = restRequest()
        .path(ApplicationsResource.SBOMS_APPLICATIONS_PATH)
        .query("applicationName")
        .query("sortBy", SbomApplicationsSortableField.IMPORT_DATE)
        .query("asc", false)
        .query("page", 1)
        .query("pageSize", 3)
        .get();
    ctx.assertResponseStatus(200, response);
    SbomApplicationListSummaryDTO resultDtoList = response.getBody(SbomApplicationListSummaryDTO.class);

    assertThat(resultDtoList.getApplications()).hasSize(1);
    SbomApplicationSummaryDTO applicationPageApplicationSummaryDTO = resultDtoList.getApplications().get(0);
    assertThat(applicationPageApplicationSummaryDTO.getReleaseStatusPercentage()).isEqualTo(25.0);
    assertThat(applicationPageApplicationSummaryDTO.getVulnerabilitySummary().getNone())
        .isEqualTo(1);
    assertThat(applicationPageApplicationSummaryDTO.getVulnerabilitySummary().getLow())
        .isEqualTo(1);
    assertThat(applicationPageApplicationSummaryDTO.getVulnerabilitySummary().getMedium())
        .isEqualTo(1);
    assertThat(applicationPageApplicationSummaryDTO.getVulnerabilitySummary().getHigh())
        .isEqualTo(2);
    assertThat(applicationPageApplicationSummaryDTO.getVulnerabilitySummary().getCritical())
        .isEqualTo(2);
    assertThat(applicationPageApplicationSummaryDTO.getPolicyViolationSummary()
        .getCritical()).isEqualTo(0);
    assertThat(applicationPageApplicationSummaryDTO.getPolicyViolationSummary()
        .getSevere()).isEqualTo(1);
    assertThat(applicationPageApplicationSummaryDTO.getPolicyViolationSummary()
        .getModerate()).isEqualTo(0);
    assertThat(applicationPageApplicationSummaryDTO.getPolicyViolationSummary()
        .getLow()).isEqualTo(0);
  }

  private void insertVEXToThirdPartyCoordinateSecurity(ThirdPartyCoordinateSecurity coordinateSecurity) {
    ctx.tempEntity()
        .newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity, coordinateSecurity.getRefId(),
            "state", "justification", "response", "detail");
  }
}
