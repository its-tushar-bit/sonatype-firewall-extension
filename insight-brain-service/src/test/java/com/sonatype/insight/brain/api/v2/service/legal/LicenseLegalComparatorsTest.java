/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalResultsOrder;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.service.legal.LicenseLegalComparators.newApplicationDashboardComparator;
import static com.sonatype.insight.brain.api.v2.service.legal.LicenseLegalComparators.newComponentDashboardComparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class LicenseLegalComparatorsTest
{
  @Test
  public void testNewApplicationDashboardComparator_ByApplicationName() {
    ApiLicenseLegalApplicationDashboardDTO dto1 = new ApiLicenseLegalApplicationDashboardDTO();
    dto1.applicationName = "a1";
    ApiLicenseLegalApplicationDashboardDTO dto2 = new ApiLicenseLegalApplicationDashboardDTO();
    dto2.applicationName = "A2";
    ApiLicenseLegalApplicationDashboardDTO dto3 = new ApiLicenseLegalApplicationDashboardDTO();
    dto3.applicationName = "a3";

    List<ApiLicenseLegalApplicationDashboardDTO> dtos = Arrays.asList(dto3, dto1, dto2);
    dtos.sort(newApplicationDashboardComparator(null));
    assertThat(dtos).extracting(dto -> dto.applicationName).containsExactly("a1", "A2", "a3");

    dtos = Arrays.asList(dto3, dto1, dto2);
    dtos.sort(newApplicationDashboardComparator(LicenseLegalResultsOrder.APPLICATION_NAME_ASC));
    assertThat(dtos).extracting(dto -> dto.applicationName).containsExactly("a1", "A2", "a3");

    dtos = Arrays.asList(dto3, dto1, dto2);
    dtos.sort(newApplicationDashboardComparator(LicenseLegalResultsOrder.APPLICATION_NAME_DESC));
    assertThat(dtos).extracting(dto -> dto.applicationName).containsExactly("a3", "A2", "a1");
  }

  @Test
  public void testNewApplicationDashboardComparator_ByLastScanTime() {
    long currentTimestamp = System.currentTimeMillis();

    ApiLicenseLegalApplicationDashboardDTO dto1 = new ApiLicenseLegalApplicationDashboardDTO();
    dto1.lastScanTime = currentTimestamp + 3000;
    ApiLicenseLegalApplicationDashboardDTO dto2 = new ApiLicenseLegalApplicationDashboardDTO();
    dto2.lastScanTime = currentTimestamp;
    ApiLicenseLegalApplicationDashboardDTO dto3 = new ApiLicenseLegalApplicationDashboardDTO();
    dto3.lastScanTime = currentTimestamp + 2000;

    List<ApiLicenseLegalApplicationDashboardDTO> dtos = Arrays.asList(dto3, dto1, dto2);
    dtos.sort(newApplicationDashboardComparator(LicenseLegalResultsOrder.LAST_SCAN_TIME_ASC));
    assertThat(dtos).extracting(dto -> dto.lastScanTime)
        .containsExactly(dto2.lastScanTime, dto3.lastScanTime,
            dto1.lastScanTime);

    dtos = Arrays.asList(dto3, dto1, dto2);
    dtos.sort(newApplicationDashboardComparator(LicenseLegalResultsOrder.LAST_SCAN_TIME_DESC));
    assertThat(dtos).extracting(dto -> dto.lastScanTime)
        .containsExactly(dto1.lastScanTime, dto3.lastScanTime,
            dto2.lastScanTime);
  }

  @Test
  public void testNewApplicationDashboardComparator_ByTagNames() {
    ApiLicenseLegalApplicationDashboardDTO dto1 = new ApiLicenseLegalApplicationDashboardDTO();
    dto1.applicationTagNames = Collections.singletonList("t1");
    ApiLicenseLegalApplicationDashboardDTO dto2 = new ApiLicenseLegalApplicationDashboardDTO();
    dto2.applicationTagNames = Collections.singletonList("T2");
    ApiLicenseLegalApplicationDashboardDTO dto3 = new ApiLicenseLegalApplicationDashboardDTO();
    dto3.applicationTagNames = Collections.singletonList("t3");

    List<ApiLicenseLegalApplicationDashboardDTO> dtos = Arrays.asList(dto3, dto1, dto2);
    dtos.sort(newApplicationDashboardComparator(LicenseLegalResultsOrder.TAG_NAMES_ASC));
    assertThat(dtos).flatExtracting(dto -> dto.applicationTagNames).containsExactly("t1", "T2", "t3");

    dtos = Arrays.asList(dto3, dto1, dto2);
    dtos.sort(newApplicationDashboardComparator(LicenseLegalResultsOrder.TAG_NAMES_DESC));
    assertThat(dtos).flatExtracting(dto -> dto.applicationTagNames).containsExactly("t3", "T2", "t1");
  }

  @Test
  public void testNewApplicationDashboardComparator_ByInvalidCriteria() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> newApplicationDashboardComparator(LicenseLegalResultsOrder.COMPONENT_NAME_ASC))
        .withMessage("Unknown ordering: COMPONENT_NAME_ASC");
  }

  @Test
  public void testNewComponentDashboardComparator_ByComponentName() {
    ApiLicenseLegalComponentDashboardDTO dto1 = new ApiLicenseLegalComponentDashboardDTO();
    dto1.displayName = "c1";
    ApiLicenseLegalComponentDashboardDTO dto2 = new ApiLicenseLegalComponentDashboardDTO();
    dto2.displayName = "C2";
    ApiLicenseLegalComponentDashboardDTO dto3 = new ApiLicenseLegalComponentDashboardDTO();
    dto3.displayName = "c3";

    List<ApiLicenseLegalComponentDashboardDTO> dtos = Arrays.asList(dto2, dto3, dto1);
    dtos.sort(newComponentDashboardComparator(null));
    assertThat(dtos).extracting(dto -> dto.displayName).containsExactly("c1", "C2", "c3");

    dtos = Arrays.asList(dto2, dto3, dto1);
    dtos.sort(newComponentDashboardComparator(LicenseLegalResultsOrder.COMPONENT_NAME_ASC));
    assertThat(dtos).extracting(dto -> dto.displayName).containsExactly("c1", "C2", "c3");

    dtos = Arrays.asList(dto2, dto3, dto1);
    dtos.sort(newComponentDashboardComparator(LicenseLegalResultsOrder.COMPONENT_NAME_DESC));
    assertThat(dtos).extracting(dto -> dto.displayName).containsExactly("c3", "C2", "c1");
  }

  @Test
  public void testNewComponentDashboardComparator_ByLicenseName() {
    List<ApiLicenseThreatDTOV2> licenseThreatGroups = Collections.emptyList();
    Set<ApiLicenseDTOV2> dtoSet1 = new HashSet<>();
    ApiLicenseDTOV2 dto1 = new ApiLicenseDTOV2("id1", "l1", licenseThreatGroups);
    dtoSet1.add(dto1);
    Set<ApiLicenseDTOV2> dtoSet2 = new HashSet<>();
    ApiLicenseDTOV2 dto2 = new ApiLicenseDTOV2("id2", "l2", licenseThreatGroups);
    dtoSet2.add(dto2);
    Set<ApiLicenseDTOV2> dtoSet3 = new HashSet<>();
    ApiLicenseDTOV2 dto3 = new ApiLicenseDTOV2("id3", "l3", licenseThreatGroups);
    dtoSet3.add(dto3);

    ApiLicenseLegalComponentDashboardDTO apiDTO1 = new ApiLicenseLegalComponentDashboardDTO();
    apiDTO1.licenses = dtoSet1;
    ApiLicenseLegalComponentDashboardDTO apiDTO2 = new ApiLicenseLegalComponentDashboardDTO();
    apiDTO2.licenses = dtoSet2;
    ApiLicenseLegalComponentDashboardDTO apiDTO3 = new ApiLicenseLegalComponentDashboardDTO();
    apiDTO3.licenses = dtoSet3;

    List<ApiLicenseLegalComponentDashboardDTO> dtos = Arrays.asList(apiDTO2, apiDTO1, apiDTO3);
    dtos.sort(newComponentDashboardComparator(LicenseLegalResultsOrder.LICENSE_NAME_ASC));
    assertThat(dtos).extracting(dto -> dto.licenses).containsExactly(dtoSet1, dtoSet2, dtoSet3);
    dtos = Arrays.asList(apiDTO2, apiDTO3, apiDTO1);
    dtos.sort(newComponentDashboardComparator(LicenseLegalResultsOrder.LICENSE_NAME_DESC));
    assertThat(dtos).extracting(dto -> dto.licenses).containsExactly(dtoSet3, dtoSet2, dtoSet1);
  }

  @Test
  public void testNewComponentDashboardComparator_ByApplicationCount() {
    ApiLicenseLegalComponentDashboardDTO dto1 = new ApiLicenseLegalComponentDashboardDTO();
    dto1.applicationOccurrences = 1;
    ApiLicenseLegalComponentDashboardDTO dto2 = new ApiLicenseLegalComponentDashboardDTO();
    dto2.applicationOccurrences = 2;
    ApiLicenseLegalComponentDashboardDTO dto3 = new ApiLicenseLegalComponentDashboardDTO();
    dto3.applicationOccurrences = 3;

    List<ApiLicenseLegalComponentDashboardDTO> dtos = Arrays.asList(dto2, dto3, dto1);
    dtos.sort(newComponentDashboardComparator(LicenseLegalResultsOrder.APPLICATION_COUNT_ASC));
    assertThat(dtos).extracting(dto -> dto.applicationOccurrences).containsExactly(1, 2, 3);

    dtos = Arrays.asList(dto2, dto3, dto1);
    dtos.sort(newComponentDashboardComparator(LicenseLegalResultsOrder.APPLICATION_COUNT_DESC));
    assertThat(dtos).extracting(dto -> dto.applicationOccurrences).containsExactly(3, 2, 1);
  }

  @Test
  public void testNewComponentDashboardComparator_ByInvalidCriteria() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> newComponentDashboardComparator(LicenseLegalResultsOrder.LAST_SCAN_TIME_ASC))
        .withMessage("Unknown ordering: LAST_SCAN_TIME_ASC");
  }
}
