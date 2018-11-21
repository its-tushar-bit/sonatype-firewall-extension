/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.After;
import org.junit.Before;

public class AbstractMembershipMappingAuditTest
    extends AbstractAuditTest
{
  private List<MembershipMapping> originalMembershipMapppings;

  @Before
  public void saveOriginalMembershipMapppings() {
    originalMembershipMapppings = new MembershipMappingDAO().getAll().stream()
        .map(mm -> new MembershipMapping(mm.getContextId(), mm.getRoleId(), mm.getMemberName(), mm.getMemberType()))
        .collect(Collectors.toList());
  }

  @After
  public void restoreOriginalMembershipMapppings() {
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    membershipMappingDAO.getAll().forEach(membershipMappingDAO::delete);
    originalMembershipMapppings.forEach(membershipMappingDAO::insert);
  }

  protected void assertRoleMembershipData(AuditDTO auditDTO, String roleId, List<Member> members) {
    Role role = new RoleDAO().getByIdNotNull(roleId);
    assertCustomData(auditDTO, "roleId", role.getId());
    assertCustomData(auditDTO, "roleName", role.getName());
    assertCustomObject(auditDTO, "roleMembers", MemberDTO.transcribe(members));
  }
}
