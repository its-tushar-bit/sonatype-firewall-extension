/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiType;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.brain.version.VersionService;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
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
  // Cache of OpenAPI JSON by API type
  private static final Map<ApiType, String> OPEN_API_JSON_BY_API_TYPE = new ConcurrentHashMap<>();

  // Cache of lambda to calculate if an operation is supported by operation ID
  private static final Map<String, BooleanSupplier> IS_SUPPORTED_BY_OPERATION_ID = new ConcurrentHashMap<>();

  private final VersionService versionService;

  private final ProductLicense productLicense;

  @Inject
  public ApiEndpointsService(final VersionService versionService, final ProductLicense productLicense) {
    this.versionService = versionService;
    this.productLicense = productLicense;
  }

  public static Map<ApiType, String> getOpenApiJsonCacheCopy() {
    return new HashMap<>(OPEN_API_JSON_BY_API_TYPE);
  }

  public static void clearCaches() {
    OPEN_API_JSON_BY_API_TYPE.clear();
    IS_SUPPORTED_BY_OPERATION_ID.clear();
  }

  public String getOpenAPI(final Application application, final ApiType apiType) {
    String openAPIJson =
        OPEN_API_JSON_BY_API_TYPE.computeIfAbsent(apiType, key -> toJson(createOpenAPI(application, apiType)));
    OpenAPI openAPI = fromJson(openAPIJson);
    trimIfNeeded(openAPI);
    return toJson(openAPI);
  }

  private OpenAPI createOpenAPI(final Application application, final ApiType apiType) {
    OpenAPI openAPI = new InsightOpenAPIReader().read(getResourceHandlerClasses(application, apiType));
    handlePathsAndOperations(openAPI, apiType);
    handleTags(openAPI);
    addInfo(openAPI, apiType);
    return openAPI;
  }

  private Set<Class<?>> getResourceHandlerClasses(final Application application, final ApiType apiType) {
    return Stream.concat(
            application.getClasses().stream(),
            application.getSingletons().stream().map(Object::getClass)
        )
        .map(Resource::from)
        .filter(Objects::nonNull)
        .filter(resource -> isResourceMatchingApiType(resource, apiType))
        .flatMap(resource -> resource.getHandlerClasses().stream())
        .collect(Collectors.toSet());
  }

  private boolean isResourceMatchingApiType(final Resource resource, final ApiType apiType) {
    return resource.getPath().startsWith(apiType.getPathPrefix()) ||
        resource.getPath().startsWith(apiType.getPathPrefix().substring(1));
  }

  private void handlePathsAndOperations(final OpenAPI openAPI, final ApiType apiType) {
    if (openAPI.getPaths() == null) {
      return;
    }
    Set<String> tags = new HashSet<>();
    openAPI.getPaths().forEach((key, pathItem) -> {
      // Create a default tag for the path
      String tag = createTag(key, apiType.getPathPrefix());
      pathItem.readOperations().forEach(operation -> {
        // If the operation has no tags, then we add the default tag for the path
        if (CollectionUtils.isEmpty(operation.getTags())) {
          operation.addTagsItem(tag);
        }
        tags.addAll(operation.getTags());
        removeImpossibleEnumValues(operation);
      });
    });
    // If a tag has a description, then it is automatically added, otherwise we need to add it manually
    tags.forEach(tag -> addTagIfNeeded(openAPI, tag));
  }

  private String createTag(final String path, final String pathPrefix) {
    int endIndex = path.indexOf("/", pathPrefix.length());
    return camelCaseToTitleCase(path.substring(pathPrefix.length(), endIndex > -1 ? endIndex : path.length()));
  }

  private String camelCaseToTitleCase(final String camelCase) {
    return WordUtils.capitalizeFully(
        StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(camelCase), StringUtils.SPACE));
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
      Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
      values.removeIf(value -> !pattern.matcher(value.toString()).matches());
    }
  }

  private void addTagIfNeeded(final OpenAPI openAPI, final String name) {
    if (openAPI.getTags() == null || openAPI.getTags().stream().noneMatch(tag -> tag.getName().equals(name))) {
      Tag tag = new Tag();
      tag.setName(name);
      openAPI.addTagsItem(tag);
    }
  }

  private void handleTags(final OpenAPI openAPI) {
    if (openAPI.getTags() == null) {
      return;
    }
    openAPI.getTags().sort((tag1, tag2) -> tag1.getName().compareToIgnoreCase(tag2.getName()));
  }

  private void addInfo(final OpenAPI openAPI, final ApiType apiType) {
    Info info = new Info();
    info.setTitle(String.format("Sonatype Lifecycle %s REST API", StringUtils.capitalize(apiType.toString())));
    info.setVersion(versionService.getVersion());
    openAPI.setInfo(info);
  }

  private OpenAPI fromJson(final String openAPIJson) {
    try {
      return Json.mapper().readValue(openAPIJson, OpenAPI.class);
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private void trimIfNeeded(final OpenAPI openAPI) {
    if (openAPI.getPaths() == null) {
      return;
    }
    Set<String> usedTags = new HashSet<>();
    Iterator<Entry<String, PathItem>> iterator = openAPI.getPaths().entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<String, PathItem> entry = iterator.next();
      PathItem pathItem = entry.getValue();

      if (pathItem.getGet() != null && !isSupported(pathItem.getGet())) {
        pathItem.setGet(null);
      }

      if (pathItem.getPut() != null && !isSupported(pathItem.getPut())) {
        pathItem.setPut(null);
      }

      if (pathItem.getPost() != null && !isSupported(pathItem.getPost())) {
        pathItem.setPost(null);
      }

      if (pathItem.getDelete() != null && !isSupported(pathItem.getDelete())) {
        pathItem.setDelete(null);
      }

      if (pathItem.getPatch() != null && !isSupported(pathItem.getPatch())) {
        pathItem.setPatch(null);
      }

      if (pathItem.getHead() != null && !isSupported(pathItem.getHead())) {
        pathItem.setHead(null);
      }

      if (pathItem.getOptions() != null && !isSupported(pathItem.getOptions())) {
        pathItem.setOptions(null);
      }

      if (pathItem.getTrace() != null && !isSupported(pathItem.getTrace())) {
        pathItem.setTrace(null);
      }

      pathItem.readOperations().forEach(operation -> usedTags.addAll(operation.getTags()));

      if (pathItem.readOperations().isEmpty()) {
        iterator.remove();
      }
    }
    if (openAPI.getTags() == null) {
      return;
    }
    openAPI.getTags().removeIf(tag -> !usedTags.contains(tag.getName()));
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean isSupported(final Operation operation) {
    BooleanSupplier isSupported = IS_SUPPORTED_BY_OPERATION_ID.get(operation.getOperationId());
    return isSupported.getAsBoolean();
  }

  private String toJson(final OpenAPI openAPI) {
    try {
      return Json.mapper().writeValueAsString(openAPI);
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private final class InsightOpenAPIReader
      extends Reader
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
      Operation operation =
          super.parseMethod(cls, method, globalParameters, methodProduces, classProduces, methodConsumes, classConsumes,
              classSecurityRequirements, classExternalDocs, classTags, classServers, isSubresource, parentRequestBody,
              parentResponses, jsonViewAnnotation, classResponses, annotatedMethod);
      IS_SUPPORTED_BY_OPERATION_ID.put(operation.getOperationId(), getIsSupported(method));
      return operation;
    }

    private BooleanSupplier getIsSupported(final Method method) {
      List<BooleanSupplier> booleanSuppliers =
          List.of(getIsSupportedByProductLicense(method), getIsSupportedByFeatureFlag(method));
      return () -> booleanSuppliers.stream().allMatch(BooleanSupplier::getAsBoolean);
    }

    private BooleanSupplier getIsSupportedByProductLicense(final Method method) {
      // Note that method annotations override class annotations
      // See also LicenseAwareContainerDynamicFeature#configure

      // Method explicitly does not need a product license
      if (method.isAnnotationPresent(UnlicensedPath.class)) {
        return () -> true;
      }

      Class<?> clazz = method.getDeclaringClass();
      // Class explicitly does not need a product license and method inherits
      if (clazz.isAnnotationPresent(UnlicensedPath.class) &&
          !method.isAnnotationPresent(ProductLicenseEnforcementPoint.class)) {
        return () -> true;
      }

      ProductLicenseEnforcementPoint methodProductLicenseEnforcementPoint =
          method.getAnnotation(ProductLicenseEnforcementPoint.class);
      // Method needs a specific product license feature
      if (methodProductLicenseEnforcementPoint != null) {
        return () -> productLicense.hasFeature(methodProductLicenseEnforcementPoint.value());
      }

      ProductLicenseEnforcementPoint classProductLicenseEnforcementPoint =
          clazz.getAnnotation(ProductLicenseEnforcementPoint.class);
      // Class needs a specific product license feature and method inherits
      if (classProductLicenseEnforcementPoint != null) {
        return () -> productLicense.hasFeature(classProductLicenseEnforcementPoint.value());
      }

      // Method implicitly does not need any product license feature
      return () -> true;
    }

    private BooleanSupplier getIsSupportedByFeatureFlag(final Method method) {
      // Note that method annotations override class annotations
      // See also HasFeatureMethodInterceptor#getAnnotation

      HasFeature methodHasFeature = method.getAnnotation(HasFeature.class);
      // Method needs a specific feature
      if (methodHasFeature != null) {
        return () -> methodHasFeature.value().isEnabled();
      }

      HasFeature classHasFeature = method.getDeclaringClass().getAnnotation(HasFeature.class);
      // Class needs a specific feature and method inherits
      if (classHasFeature != null) {
        return () -> classHasFeature.value().isEnabled();
      }

      // Method implicitly does not need any feature
      return () -> true;
    }
  }
}
