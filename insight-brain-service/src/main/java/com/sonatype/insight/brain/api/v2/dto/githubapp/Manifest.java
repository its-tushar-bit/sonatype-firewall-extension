/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.githubapp;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTO for GitHub App manifest.
 *
 * This manifest is used to register a new GitHub App via the GitHub App Manifest flow.
 * Field names use snake_case to match GitHub's API expectations.
 */
public record Manifest(
    String name,
    String url,
    String redirect_url,
    String setup_url,
    List<String> callback_urls,
    Boolean request_oauth_on_install,
    String description,
    @JsonProperty("public") Boolean isPublic,
    Map<String, String> default_permissions,
    Boolean setup_on_update)
{
}
