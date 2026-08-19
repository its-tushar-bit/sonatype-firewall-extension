/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal;

import java.util.Comparator;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalSourceLinkDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalResultsOrder;

import org.apache.commons.lang3.StringUtils;

public class LicenseLegalComparators
{
  public static final Comparator<LegalSourceLinkDTO> LEGAL_SOURCE_LINK_COMPARATOR =
      Comparator.comparing(legalSourceLinkDTO -> legalSourceLinkDTO.content, String.CASE_INSENSITIVE_ORDER);

  private LicenseLegalComparators() {
    // utility class
  }

  public static Comparator<ApiLicenseLegalApplicationDashboardDTO> newApplicationDashboardComparator(
      LicenseLegalResultsOrder order)
  {
    Comparator<ApiLicenseLegalApplicationDashboardDTO> comparator;
    switch (order != null ? order : LicenseLegalResultsOrder.APPLICATION_NAME_ASC) {
      case APPLICATION_NAME_ASC:
        comparator = Comparator.comparing(dto -> dto.applicationName, String.CASE_INSENSITIVE_ORDER);
        break;
      case APPLICATION_NAME_DESC:
        comparator = Comparator.comparing(dto -> dto.applicationName, String.CASE_INSENSITIVE_ORDER);
        comparator = comparator.reversed();
        break;
      case LAST_SCAN_TIME_ASC:
        comparator = Comparator.comparing(dto -> dto.lastScanTime);
        break;
      case LAST_SCAN_TIME_DESC:
        comparator = Comparator.comparing(dto -> dto.lastScanTime);
        comparator = comparator.reversed();
        break;
      case TAG_NAMES_ASC:
        comparator =
            Comparator.comparing(dto -> StringUtils.join(dto.applicationTagNames, ','), String.CASE_INSENSITIVE_ORDER);
        break;
      case TAG_NAMES_DESC:
        comparator =
            Comparator.comparing(dto -> StringUtils.join(dto.applicationTagNames, ','), String.CASE_INSENSITIVE_ORDER);
        comparator = comparator.reversed();
        break;
      default:
        throw new IllegalArgumentException("Unknown ordering: " + order);
    }
    return comparator.thenComparing(dto -> dto.stageTypeName);
  }

  public static Comparator<ApiLicenseLegalComponentDashboardDTO> newComponentDashboardComparator(
      LicenseLegalResultsOrder order)
  {
    Comparator<ApiLicenseLegalComponentDashboardDTO> comparator;
    switch (order != null ? order : LicenseLegalResultsOrder.COMPONENT_NAME_ASC) {
      case COMPONENT_NAME_ASC:
        comparator = Comparator.comparing(dto -> dto.displayName, String.CASE_INSENSITIVE_ORDER);
        break;
      case COMPONENT_NAME_DESC:
        comparator = Comparator.comparing(dto -> dto.displayName, String.CASE_INSENSITIVE_ORDER);
        comparator = comparator.reversed();
        break;
      case LICENSE_NAME_ASC:
        comparator =
            Comparator.comparing(dto -> dto.licenses.stream().map(l -> l.licenseName).collect(Collectors.joining(",")),
                String.CASE_INSENSITIVE_ORDER);
        break;
      case LICENSE_NAME_DESC:
        comparator =
            Comparator.comparing(dto -> dto.licenses.stream().map(l -> l.licenseName).collect(Collectors.joining(",")),
                String.CASE_INSENSITIVE_ORDER);
        comparator = comparator.reversed();
        break;
      case APPLICATION_COUNT_ASC:
        comparator = Comparator.comparingInt(dto -> dto.applicationOccurrences);
        break;
      case APPLICATION_COUNT_DESC:
        comparator = Comparator.comparingInt(dto -> dto.applicationOccurrences);
        comparator = comparator.reversed();
        break;
      default:
        throw new IllegalArgumentException("Unknown ordering: " + order);
    }
    return comparator;
  }
}
