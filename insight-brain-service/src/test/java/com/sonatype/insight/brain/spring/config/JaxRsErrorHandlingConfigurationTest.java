/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;
import com.sonatype.insight.jaxrs.error.JavaLangErrorHandler;
import java.lang.reflect.Field;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

public class JaxRsErrorHandlingConfigurationTest
{
  @Test
  public void shouldWireSpringManagedMapperToFeatureFlagAwareJavaLangErrorHandler() throws Exception {
    SystemConfigurationPropertyDAO originalSystemConfigurationPropertyDAO = getSystemConfigurationPropertyDAO();
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = mock(SystemConfigurationPropertyDAO.class);
    String propertyName = SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.getPropertyName();

    try {
      when(systemConfigurationPropertyDAO.getByName(propertyName))
          .thenReturn(new SystemConfigurationProperty(propertyName, "false"),
              new SystemConfigurationProperty(propertyName, "true"));
      SystemConfigurationPropertyFeature.injectDependencies(systemConfigurationPropertyDAO);

      try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
        context.register(JaxRsErrorHandlingConfiguration.class);
        context.registerBean(JavaLangErrorHandler.class);
        context.registerBean(ErrorResponseGenerator.class);
        context.registerBean(JaxRsExceptionMapper.class);
        context.refresh();

        JavaLangErrorHandler javaLangErrorHandler = context.getBean(JavaLangErrorHandler.class);
        JaxRsExceptionMapper jaxRsExceptionMapper = context.getBean(JaxRsExceptionMapper.class);

        assertThat(ReflectionTestUtils.getField(jaxRsExceptionMapper, "javaLangErrorHandler"))
            .isSameAs(javaLangErrorHandler);

        Runtime runtime = mock(Runtime.class);
        javaLangErrorHandler.handleExit(runtime);
        verify(runtime, never()).exit(1);

        javaLangErrorHandler.handleExit(runtime);
        verify(runtime).exit(1);
        verify(systemConfigurationPropertyDAO, times(2)).getByName(propertyName);
      }
    }
    finally {
      SystemConfigurationPropertyFeature.injectDependencies(originalSystemConfigurationPropertyDAO);
    }
  }

  private SystemConfigurationPropertyDAO getSystemConfigurationPropertyDAO() throws Exception {
    Field field = SystemConfigurationPropertyFeature.class.getDeclaredField("systemConfigurationPropertyDAO");
    field.setAccessible(true);
    return (SystemConfigurationPropertyDAO) field.get(null);
  }
}
