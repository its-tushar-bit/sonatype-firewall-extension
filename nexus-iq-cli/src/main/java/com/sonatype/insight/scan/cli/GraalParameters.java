/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;

import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.SystemUtils;

public class GraalParameters
    extends Parameters
{
  @Parameter(names = {"--ssl-key-store-password"},
      description = "Password for custom SSL key store (equivalent to -Djavax.net.ssl.keyStorePassword JVM property)",
      password = true)
  private String keyStorePassword;

  @Parameter(names = {"--ssl-key-store-path"},
      description = "Path to custom SSL key store (equivalent to -Djavax.net.ssl.keyStore JVM property)")
  private File keyStorePath;

  @Parameter(names = {"--ssl-key-store-type"},
      description = "Type of custom SSL key store (equivalent to -Djavax.net.ssl.keyStoreType JVM property)")
  private String keyStoreType;

  @Parameter(names = {"--ssl-trust-store-password"}, description =
      "Password for custom SSL trust store (equivalent to -Djavax.net.ssl.trustStorePassword JVM property)",
      password = true)
  private String trustStorePassword;

  @Parameter(names = {"--ssl-trust-store-path"},
      description = "Path to custom SSL trust store (equivalent to -Djavax.net.ssl.trustStore JVM property)")
  private File trustStorePath;

  @Parameter(names = {"--ssl-trust-store-type"},
      description = "Type of custom SSL trust store (equivalent to -Djavax.net.ssl.trustStoreType JVM property)")
  private String trustStoreType;

  public GraalParameters() {
    super();
  }

  public GraalParameters(final String[] args) {
    super(args);
  }

  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  public File getKeyStorePath() {
    return keyStorePath;
  }

  public String getKeyStoreType() {
    return keyStoreType;
  }

  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  public File getTrustStorePath() {
    return trustStorePath;
  }

  public String getTrustStoreType() {
    return trustStoreType;
  }

  public boolean hasSslParams() {
    return keyStorePath != null || trustStorePath != null;
  }

  @Override
  protected String getProgramName() {
    return "nexus-iq-cli" + (SystemUtils.IS_OS_WINDOWS ? ".exe" : "");
  }
}
