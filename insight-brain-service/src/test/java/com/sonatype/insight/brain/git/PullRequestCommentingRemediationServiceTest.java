/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestCommentingRemediationServiceTest
{
  @Mock
  private ApiComponentRemediationService mockRemediationService;

  private final ComponentIdentifier id1 = ComponentIdentifier.createNpmCoordinates("artifact-1", "1.0.0");

  private final ComponentIdentifier remediationForId1 = ComponentIdentifier.createNpmCoordinates("artifact-1", "1.2.0");

  private final ComponentIdentifier id2 = ComponentIdentifier.createNpmCoordinates("artifact-2", "2.0.0");
  
  @Test
  public void testGetRemediationVersionMap_remediationFound() {
    // given: test subject and a policy violation list
    PullRequestCommentingRemediationService service =
        new PullRequestCommentingRemediationService(mockRemediationService);
    
    List<PolicyViolation> violationList = new LinkedList<>();
    PolicyViolation violation = new PolicyViolation();
    violation.setComponentIdentifier(id1);
    violationList.add(violation);
    
    // and: there is a remediation version for the component identifier   
    ApiComponentDTOV2 componentDto = new ApiComponentDTOV2();
    componentDto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(remediationForId1);
    ApiComponentChangeActionDTO componentChangeActionDTO = new ApiComponentChangeActionDTO(componentDto);
    ApiVersionChangeOptionDTO versionChangeOptionDTO = new ApiVersionChangeOptionDTO();
    versionChangeOptionDTO.setType(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    versionChangeOptionDTO.setData(componentChangeActionDTO);
    ApiComponentRemediationDTO remediationDTO = new ApiComponentRemediationDTO();
    remediationDTO.remediation.versionChanges.add(versionChangeOptionDTO);
    
    when(mockRemediationService.getSuggestedRemediationForComponentNoAuth(
        any(), any(), any(), any(), any(), any())).thenReturn(remediationDTO);

    // when: 
    Map<ComponentIdentifier, String> versionMap = service.getRemediationVersionMap(violationList, "appId");

    // then: remediation version returned in map 
    assertThat(versionMap.containsKey(id1)).isTrue();
    assertThat(versionMap.get(id1)).isEqualTo("1.2.0");
  }

  @Test
  public void testGetRemediationVersionMap_remediationNotFound() {
    // given: test subject and a policy violation list
    PullRequestCommentingRemediationService service =
        new PullRequestCommentingRemediationService(mockRemediationService);

    List<PolicyViolation> violationList = new LinkedList<>();
    PolicyViolation violation = new PolicyViolation();
    violation.setComponentIdentifier(id2);
    violationList.add(violation);
    
    // and: there is no remediation version for the component identifier   
    ApiComponentRemediationDTO remediationDTO = new ApiComponentRemediationDTO();
    when(mockRemediationService.getSuggestedRemediationForComponentNoAuth(
        any(), any(), any(), any(), any(), any())).thenReturn(remediationDTO);

    // when: 
    Map<ComponentIdentifier, String> versionMap = service.getRemediationVersionMap(violationList, "appId");

    // then: remediation version returned in map
    assertThat(versionMap.containsKey(id2)).isFalse();
  }
}
