/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.assertEqualExceptNullDTOPassword;
import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.assertMatchingUser;
import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.createUserDTOToAdd;
import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.createUserDTOToUpdate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.sonatype.insight.brain.api.v2.dto.ApiUserDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserListDTO;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.UserService.ChangePasswordDTO;
import com.sonatype.insight.brain.security.UserService.FindMembersDTO;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class UserServiceTest
    extends AbstractComponentTest
{
  @Inject
  private UserService userService;

  @Inject
  private UserDAO userDAO;

  @Inject
  private SamlUserDAO samlUserDAO;

  @Inject
  private OAuth2UserDAO oAuth2UserDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private ApiConfigurationService configurationService;

  @Mock
  private SessionDAO sessionDAOMock;

  @Mock
  private ProductLicense productLicenseMock;

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();

  @Override
  protected void grantDefaultTestUserAllPermissions() {
    tempEntity.newUser(USERNAME, "Fixture", "User", USERNAME + "@void.com");
    var role = tempEntity.newRole(true, Permission.values());
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), USERNAME);
  }

  @After
  public void resetDefaultPasswordWarningConfiguration() {
    configurationService
        .deleteConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING);
  }

  @Test
  public void testDeleteUserNoLdapRemovesContact() {
    // Create a user
    String clmUserName = "test-user";
    User user = tempEntity.newUser(clmUserName);

    // Create an application with the user as the contact
    Application application = createApplication(user);
    // Check to see that the contact is the userName
    assertThat(application.getContactInternalName()).isEqualTo(clmUserName);

    // Delete the user
    userService.deleteUser(user.getId());

    // Check to see if the contact has also been deleted
    application = applicationDAO.getById(application.getId());
    assertThat(application).isNotNull();
    assertThat(application.getContactInternalName()).isNull();
  }

  @Test
  public void testDeleteUserNoLdapMatchRemovesContact() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserServiceTest/ldap_users.ldif");

    final LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    final String clmUserName = "clm-test-user";

    // Make sure the clm user is not in LDAP
    final FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, clmUserName, false /* groupsEnabled */);
    assertThat(findMembersDTO.getMembers()).isEmpty();

    // Add the user to CLM
    final User user = tempEntity.newUser(clmUserName);

    // Create an application with the user as the contact
    Application application = createApplication(user);
    // Check to see that the contact is the userName
    assertThat(application.getContactInternalName()).isEqualTo(clmUserName);

    // Delete the user
    userService.deleteUser(user.getId());

    // Check to see if the application contact has also been deleted
    application = applicationDAO.getById(application.getId());
    assertThat(application).isNotNull();
    assertThat(application.getContactInternalName()).isNull();
  }

  @Test
  public void testDeleteUserWithLdapMatchDoesNotRemoveContact() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserServiceTest/ldap_users.ldif");

    final LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    final String clmAndLdapUserName = "johndoe";

    // Check LDAP for the user
    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, "John Doe", false /* groupsEnabled */);
    assertMember(findMembersDTO, null, MemberType.USER, clmAndLdapUserName, "John Doe", "test.user@company.com",
        "LDAP");

    // Create the same user in CLM
    final User user = tempEntity.newUser(clmAndLdapUserName);

    // Create an application with the user as the contact
    Application application = createApplication(user);
    // Check to see that the contact is the userName
    assertThat(application.getContactInternalName()).isEqualTo(clmAndLdapUserName);

    // Delete the user
    userService.deleteUser(user.getId());

    // Check to see if the application contact has not been deleted
    application = applicationDAO.getById(application.getId());
    assertThat(application).isNotNull();
    assertThat(application.getContactInternalName()).isEqualTo(clmAndLdapUserName);
  }

  private Application createApplication(User contactUser) {
    // Create an organization and application
    final Organization organization = tempEntity.newOrganization();
    return tempEntity.newApplication("My App", "my-app", organization.getId(), contactUser.getUsername());
  }

  @Test
  public void testResetPassword() {
    User user = tempEntity.newUser("testResetPassword");
    user.setPassword("testResetPasswordPassword");

    ChangePasswordDTO dto = userService.resetPassword(user.getId());
    assertThat(dto.newPassword).hasSize(12);
    assertThat(StringUtils.isAlphanumeric(dto.newPassword)).isTrue();
  }

  @Test
  public void testUpdateUser_InternalUser() {
    User user = tempEntity.newUser("TestUsername", "TestFirstName", "TestLastName", "TestEmail@example.com");
    user = userDAO.getByIdNotNull(user.getId());

    User updatedUser = userDAO.getByIdNotNull(user.getId());
    updatedUser.setPassword(UserService.FAKE_PASSWORD);
    updatedUser.setFirstName("TestFirstNameUpdated");
    updatedUser.setLastName("TestLastNameUpdated");
    updatedUser.setEmail("TestEmailUpdated@example.com");

    updatedUser = userService.updateUser(updatedUser);

    assertInternalUser(updatedUser, user.getId(), "TestUsername", UserService.FAKE_PASSWORD, "TestFirstNameUpdated",
        "TestLastNameUpdated", "TestEmailUpdated@example.com");
    assertInternalUser(userDAO.getByIdNotNull(user.getId()), user.getId(), "TestUsername", user.getPassword(),
        "TestFirstNameUpdated", "TestLastNameUpdated", "TestEmailUpdated@example.com");
  }

  @Test
  public void testUpdateUser_InternalUser_CannotChangePassword() {
    User user = tempEntity.newUser("TestUsername", "TestFirstName", "TestLastName", "TestEmail@example.com");
    user = userDAO.getByIdNotNull(user.getId());

    User updatedUser = userDAO.getByIdNotNull(user.getId());
    updatedUser.setPassword("PasswordUpdated");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> userService.updateUser(updatedUser))
        .withMessage("Cannot change user password.");

    assertInternalUser(userDAO.getByIdNotNull(user.getId()), user.getId(), "TestUsername", user.getPassword(),
        "TestFirstName", "TestLastName", "TestEmail@example.com");
  }

  @Test
  public void testUpdateUser_InternalUser_CannotChangeUsername() {
    User user = tempEntity.newUser("TestUsername", "TestFirstName", "TestLastName", "TestEmail@example.com");
    user = userDAO.getByIdNotNull(user.getId());

    User updatedUser = userDAO.getByIdNotNull(user.getId());
    updatedUser.setPassword(UserService.FAKE_PASSWORD);
    updatedUser.setUsername("TestUsernameUpdated");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> userService.updateUser(updatedUser))
        .withMessage("Cannot change username.");

    assertInternalUser(userDAO.getByIdNotNull(user.getId()), user.getId(), "TestUsername", user.getPassword(),
        "TestFirstName", "TestLastName", "TestEmail@example.com");
  }

  private void assertInternalUser(
      User actualUser,
      String id,
      String username,
      String password,
      String firstName,
      String lastName,
      String email)
  {
    assertThat(actualUser.getId()).isEqualTo(id);
    assertThat(actualUser.getUsername()).isEqualTo(username);
    assertThat(actualUser.getPassword()).isEqualTo(password);
    assertThat(actualUser.getFirstName()).isEqualTo(firstName);
    assertThat(actualUser.getLastName()).isEqualTo(lastName);
    assertThat(actualUser.getEmail()).isEqualTo(email);
  }

  @Test
  public void testFindMembersForGlobalRoles_EmptyQuery() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> userService.findMembersForRoles(OwnerType.GLOBAL, null, "" /* query */, false /* groupsEnabled */))
        .withMessage("No search term specified.");
  }

  @Test
  public void testFindMembersForNonGlobalRoles_EmptyQuery() {
    Organization org = tempEntity.newOrganization();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> userService.findMembersForRoles(OwnerType.ORGANIZATION, org.getId(), "" /* query */,
            false /* groupsEnabled */))
        .withMessage("No search term specified.");
  }

  @Test
  public void testFindIqUsers() {
    FindMembersDTO findMembersDTO = userService.findMembersForRoles(OwnerType.GLOBAL, null, User.ADMIN_USERNAME + "*",
        false /* groupsEnabled */);
    assertMember(findMembersDTO, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost",
        "IQ Server");

    findMembersDTO = userService.findMembersForRoles(OwnerType.GLOBAL, null,
        User.ADMIN_USERNAME.substring(0, User.ADMIN_USERNAME.length() - 1) + "*", false /* groupsEnabled */);
    assertMember(findMembersDTO, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost",
        "IQ Server");

    findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, "nobody-has-such-a-name-really*", false /* groupsEnabled */);
    assertThat(findMembersDTO.getError()).isNull();
    assertThat(findMembersDTO.getMembers()).isEmpty();
  }

  @Test
  public void testFindLdapUser() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserServiceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, "John Doe", false /* groupsEnabled */);
    assertMember(findMembersDTO, null, MemberType.USER, "johndoe", "John Doe", "test.user@company.com", "LDAP");

    tempEntity.newUser("johndoe");

    // Test shading. johndoe loaded from "/UserServiceTest/ldap_users.ldif" should not be returned
    findMembersDTO = userService.findMembersForRoles(OwnerType.GLOBAL, null, "John Doe", false /* groupsEnabled */);
    assertMember(findMembersDTO, null, MemberType.USER, "johndoe", "John Doe", "johndoe@void.com", "IQ Server");
  }

  @Test
  public void testFindLdapGroup() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserServiceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, "Alpha", true /* groupsEnabled */);
    assertMember(findMembersDTO, null, MemberType.GROUP, "Alpha", "Alpha", null, "LDAP");
  }

  @Test
  public void testFindLdapUserAndGroupWithSameName() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserServiceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, "Beta", true /* groupsEnabled */);
    List<Member> members = findMembersDTO.getMembers();
    assertThat(members).hasSize(2);

    assertMember(members.get(0), MemberType.USER, "Beta", "Beta", "beta.user@company.com", "LDAP");
    assertMember(members.get(1), MemberType.GROUP, "Beta", "Beta", null, "LDAP");
  }

  @Test
  public void testNoLdapConnection() {
    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    FindMembersDTO findMembersDTO = userService.findMembersForRoles(OwnerType.GLOBAL, null, User.ADMIN_USERNAME + "*",
        false /* groupsEnabled */);

    // Should not try to use Ldap until server is added and configured
    assertMember(findMembersDTO, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost",
        "IQ Server");

    tempEntity.newLdapConnection(ldapServer.getId());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, User.ADMIN_USERNAME + "*", false /* groupsEnabled */);
    assertMember(findMembersDTO, "LDAP error, displaying partial results.", MemberType.USER, User.ADMIN_USERNAME,
        "Admin BuiltIn", "admin@localhost", "IQ Server");
  }

  @Test
  public void testShouldDisplayDefaultPasswordWarning() {
    assertThat(userService.shouldDisplayDefaultPasswordWarning()).isTrue();

    User admin = userDAO.getByUsername(User.ADMIN_USERNAME);
    String originalAdminPassword = admin.getPassword();
    try {
      admin.setPassword("foo");
      userDAO.update(admin);
      assertThat(userService.shouldDisplayDefaultPasswordWarning()).isFalse();
    }
    finally {
      admin.setPassword(originalAdminPassword);
      userDAO.update(admin);
    }

    assertThat(userService.shouldDisplayDefaultPasswordWarning()).isTrue();
  }

  @Test
  public void testShouldDisplayDefaultPasswordWarning_DisabledByConfig() {
    assertThat(userService.shouldDisplayDefaultPasswordWarning()).isTrue();

    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING,
        false);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING);

    assertThat(userService.shouldDisplayDefaultPasswordWarning()).isFalse();
  }

  @Test
  public void testShouldDisplayDefaultPasswordWarning_NoAdminUser() {
    assertThat(userService.shouldDisplayDefaultPasswordWarning()).isTrue();

    User admin = userDAO.getByUsername(User.ADMIN_USERNAME);
    admin.setUsername("admin2");
    userDAO.update(admin);

    try {
      assertThat(userService.shouldDisplayDefaultPasswordWarning()).isFalse();
    }
    finally {
      admin.setUsername(User.ADMIN_USERNAME);
      userDAO.update(admin);
    }
  }

  @Test
  public void testGetAllApiUserDTOs_Internal() {
    testGetAllApiUserDTOs_User(User.INTERNAL_REALM_ID, User.INTERNAL_REALM_ID);
  }

  @Test
  public void testGetAllApiUserDTOs_Saml() {
    enableSsoWithSaml();

    SamlUser user1 = tempEntity.newSamlUser();
    SamlUser user2 = tempEntity.newSamlUser();

    ApiUserListDTO apiUserListDTO = userService.getAllApiUserDTOs(SamlRealm.ID);

    assertThat(apiUserListDTO).isNotNull();
    assertThat(apiUserListDTO.users).hasSize(2);
    assertContainsApiUserDTOMatchingUser(apiUserListDTO.users, SsoUser.fromSamlUser(user1));
    assertContainsApiUserDTOMatchingUser(apiUserListDTO.users, SsoUser.fromSamlUser(user2));
  }

  @Test
  public void testGetAllApiUserDTOs_OAuth2() {
    enableSsoWithOAuth2();

    OAuth2User user1 = tempEntity.newOAuth2User();
    OAuth2User user2 = tempEntity.newOAuth2User();

    ApiUserListDTO apiUserListDTO = userService.getAllApiUserDTOs(OAuth2Realm.ID);

    assertThat(apiUserListDTO).isNotNull();
    assertThat(apiUserListDTO.users).hasSize(2);
    assertContainsApiUserDTOMatchingUser(apiUserListDTO.users, SsoUser.fromOAuth2User(user1));
    assertContainsApiUserDTOMatchingUser(apiUserListDTO.users, SsoUser.fromOAuth2User(user2));
  }

  private void testGetAllApiUserDTOs_User(String queryRealm, String expectedRealm) {
    User admin = userDAO.getByUsername(User.ADMIN_USERNAME);
    User fixtureUser = userDAO.getByUsernameNotNull(USERNAME);
    User user = tempEntity.newUser();

    ApiUserListDTO apiUserListDTO = userService.getAllApiUserDTOs(queryRealm);

    assertThat(apiUserListDTO).isNotNull();
    assertThat(apiUserListDTO.users).hasSize(3);
    assertContainsApiUserDTOMatchingUser(apiUserListDTO.users, admin, expectedRealm);
    assertContainsApiUserDTOMatchingUser(apiUserListDTO.users, fixtureUser, expectedRealm);
    assertContainsApiUserDTOMatchingUser(apiUserListDTO.users, user, expectedRealm);
  }

  private void assertContainsApiUserDTOMatchingUser(List<ApiUserDTO> apiUserDTOs, User user, String realm) {
    ApiUserDTO apiUserDTO =
        apiUserDTOs.stream().filter(dto -> dto.username.equals(user.getUsername())).findFirst().orElse(null);
    assertThat(apiUserDTO).isNotNull();
    assertEqualExceptNullDTOPassword(user, apiUserDTO);
    assertThat(apiUserDTO.realm).isEqualTo(realm);
  }

  private void assertContainsApiUserDTOMatchingUser(List<ApiUserDTO> apiUserDTOs, SsoUser user) {
    ApiUserDTO userDTO =
        apiUserDTOs.stream().filter(dto -> dto.username.equals(user.getUsername())).findFirst().orElse(null);
    assertThat(userDTO).isNotNull();
    assertThat(userDTO.username).isEqualTo(user.getUsername());
    assertThat(userDTO.password).isNull();
    assertThat(userDTO.firstName).isEqualTo(user.getFirstName());
    assertThat(userDTO.lastName).isEqualTo(user.getLastName());
    assertThat(userDTO.email).isEqualTo(user.getEmail());
    assertThat(userDTO.realm).isEqualTo(user.getRealmId());
  }

  @Test
  public void testGetApiUserDTOByUsernameAndRealmId() {
    User user = tempEntity.newUser();

    assertEqualExceptNullDTOPassword(user, userService.getApiUserDTOByUsernameAndRealmId(
        user.getUsername(), User.INTERNAL_REALM_ID));
  }

  @Test
  public void testGetApiUserDTOByUsernameAndRealmId_Saml() {
    enableSsoWithSaml();

    SamlUser samlUser = tempEntity.newSamlUser();
    List<ApiUserDTO> apiUserDTOs = new ArrayList<>();

    ApiUserDTO apiUserDTO = userService.getApiUserDTOByUsernameAndRealmId(samlUser.getUsername(),
        SamlRealm.ID);
    apiUserDTOs.add(apiUserDTO);
    assertContainsApiUserDTOMatchingUser(apiUserDTOs, SsoUser.fromSamlUser(samlUser));
  }

  @Test
  public void testGetApiUserDTOByUsernameAndRealmId_OAuth2() {
    enableSsoWithOAuth2();

    OAuth2User oauth2User = tempEntity.newOAuth2User();
    List<ApiUserDTO> apiUserDTOs = new ArrayList<>();

    ApiUserDTO apiUserDTO = userService.getApiUserDTOByUsernameAndRealmId(oauth2User.getUsername(),
        OAuth2Realm.ID);
    apiUserDTOs.add(apiUserDTO);
    assertContainsApiUserDTOMatchingUser(apiUserDTOs, SsoUser.fromOAuth2User(oauth2User));
  }

  @Test
  public void testGetApiUserDTOByUsernameAndRealmId_Internal() {
    testGetApiUserDTOByUsernameAndRealmId(User.INTERNAL_REALM_ID, User.INTERNAL_REALM_ID);
  }

  private void testGetApiUserDTOByUsernameAndRealmId(String queryRealm, String expectedRealm) {
    User user = tempEntity.newUser();
    List<ApiUserDTO> apiUserDTOs = new ArrayList<>();

    ApiUserDTO apiUserDTO = userService.getApiUserDTOByUsernameAndRealmId(user.getUsername(), queryRealm);
    apiUserDTOs.add(apiUserDTO);
    assertContainsApiUserDTOMatchingUser(apiUserDTOs, user, expectedRealm);

  }

  @Test
  public void testGetApiUserDTOByUsernameAndRealmId_UserDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> userService.getApiUserDTOByUsernameAndRealmId("doesNotExist", User.INTERNAL_REALM_ID))
        .withMessage("Cannot find a user with username doesNotExist.");
  }

  @Test
  public void testGetApiUserDTOByUsernameAndRealmId_UserDoesNotExist_Saml() {
    enableSsoWithSaml();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> userService.getApiUserDTOByUsernameAndRealmId("doesNotExist", SamlRealm.ID))
        .withMessage("Cannot find a SAML user with username doesNotExist.");
  }

  @Test
  public void testGetApiUserDTOByUsernameAndRealmId_UserDoesNotExist_OAuth2() {
    enableSsoWithOAuth2();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> userService.getApiUserDTOByUsernameAndRealmId("doesNotExist", OAuth2Realm.ID))
        .withMessage("Cannot find a OAuth2 user with username doesNotExist.");
  }

  @Test
  public void testAddUser() {
    ApiUserDTO inputUserDTO = createUserDTOToAdd();

    userService.addUser(inputUserDTO);

    assertMatchingUser(inputUserDTO, userDAO.getByUsernameNotNull(inputUserDTO.username));
  }

  @Test
  public void testAddUser_NullBody() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> userService.addUser((ApiUserDTO) null))
        .withMessageContaining("No user details specified");
  }

  @Test
  public void testUpdateUser() {
    ApiUserDTO inputUserDTO = createUserDTOToUpdate(tempEntity.newUser());

    ApiUserDTO outputUserDTO = userService.updateUser(inputUserDTO.username, inputUserDTO);

    assertOnlyPasswordNull(outputUserDTO);
    assertInputNullOrEqualToOutputIgnoringPassword(inputUserDTO, outputUserDTO);
    assertMatchingUser(outputUserDTO, userDAO.getByUsernameNotNull(inputUserDTO.username));
  }

  @Test
  public void testUpdateUser_NullBody() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> userService.updateUser(tempEntity.newUser().getUsername(), null))
        .withMessageContaining("No user details specified");
  }

  @Test
  public void testUpdateUser_NoUsername() {
    User user = tempEntity.newUser();
    ApiUserDTO inputUserDTO = createUserDTOToUpdate(user);
    inputUserDTO.username = null;

    ApiUserDTO outputUserDTO = userService.updateUser(user.getUsername(), inputUserDTO);

    assertOnlyPasswordNull(outputUserDTO);
    assertInputNullOrEqualToOutputIgnoringPassword(inputUserDTO, outputUserDTO);
    assertMatchingUser(outputUserDTO, userDAO.getByUsernameNotNull(user.getUsername()));
  }

  @Test
  public void testUpdateUser_OnlyUsername() {
    ApiUserDTO inputUserDTO = new ApiUserDTO();
    inputUserDTO.username = tempEntity.newUser().getUsername();

    ApiUserDTO outputUserDTO = userService.updateUser(inputUserDTO.username, inputUserDTO);

    assertOnlyPasswordNull(outputUserDTO);
    assertInputNullOrEqualToOutputIgnoringPassword(inputUserDTO, outputUserDTO);
    assertMatchingUser(outputUserDTO, userDAO.getByUsernameNotNull(inputUserDTO.username));
  }

  @Test
  public void testUpdateUser_Username() {
    ApiUserDTO inputUserDTO = createUserDTOToUpdate(tempEntity.newUser());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> userService.updateUser("differentUsername", inputUserDTO))
        .withMessage("Cannot change username.");
  }

  @Test
  public void testUpdateUser_Password() {
    ApiUserDTO inputUserDTO = createUserDTOToUpdate(tempEntity.newUser());
    inputUserDTO.password = "";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> userService.updateUser(inputUserDTO.username, inputUserDTO))
        .withMessage("Cannot change user password.");
  }

  @Test
  public void testDeleteUserByRealmIdAndUsername_InternalUserDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> userService.deleteUserByRealmIdAndUsername(User.INTERNAL_REALM_ID, "doesNotExist"))
        .withMessage("Cannot find a user with username doesNotExist.");
  }

  @Test
  public void testDeleteUserByRealmIdAndUsername_SamlUserDoesNotExist() {
    enableSsoWithSaml();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> userService.deleteUserByRealmIdAndUsername(SamlRealm.ID, "doesNotExist"))
        .withMessage("Cannot find a SAML user with username doesNotExist.");
  }

  @Test
  public void testDeleteUserByRealmIdAndUsername_OAuth2UserDoesNotExist() {
    enableSsoWithOAuth2();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> userService.deleteUserByRealmIdAndUsername(OAuth2Realm.ID, "doesNotExist"))
        .withMessage("Cannot find a OAuth2 user with username doesNotExist.");
  }

  @Test
  public void testDeleteUserByRealmIdAndUsername_NoRealmId() {
    testDeleteUserByRealmIdAndUsername(null);
  }

  @Test
  public void testDeleteUserByRealmIdAndUsername_UnknownRealmId() {
    testDeleteUserByRealmIdAndUsername("unknown");
  }

  @Test
  public void testDeleteUserByRealmIdAndUsername_InternalRealmId() {
    testDeleteUserByRealmIdAndUsername("InTeRnAl");
  }

  @Test
  public void testDeleteUserByRealmIdAndUsername_SamlRealmId() {
    enableSsoWithSaml();
    testDeleteUserByRealmIdAndUsername("SaMl");
  }

  @Test
  public void testDeleteUserByRealmIdAndUsername_OAuth2RealmId() {
    enableSsoWithOAuth2();
    testDeleteUserByRealmIdAndUsername("oauth2");
  }

  private void testDeleteUserByRealmIdAndUsername(String realmId) {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();

    OAuth2User oauth2User1 = tempEntity.newOAuth2User(samlUser1.getUsername());
    OAuth2User oauth2User2 = tempEntity.newOAuth2User();

    User user = tempEntity.newUser(samlUser1.getUsername());

    userService.deleteUserByRealmIdAndUsername(realmId, samlUser1.getUsername());
    if (SamlRealm.ID.equalsIgnoreCase(realmId)) {
      assertThat(samlUserDAO.getById(samlUser1.getId())).isNull();
      assertThat(samlUserDAO.getById(samlUser2.getId())).isNotNull();
      assertThat(oAuth2UserDAO.getById(oauth2User1.getId())).isNotNull();
      assertThat(oAuth2UserDAO.getById(oauth2User2.getId())).isNotNull();
      assertThat(userDAO.getById(user.getId())).isNotNull();
      assertThat(userDAO.getByUsername(User.ADMIN_USERNAME)).isNotNull();
    }
    else if (OAuth2Realm.ID.equalsIgnoreCase(realmId)) {
      assertThat(samlUserDAO.getById(samlUser1.getId())).isNotNull();
      assertThat(samlUserDAO.getById(samlUser2.getId())).isNotNull();
      assertThat(oAuth2UserDAO.getById(oauth2User1.getId())).isNull();
      assertThat(oAuth2UserDAO.getById(oauth2User2.getId())).isNotNull();
      assertThat(userDAO.getById(user.getId())).isNotNull();
      assertThat(userDAO.getByUsername(User.ADMIN_USERNAME)).isNotNull();
    }
    else {
      assertThat(samlUserDAO.getById(samlUser1.getId())).isNotNull();
      assertThat(samlUserDAO.getById(samlUser2.getId())).isNotNull();
      assertThat(oAuth2UserDAO.getById(oauth2User1.getId())).isNotNull();
      assertThat(oAuth2UserDAO.getById(oauth2User2.getId())).isNotNull();
      assertThat(userDAO.getById(user.getId())).isNull();
      assertThat(userDAO.getByUsername(User.ADMIN_USERNAME)).isNotNull();
    }
  }

  private void assertOnlyPasswordNull(ApiUserDTO userDTO) {
    assertThat(userDTO.username).isNotNull();
    assertThat(userDTO.password).isNull();
    assertThat(userDTO.firstName).isNotNull();
    assertThat(userDTO.lastName).isNotNull();
    assertThat(userDTO.email).isNotNull();
  }

  private void assertInputNullOrEqualToOutputIgnoringPassword(ApiUserDTO inputUserDTO, ApiUserDTO outputUserDTO) {
    assertInputNullOrEqualToOutput(inputUserDTO.username, outputUserDTO.username);
    assertInputNullOrEqualToOutput(inputUserDTO.firstName, outputUserDTO.firstName);
    assertInputNullOrEqualToOutput(inputUserDTO.lastName, outputUserDTO.lastName);
    assertInputNullOrEqualToOutput(inputUserDTO.email, outputUserDTO.email);
  }

  private void assertInputNullOrEqualToOutput(String input, String output) {
    assertThat(input).satisfiesAnyOf(s -> assertThat(s).isNull(), s -> assertThat(s).isEqualTo(output));
  }

  private void assertMember(
      FindMembersDTO findMembersDTO,
      String error,
      MemberType type,
      String name,
      String displayName,
      String email,
      String realm)
  {
    assertThat(findMembersDTO.getError()).isEqualTo(error);

    Member[] members = findMembersDTO.getMembers().toArray(new Member[0]);
    assertThat(members).hasSize(1);
    assertMember(members[0], type, name, displayName, email, realm);
  }

  private void assertMember(
      final Member member,
      final MemberType type,
      final String name,
      final String displayName,
      final String email,
      final String realm)
  {
    assertThat(member.getType()).isEqualTo(type);
    assertThat(member.getInternalName()).isEqualTo(name);
    assertThat(member.getDisplayName()).isEqualTo(displayName);
    assertThat(member.getEmail()).isEqualTo(email);
    assertThat(member.getRealm()).isEqualTo(realm);
  }
}
