/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.certificate;

import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.operator.OperatorCreationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sonatype.insight.brain.security.TestEnvironmentVariables;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_CERTIFICATE_CN_X500_NAME_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_CERTIFICATE_SIGNATURE_VALIDITY_YEARS_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_CERTIFICATE_SIGNER_ALGORITHM_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.keypair.KeyPairFactory.generateRSAKeyPair;
import static com.sonatype.insight.brain.security.certificate.CertificateFactory.CN_X500_NAME;
import static com.sonatype.insight.brain.security.certificate.CertificateFactory.SIGNATURE_ALGORITHM;
import static com.sonatype.insight.brain.security.certificate.CertificateFactory.SIGNATURE_VALIDITY_YEARS;
import static com.sonatype.insight.brain.security.certificate.CertificateFactory.createBcFipsCertificate;
import static com.sonatype.insight.brain.security.certificate.CertificateFactory.createCertificate;
import static com.sonatype.insight.brain.security.certificate.CertificateFactory.createX509Certificate;
import static java.util.Calendar.YEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class CertificateFactoryTest
{
  private final TestEnvironmentVariables environmentVariables = new TestEnvironmentVariables();

  @BeforeEach
  public void setUp() {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");
  }

  @AfterEach
  public void restoreEnvironmentVariables() {
    environmentVariables.restore();
  }

  @AfterEach
  public void tearDown() {
    removeBouncyCastleFipsProvider();
  }

  @Test
  public void testCreateX509Certificate() throws Exception {
    X509Certificate[] certificates = createX509Certificate(generateRSAKeyPair());
    assertThat(certificates).hasSize(1);

    X509Certificate certificate = certificates[0];
    assertThat(certificate.getSigAlgName()).isEqualTo(SIGNATURE_ALGORITHM);
    assertThat(certificate.getSubjectX500Principal().getName()).isEqualTo(CN_X500_NAME);
    assertThat(certificate.getIssuerX500Principal().getName()).isEqualTo(CN_X500_NAME);
    assertThat(certificate.getSigAlgName()).isEqualTo(SIGNATURE_ALGORITHM);
    assertValidityPeriod(certificate, SIGNATURE_VALIDITY_YEARS);
  }

  @Test
  public void testCreateBcFipsCertificate() throws Exception {
    insertBouncyCastleFipsProvider();

    X509Certificate[] certificates = createBcFipsCertificate(generateRSAKeyPair());
    assertThat(certificates).hasSize(1);

    X509Certificate certificate = certificates[0];
    assertThat(certificate.getSubjectX500Principal().getName()).isEqualTo("CN=SAML KeyStore");
    assertThat(certificate.getIssuerX500Principal().getName()).isEqualTo("CN=SAML KeyStore");
    assertThat(certificate.getSigAlgName()).isEqualTo("SHA256WITHRSA");
    assertValidityPeriod(certificate, 10);
  }

  @Test
  public void testCreateCertificate() throws Exception {
    X509Certificate[] certificates = createCertificate(generateRSAKeyPair());
    assertThat(certificates).hasSize(1);

    X509Certificate certificate = certificates[0];
    assertThat(certificate.getSigAlgName()).isEqualTo(SIGNATURE_ALGORITHM);
    assertThat(certificate.getSubjectX500Principal().getName()).isEqualTo(CN_X500_NAME);
    assertThat(certificate.getIssuerX500Principal().getName()).isEqualTo(CN_X500_NAME);
    assertThat(certificate.getSigAlgName()).isEqualTo(SIGNATURE_ALGORITHM);
    assertValidityPeriod(certificate, SIGNATURE_VALIDITY_YEARS);
  }

  @Test
  public void testCreateCertificate_WithFipsEnabled() throws Exception {
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    X509Certificate[] certificates = createCertificate(generateRSAKeyPair());
    assertThat(certificates).hasSize(1);

    X509Certificate certificate = certificates[0];
    assertThat(certificate.getSubjectX500Principal().getName()).isEqualTo("CN=SAML KeyStore");
    assertThat(certificate.getIssuerX500Principal().getName()).isEqualTo("CN=SAML KeyStore");
    assertThat(certificate.getSigAlgName()).isEqualTo("SHA256WITHRSA");
    assertValidityPeriod(certificate, 10);
  }

  @Test
  public void testCreateCertificate_WithFipsEnabled_And_WithDefaultsOverriddenByEnvironmentVariables() throws Exception {
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    environmentVariables.set(FIPS_CERTIFICATE_CN_X500_NAME_ENV, "CN=AAA");
    environmentVariables.set(FIPS_CERTIFICATE_SIGNER_ALGORITHM_ENV, "SHA256WITHRSAENCRYPTION");
    environmentVariables.set(FIPS_CERTIFICATE_SIGNATURE_VALIDITY_YEARS_ENV, "5");

    X509Certificate[] certificates = createCertificate(generateRSAKeyPair());
    assertThat(certificates).hasSize(1);

    X509Certificate certificate = certificates[0];
    assertThat(certificate.getSubjectX500Principal().getName()).isEqualTo("CN=AAA");
    assertThat(certificate.getIssuerX500Principal().getName()).isEqualTo("CN=AAA");
    assertThat(certificate.getSigAlgName()).isEqualTo("SHA256WITHRSA");
    assertValidityPeriod(certificate, 5);
  }

  @Test
  public void testCreateCertificate_WithFipsEnabled_Throws_OperatorCreationException() {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    assertThatThrownBy(() -> createCertificate(generateRSAKeyPair()))
        .isInstanceOf(OperatorCreationException.class)
        .hasMessageContaining("cannot create signer: no such provider: BCFIPS");
  }

  private void assertValidityPeriod(final X509Certificate certificate, final int years) {
    Date notBefore = certificate.getNotBefore();
    Date notAfter = certificate.getNotAfter();

    Calendar c = Calendar.getInstance();
    c.setTime(notBefore);
    c.add(YEAR, years);

    // allow a margin of one minute
    assertThat(c.getTime().getTime() - notAfter.getTime()).isLessThan(60L * 1000);
  }
}
