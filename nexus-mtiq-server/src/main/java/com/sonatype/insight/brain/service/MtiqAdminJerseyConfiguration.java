/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.audit.AdminAuditContainerRequestFilter;
import com.sonatype.insight.brain.spring.config.AdminCompatibilityConfiguration;
import com.sonatype.insight.brain.spring.config.InsightJacksonMessageBodyProvider;
import com.sonatype.insight.brain.spring.config.JerseyResourceRegistry;
import com.sonatype.insight.jaxrs.ComponentIdentifierParamConverterProvider;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import({
  AdminCompatibilityConfiguration.class,
  MtiqAdminJaxRsErrorConfiguration.class,
  MtiqObjectMapperConfiguration.class
})
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
public class MtiqAdminJerseyConfiguration
{
  static final String ADMIN_API_SERVLET_PATH = "/api/*";

  @Bean
  public AdminResourceBundle adminResourceBundle(ListableBeanFactory beanFactory) {
    AdminResourceBundle adminResourceBundle = new AdminResourceBundle(AdminApiPaths.ADMIN_PATH);
    JerseyResourceRegistry jerseyResourceRegistry = new JerseyResourceRegistry(beanFactory);

    jerseyResourceRegistry.getComponentsIncludingAncestors()
        .stream()
        .filter(component -> MtiqResourceInstaller.isAdminResource(AopUtils.getTargetClass(component)))
        .forEach(adminResourceBundle::register);

    return adminResourceBundle;
  }

  @Bean(name = "adminInsightJacksonMessageBodyProvider")
  public InsightJacksonMessageBodyProvider adminInsightJacksonMessageBodyProvider(
      @Qualifier(MtiqObjectMapperConfiguration.MTIQ_JERSEY_OBJECT_MAPPER) ObjectMapper objectMapper)
  {
    return new InsightJacksonMessageBodyProvider(objectMapper);
  }

  @Bean(name = "adminComponentIdentifierParamConverterProvider")
  public ComponentIdentifierParamConverterProvider adminComponentIdentifierParamConverterProvider(
      @Qualifier(MtiqObjectMapperConfiguration.MTIQ_JERSEY_OBJECT_MAPPER) ObjectMapper objectMapper)
  {
    return new ComponentIdentifierParamConverterProvider(objectMapper);
  }

  @Bean(name = "mtiqAdminResourceConfig")
  public ResourceConfig mtiqAdminResourceConfig(
      AdminResourceBundle adminResourceBundle,
      @Qualifier("adminInsightJacksonMessageBodyProvider") InsightJacksonMessageBodyProvider insightJacksonMessageBodyProvider,
      @Qualifier("adminComponentIdentifierParamConverterProvider") ComponentIdentifierParamConverterProvider componentIdentifierParamConverterProvider,
      ApplicationContext applicationContext)
  {
    ResourceConfig resourceConfig = new ResourceConfig();
    adminResourceBundle.configure(resourceConfig);
    resourceConfig.register(insightJacksonMessageBodyProvider);
    resourceConfig.register(componentIdentifierParamConverterProvider);
    resourceConfig.register(applicationContext.getBean(
        MtiqAdminJaxRsErrorConfiguration.CONFIGURED_JAX_RS_EXCEPTION_MAPPER_BEAN_NAME,
        JaxRsExceptionMapper.class));
    resourceConfig.register(AdminAuditContainerRequestFilter.class);
    return resourceConfig;
  }

  @Bean
  public ServletRegistrationBean<ServletContainer> mtiqAdminJerseyServlet(
      @Qualifier("mtiqAdminResourceConfig") ResourceConfig adminResourceConfig)
  {
    ServletRegistrationBean<ServletContainer> registration = new ServletRegistrationBean<>(
        new ServletContainer(adminResourceConfig), ADMIN_API_SERVLET_PATH);
    registration.setName("mtiqAdminJerseyServlet");
    registration.setLoadOnStartup(1);
    return registration;
  }

}
