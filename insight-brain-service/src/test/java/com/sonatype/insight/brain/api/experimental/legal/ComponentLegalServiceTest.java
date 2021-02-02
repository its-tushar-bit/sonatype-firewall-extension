/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.io.IOException;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentLegalServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ComponentLegalService componentLegalService;

  @Inject
  private ComponentCopyrightDAO componentCopyrightDAO;

  @Test
  public void testSaveNewComponentCopyright() {
    Application application = tempEntity.newApplicationWithParent();
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
        null,
        componentIdentifier,
        Lists.newArrayList(new CopyrightOverrideDTO(
                null,
                "originalContentHash",
                "content",
                ComponentLegalPartStatus.ENABLED
            ),
            new CopyrightOverrideDTO(
                null,
                "originalContentHash2",
                "content2",
                ComponentLegalPartStatus.DISABLED
            )
        ),
        null,
        null
    );

    ComponentCopyrightDTO returnedComponentCopyrightDTO =
        componentLegalService
            .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);

    assertThat(returnedComponentCopyrightDTO.getId()).isNotNull();
    assertThat(returnedComponentCopyrightDTO.getCopyrightOverrides()).hasSize(2);
    returnedComponentCopyrightDTO.getCopyrightOverrides().forEach(co ->
        assertThat(co.getId()).isNotNull());
    assertThat(returnedComponentCopyrightDTO.getLastUpdatedAt()).isNotNull();
    assertThat(returnedComponentCopyrightDTO.getLastUpdatedByUsername()).isEqualTo(USERNAME);
    assertThat(returnedComponentCopyrightDTO.getComponentIdentifier()).usingRecursiveComparison()
        .isEqualTo(componentIdentifier);

    CopyrightOverrideDTO copyrightOverrideDTO0 = returnedComponentCopyrightDTO.getCopyrightOverrides().get(0);
    CopyrightOverrideDTO copyrightOverrideDTO1 = returnedComponentCopyrightDTO.getCopyrightOverrides().get(1);

    assertThat(copyrightOverrideDTO0.getOriginalContentHash()).isEqualTo("originalContentHash");
    assertThat(copyrightOverrideDTO0.getContent()).isEqualTo("content");
    assertThat(copyrightOverrideDTO0.getStatus()).isEqualTo(ComponentLegalPartStatus.ENABLED);

    assertThat(copyrightOverrideDTO1.getOriginalContentHash()).isEqualTo("originalContentHash2");
    assertThat(copyrightOverrideDTO1.getContent()).isEqualTo("content2");
    assertThat(copyrightOverrideDTO1.getStatus()).isEqualTo(ComponentLegalPartStatus.DISABLED);
  }

  @Test
  public void testUpdatedExistingComponentCopyright() {
    Application application = tempEntity.newApplicationWithParent();
    Organization organization = tempEntity.newOrganization();

    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
        null,
        componentIdentifier,
        Lists.newArrayList(new CopyrightOverrideDTO(
                null,
                "originalContentHash",
                "content",
                ComponentLegalPartStatus.ENABLED
            ),
            new CopyrightOverrideDTO(
                null,
                "originalContentHash2",
                "content2",
                ComponentLegalPartStatus.DISABLED
            )
        ),
        null,
        null
    );

    //Persist original componentCopyright
    ComponentCopyrightDTO existingComponentCopyright =
        componentLegalService
            .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);

    //Modify certain properties
    existingComponentCopyright.getCopyrightOverrides().get(0).setContent("updated content");
    existingComponentCopyright.getCopyrightOverrides().add(
        new CopyrightOverrideDTO(
            null,
            null,
            "content3",
            ComponentLegalPartStatus.ENABLED
        ));
    assertThat(componentCopyrightDAO
        .getByOwnerIdAndComponentIdentifier(application.getId(), componentIdentifier.toComponentIdentifier()))
        .isNotNull();

    //Persist the updated values
    ComponentCopyrightDTO updatedComponentCopyrightDTO =
        componentLegalService
            .saveComponentCopyright(OwnerType.ORGANIZATION, organization.getPublicId(), existingComponentCopyright);

    assertThat(updatedComponentCopyrightDTO.getId()).isNotNull();
    assertThat(updatedComponentCopyrightDTO.getCopyrightOverrides()).hasSize(3);
    updatedComponentCopyrightDTO.getCopyrightOverrides().forEach(co ->
        assertThat(co.getId()).isNotNull());
    assertThat(updatedComponentCopyrightDTO.getComponentIdentifier()).usingRecursiveComparison()
        .isEqualTo(componentIdentifier);

    CopyrightOverrideDTO copyrightOverrideDTO0 = updatedComponentCopyrightDTO.getCopyrightOverrides().get(0);
    CopyrightOverrideDTO copyrightOverrideDTO1 = updatedComponentCopyrightDTO.getCopyrightOverrides().get(1);
    CopyrightOverrideDTO copyrightOverrideDTO2 = updatedComponentCopyrightDTO.getCopyrightOverrides().get(2);

    assertThat(copyrightOverrideDTO0.getOriginalContentHash()).isEqualTo("originalContentHash");
    assertThat(copyrightOverrideDTO0.getContent()).isEqualTo("updated content");
    assertThat(copyrightOverrideDTO0.getStatus()).isEqualTo(ComponentLegalPartStatus.ENABLED);

    assertThat(copyrightOverrideDTO1.getOriginalContentHash()).isEqualTo("originalContentHash2");
    assertThat(copyrightOverrideDTO1.getContent()).isEqualTo("content2");
    assertThat(copyrightOverrideDTO1.getStatus()).isEqualTo(ComponentLegalPartStatus.DISABLED);

    assertThat(copyrightOverrideDTO2.getOriginalContentHash()).isNull();
    assertThat(copyrightOverrideDTO2.getContent()).isEqualTo("content3");
    assertThat(copyrightOverrideDTO2.getStatus()).isEqualTo(ComponentLegalPartStatus.ENABLED);

    ComponentCopyright componentCopyright = componentCopyrightDAO.getById(updatedComponentCopyrightDTO.getId());
    assertThat(componentCopyright.getOwnerId()).isEqualTo(organization.getId());
    assertThat(componentCopyrightDAO
        .getByOwnerIdAndComponentIdentifier(application.getId(), componentIdentifier.toComponentIdentifier())).isNull();
  }

  /**
   * Removing a CopyrightOverride from an existing ComponentCopyright should delete
   */
  @Test
  public void testBlankCopyrightOverrideInsert() {
    Application application = tempEntity.newApplicationWithParent();
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
        null,
        componentIdentifier,
        Lists.newArrayList(new CopyrightOverrideDTO(
                null,
                null,
                "content",
                ComponentLegalPartStatus.ENABLED
            )
        ),
        null,
        null
    );

    ComponentCopyrightDTO returnedComponentCopyrightDTO =
        componentLegalService
            .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);

    returnedComponentCopyrightDTO.getCopyrightOverrides().get(0).setContent("");

    ComponentCopyrightDTO updatedComponentCopyrightDTO =
        componentLegalService
            .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), returnedComponentCopyrightDTO);

    assertThat(updatedComponentCopyrightDTO.getCopyrightOverrides()).isEmpty();
  }

  @Test(expected = InvalidComponentIdentifierException.class)
  public void testInvalidComponentIdentifier() throws IOException {
    Application application = tempEntity.newApplicationWithParent();
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(JsonUtils.parse("{}", ComponentIdentifier.class));

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
        null,
        componentIdentifier,
        Lists.newArrayList(new CopyrightOverrideDTO(
                null,
                null,
                "content",
                ComponentLegalPartStatus.ENABLED
            )
        ),
        null,
        null
    );

    componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);
  }

  @Test(expected = InvalidComponentCopyrightException.class)
  public void testInvalidComponentCopyright() {
    Application application = tempEntity.newApplicationWithParent();
    ApiComponentIdentifierDTOV2 componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    ComponentCopyrightDTO componentCopyrightDTO = new ComponentCopyrightDTO(
        null,
        componentIdentifier,
        Lists.newArrayList(new CopyrightOverrideDTO(
                null,
                null,
                "content",
                null
            )
        ),
        null,
        null
    );

    componentLegalService
        .saveComponentCopyright(OwnerType.APPLICATION, application.getPublicId(), componentCopyrightDTO);
  }
}
