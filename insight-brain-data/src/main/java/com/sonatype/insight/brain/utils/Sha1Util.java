/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.security.MessageDigest;

import com.sonatype.insight.brain.model.HashHelper;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * SHA1 hashing implementation that reuses the digest to increase performance.
 */
public class Sha1Util
{
  private static final ThreadLocal<MessageDigest> sha1Digest = ThreadLocal.withInitial(DigestUtils::getSha1Digest);

  public static String halfSha1(String input) {
    return HashHelper.truncateHash(sha1(input));
  }

  public static String sha1(String input) {
    sha1Digest.get().reset();
    byte[] hashBytes = sha1Digest.get().digest(input.getBytes());

    StringBuilder sb = new StringBuilder();
    for (byte b : hashBytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
