/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;

import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.google.inject.Binder;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Support class for tests of Sisu components.
 */
public class AbstractComponentTest
    extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TestName testName = new TestName();

  protected static final String USERNAME = "testuser";

  protected Subject subject;

  private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  @Before
  public void setUpDefaultLicenseThreatGroups() {
    // Make sure the default LTGs are created on the root organization
    licenseThreatGroupDAO.createDefaultLicenseThreatGroups();
  }

  @After
  public void tearDownDefaultLicenseThreatGroups() {
    // Delete the default LTGs from the root organization
    licenseThreatGroupDAO.deleteDefaultLicenseThreatGroups();
  }

  @Before
  public void setUpSecurity() {
    subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn(new UserPrincipal(USERNAME, "Test User", true));
    ThreadContext.bind(subject);
  }

  @After
  public void tearDownSecurity() {
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
  }

  @Override
  public void configure(Binder binder) {
    InsightConfig config = new InsightConfig();
    try {
      config.setSonatypeWork(tempDir.newFolder("sonatype-work").getAbsolutePath());
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
    config.setHdsUrl("http://unknownhost");
    customizeConfig(config);
    binder.bind(InsightConfig.class).toInstance(config);
    binder.bind(ProductLicenseManager.class).to(TestProductLicenseManager.class);
    binder.bind(LicenseFingerprinter.class).to(TestLicenseFingerprinter.class);
  }

  protected void customizeConfig(InsightConfig config) {
    // hook for tests to tweak config before components grab it
  }
}
