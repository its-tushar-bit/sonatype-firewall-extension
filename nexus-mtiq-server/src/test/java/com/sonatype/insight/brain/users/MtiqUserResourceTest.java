/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;
import com.sonatype.insight.jaxrs.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MtiqUserResourceTest
    extends AbstractMultiTenantResourceTest
{
  private final SamlUserDAO samlUserDAO = new SamlUserDAO();

  private final TenantMetadataDAO tenantMetadataDAO = new TenantMetadataDAO();

  private TestMultiTenantAuth0ManagementService authService = new TestMultiTenantAuth0ManagementService();

  @Override
  protected List<Module> getBrainModules() {
    List<Module> modules = new ArrayList<>(super.getBrainModules());
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(MultiTenantAuth0ManagementService.class).toInstance(authService);
      }
    });
    return modules;
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path("rest/mtiqUser");
  }

  @Test
  public void test_ListOfUser() throws Exception {
    SamlUser samlUser =
        new SamlUser("username@example.com", "firstname", "lastname", "username@example.com", Collections.emptySet());
    samlUserDAO.insert(samlUser);

    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);

    List<MtiqUserDTO> data = JsonUtils.parse(response.getBodyText(), new TypeReference<List<MtiqUserDTO>>() { });
    assertThat(data).hasSize(1);
    assertThat(MtiqUserDTO.samlUserToMtiqUser(samlUser)).usingRecursiveComparison().isEqualTo(data.get(0));
  }

  @Test
  public void test_inviteUserFailsWhenNoMetadata() throws Exception {
    MtiqUserDTO mtiqUserDTO = new MtiqUserDTO();
    mtiqUserDTO.setFirstName("foo");
    mtiqUserDTO.setLastName("bar");
    mtiqUserDTO.setEmail("foo@bar.com");
    mtiqUserDTO.setUsername("foo@bar.com");

    HttpResponse response = restRequest().body(mtiqUserDTO).post();

    assertResponseStatus(500, response);
  }

  @Test
  public void test_inviteUser() throws Exception {
    tenantMetadataDAO.insert(new TenantMetadata("appId", "appName", "connId", "connName"));

    MtiqUserDTO mtiqUserDTO = new MtiqUserDTO();
    mtiqUserDTO.setFirstName("foo");
    mtiqUserDTO.setLastName("bar");
    mtiqUserDTO.setEmail("foo@bar.com");
    mtiqUserDTO.setUsername("foo@bar.com");

    assertThat(samlUserDAO.getAll()).hasSize(0);
    HttpResponse response = restRequest().body(mtiqUserDTO).post();

    assertResponseStatus(204, response);
    assertThat(samlUserDAO.getAll()).hasSize(1);
  }

  @Test
  public void test_deleteUser() throws Exception {
    tenantMetadataDAO.insert(new TenantMetadata("appId", "appName", "connId", "connName"));
    samlUserDAO.insert(new SamlUser("foo@bar.com", "foo", "bar", "foo@bar.com", Collections.emptySet()));

    assertThat(samlUserDAO.getAll()).hasSize(1);
    HttpResponse response = restRequest().path("foo@bar.com").delete();

    assertResponseStatus(204, response);
    assertThat(samlUserDAO.getAll()).hasSize(0);
  }

  private static class TestMultiTenantAuth0ManagementService extends MultiTenantAuth0ManagementService
  {
    @Override
    public void createOrUpdateUser(
        final String email,
        final String firstName,
        final String lastName,
        final String connectionName,
        final String applicationId,
        final String connectionId)
    {
      //no-op
    }

    @Override
    public void deleteUser(final String username, final String connectionId) {
      //no-op
    }
  }
}
