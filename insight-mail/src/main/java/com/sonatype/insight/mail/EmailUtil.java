/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mail;

import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.MailRequestStatus;

public class EmailUtil
{
  public static void validate(String email) throws AddressException {
    validate(email, false);
  }

  public static void validate(String email, boolean required) throws AddressException {
    String trimmedEmail = email == null ? "" : email.trim();

    if (required && trimmedEmail.isEmpty()) {
      throw new AddressException("Mail address has not been specified.");
    }

    if (!trimmedEmail.isEmpty()) {
      InternetAddress ia = new InternetAddress(email);
      String[] tokens = ia.getAddress().split("@");
      if (tokens.length != 2 || tokens[0].isEmpty() || tokens[1].isEmpty()) {
        throw new AddressException("Missing domain in mail address " + email);
      }
    }
  }

  public static void waitForMailStatus(MailRequestStatus mailStatus) {
    boolean interrupted = false;
    while (!mailStatus.isSent() && mailStatus.getErrorCause() == null) {
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException e) {
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
    if (mailStatus.getErrorCause() != null) {
      Throwable t = mailStatus.getErrorCause();
      if (t instanceof Error) {
        throw (Error) t;
      }
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      }
      if (t instanceof Exception) {
        throw new RuntimeException(t.getMessage(), t);
      }
      throw new IllegalStateException(t);
    }
  }

  public static List<Address> split(String emails) throws AddressException {
    List<Address> result = new ArrayList<>();
    if (emails != null && !emails.isEmpty()) {
      InternetAddress[] addresses = InternetAddress.parse(emails, true);
      for (InternetAddress address : addresses) {
        result.add(new Address(address.getAddress()));
      }
    }
    return result;
  }
}
