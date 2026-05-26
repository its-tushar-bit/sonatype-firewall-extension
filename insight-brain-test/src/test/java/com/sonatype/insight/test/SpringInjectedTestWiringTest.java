/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import java.util.Set;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.AopTestUtils;

@ContextConfiguration(classes = SpringInjectedTestWiringTest.TestConfig.class)
public class SpringInjectedTestWiringTest
    extends SpringInjectedTest
{
  @Inject
  private PlainService plainService;

  @Inject
  private ProxiedService proxiedService;

  @Inject
  private SpyCompatibleService spyCompatibleService;

  @Inject
  private SpyCompatibleDependency constructorInjectedDependency;

  @Inject
  private ListenerAwareService listenerAwareService;

  @Inject
  private RealTestListener realTestListener;

  private TestDependency mockDependency = new TestDependency("mock");

  private SpyCompatibleDependency spyConstructorInjectedDependency = new SpyCompatibleDependency("legacy-spy");

  private TestListener mockTestListener = new LegacyMockTestListener();

  @Override
  protected boolean preserveAopProxies() {
    return true;
  }

  @Test
  public void shouldWireLegacyMockFieldsByNameIntoPlainInjectedBeans() {
    assertThat(plainService.getDependency()).isSameAs(mockDependency);
  }

  @Test
  public void shouldWireLegacyMockFieldsIntoProxiedInjectedBeansWhenPreservingProxies() throws Exception {
    ProxiedService target = AopTestUtils.getUltimateTargetObject(proxiedService);

    assertThat(target.getDependency()).isSameAs(mockDependency);
  }

  @Test
  public void shouldAssignLegacySpyFieldToMatchingInjectedFieldAndDependentBean() {
    assertThat(constructorInjectedDependency).isSameAs(spyConstructorInjectedDependency);
    assertThat(spyCompatibleService.getDependency()).isSameAs(spyConstructorInjectedDependency);
  }

  @Test
  public void shouldReplaceConstructorInjectedListenerCollectionsWithLegacyMocks() {
    listenerAwareService.notifyListeners();

    assertThat(listenerAwareService.getListeners()).containsExactly(mockTestListener);
    assertThat(realTestListener.getInvocations()).isZero();
  }

  @Configuration
  static class TestConfig
  {
    @Bean
    public PlainService plainService() {
      return new PlainService(new TestDependency("plain-real"));
    }

    @Bean
    public ProxiedService proxiedService() {
      ProxiedService target = new ProxiedService(new TestDependency("proxied-real"));
      ProxyFactory proxyFactory = new ProxyFactory(target);
      proxyFactory.setProxyTargetClass(true);
      proxyFactory.addAdvice((MethodInterceptor) invocation -> invocation.proceed());
      return (ProxiedService) proxyFactory.getProxy();
    }

    @Bean
    public SpyCompatibleDependency constructorInjectedDependency() {
      return new SpyCompatibleDependency("real-constructor-dependency");
    }

    @Bean
    public SpyCompatibleService spyCompatibleService(final SpyCompatibleDependency constructorInjectedDependency) {
      return new SpyCompatibleService(constructorInjectedDependency);
    }

    @Bean
    public RealTestListener realTestListener() {
      return new RealTestListener();
    }

    @Bean
    public ListenerAwareService listenerAwareService(final Set<TestListener> testListeners) {
      return new ListenerAwareService(testListeners);
    }
  }

  static class PlainService
  {
    private TestDependency dependency;

    PlainService(final TestDependency dependency) {
      this.dependency = dependency;
    }

    TestDependency getDependency() {
      return dependency;
    }
  }

  static class ProxiedService
  {
    private TestDependency dependency;

    ProxiedService(final TestDependency dependency) {
      this.dependency = dependency;
    }

    TestDependency getDependency() {
      return dependency;
    }
  }

  static class TestDependency
  {
    private final String value;

    TestDependency(final String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }
  }

  static class SpyCompatibleDependency
  {
    private final String value;

    SpyCompatibleDependency(final String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }
  }

  static class SpyCompatibleService
  {
    private final SpyCompatibleDependency constructorInjectedDependency;

    SpyCompatibleService(final SpyCompatibleDependency constructorInjectedDependency) {
      this.constructorInjectedDependency = constructorInjectedDependency;
    }

    SpyCompatibleDependency getDependency() {
      return constructorInjectedDependency;
    }
  }

  interface TestListener
  {
    void onChange();
  }

  static class RealTestListener
      implements TestListener
  {
    private int invocations;

    @Override
    public void onChange() {
      invocations++;
    }

    int getInvocations() {
      return invocations;
    }
  }

  static class ListenerAwareService
  {
    private final Set<TestListener> listeners;

    ListenerAwareService(final Set<TestListener> listeners) {
      this.listeners = listeners;
    }

    Set<TestListener> getListeners() {
      return listeners;
    }

    void notifyListeners() {
      listeners.forEach(TestListener::onChange);
    }
  }

  static class LegacyMockTestListener
      implements TestListener
  {
    private int invocations;

    @Override
    public void onChange() {
      invocations++;
    }

    int getInvocations() {
      return invocations;
    }
  }
}
