/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiType;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.brain.security.AnonymousWithFeature;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.models.Components;
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
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
  // Cache of OpenAPI JSON
  private static volatile String openApiJson;

  // Cache of lambda to calculate if an operation is supported by operation ID
  private static final Map<String, BooleanSupplier> IS_SUPPORTED_BY_OPERATION_ID = new ConcurrentHashMap<>();

  private static final String BASIC_AUTH_SCHEME_NAME = "BasicAuth";

  private static final String BEARER_AUTH_SCHEME_NAME = "BearerAuth";

  private static final String BASIC_SCHEME = "basic";

  private static final String BEARER_SCHEME = "bearer";

  private static final String JWT_FORMAT = "JWT";

  private final VersionService versionService;

  private final ProductLicense productLicense;

  @Inject
  public ApiEndpointsService(final VersionService versionService, final ProductLicense productLicense) {
    this.versionService = versionService;
    this.productLicense = productLicense;
  }

  public static String getOpenApiJsonCacheCopy() {
    return openApiJson;
  }

  public static void clearCaches() {
    openApiJson = null;
    IS_SUPPORTED_BY_OPERATION_ID.clear();
  }

  @AnonymousWithFeature
  public String getOpenAPI(final Application application, final ApiType apiType) {
    if (openApiJson == null) {
      openApiJson = toJson(createOpenAPI(application));
    }
    OpenAPI openAPI = fromJson(openApiJson);
    addInfo(openAPI, apiType);
    trimByApiType(openAPI, apiType);
    trimByProductLicenseAndFeatureFlags(openAPI);
    removeUnusedSchemas(openAPI);
    removeComponentsIfEmpty(openAPI);
    return toJson(openAPI);
  }

  private OpenAPI createOpenAPI(final Application application) {
    OpenAPI openAPI = new InsightOpenAPIReader().read(getResourceHandlerClasses(application));
    handlePathsAndOperations(openAPI);
    handleTags(openAPI);
    addSecuritySchemes(openAPI);
    return openAPI;
  }

  private void addSecuritySchemes(final OpenAPI openAPI) {
    if (openAPI.getComponents() == null) {
      openAPI.setComponents(new Components());
    }
    SecurityScheme basicAuth = new SecurityScheme();
    basicAuth.setType(SecurityScheme.Type.HTTP);
    basicAuth.setScheme(BASIC_SCHEME);
    openAPI.getComponents().addSecuritySchemes(BASIC_AUTH_SCHEME_NAME, basicAuth);

    SecurityScheme bearerAuth = new SecurityScheme();
    bearerAuth.setType(SecurityScheme.Type.HTTP);
    bearerAuth.setScheme(BEARER_SCHEME);
    bearerAuth.setBearerFormat(JWT_FORMAT);
    openAPI.getComponents().addSecuritySchemes(BEARER_AUTH_SCHEME_NAME, bearerAuth);

    SecurityRequirement securityRequirement = new SecurityRequirement();
    securityRequirement.addList(BASIC_AUTH_SCHEME_NAME);
    securityRequirement.addList(BEARER_AUTH_SCHEME_NAME);
    openAPI.addSecurityItem(securityRequirement);
  }

  private Set<Class<?>> getResourceHandlerClasses(final Application application) {
    return Stream.concat(application.getClasses().stream(), application.getSingletons().stream().map(Object::getClass))
        .map(Resource::from)
        .filter(Objects::nonNull)
        .filter(this::isResourceMatchingApiType)
        .flatMap(resource -> resource.getHandlerClasses().stream())
        .collect(Collectors.toSet());
  }

  private boolean isResourceMatchingApiType(final Resource resource) {
    return Arrays.stream(ApiType.values())
        .map(ApiType::getPathPrefix)
        .anyMatch(pathPrefix ->
            resource.getPath().startsWith(pathPrefix) || resource.getPath().startsWith(pathPrefix.substring(1))
        );
  }

  private void handlePathsAndOperations(final OpenAPI openAPI) {
    if (openAPI.getPaths() == null) {
      return;
    }
    Set<String> tags = new HashSet<>();
    openAPI.getPaths().forEach((key, pathItem) -> {
      // Create a default tag for the path
      String tag = createTag(key);
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

  private String createTag(final String path) {
    String pathPrefix = getApiType(path).getPathPrefix();
    int endIndex = path.indexOf("/", pathPrefix.length());
    return camelCaseToTitleCase(path.substring(pathPrefix.length(), endIndex > -1 ? endIndex : path.length()));
  }

  private ApiType getApiType(final String path) {
    if (path.startsWith(ApiType.PUBLIC.getPathPrefix())) {
      return ApiType.PUBLIC;
    }
    else if (path.startsWith(ApiType.EXPERIMENTAL.getPathPrefix())) {
      return ApiType.EXPERIMENTAL;
    }
    throw new RuntimeException("Invalid path prefix: " + path);
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
    String type = apiType == null ? "" : (apiType + " ");
    Info info = new Info();
    info.setTitle(String.format("Sonatype Lifecycle %sREST API", StringUtils.capitalize(type)));
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

  private void trimByApiType(final OpenAPI openAPI, final ApiType apiType) {
    if (apiType == null) {
      return;
    }
    if (openAPI.getPaths() == null) {
      return;
    }
    Set<String> usedTags = new HashSet<>();
    Iterator<Entry<String, PathItem>> iterator = openAPI.getPaths().entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<String, PathItem> entry = iterator.next();
      PathItem pathItem = entry.getValue();
      String path = entry.getKey();
      if (path.startsWith(apiType.getPathPrefix()) || path.startsWith(apiType.getPathPrefix().substring(1))) {
        pathItem.readOperations().forEach(operation -> usedTags.addAll(operation.getTags()));
      }
      else {
        iterator.remove();
      }
    }
    if (openAPI.getTags() == null) {
      return;
    }
    openAPI.getTags().removeIf(tag -> !usedTags.contains(tag.getName()));
  }

  private void trimByProductLicenseAndFeatureFlags(final OpenAPI openAPI) {
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

  private void removeUnusedSchemas(final OpenAPI openAPI) {
    Set<String> usedSchemaKeys = new HashSet<>();
    if (openAPI.getPaths() != null) {
      for (String schemaRef : findAllValues(JsonUtils.asTree(openAPI.getPaths()), "$ref").stream().map(JsonNode::asText)
          .collect(Collectors.toSet())) {
        addSchema(openAPI, schemaRef, usedSchemaKeys);
      }
    }
    if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
      openAPI.getComponents().getSchemas().keySet().removeIf(schemaKey -> !usedSchemaKeys.contains(schemaKey));
    }
  }

  private void addSchema(final OpenAPI openAPI, final String schemaRef, final Set<String> schemaKeys) {
    if (schemaRef == null || !schemaRef.startsWith("#/components/schemas/")) {
      return;
    }
    String schemaKey = schemaRef.substring("#/components/schemas/".length());
    if (!schemaKeys.add(schemaKey)) {
      return;
    }
    Schema<?> schema = openAPI.getComponents().getSchemas().get(schemaKey);
    for (String s : findAllValues(JsonUtils.asTree(schema), "$ref").stream().map(JsonNode::asText)
        .collect(Collectors.toSet())) {
      addSchema(openAPI, s, schemaKeys);
    }
  }

  private static List<JsonNode> findAllValues(final JsonNode node, final String key) {
    List<JsonNode> values = new ArrayList<>();
    findAllValues(node, key, values);
    return values;
  }

  private static void findAllValues(final JsonNode node, final String key, final List<JsonNode> results) {
    if (node == null) {
      return;
    }
    if (node.has(key)) {
      results.add(node.get(key));
    }
    for (JsonNode child : node) {
      findAllValues(child, key, results);
    }
  }

  private void removeComponentsIfEmpty(final OpenAPI openAPI) {
    Components components = openAPI.getComponents();
    if (components == null) {
      return;
    }
    if (MapUtils.isNotEmpty(components.getCallbacks())) {
      return;
    }
    if (MapUtils.isNotEmpty(components.getExamples())) {
      return;
    }
    if (MapUtils.isNotEmpty(components.getExtensions())) {
      return;
    }
    if (MapUtils.isNotEmpty(components.getHeaders())) {
      return;
    }
    if (MapUtils.isNotEmpty(components.getLinks())) {
      return;
    }
    if (MapUtils.isNotEmpty(components.getPathItems())) {
      return;
    }
    if (MapUtils.isNotEmpty(components.getParameters())) {
      return;
    }
    if (MapUtils.isNotEmpty(components.getRequestBodies())) {
      return;
    }
    if (MapUtils.isNotEmpty(components.getResponses())) {
      return;
    }
    if (MapUtils.isNotEmpty(components.getSchemas())) {
      return;
    }
    if (MapUtils.isNotEmpty(components.getSecuritySchemes())) {
      return;
    }
    openAPI.setComponents(null);
  }

  private String toJson(final Object value) {
    try {
      return Json.mapper().writeValueAsString(value);
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
      if (method.isAnnotationPresent(AnonymousWithFeature.class)) {
        operation.setSecurity(new ArrayList<>());
      }
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
