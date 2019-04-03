/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation;

import java.util.Arrays;

import javax.ws.rs.HttpMethod;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiRemediationRestActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiComponentOverrideOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiComponentOverrideOptionType;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiPolicyWaiverOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiComponentRemediationDTOTest
{
  @Test
  public void testJsonSerializationAndDeserialization() throws Exception {
    ApiRemediationRestActionDTO restAction = new ApiRemediationRestActionDTO("url", HttpMethod.POST, "{}");

    ApiRemediationRestActionDTO restAction2 = new ApiRemediationRestActionDTO("url", HttpMethod.POST, "{hello}");

    ApiPolicyWaiverOptionDTO policyWaiver = new ApiPolicyWaiverOptionDTO(restAction);

    ApiComponentDTOV2 componentIdentifier = new ApiComponentDTOV2();
    componentIdentifier.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("org.apache.logging.log4j", "log4j-core", "2.11.2"));
    componentIdentifier.hash = "1234";

    ApiVersionChangeOptionDTO versionChange =
        new ApiVersionChangeOptionDTO(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS,
            new ApiComponentChangeActionDTO(componentIdentifier));

    ApiComponentDTOV2 componentIdentifier2 = new ApiComponentDTOV2();
    componentIdentifier2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("org.apache.logging.log4j", "log4j-core", "2.10.5"));
    componentIdentifier2.hash = "1234";

    ApiVersionChangeOptionDTO versionChange2 =
        new ApiVersionChangeOptionDTO(ApiVersionChangeOptionType.NEXT_NON_FAILING,
            new ApiComponentChangeActionDTO(componentIdentifier2));

    ApiComponentOverrideOptionDTO overrideOption =
        new ApiComponentOverrideOptionDTO(ApiComponentOverrideOptionType.SECURITY_OVERRIDE,
            restAction2);

    ApiComponentRemediationDTO dto = new ApiComponentRemediationDTO();
    dto.remediation = new ApiComponentRemediationValueDTO();
    dto.remediation.versionChanges = Arrays.asList(versionChange, versionChange2);
    dto.remediation.componentOverrides = Arrays.asList(overrideOption);
    dto.remediation.policyWaivers = Arrays.asList(policyWaiver);

    String output = JsonUtils.format(dto);
    ApiComponentRemediationDTO dto2 = JsonUtils.parse(output, ApiComponentRemediationDTO.class);
    assertThat(dto).usingRecursiveComparison().isEqualTo(dto2);
  }
}
