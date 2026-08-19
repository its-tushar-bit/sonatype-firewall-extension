/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.certificate;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import com.sonatype.insight.brain.security.FIPSModeDetector;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import static com.sonatype.insight.brain.security.FIPSConfig.getFipsCertificateCNX500NameOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsCertificateSignatureProviderOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsCertificateSignatureValidityYearsOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsCertificateSignerAlgorithmOrDefault;
import static com.sonatype.insight.brain.security.FIPSConfig.getFipsCertificateSignerProviderOrDefault;
import static java.lang.System.currentTimeMillis;
import static java.util.Calendar.YEAR;

public class CertificateFactory
{
  public static final String CN_X500_NAME = "CN=SAML KeyStore";

  public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

  public static final int SIGNATURE_VALIDITY_YEARS = 10;

  private CertificateFactory() {
    // prevent instantiation
  }

  /**
   * Creates an X.509 certificate, using the provided {@link KeyPair}, based on the environment configuration. One such
   * configuration is the checking of FIPS mode. If FIPS mode is enabled, the certificate is created using the FIPS
   * permitted values.
   *
   * @param keyPair - the {@link KeyPair} to use for signing the certificate
   * @return an array of X.509 certificates
   * @throws CertificateException - if an error occurs while creating the certificate
   * @throws OperatorCreationException - if an error occurs while creating the content signer
   */
  public static X509Certificate[] createCertificate(
      final KeyPair keyPair) throws CertificateException, OperatorCreationException
  {
    if (FIPSModeDetector.isEnabled()) {
      return createBcFipsCertificate(keyPair);
    }

    return createX509Certificate(keyPair);
  }

  /**
   * Creates an X.509 certificate using the provided {@link KeyPair} using the FIPS permitted values.
   *
   * @param keyPair - the {@link KeyPair} to use for signing the certificate
   * @return an array of X.509 certificates
   * @throws CertificateException - if an error occurs while creating the certificate
   * @throws OperatorCreationException - if an error occurs while creating the content signer
   */
  public static X509Certificate[] createBcFipsCertificate(
      final KeyPair keyPair) throws OperatorCreationException, CertificateException
  {
    X500Name issuerAndSubject = new X500Name(getFipsCertificateCNX500NameOrDefault());

    X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
        issuerAndSubject,
        BigInteger.valueOf(currentTimeMillis()),
        new Date(),
        dateXYearsFromNow(getFipsCertificateSignatureValidityYearsOrDefault()),
        issuerAndSubject,
        SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));

    ContentSigner contentSigner = new JcaContentSignerBuilder(getFipsCertificateSignerAlgorithmOrDefault())
        .setProvider(getFipsCertificateSignerProviderOrDefault())
        .build(keyPair.getPrivate());

    X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

    X509Certificate x509Certificate = new JcaX509CertificateConverter()
        .setProvider(getFipsCertificateSignatureProviderOrDefault())
        .getCertificate(certificateHolder);

    return new X509Certificate[]{x509Certificate};
  }

  /**
   * Creates an X.509 certificate using the provided {@link KeyPair}.
   *
   * @param keyPair - the {@link KeyPair} to use for signing the certificate
   * @return an array of X.509 certificates
   * @throws CertificateException - if an error occurs while creating the certificate
   * @throws OperatorCreationException - if an error occurs while creating the content signer
   */
  public static X509Certificate[] createX509Certificate(
      final KeyPair keyPair) throws OperatorCreationException, CertificateException
  {
    X500Name issuerAndSubject = new X500Name(CN_X500_NAME);

    X509v1CertificateBuilder certificateBuilder = new X509v1CertificateBuilder(
        issuerAndSubject,
        BigInteger.valueOf(currentTimeMillis()),
        new Date(),
        getDateOfCertificateExpiry(),
        issuerAndSubject,
        SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));

    ContentSigner contentSigner = new JcaContentSignerBuilder(getSignatureAlgorithm()).build(keyPair.getPrivate());
    X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);
    return new X509Certificate[]{new JcaX509CertificateConverter().getCertificate(certificateHolder)};
  }

  /**
   * Returns the signature algorithm to use for signing the certificate based on the environment configuration. If FIPS
   * mode is enabled, the FIPS permitted values are used.
   *
   * @return the signature algorithm to use for signing the certificate
   */
  public static String getSignatureAlgorithm() {
    if (FIPSModeDetector.isEnabled()) {
      return getFipsCertificateSignerAlgorithmOrDefault();
    }
    return SIGNATURE_ALGORITHM;
  }

  private static Date getDateOfCertificateExpiry() {
    return dateXYearsFromNow(SIGNATURE_VALIDITY_YEARS);
  }

  private static Date dateXYearsFromNow(final int years) {
    Calendar c = Calendar.getInstance();
    c.add(YEAR, years);
    return c.getTime();
  }
}
