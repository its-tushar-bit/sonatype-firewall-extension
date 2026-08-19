/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationScoreRiskDTOTest
{
  @Test
  public void testGetCsvHeader() {
    assertThat(ApplicationRiskScoreDTO.getCsvHeader())
        .isEqualTo("Organization Name,Application Name,Total Risk,Critical,Severe,Moderate,Low");
  }

  @Test
  public void testToCsvLine() {
    ApplicationRiskScoreDTO risk = new ApplicationRiskScoreDTO("orgName", "orgId", "appName", "appId", "id");
    risk.totalApplicationRisk = new RiskDTO(5, 4, 3, 2, 1);
    assertThat(risk.toCsvLine()).isEqualTo("orgName,appName,5,4,3,2,1");
  }
}
