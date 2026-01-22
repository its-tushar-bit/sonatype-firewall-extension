/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.saml.SamlPrincipal;
import org.keycloak.common.util.MultivaluedHashMap;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SamlRealmTest
    extends AbstractComponentTest
{
  @Inject
  private SamlRealm samlRealm;

  @Mock
  private ProductLicense mockProductLicense;

  @Inject
  private SamlUserDAO samlUserDAO;

  @Inject
  private SamlConfigurationService samlConfigurationService;

  @Override
  public void configure(Binder binder) {
    binder.bind(ProductLicense.class).toInstance(mockProductLicense);
    super.configure(binder);
  }

  @Before
  public void before() {
    enableSsoWithSaml();
  }

  @After
  public void after() {
    samlUserDAO.getAll().forEach(samlUserDAO::delete);
  }

  @Test
  public void testSamlRealm() {
    assertThat(samlRealm.getCredentialsMatcher()).isInstanceOf(AllowAllCredentialsMatcher.class);
    assertThat(samlRealm.getName()).isEqualTo("SAML");
    assertThat(samlRealm.getAuthenticationTokenClass()).isSameAs(SamlAuthenticationToken.class);
  }

  @Test
  public void testDoGetAuthenticationInfo() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();

    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.add(samlConfiguration.getUsernameAttributeName(), "jonny");
    attributes.add(samlConfiguration.getFirstNameAttributeName(), "john");
    attributes.add(samlConfiguration.getGroupsAttributeName(), "group1");
    MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    friendlyAttributes.add(samlConfiguration.getUsernameAttributeName(), "jonny2");
    friendlyAttributes.add(samlConfiguration.getLastNameAttributeName(), "smith");
    friendlyAttributes.add(samlConfiguration.getGroupsAttributeName(), "group2");

    UserPrincipal userPrincipal = getUserPrincipal(doGetAuthenticationInfo(attributes, friendlyAttributes));
    assertThat(userPrincipal.getUsername()).isEqualTo("jonny");
    assertThat(userPrincipal.getDisplayName()).isEqualTo("john smith");
    assertThat(userPrincipal.getMembership())
        .containsExactlyInAnyOrder("group1", "group2", Group.AUTHENTICATED_USERS_GROUP_ID);
  }

  @Test
  public void testDoGetAuthenticationInfo_NoAttributes() {
    UserPrincipal userPrincipal =
        getUserPrincipal(doGetAuthenticationInfo(new MultivaluedHashMap<>(), new MultivaluedHashMap<>()));
    assertThat(userPrincipal.getUsername()).isEqualTo("name");
    assertThat(userPrincipal.getDisplayName()).isEqualTo("name");
    assertThat(userPrincipal.getMembership()).containsExactly(Group.AUTHENTICATED_USERS_GROUP_ID);
  }

  @Test
  public void testDoGetAuthenticationInfo_UsernameInAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();

    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.add(samlConfiguration.getUsernameAttributeName(), "jonny");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, new MultivaluedHashMap<>())).getUsername())
        .isEqualTo("jonny");
  }

  @Test
  public void testDoGetAuthenticationInfo_UsernameInFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();

    MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    friendlyAttributes.add(samlConfiguration.getUsernameAttributeName(), "jonny");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(new MultivaluedHashMap<>(), friendlyAttributes)).getUsername())
        .isEqualTo("jonny");
  }

  @Test
  public void testDoGetAuthenticationInfo_UsernameInAttributesAndFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.add(samlConfiguration.getUsernameAttributeName(), "jonny1");
    MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    friendlyAttributes.add(samlConfiguration.getUsernameAttributeName(), "jonny2");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, friendlyAttributes)).getUsername())
        .isEqualTo("jonny1");
  }

  @Test
  public void testDoGetAuthenticationInfo_NoUsername() {
    assertThatThrownBy(() -> doGetAuthenticationInfo(null, new MultivaluedHashMap<>(), new MultivaluedHashMap<>()))
        .isInstanceOf(AuthenticationException.class).hasMessageContaining("username is required");
  }

  @Test
  public void testDoGetAuthenticationInfo_FirstNameInAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.add(samlConfiguration.getFirstNameAttributeName(), "john");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, new MultivaluedHashMap<>())).getDisplayName())
        .isEqualTo("john");
  }

  @Test
  public void testDoGetAuthenticationInfo_FirstNameInFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    friendlyAttributes.add(samlConfiguration.getFirstNameAttributeName(), "john");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(new MultivaluedHashMap<>(), friendlyAttributes))
        .getDisplayName()).isEqualTo("john");
  }

  @Test
  public void testDoGetAuthenticationInfo_FirstNameInAttributesAndFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.add(samlConfiguration.getFirstNameAttributeName(), "john1");
    MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    friendlyAttributes.add(samlConfiguration.getFirstNameAttributeName(), "john2");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, friendlyAttributes)).getDisplayName())
        .isEqualTo("john1");
  }

  @Test
  public void testDoGetAuthenticationInfo_LastNameInAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.add(samlConfiguration.getLastNameAttributeName(), "smith");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, new MultivaluedHashMap<>())).getDisplayName())
        .isEqualTo("smith");
  }

  @Test
  public void testDoGetAuthenticationInfo_LastNameInFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    friendlyAttributes.add(samlConfiguration.getLastNameAttributeName(), "smith");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(new MultivaluedHashMap<>(), friendlyAttributes))
        .getDisplayName()).isEqualTo("smith");
  }

  @Test
  public void testDoGetAuthenticationInfo_LastNameInAttributesAndFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.add(samlConfiguration.getLastNameAttributeName(), "smith1");
    MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    friendlyAttributes.add(samlConfiguration.getLastNameAttributeName(), "smith2");

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, friendlyAttributes)).getDisplayName())
        .isEqualTo("smith1");
  }

  @Test
  public void testDoGetAuthenticationInfo_GroupsInAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.addAll(samlConfiguration.getGroupsAttributeName(), "group1", "group2", "", " ", null);

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, new MultivaluedHashMap<>())).getMembership())
        .containsExactlyInAnyOrder("group1", "group2", Group.AUTHENTICATED_USERS_GROUP_ID);
  }

  @Test
  public void testDoGetAuthenticationInfo_GroupsInFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    friendlyAttributes.addAll(samlConfiguration.getGroupsAttributeName(), "group1", "group2", "", " ", null);

    assertThat(getUserPrincipal(doGetAuthenticationInfo(new MultivaluedHashMap<>(), friendlyAttributes))
        .getMembership()).containsExactlyInAnyOrder("group1", "group2", Group.AUTHENTICATED_USERS_GROUP_ID);
  }

  @Test
  public void testDoGetAuthenticationInfo_GroupsInAttributesAndFriendlyAttributes() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.addAll(samlConfiguration.getGroupsAttributeName(), "group1", "group2", "", " ", null);
    MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    friendlyAttributes.addAll(samlConfiguration.getGroupsAttributeName(), "group3", "group4", "", " ", null);

    assertThat(getUserPrincipal(doGetAuthenticationInfo(attributes, friendlyAttributes)).getMembership())
        .containsExactlyInAnyOrder("group1", "group2", "group3", "group4", Group.AUTHENTICATED_USERS_GROUP_ID);
  }

  @Test
  public void testDoGetAuthenticationInfo_MultipleAttributeValues() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.addAll(samlConfiguration.getUsernameAttributeName(), "jonny", "jonny2");
    attributes.addAll(samlConfiguration.getFirstNameAttributeName(), "john", "john2");
    attributes.addAll(samlConfiguration.getLastNameAttributeName(), "smith", "smith2");

    UserPrincipal userPrincipal = getUserPrincipal(doGetAuthenticationInfo(attributes, new MultivaluedHashMap<>()));
    assertThat(userPrincipal.getUsername()).isEqualTo("jonny");
    assertThat(userPrincipal.getDisplayName()).isEqualTo("john smith");
  }

  @Test
  public void testDoGetAuthenticationInfo_MultipleFriendlyAttributeValues() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    friendlyAttributes.addAll(samlConfiguration.getUsernameAttributeName(), "jonny", "jonny2");
    friendlyAttributes.addAll(samlConfiguration.getFirstNameAttributeName(), "john", "john2");
    friendlyAttributes.addAll(samlConfiguration.getLastNameAttributeName(), "smith", "smith2");

    UserPrincipal userPrincipal =
        getUserPrincipal(doGetAuthenticationInfo(new MultivaluedHashMap<>(), friendlyAttributes));
    assertThat(userPrincipal.getUsername()).isEqualTo("jonny");
    assertThat(userPrincipal.getDisplayName()).isEqualTo("john smith");
  }

  @Test
  public void testDoGetAuthenticationInfo_InsertsSamlUser() {
    SamlUser samlUser = createSamlUser();
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.addAll(samlConfiguration.getUsernameAttributeName(), samlUser.getUsername());
    attributes.addAll(samlConfiguration.getFirstNameAttributeName(), samlUser.getFirstName());
    attributes.addAll(samlConfiguration.getLastNameAttributeName(), samlUser.getLastName());
    attributes.addAll(samlConfiguration.getEmailAttributeName(), samlUser.getEmail());
    attributes.addAll(samlConfiguration.getGroupsAttributeName(), new ArrayList<>(samlUser.getGroups()));

    UserPrincipal userPrincipal = getUserPrincipal(doGetAuthenticationInfo(new MultivaluedHashMap<>(), attributes));

    SamlUser storedSamlUser = samlUserDAO.getByUsername(samlUser.getUsername());
    assertThat(storedSamlUser).isNotNull();
    samlUser.setId(storedSamlUser.getId());
    assertThat(storedSamlUser).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(samlUser);
    assertThat(userPrincipal.getUsername()).isEqualTo(samlUser.getUsername());
    assertThat(userPrincipal.getDisplayName()).isEqualTo(samlUser.calculateDisplayName());
    assertThat(userPrincipal.getRealmId()).isEqualTo(SamlRealm.ID);
    Set<String> expectedGroups = new LinkedHashSet<>(samlUser.getGroups());
    expectedGroups.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    assertThat(userPrincipal.getMembership()).isEqualTo(expectedGroups);
  }

  @Test
  public void testDoGetAuthenticationInfo_UpdatesSamlUser() {
    SamlUser samlUser = tempEntity.newSamlUser();
    samlUser.setFirstName(samlUser.getFirstName() + "2");
    samlUser.setLastName(samlUser.getLastName() + "2");
    samlUser.setEmail(samlUser.getEmail() + "2");
    samlUser.setGroups(new LinkedHashSet<>(Arrays.asList("someGroup3", "someGroup4")));
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    attributes.addAll(samlConfiguration.getUsernameAttributeName(), samlUser.getUsername());
    attributes.addAll(samlConfiguration.getFirstNameAttributeName(), samlUser.getFirstName());
    attributes.addAll(samlConfiguration.getLastNameAttributeName(), samlUser.getLastName());
    attributes.addAll(samlConfiguration.getEmailAttributeName(), samlUser.getEmail());
    attributes.addAll(samlConfiguration.getGroupsAttributeName(), new ArrayList<>(samlUser.getGroups()));

    UserPrincipal userPrincipal = getUserPrincipal(doGetAuthenticationInfo(new MultivaluedHashMap<>(), attributes));

    SamlUser storedSamlUser = samlUserDAO.getByUsername(samlUser.getUsername());
    assertThat(storedSamlUser).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(samlUser);
    assertThat(userPrincipal.getUsername()).isEqualTo(samlUser.getUsername());
    assertThat(userPrincipal.getDisplayName()).isEqualTo(samlUser.calculateDisplayName());
    assertThat(userPrincipal.getRealmId()).isEqualTo(SamlRealm.ID);
    Set<String> expectedGroups = new LinkedHashSet<>(samlUser.getGroups());
    expectedGroups.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    assertThat(userPrincipal.getMembership()).isEqualTo(expectedGroups);
  }

  private AuthenticationInfo doGetAuthenticationInfo(
      String name,
      MultivaluedHashMap<String, String> attributes,
      MultivaluedHashMap<String, String> friendlyAttributes)
  {
    SamlPrincipal samlPrincipal = new SamlPrincipal(null, name, null, null, attributes, friendlyAttributes);
    SamlAuthenticationToken samlAuthenticationToken = new SamlAuthenticationToken(samlPrincipal);
    return samlRealm.doGetAuthenticationInfo(samlAuthenticationToken);
  }

  private AuthenticationInfo doGetAuthenticationInfo(
      MultivaluedHashMap<String, String> attributes,
      MultivaluedHashMap<String, String> friendlyAttributes)
  {
    return doGetAuthenticationInfo("name", attributes, friendlyAttributes);
  }

  private UserPrincipal getUserPrincipal(AuthenticationInfo authenticationInfo) {
    assertThat(authenticationInfo).isInstanceOf(SimpleAuthenticationInfo.class);
    SimpleAuthenticationInfo simpleAuthenticationInfo = (SimpleAuthenticationInfo) authenticationInfo;
    assertThat(simpleAuthenticationInfo.getCredentials()).isNull();
    assertThat(simpleAuthenticationInfo.getPrincipals()).isNotEmpty();
    assertThat(simpleAuthenticationInfo.getPrincipals().getRealmNames()).containsExactly("SAML");
    Object primaryPrincipal = simpleAuthenticationInfo.getPrincipals().getPrimaryPrincipal();
    assertThat(primaryPrincipal).isInstanceOf(UserPrincipal.class);
    UserPrincipal userPrincipal = (UserPrincipal) primaryPrincipal;
    assertThat(userPrincipal.getRealmId()).isEqualTo(SamlRealm.ID);
    return userPrincipal;
  }

  private SamlUser createSamlUser() {
    return new SamlUser("someUsername", "someFirstName", "someLastName", "someEmail@someDomain.com",
        new LinkedHashSet<>(Arrays.asList("someGroup1", "someGroup2")));
  }
}
