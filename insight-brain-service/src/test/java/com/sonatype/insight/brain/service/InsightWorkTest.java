/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import com.sonatype.insight.error.exception.BadRequestException;

import com.google.inject.Inject;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

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
    assertThat(file, notNullValue());
  }

  @Test
  public void testGetScanDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        work.getScanDir(invalidValue);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }

  @Test
  public void testGetScanFile() {
    File file = work.getScanFile(VALID_ID, VALID_ID);
    assertThat(file, notNullValue());
  }

  @Test
  public void testGetScanFile_InvalidAppIdValidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        work.getScanFile(invalidValue, VALID_ID);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }

  @Test
  public void testGetScanFile_ValidAppIdInvalidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        work.getScanFile(VALID_ID, invalidValue);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }

  @Test
  public void testGetAuditDir() {
    File file = work.getAuditDir(VALID_ID);
    assertThat(file, notNullValue());
  }

  @Test
  public void testGetAuditDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        work.getAuditDir(invalidValue);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }

  @Test
  public void testGetReportDir() {
    File file = work.getReportDir(VALID_ID);
    assertThat(file, notNullValue());
  }

  @Test
  public void testGetReportDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        work.getReportDir(invalidValue);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }

  @Test
  public void testGetReportDir_InvalidAppIdValidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        work.getReportDir(invalidValue, VALID_ID);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }

  @Test
  public void testGetReportDir_ValidAppIdInvalidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        work.getReportDir(VALID_ID, invalidValue);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }

  @Test
  public void testGetReportFile() {
    File file = work.getReportFile(VALID_ID, VALID_ID);
    assertThat(file, notNullValue());
  }

  @Test
  public void testGetReportFile_InvalidAppIdValidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        work.getReportFile(invalidValue, VALID_ID);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }

  @Test
  public void testGetReportFile_ValidAppIdInvalidScanId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        work.getReportFile(VALID_ID, invalidValue);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }

  @Test
  public void testComponentDetailsDir() {
    File file = work.getComponentDetailsDir(VALID_ID);
    assertThat(file, notNullValue());
  }

  @Test
  public void testComponentDetailsDir_InvalidAppId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        work.getComponentDetailsDir(invalidValue);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }

  @Test
  public void testComponentDetailsFile() {
    File file = work.getComponentDetailsFile(VALID_ID, VALID_ID);
    assertThat(file, notNullValue());
  }

  @Test
  public void testComponentDetailsFile_InvalidAppIdValidResultsId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        work.getComponentDetailsFile(invalidValue, VALID_ID);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }

  @Test
  public void testComponentDetailsFile_ValidAppIdInvalidResultsId() {
    for (String invalidValue : INVALID_CHARACTERS) {
      try {
        work.getComponentDetailsFile(VALID_ID, invalidValue);
        fail("Expected BadRequestException");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Invalid value: " + invalidValue));
      }
    }
  }
}
