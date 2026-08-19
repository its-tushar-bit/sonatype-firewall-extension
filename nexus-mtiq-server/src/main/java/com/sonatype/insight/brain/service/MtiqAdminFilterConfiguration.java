/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.api.admin.authorization.JwtHttpAuthorizationFilter;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkAuth0Provider;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkLocalProvider;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkProvider;
import com.sonatype.insight.brain.api.admin.service.MultiTenantActiveRequestCounterFilter;
import com.sonatype.insight.brain.audit.AuditFilter;
import com.sonatype.insight.brain.filter.ThrowableHandler;
import com.sonatype.insight.brain.security.ContentTypeOptionsHeaderFilter;
import com.sonatype.insight.brain.security.FrameOptionsHeaderFilter;
import com.sonatype.insight.brain.security.HstsHeaderFilter;
import com.sonatype.insight.brain.security.HttpHeaderValidatorFilter;
import com.sonatype.insight.brain.tenancy.AdminTasksTenantFilter;
import com.sonatype.insight.brain.tenancy.AdminTenantFilter;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantUrlFilter;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.spring.config.FilterOrder;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;
import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import(MtiqAdminJaxRsErrorConfiguration.class)
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
public class MtiqAdminFilterConfiguration
{
  // Admin-specific filter ordering (uses separate range from main context)
  // Shared filters (THROWABLE_HANDLER, AUDIT, etc.) reference FilterOrder constants
  private static final int ORDER_TENANT_URL = 20;

  private static final int ORDER_ADMIN_TENANT = 40;

  private static final int ORDER_ADMIN_TASKS = 50;

  private static final int ORDER_ADMIN_AUTHORIZATION = 60;

  private static final int ORDER_BASE_URL = 70;

  private static final int ORDER_HTTP_HEADER_VALIDATION = 90;

  // Admin context security headers use 100+ range
  private static final int ORDER_CONTENT_TYPE_OPTIONS = 100;

  private static final int ORDER_HSTS = 105;

  private static final int ORDER_FRAME_OPTIONS = 110;

  @Bean
  @Primary
  public MultiTenantJwkProvider multiTenantJwkProvider(
      InsightConfig insightConfig,
      MultiTenantJwkAuth0Provider multiTenantJwkAuth0Provider,
      MultiTenantJwkLocalProvider multiTenantJwkLocalProvider)
  {
    return selectMultiTenantJwkProvider(insightConfig, multiTenantJwkAuth0Provider, multiTenantJwkLocalProvider);
  }

  @Bean
  public FilterRegistrationBean<ThrowableHandler> mtiqAdminThrowableHandlerRegistration(
      final ApplicationContext applicationContext)
  {
    JaxRsExceptionMapper configuredJaxRsExceptionMapper = applicationContext.getBean(
        MtiqAdminJaxRsErrorConfiguration.CONFIGURED_JAX_RS_EXCEPTION_MAPPER_BEAN_NAME,
        JaxRsExceptionMapper.class);
    return registerFilter(new ThrowableHandler(configuredJaxRsExceptionMapper), FilterOrder.THROWABLE_HANDLER, "/*");
  }

  @Bean
  public FilterRegistrationBean<MultiTenantActiveRequestCounterFilter> mtiqAdminActiveRequestCounterFilterRegistration(
      final ObjectProvider<MultiTenantActiveRequestCounterFilter> activeRequestCounterFilterProvider,
      final ObjectProvider<ShutdownHandler> shutdownHandlerProvider,
      final ApplicationContext applicationContext)
  {
    MultiTenantActiveRequestCounterFilter activeRequestCounterFilter = resolveBean(
        applicationContext,
        "multiTenantActiveRequestCounterFilter",
        MultiTenantActiveRequestCounterFilter.class,
        activeRequestCounterFilterProvider);
    if (activeRequestCounterFilter == null) {
      ShutdownHandler shutdownHandler = shutdownHandlerProvider.getIfAvailable();
      if (shutdownHandler == null) {
        throw new IllegalStateException("ShutdownHandler is required to create the MTIQ admin active request filter");
      }
      activeRequestCounterFilter = new MultiTenantActiveRequestCounterFilter(shutdownHandler);
    }
    return registerFilter(activeRequestCounterFilter, FilterOrder.ACTIVE_REQUEST_COUNTER, "/*");
  }

  @Bean
  public FilterRegistrationBean<TenantUrlFilter> mtiqAdminTenantUrlFilterRegistration(
      final ObjectProvider<TenantUrlFilter> tenantUrlFilterProvider,
      final ObjectProvider<TenantManager> tenantManagerProvider,
      final ObjectProvider<TenantUtil> tenantUtilProvider)
  {
    TenantUrlFilter tenantUrlFilter = tenantUrlFilterProvider.getIfAvailable();
    if (tenantUrlFilter == null) {
      TenantManager tenantManager = tenantManagerProvider.getIfAvailable();
      if (tenantManager == null) {
        throw new IllegalStateException("TenantManager is required to create the MTIQ admin tenant URL filter");
      }
      TenantUtil tenantUtil = tenantUtilProvider.getIfAvailable(TenantUtil::new);
      tenantUrlFilter = new TenantUrlFilter(tenantManager, tenantUtil);
    }
    return registerFilter(tenantUrlFilter, ORDER_TENANT_URL, "/*");
  }

  @Bean
  public FilterRegistrationBean<MultiTenantServerHeaderFilter> mtiqAdminServerHeaderFilterRegistration(
      final ObjectProvider<MultiTenantServerHeaderFilter> multiTenantServerHeaderFilterProvider,
      final ObjectProvider<VersionService> versionServiceProvider)
  {
    MultiTenantServerHeaderFilter multiTenantServerHeaderFilter = multiTenantServerHeaderFilterProvider
        .getIfAvailable();
    if (multiTenantServerHeaderFilter == null) {
      VersionService versionService = versionServiceProvider.getIfAvailable();
      if (versionService == null) {
        throw new IllegalStateException("VersionService is required to create the MTIQ admin server header filter");
      }
      multiTenantServerHeaderFilter = new MultiTenantServerHeaderFilter(versionService);
    }
    return registerFilter(multiTenantServerHeaderFilter, FilterOrder.SERVER_HEADER, ServerHeaderFilter.URL_PATTERNS);
  }

  @Bean
  public FilterRegistrationBean<AdminTenantFilter> mtiqAdminTenantFilterRegistration(
      final ObjectProvider<AdminTenantFilter> adminTenantFilterProvider,
      final ObjectProvider<TenantManager> tenantManagerProvider,
      final ObjectProvider<TenantUtil> tenantUtilProvider)
  {
    AdminTenantFilter adminTenantFilter = adminTenantFilterProvider.getIfAvailable();
    if (adminTenantFilter == null) {
      TenantManager tenantManager = tenantManagerProvider.getIfAvailable();
      if (tenantManager == null) {
        throw new IllegalStateException("TenantManager is required to create the MTIQ admin tenant filter");
      }
      TenantUtil tenantUtil = tenantUtilProvider.getIfAvailable(TenantUtil::new);
      adminTenantFilter = new AdminTenantFilter(tenantManager, tenantUtil);
    }
    return registerFilter(adminTenantFilter, ORDER_ADMIN_TENANT, MtiqAdminJerseyConfiguration.ADMIN_API_SERVLET_PATH);
  }

  @Bean
  public FilterRegistrationBean<AdminTasksTenantFilter> mtiqAdminTasksTenantFilterRegistration(
      final ObjectProvider<AdminTasksTenantFilter> adminTasksTenantFilterProvider)
  {
    AdminTasksTenantFilter adminTasksTenantFilter = adminTasksTenantFilterProvider
        .getIfAvailable(AdminTasksTenantFilter::new);
    return registerFilter(adminTasksTenantFilter, ORDER_ADMIN_TASKS, "/api/admin/tenants/*", "/tasks/*");
  }

  @Bean
  public FilterRegistrationBean<JwtHttpAuthorizationFilter> mtiqAdminAuthorizationFilterRegistration(
      final ObjectProvider<JwtHttpAuthorizationFilter> jwtHttpAuthorizationFilterProvider,
      final ObjectProvider<MultiTenantJwkProvider> multiTenantJwkProviderProvider,
      final ObjectProvider<InsightConfig> insightConfigProvider,
      final ObjectProvider<MultiTenantJwkAuth0Provider> multiTenantJwkAuth0ProviderProvider,
      final ObjectProvider<MultiTenantJwkLocalProvider> multiTenantJwkLocalProviderProvider,
      final ApplicationContext applicationContext)
  {
    JwtHttpAuthorizationFilter jwtHttpAuthorizationFilter = resolveBean(
        applicationContext,
        "jwtHttpAuthorizationFilter",
        JwtHttpAuthorizationFilter.class,
        jwtHttpAuthorizationFilterProvider);
    if (jwtHttpAuthorizationFilter == null) {
      MultiTenantJwkProvider multiTenantJwkProvider =
          resolveMultiTenantJwkProvider(applicationContext, multiTenantJwkProviderProvider, insightConfigProvider,
              multiTenantJwkAuth0ProviderProvider, multiTenantJwkLocalProviderProvider);
      if (multiTenantJwkProvider == null) {
        throw new IllegalStateException("MultiTenantJwkProvider is required to create the MTIQ admin auth filter");
      }
      jwtHttpAuthorizationFilter = new JwtHttpAuthorizationFilter(multiTenantJwkProvider);
    }
    return registerFilter(jwtHttpAuthorizationFilter, ORDER_ADMIN_AUTHORIZATION,
        MtiqAdminJerseyConfiguration.ADMIN_API_SERVLET_PATH);
  }

  /**
   * Resolves a bean by trying (in order): named ancestor, named including ancestors,
   * typed ancestor, typed including ancestors, then the given ObjectProvider.
   * Returns null if not found at any level.
   */
  private <T> T resolveBean(
      ApplicationContext applicationContext,
      String beanName,
      Class<T> beanType,
      ObjectProvider<T> provider)
  {
    T bean = getNamedAncestorBean(applicationContext, beanName, beanType);
    if (bean == null) {
      bean = getNamedBeanIncludingAncestors(applicationContext, beanName, beanType);
    }
    if (bean == null) {
      bean = getAncestorBean(applicationContext, beanType);
    }
    if (bean == null) {
      bean = getBeanIncludingAncestors(applicationContext, beanType);
    }
    if (bean == null && provider != null) {
      bean = provider.getIfAvailable();
    }
    return bean;
  }

  private MultiTenantJwkProvider resolveMultiTenantJwkProvider(
      ApplicationContext applicationContext,
      ObjectProvider<MultiTenantJwkProvider> multiTenantJwkProviderProvider,
      ObjectProvider<InsightConfig> insightConfigProvider,
      ObjectProvider<MultiTenantJwkAuth0Provider> multiTenantJwkAuth0ProviderProvider,
      ObjectProvider<MultiTenantJwkLocalProvider> multiTenantJwkLocalProviderProvider)
  {
    MultiTenantJwkProvider multiTenantJwkProvider = resolveBean(
        applicationContext,
        "multiTenantJwkProvider",
        MultiTenantJwkProvider.class,
        multiTenantJwkProviderProvider);
    if (multiTenantJwkProvider != null) {
      return multiTenantJwkProvider;
    }

    InsightConfig insightConfig = getBeanIncludingAncestors(applicationContext, InsightConfig.class);
    if (insightConfig == null) {
      insightConfig = insightConfigProvider.getIfAvailable();
    }

    MultiTenantJwkAuth0Provider multiTenantJwkAuth0Provider =
        getBeanIncludingAncestors(applicationContext, MultiTenantJwkAuth0Provider.class);
    if (multiTenantJwkAuth0Provider == null) {
      multiTenantJwkAuth0Provider = multiTenantJwkAuth0ProviderProvider.getIfAvailable();
    }

    MultiTenantJwkLocalProvider multiTenantJwkLocalProvider =
        getBeanIncludingAncestors(applicationContext, MultiTenantJwkLocalProvider.class);
    if (multiTenantJwkLocalProvider == null) {
      multiTenantJwkLocalProvider = multiTenantJwkLocalProviderProvider.getIfAvailable();
    }

    if (insightConfig == null || multiTenantJwkAuth0Provider == null || multiTenantJwkLocalProvider == null) {
      return null;
    }
    return selectMultiTenantJwkProvider(insightConfig, multiTenantJwkAuth0Provider, multiTenantJwkLocalProvider);
  }

  private MultiTenantJwkProvider selectMultiTenantJwkProvider(
      InsightConfig insightConfig,
      MultiTenantJwkAuth0Provider multiTenantJwkAuth0Provider,
      MultiTenantJwkLocalProvider multiTenantJwkLocalProvider)
  {
    MultiTenantInsightConfig multiTenantInsightConfig = MtiqConfigSupport.requireMultiTenantInsightConfig(
        insightConfig,
        "MtiqAdminFilterConfiguration.selectMultiTenantJwkProvider");
    Auth0Config auth0Config = multiTenantInsightConfig.getAuth0Config();
    if (auth0Config == null || auth0Config.getDomain() == null || auth0Config.getDomain().startsWith("local/")) {
      return multiTenantJwkLocalProvider;
    }
    return multiTenantJwkAuth0Provider;
  }

  private <T> T getBeanIncludingAncestors(ApplicationContext applicationContext, Class<T> beanType) {
    Map<String, T> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, beanType);
    if (beans.isEmpty()) {
      return null;
    }
    if (beans.size() == 1) {
      return beans.values().iterator().next();
    }

    List<String> primaryBeanNames = new ArrayList<>();
    for (String beanName : beans.keySet()) {
      if (isPrimaryBean(applicationContext, beanName)) {
        primaryBeanNames.add(beanName);
      }
    }
    if (primaryBeanNames.size() == 1) {
      return beans.get(primaryBeanNames.get(0));
    }
    return null;
  }

  private boolean isPrimaryBean(ApplicationContext applicationContext, String beanName) {
    if (applicationContext instanceof ConfigurableApplicationContext configurableApplicationContext) {
      if (configurableApplicationContext.getBeanFactory().containsBeanDefinition(beanName)) {
        return configurableApplicationContext.getBeanFactory().getBeanDefinition(beanName).isPrimary();
      }
    }

    ApplicationContext parent = applicationContext.getParent();
    return parent != null && isPrimaryBean(parent, beanName);
  }

  private <T> T getNamedBeanIncludingAncestors(
      ApplicationContext applicationContext,
      String beanName,
      Class<T> beanType)
  {
    if (applicationContext.containsBean(beanName)) {
      return applicationContext.getBean(beanName, beanType);
    }

    ApplicationContext parent = applicationContext.getParent();
    return parent == null ? null : getNamedBeanIncludingAncestors(parent, beanName, beanType);
  }

  private <T> T getNamedAncestorBean(
      ApplicationContext applicationContext,
      String beanName,
      Class<T> beanType)
  {
    ApplicationContext parent = applicationContext.getParent();
    return parent == null ? null : getNamedBeanIncludingAncestors(parent, beanName, beanType);
  }

  private <T> T getAncestorBean(ApplicationContext applicationContext, Class<T> beanType) {
    ApplicationContext parent = applicationContext.getParent();
    return parent == null ? null : getBeanIncludingAncestors(parent, beanType);
  }

  private Filter createPassThroughFilter() {
    return new Filter()
    {
      @Override
      public void doFilter(
          jakarta.servlet.ServletRequest request,
          jakarta.servlet.ServletResponse response,
          jakarta.servlet.FilterChain chain) throws java.io.IOException, jakarta.servlet.ServletException
      {
        chain.doFilter(request, response);
      }
    };
  }

  @Bean
  public FilterRegistrationBean<Filter> mtiqAdminBaseUrlFilterRegistration(
      final ObjectProvider<BaseUrlFilter> baseUrlFilterProvider)
  {
    Filter baseUrlFilter = baseUrlFilterProvider.getIfAvailable();
    if (baseUrlFilter == null) {
      baseUrlFilter = createPassThroughFilter();
    }
    return registerFilter(baseUrlFilter, ORDER_BASE_URL, "/*");
  }

  @Bean
  public FilterRegistrationBean<Filter> mtiqAdminAuditFilterRegistration(
      final ObjectProvider<AuditFilter> auditFilterProvider)
  {
    Filter auditFilter = auditFilterProvider.getIfAvailable();
    if (auditFilter == null) {
      auditFilter = createPassThroughFilter();
    }
    return registerFilter(auditFilter, FilterOrder.AUDIT, AuditFilter.URL_PATTERNS);
  }

  @Bean
  public FilterRegistrationBean<HttpHeaderValidatorFilter> mtiqAdminHttpHeaderValidatorFilterRegistration(
      final ObjectProvider<HttpHeaderValidatorFilter> httpHeaderValidatorFilterProvider,
      final ApplicationContext applicationContext)
  {
    HttpHeaderValidatorFilter httpHeaderValidatorFilter = resolveBean(
        applicationContext,
        "httpHeaderValidatorFilter",
        HttpHeaderValidatorFilter.class,
        httpHeaderValidatorFilterProvider);
    if (httpHeaderValidatorFilter == null) {
      try {
        httpHeaderValidatorFilter = new HttpHeaderValidatorFilter();
      }
      catch (java.io.IOException e) {
        throw new IllegalStateException("Unable to create the MTIQ admin HTTP header validator filter", e);
      }
    }
    return registerFilter(httpHeaderValidatorFilter, ORDER_HTTP_HEADER_VALIDATION,
        HttpHeaderValidatorFilter.URL_PATTERN);
  }

  @Bean
  public FilterRegistrationBean<ContentTypeOptionsHeaderFilter> mtiqAdminContentTypeOptionsHeaderFilterRegistration(
      final ObjectProvider<ContentTypeOptionsHeaderFilter> contentTypeOptionsHeaderFilterProvider,
      final ApplicationContext applicationContext)
  {
    ContentTypeOptionsHeaderFilter contentTypeOptionsHeaderFilter = resolveBean(
        applicationContext,
        "contentTypeOptionsHeaderFilter",
        ContentTypeOptionsHeaderFilter.class,
        contentTypeOptionsHeaderFilterProvider);
    if (contentTypeOptionsHeaderFilter == null) {
      contentTypeOptionsHeaderFilter = new ContentTypeOptionsHeaderFilter();
    }
    return registerFilter(contentTypeOptionsHeaderFilter, ORDER_CONTENT_TYPE_OPTIONS, "/*");
  }

  @Bean
  public FilterRegistrationBean<HstsHeaderFilter> mtiqAdminHstsHeaderFilterRegistration(
      final ObjectProvider<HstsHeaderFilter> hstsHeaderFilterProvider,
      final ApplicationContext applicationContext)
  {
    HstsHeaderFilter hstsHeaderFilter = getBeanIncludingAncestors(applicationContext, HstsHeaderFilter.class);
    if (hstsHeaderFilter == null) {
      hstsHeaderFilter = hstsHeaderFilterProvider.getIfAvailable();
    }
    if (hstsHeaderFilter == null) {
      throw new IllegalStateException("HstsHeaderFilter is required for MTIQ admin HSTS support");
    }
    return registerFilter(hstsHeaderFilter, ORDER_HSTS, "/*");
  }

  @Bean
  public FilterRegistrationBean<FrameOptionsHeaderFilter> mtiqAdminFrameOptionsHeaderFilterRegistration(
      final ObjectProvider<FrameOptionsHeaderFilter> frameOptionsHeaderFilterProvider,
      final ApplicationContext applicationContext)
  {
    FrameOptionsHeaderFilter frameOptionsHeaderFilter = resolveBean(
        applicationContext,
        "frameOptionsHeaderFilter",
        FrameOptionsHeaderFilter.class,
        frameOptionsHeaderFilterProvider);
    if (frameOptionsHeaderFilter == null) {
      throw new IllegalStateException("FrameOptionsHeaderFilter is required for MTIQ admin X-Frame-Options support");
    }
    return registerFilter(frameOptionsHeaderFilter, ORDER_FRAME_OPTIONS, "/*");
  }

  private <T extends Filter> FilterRegistrationBean<T> registerFilter(T filter, int order, String... urlPatterns) {
    FilterRegistrationBean<T> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.addUrlPatterns(urlPatterns);
    registration.setOrder(order);
    return registration;
  }
}
