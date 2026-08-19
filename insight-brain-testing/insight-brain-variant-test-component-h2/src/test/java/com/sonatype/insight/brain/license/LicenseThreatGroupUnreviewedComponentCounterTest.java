/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupCount;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class LicenseThreatGroupUnreviewedComponentCounterTest
    extends AbstractComponentH2Test
{
  @Inject
  private LicenseThreatGroupUnreviewedComponentCounter counter;

  @Inject
  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  @Mock
  private ApiLicenseLegalHdsService mockApiLicenseLegalHdsService;

  private List<LicenseThreatGroupCount> countByOwner(final OwnerType ownerType, final String ownerId) {
    try (TransactionContext tx = licenseThreatGroupDAO.createTransactionContext()) {
      return counter.countByOwner(tx, ownerType, ownerId);
    }
  }

  private List<LicenseThreatGroupCount> countByApplicationIds(final Collection<String> applicationIds) {
    try (TransactionContext tx = licenseThreatGroupDAO.createTransactionContext()) {
      return counter.countByApplicationIds(tx, applicationIds);
    }
  }

  @Test
  public void testCountByOwner_countsOnlyUnreviewedComponents() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    tempEntity.newLicenseThreatGroup(organization.getId(), "CLM-39702-Unreviewed-Banned", 10, "GPL-2.0");

    mockGplObligations("obligation-a", "obligation-b");

    seedComponent(application, "h-unreviewed-1", "GPL-2.0");
    seedComponent(application, "h-unreviewed-2", "GPL-2.0");

    ComponentIdentifier reviewedIdentifier =
        ComponentIdentifier.createMavenCoordinates("g", "a-h-reviewed", "1.0.0");
    seedComponent(application, "h-reviewed", "GPL-2.0");
    tempEntity.newComponentObligation(reviewedIdentifier, ROOT_ORGANIZATION_ID, "obligation-a", "comment",
        ObligationStatus.FULFILLED, "hash-reviewed");
    tempEntity.newComponentObligation(reviewedIdentifier, ROOT_ORGANIZATION_ID, "obligation-b", "comment",
        ObligationStatus.FULFILLED, "hash-reviewed");

    List<LicenseThreatGroupCount> counts =
        countByOwner(OwnerType.ORGANIZATION, organization.getId());

    LicenseThreatGroupCount banned = firstByName(counts, "CLM-39702-Unreviewed-Banned");
    assertThat(banned.getUnreviewedComponentCount()).isEqualTo(2L);
  }

  @Test
  public void testCountByOwner_includesVisibleLtgWithZeroUnreviewedComponents() {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newLicenseThreatGroup(organization.getId(), "CLM-39702-Unreviewed-Banned", 10, "GPL-2.0");

    List<LicenseThreatGroupCount> counts =
        countByOwner(OwnerType.ORGANIZATION, organization.getId());

    LicenseThreatGroupCount banned = firstByName(counts, "CLM-39702-Unreviewed-Banned");
    assertThat(banned.getUnreviewedComponentCount()).isZero();
  }

  @Test
  public void testCountByOwner_sortOrderByThreatLevelThenCountThenName() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    tempEntity.newLicenseThreatGroup(organization.getId(), "CLM-39702-Sort-High", 9, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(organization.getId(), "CLM-39702-Sort-Mid", 5, "MIT");
    tempEntity.newLicenseThreatGroup(organization.getId(), "CLM-39702-Sort-LowB", 3, "BSD-2-Clause");
    tempEntity.newLicenseThreatGroup(organization.getId(), "CLM-39702-Sort-LowA", 3, "BSD-3-Clause");

    mockLicenseObligationsForLicenses(
        licenseMetadata("GPL-2.0", "obligation-gpl"),
        licenseMetadata("MIT", "obligation-mit"),
        licenseMetadata("BSD-2-Clause", "obligation-bsd2"),
        licenseMetadata("BSD-3-Clause", "obligation-bsd3"));

    seedComponent(application, "hash-mid-1", "MIT");
    seedComponent(application, "hash-mid-2", "MIT");
    seedComponent(application, "hash-lowb", "BSD-2-Clause");

    List<LicenseThreatGroupCount> counts =
        countByOwner(OwnerType.ORGANIZATION, organization.getId());

    List<String> ourNames = counts.stream()
        .map(LicenseThreatGroupCount::getLicenseThreatGroupName)
        .filter(n -> n.startsWith("CLM-39702-Sort-"))
        .toList();
    assertThat(ourNames).containsExactly("CLM-39702-Sort-High", "CLM-39702-Sort-Mid", "CLM-39702-Sort-LowB",
        "CLM-39702-Sort-LowA");
    assertThat(firstByName(counts, "CLM-39702-Sort-Mid").getUnreviewedComponentCount()).isEqualTo(2L);
    assertThat(firstByName(counts, "CLM-39702-Sort-LowB").getUnreviewedComponentCount()).isEqualTo(1L);
    assertThat(firstByName(counts, "CLM-39702-Sort-LowA").getUnreviewedComponentCount()).isZero();
  }

  @Test
  public void testCountByApplicationIds_returnsOnlyNonZeroLtgs() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    tempEntity.newLicenseThreatGroup(organization.getId(), "CLM-39702-Unreviewed-Banned", 10, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(organization.getId(), "CLM-39702-AppScope-Empty", 1, "MIT");

    mockLicenseObligationsForLicenses(
        licenseMetadata("GPL-2.0", "obligation-a"),
        licenseMetadata("MIT", "obligation-mit"));

    seedComponent(application, "h-one", "GPL-2.0");

    List<LicenseThreatGroupCount> counts =
        countByApplicationIds(Set.of(application.getId()));

    assertThat(counts.stream()
        .filter(c -> c.getLicenseThreatGroupName().startsWith("CLM-39702"))
        .toList()).hasSize(1);
    assertThat(firstByName(counts, "CLM-39702-Unreviewed-Banned").getUnreviewedComponentCount()).isEqualTo(1L);
    assertThat(counts.stream().map(LicenseThreatGroupCount::getLicenseThreatGroupName))
        .doesNotContain("CLM-39702-AppScope-Empty");
  }

  private void seedComponent(Application application, String hash, String licenseId) {
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g", "a-" + hash, "1.0.0");
    tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, hash, identifier);
    String acId = daoFactory.createOwnerComponentDAO()
        .getByOwnerIdAndStageTypeIdAndHash(application.getId(), BuildStageType.ID, hash)
        .getId();
    tempEntity.newApplicationComponentLicense(acId, licenseId);
  }

  private void mockGplObligations(String... obligationNames) {
    mockLicenseObligations("GPL-2.0", obligationNames);
  }

  private void mockLicenseObligations(String licenseId, String... obligationNames) {
    mockLicenseObligationsForLicenses(licenseMetadata(licenseId, obligationNames));
  }

  private LicenseMetadataDTO licenseMetadata(String licenseId, String... obligationNames) {
    Set<LicenseObligationDTO> obligationDtos = new LinkedHashSet<>();
    for (String obligationName : obligationNames) {
      LicenseObligationDTO dto = new LicenseObligationDTO();
      dto.setName(obligationName);
      dto.setObligationTexts(new LinkedHashSet<>(Collections.singletonList("text")));
      obligationDtos.add(dto);
    }
    LicenseMetadataDTO metadata = new LicenseMetadataDTO();
    metadata.setLicenseId(licenseId);
    metadata.setLicenseText("licenseText");
    metadata.setLicenseObligations(obligationDtos);
    return metadata;
  }

  private void mockLicenseObligationsForLicenses(LicenseMetadataDTO... metadataEntries) {
    Map<String, LicenseMetadataDTO> metadataByLicenseId = Arrays.stream(metadataEntries)
        .collect(Collectors.toMap(LicenseMetadataDTO::getLicenseId, Function.identity()));
    when(mockApiLicenseLegalHdsService.getLicenseMetadata(any())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      Collection<String> licenseIds = invocation.getArgument(0);
      return licenseIds.stream()
          .map(metadataByLicenseId::get)
          .filter(java.util.Objects::nonNull)
          .collect(Collectors.toList());
    });
  }

  private static LicenseThreatGroupCount firstByName(List<LicenseThreatGroupCount> counts, String name) {
    return counts.stream()
        .filter(c -> name.equals(c.getLicenseThreatGroupName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No LTG named " + name + " in result"));
  }
}
