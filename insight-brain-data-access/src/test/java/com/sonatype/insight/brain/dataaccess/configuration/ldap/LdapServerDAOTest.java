/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.util.LinkedHashSet;
import java.util.Set;

import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class LdapServerDAOTest
    extends AbstractDbDAOTest
{
  private LdapServerDAO dao = new LdapServerDAO();

  protected Set<LdapServer> serversToDelete = new LinkedHashSet<LdapServer>();

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
    String[] invalidAlphaNumericNames = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };
    for (String name : invalidAlphaNumericNames) {
      LdapServer config = createLdapServer(name);
      try {
        dao.insert(config);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameInvalidChars_Update() {
    LdapServer config = insertLdapServer("testValidateNameInvalidChars");
    String[] invalidAlphaNumericNames = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };
    for (String name : invalidAlphaNumericNames) {
      config.setName(name);
      try {
        dao.update(config);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    String[] invalidSpacingNames = { " leading space", "trailing space ", "double  space",
        "  starts with double space", "ends with double space  " };
    for (String name : invalidSpacingNames) {
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

    String[] invalidSpacingNames = { " leading space", "trailing space ", "double  space",
        "  starts with double space", "ends with double space  " };
    for (String name : invalidSpacingNames) {
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

  @After
  @Override
  public void tearDown() {
    for (LdapServer config : serversToDelete) {
      config = dao.getById(config.getId());
      if (config != null) {
        dao.delete(config);
      }
    }
    super.tearDown();
  }
}
