/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.jaxrs.error.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;
import com.sonatype.insight.jaxrs.error.JavaLangErrorHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MtiqAdminJaxRsErrorConfiguration
{
  static final String CONFIGURED_JAX_RS_EXCEPTION_MAPPER_BEAN_NAME = "mtiqAdminConfiguredJaxRsExceptionMapper";

  @Bean(name = CONFIGURED_JAX_RS_EXCEPTION_MAPPER_BEAN_NAME, autowireCandidate = false)
  public JaxRsExceptionMapper configuredJaxRsExceptionMapper(
      final ObjectProvider<ErrorResponseGenerator> errorResponseGeneratorProvider)
  {
    return new JaxRsExceptionMapper(
        errorResponseGeneratorProvider.getIfAvailable(ErrorResponseGenerator::new),
        FatalErrorHandlingSupport.configure(new JavaLangErrorHandler()));
  }
}
