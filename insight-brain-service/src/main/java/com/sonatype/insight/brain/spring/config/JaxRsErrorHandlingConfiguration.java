/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.service.FatalErrorHandlingSupport;
import com.sonatype.insight.jaxrs.error.JavaLangErrorHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JaxRsErrorHandlingConfiguration
{
  @Bean
  public InitializingBean exitOnFatalErrorSupplierInitializer(
      final ObjectProvider<JavaLangErrorHandler> javaLangErrorHandlerProvider)
  {
    return () -> {
      JavaLangErrorHandler javaLangErrorHandler = javaLangErrorHandlerProvider.getIfAvailable();
      if (javaLangErrorHandler != null) {
        FatalErrorHandlingSupport.configure(javaLangErrorHandler);
      }
    };
  }
}
