/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.27
 */
public class LimitedFileInputStreamTest
{
  private File configYml;

  private LimitedFileInputStream limitedInputStream;

  private final byte[] buff = new byte[1024];

  @BeforeEach
  public void setUp() {
    configYml =
        new File(getClass().getResource("/" + getClass().getSimpleName() + "/config-support-test.yml").getFile());
    assertThat(configYml).isFile();
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (limitedInputStream != null) {
      limitedInputStream.close();
    }
  }

  @Test
  public void testReadLimit_ExceededWhenZero() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 0);
    assertThat(limitedInputStream.isReadLimitMet()).isTrue();
  }

  @Test
  public void testReadLimit_Read_ExceededWhenZero() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 0);
    assertThat(limitedInputStream.read()).isEqualTo(-1);
  }

  @Test
  public void testReadLimit_Read_ExceededWhenOne() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 1);
    assertThat(limitedInputStream.isReadLimitMet()).isFalse();
    assertThat(limitedInputStream.read()).isNotEqualTo(-1);
    assertThat(limitedInputStream.isReadLimitMet()).isTrue();
    assertThat(limitedInputStream.read()).isEqualTo(-1);
  }

  @Test
  public void testReadLimit_ReadArray_ExceededWhenZero() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 0);
    assertThat(limitedInputStream.read(buff)).isEqualTo(-1);
  }

  @Test
  public void testReadLimit_ReadArray_ExceededWhenFive() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 5);
    assertThat(limitedInputStream.isReadLimitMet()).isFalse();
    assertThat(limitedInputStream.read(buff)).isNotEqualTo(-1);
    assertThat(limitedInputStream.isReadLimitMet()).isTrue();
    assertThat(limitedInputStream.read(buff)).isEqualTo(-1);
  }

  @Test
  public void testReadLimit_ReadOffset_ExceededWhenZero() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 0);
    assertThat(limitedInputStream.read(buff, 1, 2)).isEqualTo(-1);
  }

  @Test
  public void testReadLimit_ReadOffset_ExceededWhenFive() throws Exception {
    limitedInputStream = new LimitedFileInputStream(configYml, 5);
    assertThat(limitedInputStream.isReadLimitMet()).isFalse();
    assertThat(limitedInputStream.read(buff, 0, 4)).isEqualTo(4);
    assertThat(limitedInputStream.isReadLimitMet()).isFalse();
    assertThat(limitedInputStream.read(buff, 0, 4)).isEqualTo(1);
    assertThat(limitedInputStream.isReadLimitMet()).isTrue();
    assertThat(limitedInputStream.read(buff, 0, 4)).isEqualTo(-1);
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
      assertThat(limitedStream.isToBeTruncated()).isEqualTo(expectedIsToBeTruncated);
    }
  }

  @Test
  public void testLimitedReadIncludesEndOfFile() throws Exception {
    final int readLimit = 40;
    limitedInputStream = new LimitedFileInputStream(configYml, readLimit);

    final byte[] buf = new byte[readLimit];
    assertThat(limitedInputStream.read(buf)).isEqualTo(readLimit);
    assertThat(limitedInputStream.isReadLimitMet()).isTrue();
    String expected = "son_seq-passwords: [3, 2, 1, \"takeoff\"]" + System.lineSeparator();
    expected = expected.substring(expected.length() - readLimit);
    assertThat(new String(buf, StandardCharsets.UTF_8)).isEqualTo(expected);
  }
}
