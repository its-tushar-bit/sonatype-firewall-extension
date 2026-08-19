/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-jaxrs-utils
package com.sonatype.insight.jaxrs;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Allows to use {@link ComponentIdentifier} as query/header parameters in JAX-RS resources.
 */
@Provider
public class ComponentIdentifierParamConverterProvider
    implements ParamConverterProvider
{
  private final ParamConverter<ComponentIdentifier> paramConverter;

  public ComponentIdentifierParamConverterProvider(ObjectMapper objectMapper) {
    paramConverter = new ParamConverter<ComponentIdentifier>()
    {
      @Override
      public ComponentIdentifier fromString(String json) {
        try {
          ComponentIdentifier componentIdentifier =
              json != null ? objectMapper.readValue(json, ComponentIdentifier.class) : null;
          if (componentIdentifier != null) {
            componentIdentifier.validate();
          }
          return componentIdentifier;
        }
        catch (IOException e) {
          throw new WebApplicationException(e, Response.status(Response.Status.BAD_REQUEST)
              .type(ErrorResponse.CONTENT_TYPE)
              .entity("Invalid component identifier")
              .build());
        }
        catch (InvalidComponentIdentifierException e) {
          throw new WebApplicationException(e, Response.status(Response.Status.BAD_REQUEST)
              .type(ErrorResponse.CONTENT_TYPE)
              .entity(e.getMessage())
              .build());
        }
      }

      @Override
      public String toString(ComponentIdentifier value) {
        try {
          return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException e) {
          throw new ProcessingException(e);
        }
      }
    };
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
    return ComponentIdentifier.class.equals(rawType) ? (ParamConverter<T>) paramConverter : null;
  }
}
