/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulate the nuances of custom SSL for the Graal native-images.
 * <p>
 * There are a couple of nuances:
 * <ol>
 *  <li>The {@code --enable-http} and {@code --enable-https} command line options are needed to enable http and https at all. Those options are enabled in the <a href="https://github.com/sonatype/native-image-nexus-iq-cli/">native-image-nexus-iq-cli/</a> repository by default.</li>
 *  <li>Custom key store options (i.e. the {@code -Djavax.net.ssl.keyStore} family of options) do NOT work as expected at runtime</li>
 *  <li>Custom trust store options (i.e. the {@code -Djavax.net.ssl.trustStore} family of options) do NOT WORK AT ALL. See <a href="https://github.com/oracle/graal/issues/1999">graal issue 1999</a> where it describes the decisions the Graal team went through to come to the current (as of 20.2) situation where you CANNOT override the trust store at runtime at all by default.</li>
 * </ol>
 * <p>
 * In order to fix these items we do the following:
 * <ol>
 *   <li>Provide arguments to override the key store at runtime. See the {@code --ssl-key-store-*} family of options in {@link GraalParameters}</li>
 *   <li>Provide arguments to override the trust store at runtime. See the {@code --ssl-trust-store-*} family of options in {@link GraalParameters}</li>
 *   <li>If any of these options are provided we override the default {@link javax.net.ssl.SSLContext} with the custom provided options</li>
 *   <li>Since the CLI only needs to communicate with IQ Server, we have two simple implementations for the key and trust store managers: {@link SimpleX509KeyManager} and {@link SimpleX509TrustManager}</li>
 *   <li>The {@link SimpleX509KeyManager} will set any provided key store as the only {@link KeyManager}</li>
 *   <li>The {@link SimpleX509TrustManager} will set any provided trust store as the only {@link TrustManager}</li>
 *   <li>These two managers allow for custom certificates as well as PKI auth</li>
 * </ol>
 */
public class GraalSslContext
{
  private static final Logger log = LoggerFactory.getLogger(GraalSslContext.class);

  public static void maybeDoCustomSslContext(final AbstractParameters params) throws ExitException {
    GraalParameters graalParameters = (GraalParameters) params;

    if (!graalParameters.hasKeyStoreSsl() && !graalParameters.hasTrustStoreSsl()) {
      return;
    }

    try {
      KeyManager[] keyManagers = null;
      TrustManager[] trustManagers = null;

      if (graalParameters.hasKeyStoreSsl()) {
        char[] keyStorePassword = graalParameters.getKeyStorePassword().toCharArray();
        KeyStore keyStore = KeyStore.getInstance(graalParameters.getKeyStoreType());
        keyStore.load(new FileInputStream(graalParameters.getKeyStorePath()), keyStorePassword);
        X509KeyManager keyManager = new SimpleX509KeyManager(keyStore, keyStorePassword);
        keyManagers = new KeyManager[]{keyManager};
        log.info("Loaded provided custom key store '{}'", graalParameters.getKeyStorePath());
      }

      if (graalParameters.hasTrustStoreSsl()) {
        char[] trustStorePassword = graalParameters.getTrustStorePassword().toCharArray();
        KeyStore trustStore = KeyStore.getInstance(graalParameters.getTrustStoreType());
        trustStore.load(new FileInputStream(graalParameters.getTrustStorePath()), trustStorePassword);
        X509TrustManager trustManager = new SimpleX509TrustManager(trustStore);
        trustManagers = new TrustManager[]{trustManager};
        log.info("Loaded provided custom trust store '{}'", graalParameters.getTrustStorePath());
      }

      SSLContext context = SSLContext.getInstance("SSL");
      context.init(keyManagers, trustManagers, null);
      SSLContext.setDefault(context);
      log.info("Set custom default SSL Context");
    }
    catch (IOException | GeneralSecurityException ex) {
      log.error("Failed to load custom SSL configuration", ex);
      throw new ExitException(1);
    }
  }

  private static class SimpleX509KeyManager
      implements X509KeyManager
  {
    private final X509KeyManager keyManager;

    public SimpleX509KeyManager(final KeyStore keyStore, final char[] password) {
      this.keyManager = createKeyManager(keyStore, password);
    }

    private X509KeyManager createKeyManager(final KeyStore keyStore, final char[] password) {
      try {
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore, password);
        return Iterables.getFirst(Iterables.filter(
            Arrays.asList(factory.getKeyManagers()), X509KeyManager.class), null);
      }
      catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
        log.error("Unable to create key manager");
        throw new RuntimeException(e);
      }
    }

    @Override
    public String[] getClientAliases(final String keyType, final Principal[] issuers) {
      return keyManager.getClientAliases(keyType, issuers);
    }

    @Override
    public String chooseClientAlias(final String[] keyType, final Principal[] issuers, final Socket socket) {
      return keyManager.chooseClientAlias(keyType, issuers, socket);
    }

    @Override
    public String[] getServerAliases(final String keyType, final Principal[] issuers) {
      return keyManager.getServerAliases(keyType, issuers);
    }

    @Override
    public String chooseServerAlias(final String keyType, final Principal[] issuers, final Socket socket) {
      return keyManager.chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(final String alias) {
      X509Certificate[] chain = keyManager.getCertificateChain(alias);
      if (chain != null && chain.length > 0) {
        return chain;
      }
      return null;
    }

    @Override
    public PrivateKey getPrivateKey(final String alias) {
      return keyManager.getPrivateKey(alias);
    }
  }

  private static class SimpleX509TrustManager
      implements X509TrustManager
  {
    private final X509TrustManager trustManager;

    public SimpleX509TrustManager(final KeyStore keyStore) {
      this.trustManager = createTrustManager(keyStore);
    }

    private X509TrustManager createTrustManager(final KeyStore keyStore) {
      try {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);
        return Iterables.getFirst(Iterables.filter(
            Arrays.asList(factory.getTrustManagers()), X509TrustManager.class), null);
      }
      catch (NoSuchAlgorithmException | KeyStoreException e) {
        log.error("Unable to create trust manager");
        throw new RuntimeException(e);
      }
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
      try {
        trustManager.checkClientTrusted(chain, authType);
      }
      catch (CertificateException e) {
        log.error("Certificate exception while checking if client is trusted", e);
      }
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
      try {
        trustManager.checkServerTrusted(chain, authType);
      }
      catch (CertificateException e) {
        log.error("Certificate exception while checking if server is trusted", e);
      }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      ImmutableList.Builder<X509Certificate> certificates = ImmutableList.builder();
      for (X509Certificate cert : trustManager.getAcceptedIssuers()) {
        certificates.add(cert);
      }
      return Iterables.toArray(certificates.build(), X509Certificate.class);
    }
  }
}
