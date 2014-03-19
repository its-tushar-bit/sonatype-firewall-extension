/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LegacyPathAdjusterTest
{

  public static final String ECLIPSE_INDEX_HTML = "eclipse/index.html";

  private final AssetPathAdjuster assetPathAdjuster = new LegacyPathAdjuster();

  @Test
  public void testAdjustPathForLegacyVersions() throws Exception {
    //older versions should be adjusted for the index file only
    assertThat(assetPathAdjuster.adjustPath(ECLIPSE_INDEX_HTML,
            "Sonatype_CLM_IDE_Eclipse/2.5.0.20131209-2124 (Java 1.7.0_51; Mac OS X 10.8.5)"),
        is("eclipse-legacy/index.html")
    );
    assertThat(assetPathAdjuster.adjustPath(ECLIPSE_INDEX_HTML,
            "Sonatype_CLM_IDE_Eclipse/2.1.1.20110101-0000"),
        is("eclipse-legacy/index.html")
    );

    //files other than the index are presently ignored
    assertThat(assetPathAdjuster.adjustPath("foo",
            "Sonatype_CLM_IDE_Eclipse/2.5.0.20131209-2124 (Java 1.7.0_51; Mac OS X 10.8.5)"),
        is("foo")
    );
  }

  @Test
  public void testAdjustPathForNewerVersions() throws Exception {
    //newer versions should not be adjusted for any content
    assertThat(assetPathAdjuster.adjustPath(ECLIPSE_INDEX_HTML,
            "Sonatype_CLM_IDE_Eclipse/2.5.1.qualifier (Java 1.6.0_65; Mac OS X 10.8.5)"),
        is(ECLIPSE_INDEX_HTML)
    );
    assertThat(assetPathAdjuster.adjustPath(ECLIPSE_INDEX_HTML,
            "Sonatype_CLM_IDE_Eclipse/2.5.1.20140101-0000"),
        is(ECLIPSE_INDEX_HTML)
    );
    assertThat(assetPathAdjuster.adjustPath(ECLIPSE_INDEX_HTML,
            "Sonatype_CLM_IDE_Eclipse/10.5.1.20140101-0000"),
        is(ECLIPSE_INDEX_HTML)
    );
    assertThat(assetPathAdjuster.adjustPath("foo",
            "Sonatype_CLM_IDE_Eclipse/10.5.1.20140101-0000"),
        is("foo")
    );
  }

  @Test
  public void testNonMatchingUserAgent() throws Exception {
    assertThat(assetPathAdjuster.adjustPath(ECLIPSE_INDEX_HTML,
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5"),
        is(ECLIPSE_INDEX_HTML)
    );
  }
}
