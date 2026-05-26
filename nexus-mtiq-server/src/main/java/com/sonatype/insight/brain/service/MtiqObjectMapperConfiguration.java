/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MtiqObjectMapperConfiguration
{
  static final String MTIQ_JERSEY_OBJECT_MAPPER = "mtiqJerseyObjectMapper";

  @Bean(name = MTIQ_JERSEY_OBJECT_MAPPER)
  public ObjectMapper mtiqJerseyObjectMapper(ObjectMapper objectMapper) {
    ObjectMapper mapper = objectMapper.copy();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper;
  }
}
