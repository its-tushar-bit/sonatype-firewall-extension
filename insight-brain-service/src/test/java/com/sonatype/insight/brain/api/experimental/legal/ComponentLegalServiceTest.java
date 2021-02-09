/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightOverrideDTO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentCopyrightDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComponentLegalServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ComponentLegalService componentLegalService;

  @Inject
  private ComponentCopyrightDAO componentCopyrightDAO;

  @Inject
  private ComponentObligationAttributionDAO componentObligationAttributionDAO;

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

  @Test
  public void testSaveComponentObligationAttribution_NullComponentIdentifier() {
    Application application = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            new ComponentObligationAttributionDTO()
        )
    ).withMessageContaining("The component identifier cannot be null.");
  }

  @Test
  public void testSaveComponentObligationAttribution_InvalidComponentIdentifier() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setComponentIdentifier(new ApiComponentIdentifierDTOV2());
    assertThatExceptionOfType(InvalidComponentIdentifierException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    );
  }

  @Test
  public void testSaveComponentObligationAttribution_BlankContent() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining("ComponentObligationAttribution must have content.");
    componentObligationAttributionDTO.setContent("");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining("ComponentObligationAttribution must have content.");
    componentObligationAttributionDTO.setContent(" ");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining("ComponentObligationAttribution must have content.");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentIdentifier, application.getId(), "obligationName", "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    componentObligationAttributionDTO.setId(componentObligationAttribution.getId());
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining("ComponentObligationAttribution must have content.");
    componentObligationAttributionDTO.setContent("");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining("ComponentObligationAttribution must have content.");
    componentObligationAttributionDTO.setContent(" ");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining("ComponentObligationAttribution must have content.");
  }

  @Test
  public void testSaveComponentObligationAttribution_Create() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    componentObligationAttributionDTO.setObligationName("obligationName");
    componentObligationAttributionDTO.setContent("content");

    ComponentObligationAttributionDTO result = componentLegalService.saveComponentObligationAttribution(
        application.getType(),
        application.getPublicId(),
        componentObligationAttributionDTO
    );

    List<ComponentObligationAttribution> componentObligationAttributions =
        componentObligationAttributionDAO.getByOwnerId(application.getId());
    assertThat(componentObligationAttributions).hasSize(1);
    ComponentObligationAttribution componentObligationAttribution = componentObligationAttributions.get(0);
    assertComponentObligationAttribution(componentObligationAttribution, application,
        componentObligationAttributionDTO);
    assertComponentObligationAttribution(result, componentObligationAttribution);
  }

  @Test
  public void testSaveComponentObligationAttribution_Update() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    String obligationName = "obligationName1";
    String content = "content1";
    ComponentObligationAttribution oldComponentObligationAttribution = tempEntity
        .newComponentObligationAttribution(componentIdentifier, organization.getPublicId(), obligationName,
            content, ComponentLegalService.NOT_IMPLEMENTED);
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setId(oldComponentObligationAttribution.getId());
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2
            .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2")));
    componentObligationAttributionDTO.setObligationName("obligationName2");
    componentObligationAttributionDTO.setContent("content2");

    ComponentObligationAttributionDTO result = componentLegalService.saveComponentObligationAttribution(
        OwnerType.APPLICATION,
        application.getPublicId(),
        componentObligationAttributionDTO
    );

    List<ComponentObligationAttribution> componentObligationAttributions =
        componentObligationAttributionDAO.getByOwnerId(application.getId());
    assertThat(componentObligationAttributions).hasSize(1);
    ComponentObligationAttribution componentObligationAttribution = componentObligationAttributions.get(0);
    assertComponentObligationAttribution(componentObligationAttribution, application,
        componentObligationAttributionDTO);
    assertThat(componentObligationAttribution.getLastUpdatedAt())
        .isAfterOrEqualTo(oldComponentObligationAttribution.getLastUpdatedAt());
    assertComponentObligationAttribution(result, componentObligationAttribution);
  }

  @Test
  public void testSaveComponentObligationAttribution_Update_DoesNotExist() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setId("doesNotExist");
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2
            .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    componentObligationAttributionDTO.setContent("content");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() ->
        componentLegalService.saveComponentObligationAttribution(
            application.getType(),
            application.getPublicId(),
            componentObligationAttributionDTO
        )
    ).withMessageContaining(
        "ComponentObligationAttribution with ID " + componentObligationAttributionDTO.getId() + " does not exist.");
  }

  @Test
  public void testDeleteComponentObligationAttribution() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentIdentifier, application.getId(), "obligationName", "content",
        ComponentLegalService.NOT_IMPLEMENTED);
    ComponentObligationAttributionDTO componentObligationAttributionDTO = new ComponentObligationAttributionDTO();
    componentObligationAttributionDTO.setId(componentObligationAttribution.getId());
    componentObligationAttributionDTO.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));

    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());

    assertThat(componentObligationAttributionDAO.getByOwnerId(application.getId())).isEmpty();

    componentObligationAttribution = tempEntity.newComponentObligationAttribution(componentIdentifier,
        application.getId(), "obligationName", "content", ComponentLegalService.NOT_IMPLEMENTED);
    componentObligationAttributionDTO.setId(componentObligationAttribution.getId());
    componentObligationAttributionDTO.setContent("");

    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());

    assertThat(componentObligationAttributionDAO.getByOwnerId(application.getId())).isEmpty();

    componentObligationAttribution = tempEntity.newComponentObligationAttribution(componentIdentifier,
        application.getId(), "obligationName", "content", ComponentLegalService.NOT_IMPLEMENTED);
    componentObligationAttributionDTO.setId(componentObligationAttribution.getId());
    componentObligationAttributionDTO.setContent(" ");

    componentLegalService.deleteComponentObligationAttribution(componentObligationAttribution.getId());

    assertThat(componentObligationAttributionDAO.getByOwnerId(application.getId())).isEmpty();
  }

  @Test
  public void testDeleteComponentObligationAttribution_DoesNotExist() {
    String id = "doesNotExist";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentLegalService.deleteComponentObligationAttribution(id))
        .withMessageContaining("ComponentObligationAttribution with ID " + id + " does not exist.");
  }

  @Test
  public void testGetComponentObligationAttributions() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    String obligationName = "obligationName";

    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentIdentifier, Organization.ROOT_ORGANIZATION_ID, obligationName, "content",
        ComponentLegalService.NOT_IMPLEMENTED);

    assertThat(componentLegalService.getComponentObligationAttributions(application.getType(),
        application.getPublicId(), componentIdentifier, obligationName))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(new ComponentObligationAttributionDTO(componentObligationAttribution));

    ComponentObligationAttribution override = tempEntity.newComponentObligationAttribution(
        componentIdentifier, application.getId(), obligationName, "override",
        ComponentLegalService.NOT_IMPLEMENTED);

    assertThat(componentLegalService.getComponentObligationAttributions(application.getType(),
        application.getPublicId(), componentIdentifier, obligationName))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(new ComponentObligationAttributionDTO(override));
  }

  private void assertComponentObligationAttribution(
      ComponentObligationAttribution actual,
      Owner expectedOwner,
      ComponentObligationAttributionDTO expected)
  {
    if (expected.getId() == null) {
      assertThat(actual.getId()).isNotNull();
    }
    else {
      assertThat(actual.getId()).isEqualTo(expected.getId());
    }
    assertThat(actual.getOwnerId()).isEqualTo(expectedOwner.getId());
    assertThat(actual.getComponentIdentifier()).isEqualTo(expected.getComponentIdentifier().toComponentIdentifier());
    assertThat(actual.getObligationName()).isEqualTo(expected.getObligationName());
    assertThat(actual.getContent()).isEqualTo(expected.getContent());
    assertThat(actual.getLegalContentHash()).isEqualTo(ComponentLegalService.NOT_IMPLEMENTED);
    assertThat(actual.getLastUpdatedAt()).isNotNull();
    assertThat(actual.getLastUpdatedByUsername()).isEqualTo(USERNAME);
  }

  private void assertComponentObligationAttribution(
      ComponentObligationAttributionDTO actual,
      ComponentObligationAttribution expected)
  {
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getComponentIdentifier().toComponentIdentifier()).isEqualTo(expected.getComponentIdentifier());
    assertThat(actual.getObligationName()).isEqualTo(expected.getObligationName());
    assertThat(actual.getContent()).isEqualTo(expected.getContent());
    assertThat(actual.getLastUpdatedAt()).isEqualTo(expected.getLastUpdatedAt());
    assertThat(actual.getLastUpdatedByUsername()).isEqualTo(expected.getLastUpdatedByUsername());
  }
}
