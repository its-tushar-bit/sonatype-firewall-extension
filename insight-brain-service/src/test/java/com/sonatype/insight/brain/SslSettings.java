/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.sonatype.insight.test.networking.SslProperties;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;

/**
 * JUnit rule to configure {@link SSLContext#getDefault()} with our testing key/trust store and cleanup after
 * {@link #use()}. Implements the JUnit 5 callbacks so it can be used via {@code @RegisterExtension} as well as the
 * legacy {@code @Rule}.
 */
public class SslSettings
    extends ExternalResource
    implements BeforeEachCallback, AfterEachCallback
{
  private SSLContext previousContext;

  @Override
  protected void before() throws Exception {
    previousContext = SSLContext.getDefault();
  }

  @Override
  protected void after() {
    if (previousContext != null) {
      SSLContext.setDefault(previousContext);
    }
  }

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    before();
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    after();
  }

  public void use() {
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(getKeyManagers(), getTrustManagers(), null);
      SSLContext.setDefault(sslContext);
    }
    catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException("Could not configure SSL", e);
    }
  }

  private KeyStore getKeyStore(File keyStoreFile, String password) throws GeneralSecurityException, IOException {
    KeyStore keyStore = KeyStore.getInstance("jks");
    try (InputStream in = Files.newInputStream(keyStoreFile.toPath())) {
      keyStore.load(in, password.toCharArray());
    }
    return keyStore;
  }

  private KeyManager[] getKeyManagers() throws GeneralSecurityException, IOException {
    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(getKeyStore(SslProperties.CLIENT_STORE_FILE, SslProperties.KEY_STORE_PASSWORD),
        SslProperties.KEY_STORE_PASSWORD.toCharArray());
    return keyManagerFactory.getKeyManagers();
  }

  private TrustManager[] getTrustManagers() throws GeneralSecurityException, IOException {
    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(getKeyStore(SslProperties.SERVER_STORE_FILE, SslProperties.TRUST_STORE_PASSWORD));
    return trustManagerFactory.getTrustManagers();
  }
}
