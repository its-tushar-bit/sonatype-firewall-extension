/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource.FilePathRegex;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource.ProprietaryConfigByOwner;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource.ProprietaryConfigHierarchy;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.Assert.assertProprietaryConfig;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ProprietaryConfigResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testUpsertProprietaryConfig() throws Exception {
    Organization org = tempEntity.newOrganization("ProprietaryConfigResourceTest");

    HttpRequest request = restRequest().path(ProprietaryConfigResource.RESOURCE_PATH)
        .parameter(org.getType(), org.getId());

    // Inserts when there is no existing persisted value
    HttpResponse response = request.body(new ProprietaryConfig(org.getPublicId(), Collections.singletonList("package1"),
        Collections.singletonList("regex1"))).put();
    assertResponseStatus(200, response);

    ProprietaryConfig proprietaryConfig = response.getBody(ProprietaryConfig.class);
    assertThat(proprietaryConfig, is(notNullValue()));
    assertThat(proprietaryConfig.getId(), is(notNullValue()));
    assertThat(proprietaryConfig.getPackages(), hasSize(1));
    assertThat(proprietaryConfig.getPackages().get(0), is("package1"));
    assertThat(proprietaryConfig.getRegexes(), hasSize(1));
    assertThat(proprietaryConfig.getRegexes().get(0), is("regex1"));
    assertThat(proprietaryConfig.getOwnerId(), is(org.getId()));
  }

  @Test
  public void testGetProprietaryConfigHierarchy() throws Exception {
    Organization org = tempEntity.newOrganization("ProprietaryConfigResourceTest");
    ProprietaryConfig orgConfig = tempEntity
        .newProprietaryConfig(org.getId(), Arrays.asList("package1", "package2"), Arrays.asList("regex1", "regex2"));
    Application app = tempEntity.newApplication(org.getId());
    ProprietaryConfig appConfig = tempEntity
        .newProprietaryConfig(app.getId(), Arrays.asList("package11", "package22"),
            Arrays.asList("regex11", "regex22"));

    ProprietaryConfig rootOrgConfig = new ProprietaryConfig(Organization.ROOT_ORGANIZATION_ID, null, null);

    HttpRequest request = restRequest().path(ProprietaryConfigResource.RESOURCE_PATH)
        .parameter(app.getType(), app.getPublicId());

    // Get ProprietaryConfig Hierarchy
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    ProprietaryConfigHierarchy proprietaryConfigHierarchy = response.getBody(ProprietaryConfigHierarchy.class);
    assertThat(proprietaryConfigHierarchy, is(notNullValue()));
    assertThat(proprietaryConfigHierarchy.proprietaryConfigByOwners, hasSize(3)); // App -> Org -> Root Org

    // Assert App Data is Correct
    ProprietaryConfigByOwner proprietaryConfigByOwner = proprietaryConfigHierarchy.proprietaryConfigByOwners.get(0);
    assertThat(proprietaryConfigByOwner.ownerId, is(app.getId()));
    assertThat(proprietaryConfigByOwner.ownerName, is(app.getName()));
    assertThat(proprietaryConfigByOwner.ownerType, is(app.getType()));
    assertProprietaryConfig(appConfig, proprietaryConfigByOwner.proprietaryConfig);

    // Assert Org Data is Correct
    proprietaryConfigByOwner = proprietaryConfigHierarchy.proprietaryConfigByOwners.get(1);
    assertThat(proprietaryConfigByOwner.ownerId, is(org.getId()));
    assertThat(proprietaryConfigByOwner.ownerName, is(org.getName()));
    assertThat(proprietaryConfigByOwner.ownerType, is(org.getType()));
    assertProprietaryConfig(orgConfig, proprietaryConfigByOwner.proprietaryConfig);

    // Assert Root Org Data is Correct
    proprietaryConfigByOwner = proprietaryConfigHierarchy.proprietaryConfigByOwners.get(2);
    assertThat(proprietaryConfigByOwner.ownerId, is(Organization.ROOT_ORGANIZATION_ID));
    assertThat(proprietaryConfigByOwner.ownerName, equalToIgnoringCase("root organization"));
    assertThat(proprietaryConfigByOwner.ownerType, is(OwnerType.ORGANIZATION));
    assertProprietaryConfig(rootOrgConfig, proprietaryConfigByOwner.proprietaryConfig);
  }

  @Test
  public void testAddFilePathRegexToProprietaryConfig() throws Exception {
    Organization org = tempEntity.newOrganization("ProprietaryConfigResourceTest");
    tempEntity.newProprietaryConfig(org.getId(), null, Collections.singletonList("regex1"));

    HttpRequest request = restRequest()
        .path(ProprietaryConfigResource.RESOURCE_PATH).path(ProprietaryConfigResource.ADD_FILE_PATH_REGEX)
        .parameter(org.getType(), org.getId());

    // Add File Path regex
    FilePathRegex filePathRegex = new FilePathRegex();
    filePathRegex.paths = Arrays.asList("path1", "path2", "path3");
    filePathRegex.regex = "regex2";
    HttpResponse response = request.body(filePathRegex).post();
    assertResponseStatus(200, response);

    ProprietaryConfig proprietaryConfig = response.getBody(ProprietaryConfig.class);
    assertThat(proprietaryConfig.getRegexes(), hasSize(5));
    assertThat(proprietaryConfig.getRegexes().get(0), is("regex1"));
    assertThat(proprietaryConfig.getRegexes().get(1), is(Pattern.quote("path1")));
    assertThat(proprietaryConfig.getRegexes().get(2), is(Pattern.quote("path2")));
    assertThat(proprietaryConfig.getRegexes().get(3), is(Pattern.quote("path3")));
    assertThat(proprietaryConfig.getRegexes().get(4), is("regex2"));
  }
}
