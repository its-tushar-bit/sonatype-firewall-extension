/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.InputStream;
import java.util.Collections;
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

  @Override
  protected void configureBrain(TestInsightBrainService brain) {
    super.configureBrain(brain);
    brain.addModule(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(ProductLicenseManager.class).toInstance(getTestProductLicenseManager());
        bind(LicenseFingerprinter.class).toInstance(licenseFingerprinter);
      }
    });
  }

  protected TestProductLicenseManager getTestProductLicenseManager() {
    return licenseManager;
  }

  protected String installLicense() throws Exception {
    Response response = uploadLicense(null);
    assertResponseStatus(200, response);

    Assert.assertTrue(getTestProductLicenseManager().isValid());

    return response.getResponseBody();
  }

  protected Response installLicense(boolean forceSuccess) throws Exception {
    return uploadLicense(Collections.singletonMap("forceSuccess", Boolean.toString(forceSuccess)));
  }

  protected Response uploadLicense(Map<String, String> queryParams, String username, String password) throws Exception {
    InputStream license = AbstractLicenseTest.class.getResourceAsStream("/productlicense/license.lic");
    try {
      AsyncHttpClient.BoundRequestBuilder builder = AuthedRestAccess.getClient().preparePost(getServiceURL());
      builder.addBodyPart(new FilePart("file", new ByteArrayPartSource(null, IOUtil.toByteArray(license))));
      if (queryParams != null) {
        for (String key : queryParams.keySet()) {
          builder.addQueryParameter(key, queryParams.get(key));
        }
      }
      if (username == null) {
        return AuthedRestAccess.execute(builder);
      }
      else {
        return AuthedRestAccess.execute(builder, username, password);
      }
    }
    finally {
      IOUtil.close(license);
    }
  }

  private Response uploadLicense(Map<String, String> queryParams) throws Exception {
    return uploadLicense(queryParams, null /* username */, null /* password */);
  }

  protected void uninstallLicense() throws Exception {
    AuthedRestAccess.delete(getServiceURL());

    Assert.assertFalse(getTestProductLicenseManager().isValid());
  }

  private String getServiceURL() {
    return getRestBaseUrl() + ProductLicenseResource.SERVICE_PATH;
  }

  protected void setEnforcementPoints(CLMEnforcementPoint... enforcementPoints) throws Exception {
    getTestProductLicenseManager().setEnforcementPoints(enforcementPoints);
    installLicense();
  }

  protected void setApplicationLimit(int applicationLimit) throws Exception {
    getTestProductLicenseManager().setApplicationLimit(applicationLimit);
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
