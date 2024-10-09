/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.Arrays;
import java.util.Collection;

import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class SbomIdentityUtilsTest
{
  private String input;

  private String expectedOutput;

  public SbomIdentityUtilsTest(final String input, final String expectedOutput) {
    this.input = input;
    this.expectedOutput = expectedOutput;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {null, null},
        {"", null},
        {"cpe:invalid", null},
        {"cpe:/a:apache:log4j:2.11.2:rc3", "pkg:generic/apache/log4j@2.11.2?update=rc3"},
        {"cpe:2.3:a:apache:log4j:2.11.2:rc3:*:*:*:*:*:*", "pkg:generic/apache/log4j@2.11.2?update=rc3"},
        {"cpe:/o:micro%24oft:window%24_2000::sp3:pro%2A",
            "pkg:generic/micro%24oft/window%24_2000?edition=pro%2A&update=sp3"},
        {"cpe:2.3:a:microsoft:internet_explorer:8.0.6001:beta:*:en:*:*:*:*",
            "pkg:generic/microsoft/internet_explorer@8.0.6001?language=en&update=beta"},
    });
  }

  @Test
  public void testBuildPackageUrlFromCpe() {
    PackageUrlIdentifier purl = SbomIdentityUtils.buildPackageUrlFromCpe(input);

    if (expectedOutput == null) {
      assertThat(purl).isNull();
    }
    else {
      assertThat(purl).isNotNull();
      assertThat(purl.getPackageUrl()).isEqualTo(expectedOutput);
    }
  }
}
