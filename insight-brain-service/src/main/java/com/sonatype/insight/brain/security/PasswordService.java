/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.crypto.hash.format.Shiro1CryptFormat;
import org.apache.shiro.lang.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PasswordService
    extends DefaultPasswordService
{
  private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

  private static DefaultHashService hashService;

  private static boolean useWeakHashService = false;

  public static void useWeakHashIterationForTestsOnly() {
    useWeakHashService = true;
    hashService = new DefaultHashService();
    hashService.setDefaultAlgorithmName(Sha256Hash.ALGORITHM_NAME);
  }

  public PasswordService() {
    if (useWeakHashService) {
      setHashService(hashService);
      setHashFormat(new Shiro1CryptFormat());
    }
  }

  @Override
  protected HashRequest createHashRequest(final ByteSource plaintext) {
    if (useWeakHashService) {
      // This reduces the test execution time for this module by ~30%.
      // In my tests, it doesn't make a big difference if we use 1 or 100 for hashIterations. I didn't want to use 1
      // because it is a very special value and I chose 10 without any really good reason. :)
      return new HashRequest.Builder().setSource(plaintext)
          .setAlgorithmName(Sha256Hash.ALGORITHM_NAME).addParameter("SimpleHash.iterations", 10)
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
}
