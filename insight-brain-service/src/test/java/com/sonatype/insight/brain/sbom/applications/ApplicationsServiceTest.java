/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.applications;

import java.util.Date;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomApplicationListSummaryDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomApplicationSummaryDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomApplicationsSortableField;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApplicationsServiceTest
    extends AbstractComponentTest
{
  private Application app;

  private Organization org;

  private Policy policy;

  @Inject
  private SbomApplicationsService service;

  @Inject
  private ApplicationDAO appDao;

  @Before
  public void before() throws Exception {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplicationWithParent(org);
    policy = tempEntity.newPolicy(org);
  }

  @Test
  public void testGetApplications_InvalidPagination() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getApplications("app", SbomApplicationsSortableField.APPLICATION_NAME,
            true, 1, -2))
        .withMessage("pageSize must not be less than one!");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getApplications("app", SbomApplicationsSortableField.APPLICATION_NAME,
            true, -1, 2))
        .withMessage("page index must not be less than one!");
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetApplications_NoSbom() {
    tempEntity.newApplicationWithParent();

    SbomApplicationListSummaryDTO result = service.getApplications("DUMMY",
        null, true, 1, 1);

    assertThat(result).isNotNull();
    assertThat(result.getApplications()).isEmpty();
    assertThat(result.getTotalCount()).isZero();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetApplications_WithResults() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withCreatedAt(new Date())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
        "cve-2", sbomMetadata.getId(), "description2", "link2", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix1");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity1);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity2 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2,
        "cve-2", sbomMetadata.getId(), "description2", "link2", CvssV3Severity.NONE.getStartScoreRange(),
        CvssV3Severity.NONE.getDisplayName(), "fix2");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-3", sbomMetadata.getId(), "description3", "link3",
        CvssV3Severity.LOW.getStartScoreRange() + 0.2f, CvssV3Severity.LOW.getDisplayName(), "fix3");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-4", sbomMetadata.getId(), "description4", "link4",
        CvssV3Severity.MEDIUM.getStartScoreRange() + 1f, CvssV3Severity.MEDIUM.getDisplayName(), "fix4");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-5", sbomMetadata.getId(), "description5", "link5",
        CvssV3Severity.HIGH.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix5");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-6", sbomMetadata.getId(), "description6", "link6",
        CvssV3Severity.HIGH.getEndScoreRange() - 0.1f, CvssV3Severity.HIGH.getDisplayName(), "fix6");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-7", sbomMetadata.getId(), "description7", "link7",
        CvssV3Severity.CRITICAL.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix7");

    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity2);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(),
        ComplianceStageType.ID, "scanId1App1");
    tempEntity.newPolicyViolation(policyEvaluation, policy, "g1",
        "a1", "v1", "h1", "r1");

    SbomApplicationListSummaryDTO resultDtoList = service.getApplications(null,
        SbomApplicationsSortableField.IMPORT_DATE, false, 1, 3);

    assertThat(resultDtoList.getApplications()).hasSize(1);
    assertThat(resultDtoList.getTotalCount()).isEqualTo(1);
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

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetApplications_WithResults_ApplicationNameFilter() {
    Application application = tempEntity.newApplicationWithParent();
    application.setName("new-appName");
    appDao.update(application);
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    Date yesterday = DateUtils.addDays(new Date(), -1);
    ThirdPartySbomMetadata sbomMetadata1 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withCreatedAt(new Date())
        .build();
    ThirdPartySbomMetadata sbomMetadata2 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withCreatedAt(yesterday)
        .build();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    PackageUrlIdentifier packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    ThirdPartyFileCoordinate coordinate = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s", packageUrlIdentifier.getFormat(), packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion(),
        "h", packageUrlIdentifier.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity(coordinate,
        "cve-2", sbomMetadata.getId(), "description", "link", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata1.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
        "cve-2", sbomMetadata1.getId(), "description2", "link2", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix1");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity1);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata1.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity2 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2,
        "cve-2", sbomMetadata1.getId(), "description2", "link2", CvssV3Severity.NONE.getStartScoreRange(),
        CvssV3Severity.NONE.getDisplayName(), "fix2");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-3", sbomMetadata1.getId(), "description3", "link3",
        CvssV3Severity.LOW.getStartScoreRange() + 0.2f, CvssV3Severity.LOW.getDisplayName(), "fix3");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-4", sbomMetadata1.getId(), "description4", "link4",
        CvssV3Severity.MEDIUM.getStartScoreRange() + 1f, CvssV3Severity.MEDIUM.getDisplayName(), "fix4");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-5", sbomMetadata1.getId(), "description5", "link5",
        CvssV3Severity.HIGH.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix5");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-6", sbomMetadata1.getId(), "description6", "link6",
        CvssV3Severity.HIGH.getEndScoreRange() - 0.1f, CvssV3Severity.HIGH.getDisplayName(), "fix6");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-7", sbomMetadata1.getId(), "description7", "link7",
        CvssV3Severity.CRITICAL.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix7");

    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity2);

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("p3", "v3");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    ThirdPartyFileCoordinate coordinate3 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata2.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity3 = tempEntity.newThirdPartyCoordinateSecurity(coordinate3,
        "cve-3", sbomMetadata2.getId(), "description3", "link3", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix3");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity3);

    PolicyEvaluation
        policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ComplianceStageType.ID, "scanId1App1");
    tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    SbomApplicationListSummaryDTO resultDtoList = service.getApplications("appName",
        SbomApplicationsSortableField.IMPORT_DATE, false, 1, 5);

    assertThat(resultDtoList.getApplications()).hasSize(1);
    assertThat(resultDtoList.getTotalCount()).isEqualTo(1);
    SbomApplicationSummaryDTO applicationPageApplicationSummaryDTO1 = resultDtoList.getApplications().get(0);
    assertThat(applicationPageApplicationSummaryDTO1.getReleaseStatusPercentage()).isEqualTo(100);
    assertThat(applicationPageApplicationSummaryDTO1.getVulnerabilitySummary().getNone())
        .isEqualTo(0);
    assertThat(applicationPageApplicationSummaryDTO1.getVulnerabilitySummary().getLow())
        .isEqualTo(0);
    assertThat(applicationPageApplicationSummaryDTO1.getVulnerabilitySummary().getMedium())
        .isEqualTo(0);
    assertThat(applicationPageApplicationSummaryDTO1.getVulnerabilitySummary().getHigh())
        .isEqualTo(0);
    assertThat(applicationPageApplicationSummaryDTO1.getVulnerabilitySummary()
        .getCritical()).isEqualTo(1);
    assertThat(applicationPageApplicationSummaryDTO1.getPolicyViolationSummary()).isNull();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetApplications_WithResults_SortByReleaseStatusPercentage() {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    Application application1 = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata1 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application1.getId())
        .build();

    Date yesterday = DateUtils.addDays(new Date(), -1);
    ThirdPartySbomMetadata sbomMetadata2 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withCreatedAt(new Date())
        .build();
    ThirdPartySbomMetadata sbomMetadata3 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withCreatedAt(yesterday)
        .build();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    PackageUrlIdentifier packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    ThirdPartyFileCoordinate coordinate = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s", packageUrlIdentifier.getFormat(), packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion(),
        "h", packageUrlIdentifier.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity(coordinate,
        "cve-2", sbomMetadata.getId(), "description", "link", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata2.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
        "cve-2", sbomMetadata2.getId(), "description2", "link2", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix1");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity1);

    tempEntity.newThirdPartyCoordinateSecurity(coordinate,
        "cve-21", sbomMetadata1.getId(), "description", "link", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix");

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata2.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity2 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2,
        "cve-2", sbomMetadata2.getId(), "description2", "link2", CvssV3Severity.NONE.getStartScoreRange(),
        CvssV3Severity.NONE.getDisplayName(), "fix2");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-3", sbomMetadata2.getId(), "description3", "link3",
        CvssV3Severity.LOW.getStartScoreRange() + 0.2f, CvssV3Severity.LOW.getDisplayName(), "fix3");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-4", sbomMetadata2.getId(), "description4", "link4",
        CvssV3Severity.MEDIUM.getStartScoreRange() + 1f, CvssV3Severity.MEDIUM.getDisplayName(), "fix4");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-5", sbomMetadata2.getId(), "description5", "link5",
        CvssV3Severity.HIGH.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix5");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-6", sbomMetadata2.getId(), "description6", "link6",
        CvssV3Severity.HIGH.getEndScoreRange() - 0.1f, CvssV3Severity.HIGH.getDisplayName(), "fix6");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-7", sbomMetadata2.getId(), "description7", "link7",
        CvssV3Severity.CRITICAL.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix7");

    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity2);

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("p3", "v3");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    ThirdPartyFileCoordinate coordinate3 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata3.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity3 = tempEntity.newThirdPartyCoordinateSecurity(coordinate3,
        "cve-3", sbomMetadata3.getId(), "description3", "link3", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix3");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity3);

    PolicyEvaluation
        policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ComplianceStageType.ID, "scanId1App1");
    tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    SbomApplicationListSummaryDTO resultDtoList = service.getApplications(null,
        SbomApplicationsSortableField.RELEASE_STATUS_PERCENTAGE, false, 1, 5);

    assertThat(resultDtoList.getApplications()).hasSize(3);
    SbomApplicationSummaryDTO applicationPageApplicationSummaryDTO1 = resultDtoList.getApplications().get(0);
    assertThat(applicationPageApplicationSummaryDTO1.getApplicationInternalId()).isEqualTo(application.getId());
    assertThat(applicationPageApplicationSummaryDTO1.getReleaseStatusPercentage()).isEqualTo(100);

    SbomApplicationSummaryDTO applicationPageApplicationSummaryDTO2 = resultDtoList.getApplications().get(1);
    assertThat(applicationPageApplicationSummaryDTO2.getApplicationInternalId()).isEqualTo(app.getId());
    assertThat(applicationPageApplicationSummaryDTO2.getReleaseStatusPercentage()).isEqualTo(25.0);

    SbomApplicationSummaryDTO applicationPageApplicationSummaryDTO3 = resultDtoList.getApplications().get(2);
    assertThat(applicationPageApplicationSummaryDTO3.getApplicationInternalId()).isEqualTo(application1.getId());
    assertThat(applicationPageApplicationSummaryDTO3.getReleaseStatusPercentage()).isEqualTo(0.0);
  }

  private void insertVEXToThirdPartyCoordinateSecurity(ThirdPartyCoordinateSecurity coordinateSecurity) {
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity, coordinateSecurity.getRefId(),
        "state", "justification", "response", "detail");
  }
}
