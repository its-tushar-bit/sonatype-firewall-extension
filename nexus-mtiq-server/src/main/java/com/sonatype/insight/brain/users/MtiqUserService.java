/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.List;

public interface MtiqUserService
{
  List<MtiqUserDTO> getAllUsers();

  void inviteUser(MtiqUserDTO user);

  void deleteByUsername(String username);
}
