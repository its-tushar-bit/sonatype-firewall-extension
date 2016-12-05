/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class LdapServerDAOTest
    extends AbstractDbDAOTest
{
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private LdapServerDAO dao = new LdapServerDAO();

  protected Set<LdapServer> serversToDelete = new LinkedHashSet<>();

  @Test
  public void testCRUD() {
    String name = "name";

    // insert

    LdapServer server = new LdapServer();
    server.setName(name);
    Assert.assertNull(server.getId()); // sanity check
    dao.insert(server);

    // select by id

    LdapServer echo = dao.getById(server.getId());
    Assert.assertNotNull(echo);
    Assert.assertEquals(name, echo.getName());
    Assert.assertEquals(NameHelper.normalize(name), echo.getNameLowercaseNoWhitespace());

    // select by name

    echo = dao.getByName(name);
    Assert.assertNotNull(echo);

    // update

    String changedName = "changedName";
    server.setName(changedName);
    dao.update(server);
    echo = dao.getById(server.getId());
    Assert.assertEquals(changedName, echo.getName());

    // delete
    dao.delete(server);
    Assert.assertNull(dao.getById(server.getId()));
  }

  @Test
  public void testValidateNullName_Insert() {
    LdapServer config = createLdapServer(null /* name */);
    try {
      dao.insert(config);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullName_Update() {
    LdapServer config = insertLdapServer("testValidateNullName");
    assertEquals("testvalidatenullname", config.getNameLowercaseNoWhitespace());

    config.setName(null);
    assertNull(config.getNameLowercaseNoWhitespace());
    try {
      dao.update(config);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Insert() {
    try {
      insertLdapServer(" ");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Update() {
    LdapServer config = insertLdapServer("testValidateEmptyName");
    assertEquals("testvalidateemptyname", config.getNameLowercaseNoWhitespace());

    config.setName(" ");
    assertEquals("", config.getNameLowercaseNoWhitespace());
    try {
      dao.update(config);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      LdapServer config = createLdapServer(name);
      try {
        dao.insert(config);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0)), expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    LdapServer config = insertLdapServer("testValidateNameInvalidChars");
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      config.setName(name);
      try {
        dao.update(config);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0)), expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameValidChars_Insert() {
    for (String name : NameHelperTest.VALID_NAMES) {
      tempEntity.newLdapServer(name);
    }
  }

  @Test
  public void testValidateNameValidChars_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("a");
    for (String name : NameHelperTest.VALID_NAMES) {
      ldapServer.setName(name);
      dao.update(ldapServer);
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      try {
        insertLdapServer(name);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must not have leading or trailing spaces, or have two spaces in a row.",
            expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameSpaces_Update() {
    LdapServer config = insertLdapServer("testValidateNameSpaces");
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      config.setName(name);
      try {
        dao.update(config);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must not have leading or trailing spaces, or have two spaces in a row.",
            expected.getMessage());
      }
    }
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String name = "test string With Case and Whitespace";

    LdapServer config = insertLdapServer(name);

    assertEquals(name, config.getName());
    assertEquals("teststringwithcaseandwhitespace", config.getNameLowercaseNoWhitespace());

    String name1 = "TEST String      With    cASE and      whitespace";
    LdapServer config1 = dao.getByName(name1);
    assertNotNull(config1);
    assertEquals(config.getId(), config1.getId());
  }

  @Test
  public void testDuplicateName_Insert() {
    insertLdapServer("testDuplicateName");

    try {
      insertLdapServer("testDuplicateName");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("testDuplicateName is already used as a name.", expected.getMessage());
    }
  }

  @Test
  public void testDuplicateName_Update() {
    insertLdapServer("testDuplicateName");
    LdapServer config = insertLdapServer("testDuplicateName1");

    config.setName("Test Duplicate Name");
    try {
      dao.update(config);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Test Duplicate Name is already used as a name.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameLength_Insert() {
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    try {
      insertLdapServer(name + "a");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    insertLdapServer(name);
  }

  @Test
  public void testValidateNameLength_Update() {
    LdapServer config = insertLdapServer("test name");

    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    config.setName(name + "a");
    try {
      dao.update(config);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    config.setName(name);
    dao.update(config);
  }

  protected LdapServer createLdapServer(String name) {
    LdapServer config = new LdapServer();
    config.setName(name);
    return config;
  }

  protected LdapServer insertLdapServer(String name) {
    LdapServer config = createLdapServer(name);
    dao.insert(config);
    serversToDelete.add(config);
    return config;
  }

  @Test
  public void testInsert_AutoIncrementsPriority() {
    LdapServer config1 = tempEntity.newLdapServer("test1");
    LdapServer config2 = tempEntity.newLdapServer("test2");

    assertThat(config2.getPriority(), is(greaterThan(config1.getPriority())));
  }

  @Test
  public void testUpdatePriority() {
    LdapServer config1 = tempEntity.newLdapServer("test1");
    LdapServer config2 = tempEntity.newLdapServer("test2");

    List<String> serverPriorityList = new ArrayList<>();
    serverPriorityList.add(config2.getId());
    serverPriorityList.add(config1.getId());

    dao.updatePriority(serverPriorityList);

    List<LdapServer> servers = dao.getAll();
    assertThat(servers.get(0).getName(), is("test2"));
    assertThat(servers.get(0).getPriority(), is(1));
    assertThat(servers.get(1).getName(), is("test1"));
    assertThat(servers.get(1).getPriority(), is(2));
  }

  @Test
  public void testUpdatePriority_IncorrectNumberOfServers() {
    tempEntity.newLdapServer("test1");
    LdapServer config2 = tempEntity.newLdapServer("test2");

    List<String> mismatchServerList = Collections.singletonList(config2.getId());

    expectedException.expect(DataAccessException.class);
    expectedException.expectMessage("Unable to update priority of Ldap servers due to server list mismatch.");

    dao.updatePriority(mismatchServerList);
  }

  @Test
  public void testUpdatePriority_DuplicateServers() {
    LdapServer config1 = tempEntity.newLdapServer("test1");
    LdapServer config2 = tempEntity.newLdapServer("test2");

    List<String> serverPriorityList = new ArrayList<>();
    serverPriorityList.add(config1.getId());
    serverPriorityList.add(config1.getId());
    serverPriorityList.add(config2.getId());

    expectedException.expect(DataAccessException.class);
    expectedException.expectMessage("Unable to update priority of Ldap servers due to duplicate server IDs.");

    dao.updatePriority(serverPriorityList);
  }

  @Test
  public void testUpdatePriority_IncorrectServerId() {
    tempEntity.newLdapServer("test1");
    LdapServer config2 = tempEntity.newLdapServer("test2");

    List<String> serverPriorityList = new ArrayList<>();
    serverPriorityList.add(config2.getId());
    serverPriorityList.add("incorrectServerId");

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Cannot find LdapServer with ID incorrectServerId");

    dao.updatePriority(serverPriorityList);
  }

  @After
  public void deleteLdapEntities() {
    for (LdapServer config : serversToDelete) {
      config = dao.getById(config.getId());
      if (config != null) {
        dao.delete(config);
      }
    }
  }
}
