/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import com.sonatype.insight.test.InjectedTest;
import com.sonatype.insight.test.LogOutput;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ScannerTest
    extends InjectedTest
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Rule
  public LogOutput logOutput = new LogOutput();

  @Inject
  private Scanner scanner;

  @Test
  public void testScan_FingerprintPerformanceLoggingMessage() throws Exception {
    List<File> targets = Collections.singletonList(new File("src/test/resources/ScannerTest/app.ear"));

    scanner.scan(tmpDir.newFile("scan-test.xml.gz"), targets, new Properties());

    logOutput.assertInfo(regexMatch("Fingerprinting completed in \\d+ seconds for 4 archives, 60 total files"));
  }

  private Matcher<String> regexMatch(final String regex) {
    return new TypeSafeMatcher<String>() {
      @Override
      protected boolean matchesSafely(final String item) {
        return item.matches(regex);
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("No match for regular expression : " + regex );
      }
    };
  }
}
