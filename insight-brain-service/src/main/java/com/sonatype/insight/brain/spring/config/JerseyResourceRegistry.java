/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.WriterInterceptor;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;

public class JerseyResourceRegistry
{
  private static final List<Class<?>> JERSEY_COMPONENT_TYPES = List.of(
      ExceptionMapper.class,
      ParamConverterProvider.class,
      ContextResolver.class,
      MessageBodyReader.class,
      MessageBodyWriter.class,
      ContainerRequestFilter.class,
      ContainerResponseFilter.class,
      ReaderInterceptor.class,
      WriterInterceptor.class,
      DynamicFeature.class,
      Feature.class);

  private final ListableBeanFactory beanFactory;

  public JerseyResourceRegistry(final ListableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  public Collection<Object> getComponents() {
    Map<String, Object> components = new LinkedHashMap<>();
    collectComponents(beanFactory, components);
    return components.values();
  }

  public Collection<Object> getComponentsIncludingAncestors() {
    Map<String, Object> components = new LinkedHashMap<>();
    for (ListableBeanFactory current = beanFactory; current != null; current = getParentBeanFactory(current)) {
      collectComponents(current, components);
    }
    return components.values();
  }

  private void collectComponents(final ListableBeanFactory sourceBeanFactory, final Map<String, Object> components) {
    collectByAnnotation(sourceBeanFactory, components, Path.class);
    collectByAnnotation(sourceBeanFactory, components, Provider.class);
    JERSEY_COMPONENT_TYPES.forEach(type -> collectByType(sourceBeanFactory, components, type));
  }

  private ListableBeanFactory getParentBeanFactory(final ListableBeanFactory sourceBeanFactory) {
    if (sourceBeanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory) {
      BeanFactory parentBeanFactory = hierarchicalBeanFactory.getParentBeanFactory();
      if (parentBeanFactory instanceof ListableBeanFactory listableBeanFactory) {
        return listableBeanFactory;
      }
    }
    return null;
  }

  private void collectByAnnotation(
      final ListableBeanFactory sourceBeanFactory,
      final Map<String, Object> components,
      final Class<? extends Annotation> annotationType)
  {
    for (String beanName : sourceBeanFactory.getBeanNamesForAnnotation(annotationType)) {
      collectBean(sourceBeanFactory, components, beanName);
    }
  }

  @SuppressWarnings("rawtypes")
  private void collectByType(
      final ListableBeanFactory sourceBeanFactory,
      final Map<String, Object> components,
      final Class<?> type)
  {
    for (String beanName : sourceBeanFactory.getBeanNamesForType((Class) type, true, false)) {
      collectBean(sourceBeanFactory, components, beanName);
    }
  }

  private void collectBean(
      final ListableBeanFactory sourceBeanFactory,
      final Map<String, Object> components,
      final String beanName)
  {
    if (components.containsKey(beanName)) {
      return;
    }
    components.put(beanName, sourceBeanFactory.getBean(beanName));
  }
}
