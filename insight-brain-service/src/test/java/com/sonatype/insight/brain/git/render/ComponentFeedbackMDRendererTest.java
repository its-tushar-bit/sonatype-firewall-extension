/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import java.util.Optional;

import com.sonatype.insight.brain.git.render.model.ComponentFeedbackContext;
import com.sonatype.insight.brain.git.render.model.SeverityInfo;
import com.sonatype.insight.brain.git.render.model.SecurityIssue;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.git.render.model.MDImages.*;
import static com.sonatype.insight.brain.utils.TemplateHelper.assertRenderedOutput;

public class ComponentFeedbackMDRendererTest
{
  private static final int MANY_BREAKING_CHANGES_COUNT = 6;

  private static final Logger log = LoggerFactory.getLogger(ComponentFeedbackMDRendererTest.class);

  /**
   * This sanity test ensures the template renders for a basic
   * configuration and that all required template variables are defined
   */
  @Test
  public void testRender_sanity() throws Exception {
    String multiLineDescription = """
        Paragraph 1.

        Paragraph 2.
        """;
    final ComponentFeedbackContext componentFeedbackContext = new ComponentFeedbackContext(
        true, // Only support html for now
        ThreatLevelDisplay.fromValue(10),
        "http://example.com/com.fasterxml.jackson.core/jackson-databind:2.13.1",
        "com.fasterxml.jackson.core.jackson-databind:2.13.1",
        SourceControlProvider.GITHUB,
        MANY_BREAKING_CHANGES_COUNT,
        "2.15.0",
        "next-no-violations",
        true,
        ImmutableList.of(
            new SecurityIssue(3, new SeverityInfo("CVE-123", 7.6f, SONATYPE_DEEP_DIVE_TAG),
                "some desc1", "https://example.com/policyViolations/1"),

            new SecurityIssue(4, new SeverityInfo("CVE-456", 6.6f, SONATYPE_FAST_TRACK_TAG),
                "some desc2", "https://example.com/policyViolations/2"),

            new SecurityIssue(6, new SeverityInfo("CVE-789", 8.6f, null),
                multiLineDescription, "https://example.com/policyViolations/3"),

            new SecurityIssue(7, null,
                null, "https://example.com/policyViolations/4"),

            new SecurityIssue(8, null,
                null, "https://example.com/policyViolations/5")),
        DIRECT_DEP_LOGO,
        "        <version>2.15.0</version>",
        true,
        false);
    runTest(componentFeedbackContext, "testRender_sanity.md");
  }

  private void runTest(final ComponentFeedbackContext context, final String expectedOutputFile) throws Exception {
    log.info("Running Test for " + expectedOutputFile);
    final Optional<String> actualContent = ComponentFeedbackMDRenderer.render(context);
    assertRenderedOutput(actualContent, getClass(), expectedOutputFile);
  }
}
