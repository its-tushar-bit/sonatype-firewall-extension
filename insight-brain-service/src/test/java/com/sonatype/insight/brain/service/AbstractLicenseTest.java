/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicenseResource;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.google.inject.AbstractModule;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
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
      FormDataMultiPart form = new FormDataMultiPart();
      form.bodyPart(new FormDataBodyPart("file", license, MediaType.APPLICATION_OCTET_STREAM_TYPE));

      WebResource resource = Client.create().resource(getServiceURL());
      if (queryParams != null) {
        for (String key : queryParams.keySet()) {
          resource = resource.queryParam(key, queryParams.get(key));
        }
      }

      String result = resource.type(MediaType.MULTIPART_FORM_DATA).post(String.class, form);

      Assert.assertTrue(licenseManager.isValid());

      return result;
    }
    finally {
      IOUtil.close(license);
    }
  }

  protected void uninstallLicense() throws Exception {
    WebResource resource = Client.create().resource(getServiceURL());

    resource.delete();

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
