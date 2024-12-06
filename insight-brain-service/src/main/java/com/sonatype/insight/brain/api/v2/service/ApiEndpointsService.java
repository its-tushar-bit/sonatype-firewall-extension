/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import com.sonatype.insight.brain.api.v2.dto.ApiType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.glassfish.jersey.server.model.Resource;

/**
 * @since 1.143.0
 */
@Named
@Singleton
public class ApiEndpointsService
{
  // Visible for testing
  public static final EnumMap<ApiType, String> OPEN_API_JSON_BY_API_TYPE = new EnumMap<>(ApiType.class);

  private final VersionService versionService;

  private final ProductLicense productLicense;

  @Inject
  public ApiEndpointsService(final VersionService versionService, final ProductLicense productLicense) {
    this.versionService = versionService;
    this.productLicense = productLicense;
  }

  public String getOpenAPI(final Application application, final ApiType apiType) {
    checkApiPageEnabled();
    return OPEN_API_JSON_BY_API_TYPE.computeIfAbsent(apiType,
        key -> toJson(createOpenAPI(application, apiType)));
  }

  private void checkApiPageEnabled() {
    if (!SystemConfigurationPropertyFeature.API_PAGE.isEnabled()) {
      throw new NotAuthorizedException(
          SystemConfigurationPropertyFeature.API_PAGE.getId() + " feature is disabled.");
    }
  }

  private OpenAPI createOpenAPI(final Application application, final ApiType apiType) {
    OpenAPI openAPI = new Reader()
    {
      @Override
      protected Operation parseMethod(
          final Class<?> cls,
          final Method method,
          final List<Parameter> globalParameters,
          final Produces methodProduces,
          final Produces classProduces,
          final Consumes methodConsumes,
          final Consumes classConsumes,
          final List<SecurityRequirement> classSecurityRequirements,
          final Optional<ExternalDocumentation> classExternalDocs,
          final Set<String> classTags,
          final List<Server> classServers,
          final boolean isSubresource,
          final RequestBody parentRequestBody,
          final ApiResponses parentResponses,
          final JsonView jsonViewAnnotation,
          final ApiResponse[] classResponses,
          final AnnotatedMethod annotatedMethod)
      {
        Operation operation = null;
        if (isMethodSupportedByProductLicense(method)) {
          operation = super.parseMethod(cls, method, globalParameters, methodProduces, classProduces, methodConsumes,
              classConsumes, classSecurityRequirements, classExternalDocs, classTags, classServers, isSubresource,
              parentRequestBody, parentResponses, jsonViewAnnotation, classResponses, annotatedMethod);
        }
        return operation;
      }
    }.read(
        Stream.concat(
                application.getClasses().stream(),
                application.getSingletons().stream().map(Object::getClass)
            )
            .map(Resource::from)
            .filter(Objects::nonNull)
            .filter(resource -> isResourceMatchingApiType(resource, apiType))
            .flatMap(resource -> resource.getHandlerClasses().stream())
            .filter(this::isClassSupportedByProductLicense)
            .collect(Collectors.toSet()));
    SortedSet<String> tags = new TreeSet<>();
    if (openAPI.getPaths() != null) {
      openAPI.getPaths().forEach((key, pathItem) -> {
        String tag = createTag(key, apiType.getPathPrefix());
        pathItem.readOperations().forEach(operation -> {
          addTagsItemIfNeeded(tag, operation);
          tags.addAll(operation.getTags());
          removeImpossibleEnumValues(operation);
        });
      });
    }
    tags.forEach(tag -> addTagToOpenAPI(tag, openAPI));
    if (openAPI.getTags() != null) {
      openAPI.getTags().sort((tag1, tag2) -> tag1.getName().compareToIgnoreCase(tag2.getName()));
    }
    Info info = new Info();
    info.setTitle(String.format("Sonatype Lifecycle %s REST API", StringUtils.capitalize(apiType.toString())));
    info.setVersion(versionService.getVersion());
    openAPI.setInfo(info);
    return openAPI;
  }

  private boolean isResourceMatchingApiType(final Resource resource, final ApiType apiType) {
    return resource.getPath().startsWith(apiType.getPathPrefix()) ||
        resource.getPath().startsWith(apiType.getPathPrefix().substring(1));
  }

  private boolean isClassSupportedByProductLicense(final Class<?> clazz) {
    return isAnnotationSupportedByProductLicense(clazz.getAnnotation(ProductLicenseEnforcementPoint.class));
  }

  private boolean isMethodSupportedByProductLicense(final Method method) {
    return isAnnotationSupportedByProductLicense(method.getAnnotation(ProductLicenseEnforcementPoint.class));
  }

  private boolean isAnnotationSupportedByProductLicense(
      final ProductLicenseEnforcementPoint productLicenseEnforcementPoint
  )
  {
    if (productLicenseEnforcementPoint == null) {
      return true;
    }
    return productLicense.hasFeature(productLicenseEnforcementPoint.value());
  }

  private void addTagsItemIfNeeded(final String tag, final Operation operation) {
    if (CollectionUtils.isEmpty(operation.getTags())) {
      operation.addTagsItem(tag);
    }
  }

  private void removeImpossibleEnumValues(final Operation operation) {
    if (operation.getParameters() == null) {
      return;
    }
    for (Parameter parameter : operation.getParameters()) {
      Schema<?> schema = parameter.getSchema();
      if (schema == null) {
        continue;
      }
      String patternString = schema.getPattern();
      if (patternString == null) {
        continue;
      }
      List<?> values = schema.getEnum();
      if (values == null) {
        continue;
      }
      Pattern pattern = Pattern.compile(patternString);
      values.removeIf(value -> !pattern.matcher(value.toString()).matches());
    }
  }

  private String createTag(final String path, final String pathPrefix) {
    int endIndex = path.indexOf("/", pathPrefix.length());
    return camelCaseToTitleCase(path.substring(pathPrefix.length(), endIndex > -1 ? endIndex : path.length()));
  }

  private static String camelCaseToTitleCase(final String camelCase) {
    return WordUtils.capitalizeFully(
        StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(camelCase), StringUtils.SPACE));
  }

  private void addTagToOpenAPI(final String name, final OpenAPI openAPI) {
    Tag tag = new Tag();
    tag.setName(name);
    if (openAPI.getTags() == null || openAPI.getTags().stream().noneMatch(t -> t.getName().equals(name))) {
      openAPI.addTagsItem(tag);
    }
  }

  private static String toJson(final OpenAPI openAPI) {
    try {
      return Json.mapper().writeValueAsString(openAPI);
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
