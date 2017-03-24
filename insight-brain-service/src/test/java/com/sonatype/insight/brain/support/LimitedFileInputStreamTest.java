/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

/**
 * @since 1.27
 */
public class LimitedFileInputStreamTest
{

  static final String CONFIG_YML_FILENAME = "config-support-test.yml";

  static final String CONFIG_YML = "/SupportTest/" + CONFIG_YML_FILENAME;

  private File configYml;

  private LimitedFileInputStream limitedInputStream;

  private final byte[] buff = new byte[1024];

  @Before
  public void setUp() throws Exception {
    configYml = new File(LimitedFileInputStream.class.getResource(CONFIG_YML).getFile());
    assertThat(configYml.exists(), is(true));
  }

  @After
  public void tearDown() throws Exception {
    if (limitedInputStream != null) {
      limitedInputStream.close();
    }
  }

  @Test
  public void testReadLimit_ExceededWhenZero() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 0);
    assertThat(limitedInputStream.isReadLimitMet(), is(true));
  }

  @Test
  public void testReadLimit_Read_ExceededWhenZero() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 0);
    assertThat(limitedInputStream.read(), is(-1));
  }

  @Test
  public void testReadLimit_Read_ExceededWhenOne() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 1);
    assertThat(limitedInputStream.isReadLimitMet(), is(false));
    assertThat(limitedInputStream.read(), not(is(-1)));
    assertThat(limitedInputStream.isReadLimitMet(), is(true));
    assertThat(limitedInputStream.read(), is(-1));
  }

  @Test
  public void testReadLimit_ReadArray_ExceededWhenZero() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 0);
    assertThat(limitedInputStream.read(buff), is(-1));
  }

  @Test
  public void testReadLimit_ReadArray_ExceededWhenFive() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 5);
    assertThat(limitedInputStream.isReadLimitMet(), is(false));
    assertThat(limitedInputStream.read(buff), not(is(-1)));
    assertThat(limitedInputStream.isReadLimitMet(), is(true));
    assertThat(limitedInputStream.read(buff), is(-1));
  }


  @Test
  public void testReadLimit_ReadOffset_ExceededWhenZero() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 0);
    assertThat(limitedInputStream.read(buff, 1, 2), is(-1));
  }

  @Test
  public void testReadLimit_ReadOffset_ExceededWhenFive() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 5);
    assertThat(limitedInputStream.isReadLimitMet(), is(false));
    assertThat(limitedInputStream.read(buff, 0, 4), is(4));
    assertThat(limitedInputStream.isReadLimitMet(), is(false));
    assertThat(limitedInputStream.read(buff, 0, 4), is(1));
    assertThat(limitedInputStream.isReadLimitMet(), is(true));
    assertThat(limitedInputStream.read(buff, 0, 4), is(-1));
  }

  @Test
  public void testIsToBeTruncated() throws Exception {
    verifyIsToBeTruncated(0, true);
    verifyIsToBeTruncated(1, true);
    verifyIsToBeTruncated(configYml.length(), false);
    verifyIsToBeTruncated(configYml.length() + 1, false);
  }

  private void verifyIsToBeTruncated(final long readLimit, boolean expectedIsToBeTruncated) throws IOException {
    try (final LimitedFileInputStream limitedStream = new LimitedFileInputStream(configYml, readLimit)) {
      assertThat(limitedStream.isToBeTruncated(), is(expectedIsToBeTruncated));
    }
  }

  @Test
  public void testLimitedReadIncludesEndOfFile() throws Exception {
    final int readLimit = 40;
    limitedInputStream = new LimitedFileInputStream(configYml, readLimit);

    final byte[] buf = new byte[readLimit];
    assertThat(limitedInputStream.read(buf), is(readLimit));
    assertThat(limitedInputStream.isReadLimitMet(), is(true));
    assertThat(new String(buf, "UTF-8"), is("son_seq-passwords: [3, 2, 1, \"takeoff\"]" + System.lineSeparator()));
  }
}
