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

public class ComponentRiskDTOTest
{
  private ComponentRiskDTO risk;

  @BeforeEach
  public void before() {
    risk = new ComponentRiskDTO();
    risk.affectedApplications = 1;
    risk.scoreCritical = 2;
    risk.scoreSevere = 3;
    risk.scoreModerate = 4;
    risk.scoreLow = 5;
    risk.score = 14;
    risk.displayName = new ComponentDisplayName();
    risk.displayName.add("displayNameField", "displayNameValue");
    risk.filename = "filename";
    risk.hash = "theHash";
  }

  @Test
  public void testToCsvLine_WithDisplayName() {
    assertThat(risk.toCsvLine()).isEqualTo("displayNameValue,1,14,2,3,4,5");
  }

  @Test
  public void testToCsvLine_WithoutDisplayName() {
    risk.displayName = null;
    assertThat(risk.toCsvLine()).isEqualTo("filename,1,14,2,3,4,5");
  }

  @Test
  public void testToCsvLine_WithoutDisplayNameOrFilename() {
    risk.displayName = null;
    risk.filename = null;
    assertThat(risk.toCsvLine()).isEqualTo("(Anonymized Path) SHA1: theHash,1,14,2,3,4,5");
    risk.filename = "";
    assertThat(risk.toCsvLine()).isEqualTo("(Anonymized Path) SHA1: theHash,1,14,2,3,4,5");
  }

  @Test
  public void testToCsvLine_QuotedIfNecessary() {
    risk.displayName = null;
    risk.filename = "filename,1";
    assertThat(risk.toCsvLine()).isEqualTo("\"filename,1\",1,14,2,3,4,5");
  }
}
