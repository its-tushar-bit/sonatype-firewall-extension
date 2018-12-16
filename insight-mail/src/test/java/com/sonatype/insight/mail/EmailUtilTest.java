/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mail;

import javax.mail.internet.AddressException;

import org.sonatype.micromailer.Address;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EmailUtilTest
{
  @Test
  public void testValidateMailAddress() throws Exception {
    EmailUtil.validate("info@sonatype.com");
    EmailUtil.validate("john.doe@sonatype.com");
  }

  @Test
  public void testValidateMailAddress_Null() throws Exception {
    EmailUtil.validate(null);
  }

  @Test
  public void testValidateMailAddress_Empty() throws Exception {
    EmailUtil.validate("");
  }

  @Test
  public void testValidateMailAddress_Blank() throws Exception {
    EmailUtil.validate("   ");
  }

  @Test(expected = AddressException.class)
  public void testValidateMailAddress_MissingAt() throws Exception {
    EmailUtil.validate("john");
  }

  @Test(expected = AddressException.class)
  public void testValidateMailAddress_EmptyDomain() throws Exception {
    EmailUtil.validate("john@");
  }

  @Test(expected = AddressException.class)
  public void testValidateMailAddress_EmptyUser() throws Exception {
    EmailUtil.validate("@domain.com");
  }

  @Test
  public void testValidateMailAddressRequired() throws Exception {
    EmailUtil.validate("info@sonatype.com", true);
    EmailUtil.validate("john.doe@sonatype.com", true);
  }

  @Test(expected = AddressException.class)
  public void testValidateMailAddressRequired_Null() throws Exception {
    EmailUtil.validate(null, true);
  }

  @Test(expected = AddressException.class)
  public void testValidateMailAddressRequired_Empty() throws Exception {
    EmailUtil.validate("", true);
  }

  @Test(expected = AddressException.class)
  public void testValidateMailAddressRequired_Blank() throws Exception {
    EmailUtil.validate("   ", true);
  }

  @Test(expected = AddressException.class)
  public void testValidateMailAddressRequired_MissingAt() throws Exception {
    EmailUtil.validate("john", true);
  }

  @Test(expected = AddressException.class)
  public void testValidateMailAddressRequired_EmptyDomain() throws Exception {
    EmailUtil.validate("john@", true);
  }

  @Test(expected = AddressException.class)
  public void testValidateMailAddressRequired_EmptyUser() throws Exception {
    EmailUtil.validate("@domain.com", true);
  }

  @Test
  public void testSplit_Null() throws Exception {
    assertThat(EmailUtil.split(null)).isEmpty();
  }

  @Test
  public void testSplit_Empty() throws Exception {
    assertThat(EmailUtil.split("")).isEmpty();
  }

  @Test
  public void testSplit_One() throws Exception {
    assertThat(EmailUtil.split("info@sonatype.com")).extracting(Address::getMailAddress)
        .containsExactly("info@sonatype.com");
  }

  @Test
  public void testSplit_Many() throws Exception {
    assertThat(EmailUtil.split("info@sonatype.com, nul@sonatype.com")).extracting(Address::getMailAddress)
        .containsExactly("info@sonatype.com", "nul@sonatype.com");
  }
}
