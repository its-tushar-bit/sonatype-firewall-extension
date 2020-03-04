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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PasswordService
    extends DefaultPasswordService
{
  private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

  private static int hashIterations = DefaultPasswordService.DEFAULT_HASH_ITERATIONS;

  // This reduces the test execution time for this module by ~30%.
  // In my tests, it doesn't make a big difference if we use 1 or 100 for hashIterations. I didn't want to use 1 because
  // it is a very special value and I chose 10 without any really good reason. :)
  public static void useWeakHashIterationForTestsOnly() {
    hashIterations = 10;
  }

  public PasswordService() {
    // We create a DefaultHashService instance only to be able to change the default hash iteration count. Using the
    // default (500000), a password encryption takes about 500ms on my machine. Using 1000, it takes about 30ms.
    DefaultHashService hashService = new DefaultHashService();
    hashService.setHashAlgorithmName(DefaultPasswordService.DEFAULT_HASH_ALGORITHM);
    hashService.setHashIterations(hashIterations);
    hashService.setGeneratePublicSalt(true);
    setHashService(hashService);
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
