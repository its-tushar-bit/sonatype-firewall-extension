/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DashboardViolationRiskDTOTest
{
  private DashboardViolationRiskDTO risk;

  @BeforeEach
  public void before() {
    risk = new DashboardViolationRiskDTO();
    risk.threatLevel = 7;
    risk.policyName = "p";
    risk.applicationName = "a";
    risk.organizationName = "o";
    risk.firstOccurrenceTime = 0;
    risk.displayName = new ComponentDisplayName();
    risk.displayName.add("nameField", "nameValue");
    risk.filename = "filename";
    risk.hash = "theHash";
    risk.policyViolationId = "policyViolationId1";
  }

  @Test
  public void testToCsvLine_WithDisplayName() {
    assertThat(risk.toCsvLine()).isEqualTo("7,p,o,a,nameValue,1970-01-01T00:00:00Z,0,,policyViolationId1");
  }

  @Test
  public void testToCsvLine_WithoutDisplayName() {
    risk.displayName = null;
    assertThat(risk.toCsvLine()).isEqualTo("7,p,o,a,filename,1970-01-01T00:00:00Z,0,,policyViolationId1");
  }

  @Test
  public void testToCsvLine_WithoutDisplayNameOrFilename() {
    risk.displayName = null;
    risk.filename = null;
    assertThat(risk.toCsvLine())
        .isEqualTo("7,p,o,a,(Anonymized Path) SHA1: theHash,1970-01-01T00:00:00Z,0,,policyViolationId1");
    risk.filename = "";
    assertThat(risk.toCsvLine())
        .isEqualTo("7,p,o,a,(Anonymized Path) SHA1: theHash,1970-01-01T00:00:00Z,0,,policyViolationId1");
  }

  @Test
  public void testToCsvLine_QuotedIfNecessary() {
    risk.displayName = null;
    risk.filename = "c,d.jar";
    assertThat(risk.toCsvLine()).isEqualTo("7,p,o,a,\"c,d.jar\",1970-01-01T00:00:00Z,0,,policyViolationId1");
  }

  @Test
  public void testToCsvLine_WithCVEReference() {
    risk.referenceId = "CVE-12345";
    assertThat(risk.toCsvLine()).isEqualTo("7,p,o,a,nameValue,1970-01-01T00:00:00Z,0,CVE-12345,policyViolationId1");
  }
}
