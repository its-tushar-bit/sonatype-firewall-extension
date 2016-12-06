/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.StringUtils;
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

  @Test
  public void testCRUD() {
    String name = "name";

    // insert

    LdapServer server = tempEntity.newLdapServer(name);

    // select by id

    LdapServer echo = dao.getById(server.getId());
    assertNotNull(echo);
    assertEquals(name, echo.getName());
    assertEquals(NameHelper.normalize(name), echo.getNameLowercaseNoWhitespace());

    // select by name

    echo = dao.getByName(name);
    assertNotNull(echo);

    // update

    String changedName = "changedName";
    server.setName(changedName);
    dao.update(server);
    echo = dao.getById(server.getId());
    assertEquals(changedName, echo.getName());

    // delete
    dao.delete(server);
    assertNull(dao.getById(server.getId()));
  }

  @Test
  public void testValidateNullName_Insert() {
    LdapServer ldapServer = new LdapServer(null /* name */);
    try {
      dao.insert(ldapServer);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullName_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("testValidateNullName");
    assertEquals("testvalidatenullname", ldapServer.getNameLowercaseNoWhitespace());

    ldapServer.setName(null);
    assertNull(ldapServer.getNameLowercaseNoWhitespace());
    try {
      dao.update(ldapServer);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Insert() {
    try {
      tempEntity.newLdapServer(" ");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("testValidateEmptyName");
    assertEquals("testvalidateemptyname", ldapServer.getNameLowercaseNoWhitespace());

    ldapServer.setName(" ");
    assertEquals("", ldapServer.getNameLowercaseNoWhitespace());
    try {
      dao.update(ldapServer);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      LdapServer ldapServer = new LdapServer(name);
      try {
        dao.insert(ldapServer);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0)), expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("testValidateNameInvalidChars");
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      ldapServer.setName(name);
      try {
        dao.update(ldapServer);
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
        tempEntity.newLdapServer(name);
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
    LdapServer ldapServer = tempEntity.newLdapServer("testValidateNameSpaces");
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      ldapServer.setName(name);
      try {
        dao.update(ldapServer);
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

    LdapServer ldapServer = tempEntity.newLdapServer(name);

    assertEquals(name, ldapServer.getName());
    assertEquals("teststringwithcaseandwhitespace", ldapServer.getNameLowercaseNoWhitespace());

    String name1 = "TEST String      With    cASE and      whitespace";
    LdapServer ldapServer1 = dao.getByName(name1);
    assertNotNull(ldapServer1);
    assertEquals(ldapServer.getId(), ldapServer1.getId());
  }

  @Test
  public void testDuplicateName_Insert() {
    tempEntity.newLdapServer("testDuplicateName");

    try {
      tempEntity.newLdapServer("testDuplicateName");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("testDuplicateName is already used as a name.", expected.getMessage());
    }
  }

  @Test
  public void testDuplicateName_Update() {
    tempEntity.newLdapServer("testDuplicateName");
    LdapServer ldapServer = tempEntity.newLdapServer("testDuplicateName1");

    ldapServer.setName("Test Duplicate Name");
    try {
      dao.update(ldapServer);
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
      tempEntity.newLdapServer(name + "a");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    tempEntity.newLdapServer(name);
  }

  @Test
  public void testValidateNameLength_Update() {
    LdapServer ldapServer = tempEntity.newLdapServer("test name");

    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    ldapServer.setName(name + "a");
    try {
      dao.update(ldapServer);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    ldapServer.setName(name);
    dao.update(ldapServer);
  }

  @Test
  public void testInsert_AutoIncrementsPriority() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    assertThat(ldapServer2.getPriority(), is(greaterThan(ldapServer1.getPriority())));
  }

  @Test
  public void testUpdatePriority() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    List<String> serverPriorityList = new ArrayList<>();
    serverPriorityList.add(ldapServer2.getId());
    serverPriorityList.add(ldapServer1.getId());

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
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    List<String> mismatchServerList = Collections.singletonList(ldapServer2.getId());

    expectedException.expect(DataAccessException.class);
    expectedException.expectMessage("Unable to update priority of Ldap servers due to server list mismatch.");

    dao.updatePriority(mismatchServerList);
  }

  @Test
  public void testUpdatePriority_DuplicateServers() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    List<String> serverPriorityList = new ArrayList<>();
    serverPriorityList.add(ldapServer1.getId());
    serverPriorityList.add(ldapServer1.getId());
    serverPriorityList.add(ldapServer2.getId());

    expectedException.expect(DataAccessException.class);
    expectedException.expectMessage("Unable to update priority of Ldap servers due to duplicate server IDs.");

    dao.updatePriority(serverPriorityList);
  }

  @Test
  public void testUpdatePriority_IncorrectServerId() {
    tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    List<String> serverPriorityList = new ArrayList<>();
    serverPriorityList.add(ldapServer2.getId());
    serverPriorityList.add("incorrectServerId");

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Cannot find LdapServer with ID incorrectServerId");

    dao.updatePriority(serverPriorityList);
  }
}
