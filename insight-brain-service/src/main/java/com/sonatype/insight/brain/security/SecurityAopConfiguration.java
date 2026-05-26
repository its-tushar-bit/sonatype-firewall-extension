/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.api.v2.HasFeatureMethodInterceptor;
import org.apache.shiro.aop.AnnotationResolver;
import org.apache.shiro.aop.DefaultAnnotationResolver;
import org.aspectj.lang.Aspects;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration providing beans for Security AOP components.
 *
 * <p>
 * Registers Shiro-based AOP interceptors as Spring beans. The actual method interception
 * is handled by AspectJ compile-time weaving of {@code @Aspect} beans ({@code AuthorizeAspect},
 * {@code AuthzFilterAspect}, {@code HasFeatureAspect}, {@code AnonymousWithFeatureAspect}).
 * </p>
 *
 * <p>
 * Aspect singletons are managed by AspectJ CTW. This configuration exposes them as Spring beans
 * via {@code Aspects.aspectOf()}, allowing Spring to inject interceptor dependencies via
 * {@code @Inject} on the aspect setter methods.
 * </p>
 *
 * <p>
 * This replaces the old Guice-based {@code SecurityAopModule} which used
 * {@code ShiroAopModule.bindShiroInterceptor()} for each interceptor.
 * </p>
 */
@Configuration
public class SecurityAopConfiguration
{

  @Bean
  public AnnotationResolver annotationResolver() {
    return new DefaultAnnotationResolver();
  }

  @Bean
  public AuthorizationChecker authorizationChecker(final ListableBeanFactory beanFactory) {
    AuthorizationChecker authzChecker = new AuthorizationChecker();
    authzChecker.injectBeanFactory(beanFactory);
    return authzChecker;
  }

  @Bean
  public SmartInitializingSingleton authorizationCheckerValidator(final AuthorizationChecker authorizationChecker) {
    return authorizationChecker::validateDaoDependencies;
  }

  // -- Interceptor beans --

  @Bean
  public AuthorizeMethodInterceptor authorizeMethodInterceptor(
      final AnnotationResolver annotationResolver,
      final AuthorizationChecker authorizationChecker)
  {
    return new AuthorizeMethodInterceptor(annotationResolver, authorizationChecker);
  }

  @Bean
  public AuthzFilterMethodInterceptor authzFilterMethodInterceptor(
      final AnnotationResolver annotationResolver,
      final AuthorizationChecker authorizationChecker)
  {
    return new AuthzFilterMethodInterceptor(annotationResolver, authorizationChecker);
  }

  @Bean
  public HasFeatureMethodInterceptor hasFeatureMethodInterceptor(final AnnotationResolver annotationResolver) {
    return new HasFeatureMethodInterceptor(annotationResolver);
  }

  @Bean
  public AnonymousWithFeatureMethodInterceptor anonymousWithFeatureMethodInterceptor(
      final AnnotationResolver annotationResolver)
  {
    return new AnonymousWithFeatureMethodInterceptor(annotationResolver);
  }

  // -- Aspect beans (CTW singletons exposed to Spring for @Inject wiring) --
  //
  // Interceptors are wired explicitly here rather than relying on @Inject setter processing
  // because the test configuration marks all beans as lazy-init. CTW aspect singletons exist
  // and fire regardless of whether Spring has initialized them, so if the aspect bean is never
  // eagerly resolved the @Inject setter is never called and the interceptor stays null -
  // causing security checks to be silently skipped.

  @Bean
  public AuthorizeAspect authorizeAspect(final AuthorizeMethodInterceptor interceptor) {
    AuthorizeAspect aspect = Aspects.aspectOf(AuthorizeAspect.class);
    aspect.setInterceptor(interceptor);
    return aspect;
  }

  @Bean
  public AuthzFilterAspect authzFilterAspect(final AuthzFilterMethodInterceptor interceptor) {
    AuthzFilterAspect aspect = Aspects.aspectOf(AuthzFilterAspect.class);
    aspect.setInterceptor(interceptor);
    return aspect;
  }

  @Bean
  public HasFeatureAspect hasFeatureAspect(final HasFeatureMethodInterceptor interceptor) {
    HasFeatureAspect aspect = Aspects.aspectOf(HasFeatureAspect.class);
    aspect.setInterceptor(interceptor);
    return aspect;
  }

  @Bean
  public AnonymousWithFeatureAspect anonymousWithFeatureAspect(
      final AnonymousWithFeatureMethodInterceptor interceptor)
  {
    AnonymousWithFeatureAspect aspect = Aspects.aspectOf(AnonymousWithFeatureAspect.class);
    aspect.setInterceptor(interceptor);
    return aspect;
  }
}
