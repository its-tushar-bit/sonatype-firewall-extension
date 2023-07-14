/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.EnumMap;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Application;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiType;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
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

  public final VersionService versionService;

  @Inject
  public ApiEndpointsService(VersionService versionService) {
    this.versionService = versionService;
  }

  public String getOpenAPI(Application application, ApiType apiType) {
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

  private OpenAPI createOpenAPI(Application application, ApiType apiType) {
    OpenAPI openAPI = new Reader().read(
        Stream.concat(
                application.getClasses().stream(),
                application.getSingletons().stream().map(Object::getClass)
            )
            .map(Resource::from)
            .filter(Objects::nonNull)
            .filter(resource -> resource.getPath().startsWith(apiType.getPathPrefix().substring(1)))
            .flatMap(resource -> resource.getHandlerClasses().stream())
            .collect(Collectors.toSet()));
    SortedSet<String> tags = new TreeSet<>();
    if (openAPI.getPaths() != null) {
      openAPI.getPaths().forEach((key, pathItem) -> {
        String tag = createTag(key, apiType.getPathPrefix());
        pathItem.readOperations().forEach(operation -> {
          if (CollectionUtils.isEmpty(operation.getTags())) {
            operation.addTagsItem(tag);
          }
          tags.addAll(operation.getTags());
        });
      });
    }
    tags.forEach(tag -> addTagToOpenAPI(tag, openAPI));
    Info info = new Info();
    info.setTitle(String.format("Sonatype Lifecycle %s REST API", StringUtils.capitalize(apiType.toString())));
    info.setVersion(versionService.getVersion());
    openAPI.setInfo(info);
    return openAPI;
  }

  private String createTag(String path, String pathPrefix) {
    int endIndex = path.indexOf("/", pathPrefix.length());
    return camelCaseToTitleCase(path.substring(pathPrefix.length(), endIndex > -1 ? endIndex : path.length()));
  }

  private static String camelCaseToTitleCase(String camelCase) {
    return WordUtils.capitalizeFully(
        StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(camelCase), StringUtils.SPACE));
  }

  private void addTagToOpenAPI(String name, OpenAPI openAPI) {
    Tag tag = new Tag();
    tag.setName(name);
    openAPI.addTagsItem(tag);
  }

  private static String toJson(OpenAPI openAPI) {
    try {
      return Json.mapper().writeValueAsString(openAPI);
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
