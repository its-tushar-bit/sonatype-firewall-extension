/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class InsightWorkTest
    extends AbstractComponentTest
{
  private static final String VALID_ID = "abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLOMOPQUSTUVWXYZ-0123456789";

  private static final String[] INVALID_CHARACTERS = { ".", "\\", "/", "%" };

  @Inject
  private InsightWork work;

  @Test
  public void testGetScanDir() {
    File file = work.getScanDir(VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testGetScanDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getScanDir(invalidValue)).withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetScanFile() {
    File file = work.getScanFile(VALID_ID, VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testGetScanFile_InvalidAppIdValidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getScanFile(invalidValue, VALID_ID)).withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetScanFile_ValidAppIdInvalidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getScanFile(VALID_ID, invalidValue)).withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetAuditDir() {
    File file = work.getAuditDir(VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testGetAuditDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getAuditDir(invalidValue)).withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetReportDir() {
    File file = work.getReportDir(VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testGetReportDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getReportDir(invalidValue)).withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetReportDir_InvalidAppIdValidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getReportDir(invalidValue, VALID_ID)).withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetReportDir_ValidAppIdInvalidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getReportDir(VALID_ID, invalidValue)).withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetReportFile() {
    File file = work.getReportFile(VALID_ID, VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testGetReportFile_InvalidAppIdValidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getReportFile(invalidValue, VALID_ID)).withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetReportFile_ValidAppIdInvalidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getReportFile(VALID_ID, invalidValue)).withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testComponentDetailsDir() {
    File file = work.getComponentDetailsDir(VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testComponentDetailsDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getComponentDetailsDir(invalidValue)).withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testComponentDetailsFile() {
    File file = work.getComponentDetailsFile(VALID_ID, VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testComponentDetailsFile_InvalidAppIdValidResultsId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getComponentDetailsFile(invalidValue, VALID_ID))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testComponentDetailsFile_ValidAppIdInvalidResultsId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getComponentDetailsFile(VALID_ID, invalidValue))
          .withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testSourceControlDir() {
    File file = work.getSourceControlDir(VALID_ID);
    assertThat(file).isNotNull();
  }

  @Test
  public void testSourceControlDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> work.getSourceControlDir(invalidValue)).withMessage("Invalid value: " + invalidValue);
    }
  }

  @Test
  public void testGetTemporaryDirectory() {
    File file = work.getTemporaryDirectory();
    assertThat(file).isNotNull();
  }

  @Test
  public void testInsightWork_HasDefaultSourceControlConfiguration() {
    assertThat(work.sourceControlConfigurationAtomicReference.get()).usingRecursiveComparison().isEqualTo(
        new SourceControlConfiguration());
  }
}
