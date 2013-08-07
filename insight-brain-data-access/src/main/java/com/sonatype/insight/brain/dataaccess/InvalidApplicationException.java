package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.error.HttpStatusCode;

@SuppressWarnings("serial")
@HttpStatusCode(400)
public class InvalidApplicationException
    extends RuntimeException
{
  public InvalidApplicationException(String message) {
    super(message);
  }
}
