/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicenseResource;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.google.inject.AbstractModule;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.multipart.ByteArrayPartSource;
import com.ning.http.multipart.FilePart;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;

public abstract class AbstractLicenseTest
    extends AbstractBrainServiceTest
{
  // by default license is always valid, to override, simply uninstall the license
  private final TestProductLicenseManager licenseManager = new TestProductLicenseManager(true);

  private final TestLicenseFingerprinter licenseFingerprinter = new TestLicenseFingerprinter();

  public AbstractLicenseTest() {
    this(false /* disableSecurity */);
  }

  // To be removed when we implement auth for clients
  public AbstractLicenseTest(boolean disableSecurity) {
    super(disableSecurity);
  }

  @Override
  protected void configureBrain(TestInsightBrainService brain) {
    super.configureBrain(brain);
    brain.addModule(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(ProductLicenseManager.class).toInstance(licenseManager);
        bind(LicenseFingerprinter.class).toInstance(licenseFingerprinter);
      }
    });
  }

  protected String installLicenseAsIE() throws Exception {

    Map<String, String> queryParams = new LinkedHashMap<String, String>();
    queryParams.put("forceSuccess", "true");
    return doInstallLicense(queryParams);
  }

  protected String installLicense() throws Exception {
    return doInstallLicense(null);
  }

  private String doInstallLicense(Map<String, String> queryParams) throws Exception {
    InputStream license = AbstractLicenseTest.class.getResourceAsStream("/productlicense/license.lic");
    try {
      AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(getServiceURL());
      builder.addBodyPart(new FilePart("file", new ByteArrayPartSource(null, IOUtil.toByteArray(license))));
      if (queryParams != null) {
        for (String key : queryParams.keySet()) {
          builder.addQueryParameter(key, queryParams.get(key));
        }
      }
      Response response = AuthedRestAccess.execute(builder);
      assertResponseStatus(200, response);

      Assert.assertTrue(licenseManager.isValid());

      return response.getResponseBody();
    }
    finally {
      IOUtil.close(license);
    }
  }

  protected void uninstallLicense() throws Exception {
    AuthedRestAccess.delete(getServiceURL());

    Assert.assertFalse(licenseManager.isValid());
  }

  private String getServiceURL() {
    return getRestBaseUrl() + ProductLicenseResource.SERVICE_PATH;
  }

  protected void setEnforcementPoints(CLMEnforcementPoint... enforcementPoints) throws Exception {
    licenseManager.setEnforcementPoints(enforcementPoints);
    installLicense();
  }

  protected void setApplicationLimit(int applicationLimit) throws Exception {
    licenseManager.setApplicationLimit(applicationLimit);
    installLicense();
  }

  protected void setLicenseFingerprint(String licenseFingerprint) throws Exception {
    licenseFingerprinter.setDummyLicenseFingerprint(licenseFingerprint);
    installLicense();
  }

  protected String getLicenseFingerprint() {
    return licenseFingerprinter.calculate();
  }

  protected TestProductLicenseManager getLicenseManager() {
    return licenseManager;
  }
}
