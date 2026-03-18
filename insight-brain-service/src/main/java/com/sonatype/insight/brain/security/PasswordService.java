/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.crypto.hash.format.Shiro1CryptFormat;
import org.apache.shiro.lang.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_HASH_ALGORITHM;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsHashAlgorithmOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getNumHashIterationsOrDefault;

@Named
@Singleton
public class PasswordService
    extends DefaultPasswordService
{
  private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

  // Visible for testing
  static final String ITERATIONS_PARAM = "SimpleHash.iterations";

  private static boolean useWeakIterationsForTests = false;

  public static void useWeakHashIterationForTestsOnly() {
    useWeakIterationsForTests = true;
  }

  public PasswordService() {
    /*
     * Shiro v2's default algorithm is argon2id, which is not FIPS compliant. It also only allows argon2 and bcrypt
     * algorithms for hashing and will throw an exception if using SHA-256 with Shiro2CryptFormat
     * (see
     * https://github.com/apache/shiro/blob/shiro-root-2.0.2/crypto/hash/src/main/java/org/apache/shiro/crypto/hash/
     * format/Shiro2CryptFormat.java#L107)
     */
    if (useWeakIterationsForTests || FIPSModeDetector.isEnabled()) {
      DefaultHashService hashService = new DefaultHashService();
      hashService.setDefaultAlgorithmName(FIPS_HASH_ALGORITHM);
      setHashService(hashService);
      setHashFormat(new Shiro1CryptFormat());
    }
  }

  @Override
  protected HashRequest createHashRequest(final ByteSource plaintext) {
    if (useWeakIterationsForTests) {
      // This reduces the test execution time for this module by ~30%.
      // In my tests, it doesn't make a big difference if we use 1 or 100 for hashIterations. I didn't want to use 1
      // because it is a very special value and I chose 10 without any really good reason. :)
      return createHashRequestBuilder(plaintext, Sha256Hash.ALGORITHM_NAME, 10).build();
    }
    if (FIPSModeDetector.isEnabled()) {
      return createHashRequestBuilder(plaintext, getFipsHashAlgorithmOrDefault(), getNumHashIterationsOrDefault())
          .build();
    }
    return super.createHashRequest(plaintext);
  }

  /**
   * Hashes the given password. The returned string can be saved as a hashed password.
   */
  public String hashPassword(String password) {
    if (StringUtils.isBlank(password)) {
      return null;
    }
    long start = System.currentTimeMillis();
    String hashedPassword = encryptPassword(password);
    log.debug("Hashed password in {} ms", System.currentTimeMillis() - start);
    return hashedPassword;
  }

  @VisibleForTesting
  boolean isUsingWeakIterationsForTests() {
    return useWeakIterationsForTests;
  }

  private HashRequest.Builder createHashRequestBuilder(
      final ByteSource plaintext,
      final String algorithm,
      final int iterations)
  {
    // A salt is automatically generated in SimpleHashProvider.generate if one is not provided here
    return new HashRequest.Builder()
        .setSource(plaintext)
        .setAlgorithmName(algorithm)
        .addParameter(ITERATIONS_PARAM, iterations);
  }
}
