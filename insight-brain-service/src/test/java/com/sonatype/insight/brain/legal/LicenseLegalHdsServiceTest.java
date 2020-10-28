/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LegalCommentDTO;
import com.sonatype.insight.license.dto.model.LegalCopyrightDTO;
import com.sonatype.insight.license.dto.model.LegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;
import com.sonatype.insight.license.dto.model.LicenseThreatGroupDTO;

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class LicenseLegalHdsServiceTest
    extends AbstractComponentTest
{
  @Mock
  private HdsClient mockHdsClient;

  @Inject
  private LicenseLegalHdsService licenseLegalHdsService;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    super.configure(binder);
  }

  @Test
  public void testGetLicenseMetadata() throws IOException {
    List<String> licenses = Arrays.asList("Adobe", "DOCBOOK", "MIT");
    List<LicenseMetadataDTO> expectedMetadata = Arrays.asList(
        new LicenseMetadataDTO("License 1", "Test 1", new LicenseThreatGroupDTO("Group 1", 1),
            Sets.newHashSet(
                new LicenseObligationDTO("Obligation 1", Sets.newHashSet("Obligation Text 1", "Obligation Text 1.1")),
                new LicenseObligationDTO("Obligation A", Sets.newHashSet("Obligation Text A", "Obligation Text A.1")))),
        new LicenseMetadataDTO("License 2", "Test 2", new LicenseThreatGroupDTO("Group 2", 2),
            Sets.newHashSet(new LicenseObligationDTO("Obligation 2", Sets.newHashSet("Obligation Text 2")))),
        new LicenseMetadataDTO("License 3", "Test 3", new LicenseThreatGroupDTO("Group 3", 3),
            Sets.newHashSet(new LicenseObligationDTO("Obligation 3", Sets.newHashSet("Obligation Text 3")))));

    when(mockHdsClient.post(eq(LicenseMetadataDTO[].class), eq("/rest/license/metadata"), eq(licenses)))
        .thenReturn(expectedMetadata.toArray(new LicenseMetadataDTO[0]));

    List<LicenseMetadataDTO> results = licenseLegalHdsService.getLicenseMetadata(licenses);

    assertThat(results).isEqualTo(expectedMetadata);
  }

  @Test
  public void testGetLicenseMetadata_Empty() throws IOException {
    List<String> licenses = Arrays.asList("Adobe", "DOCBOOK", "MIT");
    when(mockHdsClient.post(eq(LicenseMetadataDTO[].class), eq("/rest/license/metadata"), eq(licenses)))
        .thenReturn(new LicenseMetadataDTO[0]);

    List<LicenseMetadataDTO> results = licenseLegalHdsService.getLicenseMetadata(licenses);
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetComponentLegalComments() throws IOException {
    ComponentIdentifier component1 = ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");
    ComponentIdentifier component2 = ComponentIdentifier.createNpmCoordinates("npmPackageId", "npmVersion");
    List<ComponentIdentifier> components = Arrays.asList(component1, component2);

    ComponentLegalCommentDTO componentLegalComment1 = new ComponentLegalCommentDTO();
    componentLegalComment1.setComponentIdentifier(component1);
    componentLegalComment1.setHash("hash1");
    LegalCommentDTO legalComment1 = new LegalCommentDTO();
    legalComment1.setContent("Content 1");
    legalComment1.setCopyrights(Sets.newHashSet(new LegalCopyrightDTO("Content 1", "Author 1", "Year1"),
        new LegalCopyrightDTO("Content 1.1", "Author 1.1", "Year1.1")));
    LegalCommentDTO legalComment2 = new LegalCommentDTO();
    legalComment2.setContent("Content 2");
    legalComment2.setCopyrights(Sets.newHashSet(new LegalCopyrightDTO("Content 2", "Author 2", "Year2")));
    componentLegalComment1.setComments(Sets.newHashSet(legalComment1, legalComment2));

    ComponentLegalCommentDTO componentLegalComment2 = new ComponentLegalCommentDTO();
    componentLegalComment2.setComponentIdentifier(component1);
    componentLegalComment2.setHash("hash2");
    LegalCommentDTO legalComment3 = new LegalCommentDTO();
    legalComment3.setContent("Content 3");
    legalComment3.setCopyrights(Sets.newHashSet(new LegalCopyrightDTO("Content 3", "Author 3", "Year3")));
    componentLegalComment2.setComments(Sets.newHashSet(legalComment3));

    Set<ComponentLegalCommentDTO> expectedLegalComments =
        Sets.newHashSet(componentLegalComment1, componentLegalComment2);

    when(mockHdsClient.post(eq(ComponentLegalCommentDTO[].class), eq("/rest/legal/comment"), eq(components)))
        .thenReturn(expectedLegalComments.toArray(new ComponentLegalCommentDTO[2]));

    Set<ComponentLegalCommentDTO> results = licenseLegalHdsService.getComponentLegalComments(components);

    assertThat(results).isEqualTo(expectedLegalComments);
  }

  @Test
  public void testGetComponentLegalComments_Empty() throws IOException {
    List<ComponentIdentifier> components = Arrays.asList(
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));

    when(mockHdsClient.post(eq(ComponentLegalCommentDTO[].class), eq("/rest/legal/comment"), eq(components)))
        .thenReturn(new ComponentLegalCommentDTO[0]);

    Set<ComponentLegalCommentDTO> results = licenseLegalHdsService.getComponentLegalComments(components);
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetComponentLegalFiles() throws IOException {
    ComponentIdentifier component1 = ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");
    ComponentIdentifier component2 = ComponentIdentifier.createNpmCoordinates("npmPackageId", "npmVersion");
    List<ComponentIdentifier> components = Arrays.asList(component1, component2);

    ComponentLegalFileDTO componentLegalFile1 = new ComponentLegalFileDTO();
    componentLegalFile1.setComponentIdentifier(component1);
    componentLegalFile1.setHash("hash1");
    LegalFileDTO legalFile1 = new LegalFileDTO();
    legalFile1.setContent("Content 1");
    legalFile1.setRelPath("path/1");
    legalFile1.setType("Type 1");
    componentLegalFile1.setLegalFiles(Sets.newHashSet(legalFile1));

    ComponentLegalFileDTO componentLegalFile2 = new ComponentLegalFileDTO();
    componentLegalFile2.setComponentIdentifier(component2);
    componentLegalFile2.setHash("hash2");
    LegalFileDTO legalFile2 = new LegalFileDTO();
    legalFile2.setContent("Content 2");
    legalFile2.setRelPath("path/2");
    legalFile2.setType("Type 2");
    componentLegalFile2.setLegalFiles(Sets.newHashSet(legalFile2));

    Set<ComponentLegalFileDTO> expectedLegalFiles = Sets.newHashSet(componentLegalFile1, componentLegalFile2);

    when(mockHdsClient.post(eq(ComponentLegalFileDTO[].class), eq("/rest/legal/file"), eq(components)))
        .thenReturn(expectedLegalFiles.toArray(new ComponentLegalFileDTO[2]));

    Set<ComponentLegalFileDTO> results = licenseLegalHdsService.getComponentLegalFiles(components);

    assertThat(results).isEqualTo(expectedLegalFiles);
  }

  @Test
  public void testGetComponentLegalFiles_Empty() throws IOException {
    List<ComponentIdentifier> components =
        Arrays.asList(ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));

    when(mockHdsClient.post(eq(ComponentLegalFileDTO[].class), eq("/rest/legal/file"), eq(components)))
        .thenReturn(new ComponentLegalFileDTO[0]);

    Set<ComponentLegalFileDTO> results = licenseLegalHdsService.getComponentLegalFiles(components);
    assertThat(results).isEmpty();
  }
}
