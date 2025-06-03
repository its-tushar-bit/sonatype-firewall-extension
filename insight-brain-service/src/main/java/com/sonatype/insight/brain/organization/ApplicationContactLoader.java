/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.UserDirectory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationContactLoader
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationContactLoader.class);

  private final UserDirectory userDirectory;

  private ApplicationContactLoader(UserDirectory userDirectory) {
    this.userDirectory = userDirectory;
  }

  public static ApplicationContactLoader getInstance(UserDirectory userDirectory) {
    return new ApplicationContactLoader(userDirectory);
  }

  public ContactDTO getContact(String internalName) {
    return getContacts(Arrays.asList(internalName))[0];
  }

  /**
   * @param internalNamesList the list of contact internal names to look up
   * @return the contact DTO array (guaranteed to be the same size as the input list)
   */
  ContactDTO[] getContacts(List<String> internalNamesList) {
    if (internalNamesList == null || internalNamesList.isEmpty()) {
      return new ContactDTO[0];
    }

    // Preserving the original choice of an array and ordering as other parts of the API depend on this.
    ContactDTO[] contacts = new ContactDTO[internalNamesList.size()];

    Map<String, ContactDTO> nameToContactMap;
    UserDirectory.QueryResult result = userDirectory.getUsersByNames(new HashSet<>(internalNamesList));
    if (result.hasException()) {
      log.error("An exception occurred while trying to resolve user names; "
          + "attempting to resolve user names using the local Nexus IQ realm.", result.getException());

      // Map the existing names potentially loaded by the CLM data store.
      nameToContactMap = mapNameToContact(result.get());
      // Add the remaining names as user directory errors.
      putUserDirectoryErrorContacts(internalNamesList, nameToContactMap);
    }
    else {
      nameToContactMap = mapNameToContact(result.get());
    }

    putUnknownErrorContacts(internalNamesList, nameToContactMap);

    // Place the contacts into the contact array in the order the names were given.
    for (int i = 0; i < contacts.length; i++) {
      contacts[i] = nameToContactMap.get(toLowerCase(internalNamesList.get(i)));
    }

    return contacts;
  }

  private Map<String, ContactDTO> mapNameToContact(List<Member> members) {
    Map<String, ContactDTO> result = new HashMap<>();

    for (Member member : members) {
      result.put(member.getInternalNameLowerCase(),
          new ContactDTO(member.getInternalName(), member.getDisplayName(), member.getEmail(), member.getRealm()));
    }

    return result;
  }

  private void putUnknownErrorContacts(List<String> internalNamesList, Map<String, ContactDTO> nameToContactMap) {
    // If we've already mapped all the names no work needs to be done.
    if (nameToContactMap.size() == internalNamesList.size()) {
      return;
    }

    for (String internalName : internalNamesList) {
      if (internalName != null && !nameToContactMap.containsKey(toLowerCase(internalName))) {
        nameToContactMap.put(toLowerCase(internalName),
            createErrorContact(internalName, "The username " + internalName + " no longer exists."));
      }
    }
  }

  private void putUserDirectoryErrorContacts(List<String> internalNamesList, Map<String, ContactDTO> nameToContactMap) {
    // If we've already mapped all the names no work needs to be done.
    if (nameToContactMap.size() == internalNamesList.size()) {
      return;
    }

    for (String internalName : internalNamesList) {
      if (internalName != null && !nameToContactMap.containsKey(toLowerCase(internalName))) {
        nameToContactMap.put(toLowerCase(internalName),
            createErrorContact(internalName, "User directory query result error."));
      }
    }
  }

  private String toLowerCase(String string) {
    if (string == null) {
      return null;
    }

    return string.toLowerCase(Locale.ENGLISH);
  }

  private ContactDTO createErrorContact(String internalName, String errorMessage) {
    ContactDTO contact = new ContactDTO(internalName, null, null, null);
    contact.setError(errorMessage);

    return contact;
  }
}
