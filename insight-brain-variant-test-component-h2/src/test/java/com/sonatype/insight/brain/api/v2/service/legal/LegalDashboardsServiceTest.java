/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseObligationReviewStatus;
import com.sonatype.insight.brain.model.OwnerComponentLicensesDTO;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class LegalDashboardsServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private LegalDashboardsService legalDashboardService;

  @Mock
  private ApiLicenseLegalHdsService mockApiLicenseLegalHdsService;

  @Test
  public void testObligationsIgnored() {
    assertObligationsList(0, 0, 2, ObligationStatus.IGNORED, ObligationStatus.IGNORED);
  }

  @Test
  public void testObligationsFullfilled() {
    assertObligationsList(0, 0, 2, ObligationStatus.FULFILLED, ObligationStatus.FULFILLED);
  }

  @Test
  public void testObligationsFlagged() {
    assertObligationsList(0, 2, 0, ObligationStatus.FLAGGED, ObligationStatus.FLAGGED);
  }

  @Test
  public void testObligationsOpen() {
    assertObligationsList(2, 0, 0, ObligationStatus.OPEN, ObligationStatus.OPEN);
  }

  @Test
  public void testObligationsMixedFO() {
    assertObligationsList(2, 2, 0, ObligationStatus.FLAGGED, ObligationStatus.FLAGGED, ObligationStatus.OPEN,
        ObligationStatus.OPEN);
  }

  @Test
  public void testObligationsMixedFIO() {
    assertObligationsList(1, 2, 1, ObligationStatus.FLAGGED, ObligationStatus.FLAGGED, ObligationStatus.IGNORED,
        ObligationStatus.OPEN);
  }

  @Test
  public void testObligationsMixedFIFO() {
    assertObligationsList(1, 1, 2, ObligationStatus.FLAGGED, ObligationStatus.FULFILLED, ObligationStatus.IGNORED,
        ObligationStatus.OPEN);
  }

  @Test
  public void testReviewStatusUnreviewed() {
    Set<String> allObligationNames = Collections.emptySet();
    Set<String> multiLicenseIds = Collections.emptySet();
    asserGetReviewStatus(0, 0, 0, allObligationNames, multiLicenseIds, LicenseObligationReviewStatus.UNREVIEWED);
  }

  @Test
  public void testReviewStatusUnreviewedWithObligation() {
    Set<String> allObligationNames = new HashSet<>(Arrays.asList("Ob1", "Ob2"));
    Set<String> multiLicenseIds = Collections.emptySet();

    asserGetReviewStatus(0, 2, 0, allObligationNames, multiLicenseIds, LicenseObligationReviewStatus.UNREVIEWED);
  }

  @Test
  public void testReviewStatusCompletedWithMultilicense() {
    Set<String> allObligationNames = Collections.emptySet();
    Set<String> multiLicenseIds = new HashSet<>(Collections.singletonList("MIT"));
    asserGetReviewStatus(0, 0, 0, allObligationNames, multiLicenseIds, LicenseObligationReviewStatus.COMPLETED);
  }

  @Test
  public void testReviewStatusCompletedAddressed() {
    Set<String> allObligationNames = new HashSet<>(Arrays.asList("Ob1", "Ob2"));
    Set<String> multiLicenseIds = Collections.emptySet();
    asserGetReviewStatus(0, 5, 0, allObligationNames, multiLicenseIds, LicenseObligationReviewStatus.IN_PROGRESS);
  }

  @Test
  public void testReviewStatusFlagged() {
    Set<String> allObligationNames = new HashSet<>(Arrays.asList("Ob1", "Ob2"));
    Set<String> multiLicenseIds = Collections.emptySet();
    asserGetReviewStatus(5, 0, 0, allObligationNames, multiLicenseIds, LicenseObligationReviewStatus.FLAGGED);
  }

  @Test
  public void testReviewStatuscompletedWithObligatios() {
    Set<String> allObligationNames = new HashSet<>(Arrays.asList("Ob1", "Ob2"));
    Set<String> multiLicenseIds = Collections.emptySet();
    asserGetReviewStatus(0, 0, 5, allObligationNames, multiLicenseIds, LicenseObligationReviewStatus.COMPLETED);
  }

  @Test
  public void testLicenseObligationsFromHDSWithEmptyList() {
    Set<String> multiLicenseIds = Collections.emptySet();
    assertThat(legalDashboardService.getLicenseObligationsFromHds(multiLicenseIds)).isEmpty();
  }

  @Test
  public void testLicenseObligationsFromHDSWithLicensesList() {
    List<String> licenses = Collections.singletonList("MIT");
    Set<String> multiLicenseIds = new HashSet<>(Collections.singletonList("MIT"));
    Set<String> obligations = new HashSet<>(Arrays.asList("obligation0", "obligation1"));
    setupLicenseObligationsWithMock(licenses, ObligationStatus.OPEN, ObligationStatus.FLAGGED);
    Map<String, Set<String>> result = legalDashboardService.getLicenseObligationsFromHds(multiLicenseIds);
    assertThat(result).contains(entry("MIT", obligations)).hasSize(1);
  }

  @Test
  public void testLicenseObligationsFromHDSWitMultiplehLicensesList() {
    List<String> licenses = Arrays.asList("MIT", "Apache 2.0");
    Set<String> multiLicenseIds = new HashSet<>(Arrays.asList("MIT", "Apache 2.0"));
    Set<String> obligations = new HashSet<>(Arrays.asList("obligation0", "obligation1", "obligation2", "obligation3"));
    setupLicenseObligationsWithMock(licenses, ObligationStatus.OPEN, ObligationStatus.FLAGGED, ObligationStatus.FLAGGED,
        ObligationStatus.FLAGGED);
    Map<String, Set<String>> result = legalDashboardService.getLicenseObligationsFromHds(multiLicenseIds);
    assertThat(result).contains(entry("MIT", obligations), entry("Apache 2.0", obligations)).hasSize(2);
  }

  @Test
  public void testgetLicenseIds() {
    List<OwnerComponentLicensesDTO> componentList = new ArrayList<>();
    OwnerComponentLicensesDTO comp1 =
        new OwnerComponentLicensesDTO("app", "hash", "format", "idJson", "Apache 1.1\nApache 1.0\nBSD");
    OwnerComponentLicensesDTO comp2 =
        new OwnerComponentLicensesDTO("app", "hash2", "format2", "idJson2", "MIT\nApache 2.0\nBSD");
    componentList.add(comp1);
    componentList.add(comp2);
    Set<String> result = legalDashboardService.getLicenseIds(componentList);
    assertThat(result)
        .containsExactlyInAnyOrderElementsOf(Arrays.asList("MIT", "Apache 2.0", "BSD", "Apache 1.0", "Apache 1.1"))
        .hasSize(5);
  }

  private void assertObligationsList(int open, int flagged, int addresed, ObligationStatus... obligationStatuses) {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    List<ComponentObligation> obligations = setupLicenseObligations(componentIdentifier1, obligationStatuses);
    Set<String> allObligationNames =
        obligations.stream().map(ComponentObligation::getObligationName).collect(Collectors.toSet());

    Map<String, Integer> mapCount = legalDashboardService.countObligations(obligations, allObligationNames);

    assertThat(mapCount).contains(entry(LegalDashboardsService.OPENCOUNT, open),
        entry(LegalDashboardsService.FLAGGEDCOUNT, flagged), entry(LegalDashboardsService.ADDRESSEDCOUNT, addresed))
        .hasSize(3);
  }

  private void asserGetReviewStatus(
      int flaggedCount,
      int openCount,
      int addressedCount,
      Set<String> allObligationNames,
      Set<String> multiLicenseIds,
      LicenseObligationReviewStatus reviewStatus)
  {
    assertThat(legalDashboardService.getReviewStatus(flaggedCount, openCount, addressedCount, allObligationNames,
        multiLicenseIds)).isEqualTo(reviewStatus);
  }

  private List<ComponentObligation> setupLicenseObligations(
      ComponentIdentifier componentIdentifier,
      ObligationStatus... obligationStatuses)
  {
    List<ComponentObligation> obligations = new ArrayList<>();

    for (int i = 0; i < obligationStatuses.length; i++) {
      ComponentObligation co = new ComponentObligation(componentIdentifier, "id" + i, "obligation" + i, "comment",
          obligationStatuses[i], "hash" + i, "test" + i);
      obligations.add(co);
    }
    return obligations;
  }

  private void setupLicenseObligationsWithMock(List<String> licenses, ObligationStatus... obligationStatuses) {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    Set<LicenseObligationDTO> obligationDtos = new LinkedHashSet<>();
    for (int i = 0; i < obligationStatuses.length; i++) {
      if (obligationStatuses[i] != null) {
        tempEntity.newComponentObligation(componentIdentifier, "id" + i, "obligation" + i, "comment",
            obligationStatuses[i], "hash" + i);
        obligationDtos.add(new LicenseObligationDTO("obligation" + i, Collections.emptySet()));
      }
    }

    List<LicenseMetadataDTO> licenseMetadataDTOs = createLicenseMetadataDTOs(licenses);
    for (LicenseMetadataDTO licenseMetadataDTO : licenseMetadataDTOs) {
      licenseMetadataDTO.setLicenseObligations(obligationDtos);
    }

    doReturn(licenseMetadataDTOs).when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(argThat(list -> list.containsAll(licenses)));
  }

  private List<LicenseMetadataDTO> createLicenseMetadataDTOs(Collection<String> licenseIds) {
    return licenseIds.stream().map(this::createLicenseMetadataDTO).collect(Collectors.toList());
  }

  private LicenseMetadataDTO createLicenseMetadataDTO(String licenseId) {
    LicenseMetadataDTO licenseMetadataDTO = new LicenseMetadataDTO();
    licenseMetadataDTO.setLicenseId(licenseId);
    licenseMetadataDTO.setLicenseText("licenseText");
    licenseMetadataDTO.setLicenseObligations(
        new LinkedHashSet<>(Arrays.asList(createLicenseObligationDTO(), createLicenseObligationDTO())));
    return licenseMetadataDTO;
  }

  private LicenseObligationDTO createLicenseObligationDTO() {
    LicenseObligationDTO licenseObligationDTO = new LicenseObligationDTO();
    licenseObligationDTO.setName("name");
    licenseObligationDTO.setObligationTexts(new LinkedHashSet<>(Arrays.asList("obligationText1", "obligationText2")));
    return licenseObligationDTO;
  }
}
