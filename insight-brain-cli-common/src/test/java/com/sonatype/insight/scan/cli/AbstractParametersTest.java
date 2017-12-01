/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.List;

import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class AbstractParametersTest
{
  static class TestParameters
      extends AbstractParameters
  {
    @Override
    protected String getProgramName() {
      return "test-cli";
    }

    @Override
    public List<String> getScanTargets() {
      return null;
    }

    @Override
    public String getServerUser() {
      return null;
    }
  }

  @Test
  public void testParse_SystemPropertiesWithCommaSeparatedValues() {
    TestParameters params = new TestParameters();
    params.parse("-D", "key1=value1a, value1b", "-D", "key2=value2");
    assertThat(params.getProperties(), contains("key1=value1a, value1b", "key2=value2"));
  }

  @Test
  public void testCreateUsageHelp_MentionsDebugOption() {
    TestParameters params = new TestParameters();
    String help = params.createUsageHelp();
    assertThat(help, containsString("--debug"));
  }
}
