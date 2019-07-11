/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicense;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.google.inject.Binder;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.util.ThreadContext;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Support class for tests of Sisu components.
 */
public class AbstractComponentTest
    extends InjectedTest
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TestName testName = new TestName();

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  protected static final String USERNAME = "testuser";

  @Mock
  protected Subject subject;

  @Mock
  private SecurityManager securityManager;

  @Before
  public final void beforeTest() {
    log.info("Before: {}", testName.getMethodName());
    setUpTestLicenseThreatGroups();
    setUpSecurity();
  }

  @After
  public final void afterTest() {
    log.info("After: {}", testName.getMethodName());
    tearDownSecurity();
  }

  protected void setUpTestLicenseThreatGroups() {
    // Make sure the test LTGs are created on the root organization
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
  }

  protected void setUpSecurity() {
    lenient().when(subject.getPrincipal()).thenReturn(new UserPrincipal(USERNAME, "Test User", true));
    lenient().when(securityManager.createSubject(any(SubjectContext.class))).thenReturn(subject);
    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);
  }

  protected void tearDownSecurity() {
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
      throw new UncheckedIOException(e);
    }
    config.setHdsUrl("http://unknownhost");
    customizeConfig(config);
    binder.bind(InsightConfig.class).toInstance(config);
    binder.bind(CLMLicenseManager.class).asEagerSingleton();
    binder.bind(ProductLicense.class).to(TestProductLicense.class);
    binder.bind(ProductLicenseManager.class).to(TestProductLicenseManager.class);
    binder.bind(LicenseFingerprinter.class).to(TestLicenseFingerprinter.class);
  }

  protected void customizeConfig(@SuppressWarnings("unused") InsightConfig config) {
    // hook for tests to tweak config before components grab it
  }
}
