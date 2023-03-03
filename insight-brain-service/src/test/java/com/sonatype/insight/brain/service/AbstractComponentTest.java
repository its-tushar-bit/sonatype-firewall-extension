/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.dataaccess.DatamartUpdaterState;
import com.sonatype.insight.brain.dataaccess.PerpetualLockDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.git.SourceControlInstanceManager;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseDetailsCache;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicenseDetailsCache;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.scheduler.TestQuartzJobStoreTx;
import com.sonatype.insight.brain.scheduler.TestTaskScheduler;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.testing.BrainInjectedTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import io.dropwizard.lifecycle.Managed;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.mockito.MockMakers;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Support class for tests of Sisu components.
 */
public class AbstractComponentTest
    extends BrainInjectedTest
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Rule
  public DatamartUpdaterState datamartUpdaterState = new DatamartUpdaterState();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TestName testName = new TestName();

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  private static final PerpetualLockDAO perpetualLockDAO = new PerpetualLockDAO();

  protected static final String USERNAME = "testuser";

  protected Subject subject;

  private SecurityManager securityManager;

  private final Collection<Managed> managedComponents = new ArrayList<>();

  @Before
  public final void beforeTest() {
    log.info("Before: {}", testName.getMethodName());
    setUpTestLicenseThreatGroups();
    setUpSecurity();
  }

  @After
  public final void afterTest() {
    log.info("After: {}", testName.getMethodName());
    releaseScmPerpetualLock();
    stopManagedComponents();
    tearDownSecurity();
    resetBaseUrl();
    resetAccessAllowlist();
  }

  public String getBaseUrl() {
    return new SystemConfigurationPropertyDAO().get(SystemConfigurationProperty.BASE_URL);
  }

  public void setBaseUrl(String baseUrl) {
    setBaseUrl(baseUrl, false);
  }

  public void setBaseUrl(String baseUrl, boolean forceBaseUrl) {
    ApiConfigurationService service = lookup(ApiConfigurationService.class);
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, baseUrl);
    properties.put(SystemConfigurationProperty.FORCE_BASE_URL, forceBaseUrl);
    service.setConfigurationNoAuthz(properties);
    service.applyConfigurationToClients(properties.keySet());
  }

  public void resetBaseUrl() {
    ApiConfigurationService service = lookup(ApiConfigurationService.class);
    Set<String> propertyNames =
        ImmutableSet.of(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL);
    service.deleteConfigurationNoAuthz(propertyNames);
    service.applyConfigurationToClients(propertyNames);
  }

  public void resetAccessAllowlist() {
    ApiConfigurationService service = lookup(ApiConfigurationService.class);
    Set<String> propertyNames = ImmutableSet.of(SystemConfigurationProperty.ACCESS_ALLOWLIST);
    service.deleteConfigurationNoAuthz(propertyNames);
    service.applyConfigurationToClients(propertyNames);
  }

  private void releaseScmPerpetualLock() {
    String perpetualLockId = SourceControlInstanceManager.SOURCE_CONTROL_ACCESS_LOCK;
    PerpetualLock perpetualLock = perpetualLockDAO.getPerpetualLockById(perpetualLockId);
    if (perpetualLock != null) {
      perpetualLockDAO.releasePerpetualLockForOwner(perpetualLockId, perpetualLock.getOwner());
    }
  }

  protected void setUpTestLicenseThreatGroups() {
    // Make sure the test LTGs are created on the root organization
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
  }

  protected String getMockMaker() {
    return MockMakers.INLINE;
  }

  protected void setUpSecurity() {
    subject = mock(Subject.class, withSettings().mockMaker(getMockMaker()));
    lenient().when(subject.getPrincipal()).thenReturn(new UserPrincipal(USERNAME, "Test User", InternalRealm.ID));
    securityManager = mock(SecurityManager.class, withSettings().mockMaker(getMockMaker()));
    lenient().when(securityManager.createSubject(any(SubjectContext.class))).thenReturn(subject);
    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);
  }

  protected void tearDownSecurity() {
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
  }

  private void bindManagedComponentObserver(Binder binder) {
    InjectionListener<Object> injectionListener = injectee -> managedComponents.add((Managed) injectee);
    TypeListener typeListener = new TypeListener()
    {
      @Override
      public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
        encounter.register(injectionListener);
      }
    };
    binder.bindListener(new AbstractMatcher<TypeLiteral<?>>()
    {
      @Override
      public boolean matches(TypeLiteral<?> typeLiteral) {
        return Managed.class.isAssignableFrom(typeLiteral.getRawType());
      }
    }, typeListener);
  }

  private void stopManagedComponents() {
    // avoid leaking resources like thread pools
    for (Managed managedComponent : managedComponents) {
      try {
        managedComponent.stop();
      }
      catch (Exception ignored) {
        // irrelevant
      }
    }
  }

  @Override
  public void configure(Binder binder) {
    bindManagedComponentObserver(binder);
    InsightConfig config = new InsightConfig();
    try {
      config.setSonatypeWork(tempDir.newFolder("sonatype-work").getAbsolutePath());
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.HDS_URL, "http://unknownhost");
    customizeConfig(config);
    binder.bind(InsightConfig.class).toInstance(config);
    binder.bind(ProductLicense.class).to(TestProductLicense.class);
    binder.bind(ProductLicenseDetailsCache.class).to(TestProductLicenseDetailsCache.class);
    binder.bind(ProductLicenseManager.class).to(TestProductLicenseManager.class);
    binder.bind(LicenseFingerprinter.class).to(TestLicenseFingerprinter.class);
    binder.bind(QuartzJobStoreTX.class).to(TestQuartzJobStoreTx.class);
    binder.bind(TaskScheduler.class).to(TestTaskScheduler.class);
    binder.bind(TelemetryId.class).toInstance(mock(TelemetryId.class, withSettings().mockMaker(getMockMaker())));

    super.configure(binder);
  }

  protected void customizeConfig(@SuppressWarnings("unused") InsightConfig config) {
    // hook for tests to tweak config before components grab it
  }

  public void createJiraConfiguration(Map<String, Object> customFields) {
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = "http://url";
    dto.customFields = customFields;
    ApiJiraConfigurationService jiraConfigurationService = lookup(ApiJiraConfigurationService.class);
    jiraConfigurationService.setConfigurationNoAuthz(JsonUtils.asTree(dto));
    jiraConfigurationService.applyJiraConfigurationToClients();
  }

  public void setHdsUrl(String hdsUrl) {
    ApiConfigurationService configurationService = lookup(ApiConfigurationService.class);
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.HDS_URL, hdsUrl);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.HDS_URL);
  }

  public void testCallable_DisallowConcurrentExecution(Callable<Void> callable, Consumer<Answer<Void>> answerConsumer)
      throws Exception
  {
    testCallable_ConcurrentExecution(callable, answerConsumer, false);
  }

  public void testCallable_AllowConcurrentExecution(Callable<Void> callable, Consumer<Answer<Void>> answerConsumer)
      throws Exception
  {
    testCallable_ConcurrentExecution(callable, answerConsumer, true);
  }

  public void testCallable_ConcurrentExecution(
      Callable<Void> callable,
      Consumer<Answer<Void>> answerConsumer,
      boolean isAllowed)
      throws Exception
  {
    CountDownLatch started = new CountDownLatch(2);
    CountDownLatch block = new CountDownLatch(1);

    answerConsumer.accept(invocation -> {
      started.countDown();
      block.await();
      return null;
    });

    CountDownLatch oneFinished = new CountDownLatch(1);
    AtomicReference<Exception> oneException = new AtomicReference<>();
    Thread threadOne = new Thread(() -> {
      try {
        callable.call();
      }
      catch (Exception e) {
        oneException.set(e);
      }
      finally {
        oneFinished.countDown();
      }
    });

    CountDownLatch twoFinished = new CountDownLatch(1);
    AtomicReference<Exception> twoException = new AtomicReference<>();
    Thread threadTwo = new Thread(() -> {
      try {
        callable.call();
      }
      catch (Exception e) {
        twoException.set(e);
      }
      finally {
        twoFinished.countDown();
      }
    });

    threadOne.start();
    await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(started.getCount()).isEqualTo(1));

    threadTwo.start();
    if (isAllowed) {
      await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(started.getCount()).isEqualTo(0));
    }
    else {
      await().pollDelay(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(started.getCount()).isEqualTo(1));
    }

    block.countDown();

    assertThat(oneFinished.await(2, TimeUnit.SECONDS)).isTrue();
    assertThat(oneException).hasValue(null);
    assertThat(twoFinished.await(2, TimeUnit.SECONDS)).isTrue();
    assertThat(twoException).hasValue(null);
  }
}
