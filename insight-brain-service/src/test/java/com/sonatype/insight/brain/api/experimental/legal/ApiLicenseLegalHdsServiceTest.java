/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalSourceLinkDTO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.dto.model.AnameAggregateFileGroup;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentFilePathsDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.ComponentSourceLinkDTO;
import com.sonatype.insight.license.dto.model.LegalCommentDTO;
import com.sonatype.insight.license.dto.model.LegalCommentFilesDTO;
import com.sonatype.insight.license.dto.model.LegalCopyrightDTO;
import com.sonatype.insight.license.dto.model.LegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;
import com.sonatype.insight.license.dto.model.LicenseThreatGroupDTO;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.mockito.Mock;

public class ApiLicenseLegalHdsServiceTest
    extends AbstractComponentTest
{
  @Mock
  private HdsClient mockHdsClient;

  @Inject
  private ApiLicenseLegalHdsService apiLicenseLegalHdsService;

  @Test
  public void testGetLicenseMetadata() {
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

    when(mockHdsClient.post(eq(LicenseMetadataDTO[].class), eq(ApiLicenseLegalHdsService.METADATA_URL), eq(licenses)))
        .thenReturn(expectedMetadata.toArray(new LicenseMetadataDTO[0]));

    List<LicenseMetadataDTO> results = apiLicenseLegalHdsService.getLicenseMetadata(licenses);

    assertThat(results).isEqualTo(expectedMetadata);
  }

  @Test
  public void testGetLicenseMetadata_Empty() {
    List<String> licenses = Arrays.asList("Adobe", "DOCBOOK", "MIT");
    when(mockHdsClient.post(eq(LicenseMetadataDTO[].class), eq(ApiLicenseLegalHdsService.METADATA_URL), eq(licenses)))
        .thenReturn(new LicenseMetadataDTO[0]);

    List<LicenseMetadataDTO> results = apiLicenseLegalHdsService.getLicenseMetadata(licenses);
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetComponentLegalComments() {
    ComponentIdentifier component1 = ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");
    ComponentIdentifier component2 = ComponentIdentifier.createNpmCoordinates("npmPackageId", "npmVersion");
    List<ComponentIdentifier> components = Arrays.asList(component1, component2);

    ComponentLegalCommentDTO componentLegalComment1 = new ComponentLegalCommentDTO();
    componentLegalComment1.setComponentIdentifier(component1);
    componentLegalComment1.setHash("hash1");
    LegalCommentDTO legalComment1 = new LegalCommentDTO();
    legalComment1.setContent("Content 1");
    legalComment1.setCopyrights(Sets.newHashSet(new LegalCopyrightDTO("Content 1", "content1Hash", "Author 1", "Year1"),
        new LegalCopyrightDTO("Content 1.1", "content11Hash", "Author 1.1", "Year1.1")));
    LegalCommentDTO legalComment2 = new LegalCommentDTO();
    legalComment2.setContent("Content 2");
    legalComment2
        .setCopyrights(Sets.newHashSet(new LegalCopyrightDTO("Content 2", "content2Hash", "Author 2", "Year2")));
    componentLegalComment1.setComments(Sets.newHashSet(legalComment1, legalComment2));

    ComponentLegalCommentDTO componentLegalComment2 = new ComponentLegalCommentDTO();
    componentLegalComment2.setComponentIdentifier(component1);
    componentLegalComment2.setHash("hash2");
    LegalCommentDTO legalComment3 = new LegalCommentDTO();
    legalComment3.setContent("Content 3");
    legalComment3
        .setCopyrights(Sets.newHashSet(new LegalCopyrightDTO("Content 3", "content3Hash", "Author 3", "Year3")));
    componentLegalComment2.setComments(Sets.newHashSet(legalComment3));

    Set<ComponentLegalCommentDTO> expectedLegalComments =
        new LinkedHashSet<>(Arrays.asList(componentLegalComment1, componentLegalComment2));

    when(mockHdsClient
        .post(eq(ComponentLegalCommentDTO[].class), eq(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL), eq(components)))
            .thenReturn(expectedLegalComments.toArray(new ComponentLegalCommentDTO[2]));

    Set<ComponentLegalCommentDTO> results = apiLicenseLegalHdsService.getComponentLegalComments(components);

    assertThat(results).isEqualTo(expectedLegalComments);
  }

  @Test
  public void getGetAnameRawComponentLegalComments() {
    final ComponentIdentifier componentId1 = ComponentIdentifier.createAnameCoordinates("groupId1", "", "version1");
    final ComponentIdentifier componentId2 = ComponentIdentifier.createAnameCoordinates("groupId2", "", "version2");
    final List<AnameAggregateFileGroup> aggregageFileGroups = ImmutableList.of(
        new AnameAggregateFileGroup(
            componentId1,
            ImmutableList.of("11111111111111111111", "22222222222222222222")),
        new AnameAggregateFileGroup(
            componentId2,
            ImmutableList.of("11111111111111111111", "33333333333333333333")));

    final LegalCommentDTO legalComment1 = new LegalCommentDTO();
    legalComment1.setContent("Content 1");
    legalComment1.setCopyrights(Sets.newHashSet(new LegalCopyrightDTO("Content 1", "content1Hash", "Author 1", "Year1"),
        new LegalCopyrightDTO("Content 1.1", "content11Hash", "Author 1.1", "Year1.1")));
    final LegalCommentDTO legalComment2 = new LegalCommentDTO();
    legalComment2.setContent("Content 2");
    legalComment2
        .setCopyrights(Sets.newHashSet(new LegalCopyrightDTO("Content 2", "content2Hash", "Author 2", "Year2")));
    final LegalCommentDTO legalComment3 = new LegalCommentDTO();
    legalComment3.setContent("Content 3");
    legalComment3
        .setCopyrights(Sets.newHashSet(new LegalCopyrightDTO("Content 3", "content3Hash", "Author 3", "Year3")));

    final ComponentLegalCommentDTO componentLegalComment1 = createComponentCommentsDTO(
        componentId1,
        "11111111111111111111",
        legalComment1);
    final ComponentLegalCommentDTO componentLegalComment2 = createComponentCommentsDTO(
        componentId1,
        "22222222222222222222",
        legalComment2);
    final ComponentLegalCommentDTO componentLegalComment3 = createComponentCommentsDTO(
        componentId2,
        "11111111111111111111",
        legalComment1);
    final ComponentLegalCommentDTO componentLegalComment4 = createComponentCommentsDTO(
        componentId2,
        "33333333333333333333",
        legalComment3);

    final Set<ComponentLegalCommentDTO> expectedLegalComments =
        new LinkedHashSet<>(Arrays.asList(
            componentLegalComment1,
            componentLegalComment2,
            componentLegalComment3,
            componentLegalComment4));

    doReturn(new ComponentLegalCommentDTO[]{
      componentLegalComment1, componentLegalComment2, componentLegalComment3, componentLegalComment4
    })
        .when(mockHdsClient)
        .post(ComponentLegalCommentDTO[].class,
            ApiLicenseLegalHdsService.LEGAL_ANAME_COMMENT_URL,
            aggregageFileGroups);

    final Set<ComponentLegalCommentDTO> results = apiLicenseLegalHdsService.getAnameRawComponentLegalComments(
        ImmutableSet.copyOf(aggregageFileGroups));

    assertThat(results).isEqualTo(expectedLegalComments);
  }

  @Test
  public void testGetANameComponentLegalComments() {
    final ComponentIdentifier componentId1 = ComponentIdentifier.createAnameCoordinates("groupId1", "", "version1");
    final ComponentIdentifier componentId2 = ComponentIdentifier.createAnameCoordinates("groupId2", "", "version2");
    final List<AnameAggregateFileGroup> aggregageFileGroups = ImmutableList.of(
        new AnameAggregateFileGroup(
            componentId1,
            ImmutableList.of("11111111111111111111", "22222222222222222222")),
        new AnameAggregateFileGroup(
            componentId2,
            ImmutableList.of("11111111111111111111", "33333333333333333333")));

    final LegalCommentDTO legalComment1 = new LegalCommentDTO();
    legalComment1.setContent("Content 1");
    legalComment1.setCopyrights(Sets.newHashSet(new LegalCopyrightDTO("Content 1", "content1Hash", "Author 1", "Year1"),
        new LegalCopyrightDTO("Content 1.1", "content11Hash", "Author 1.1", "Year1.1")));
    final LegalCommentDTO legalComment2 = new LegalCommentDTO();
    legalComment2.setContent("Content 2");
    legalComment2
        .setCopyrights(Sets.newHashSet(new LegalCopyrightDTO("Content 2", "content2Hash", "Author 2", "Year2")));
    final LegalCommentDTO legalComment3 = new LegalCommentDTO();
    legalComment3.setContent("Content 3");
    legalComment3
        .setCopyrights(Sets.newHashSet(new LegalCopyrightDTO("Content 3", "content3Hash", "Author 3", "Year3")));

    final ComponentLegalCommentDTO componentLegalComment1 = createComponentCommentsDTO(
        componentId1,
        "11111111111111111111",
        legalComment1);
    final ComponentLegalCommentDTO componentLegalComment2 = createComponentCommentsDTO(
        componentId1,
        "22222222222222222222",
        legalComment2);
    final ComponentLegalCommentDTO componentLegalComment3 = createComponentCommentsDTO(
        componentId2,
        "11111111111111111111",
        legalComment1);
    final ComponentLegalCommentDTO componentLegalComment4 = createComponentCommentsDTO(
        componentId2,
        "33333333333333333333",
        legalComment3);

    final ComponentLegalCommentDTO expectedLegalComments1 = createComponentCommentsDTO(
        componentId1,
        "component_1_hash",
        legalComment1, legalComment2);
    final ComponentLegalCommentDTO expectedLegalComments2 = createComponentCommentsDTO(
        componentId2,
        "component_2_hash",
        legalComment1, legalComment3);

    final Set<ComponentLegalCommentDTO> expectedLegalComments =
        new LinkedHashSet<>(Arrays.asList(expectedLegalComments1, expectedLegalComments2));

    doReturn(new ComponentLegalCommentDTO[]{
      componentLegalComment1, componentLegalComment2, componentLegalComment3, componentLegalComment4
    })
        .when(mockHdsClient)
        .post(ComponentLegalCommentDTO[].class,
            ApiLicenseLegalHdsService.LEGAL_ANAME_COMMENT_URL,
            aggregageFileGroups);

    final Set<ComponentLegalCommentDTO> results = apiLicenseLegalHdsService.getAnameComponentLegalComments(
        ImmutableSet.copyOf(aggregageFileGroups),
        ImmutableMap.of(
            componentId1, "component_1_hash",
            componentId2, "component_2_hash"));

    assertThat(results).isEqualTo(expectedLegalComments);
  }

  @Test
  public void testGetANameComponentLegalComments_EmptyAggregateHash() {
    final ComponentIdentifier componentId1 = ComponentIdentifier.createAnameCoordinates("groupId1", "", "version1");
    final List<AnameAggregateFileGroup> aggregageFileGroups = ImmutableList.of(
        new AnameAggregateFileGroup(componentId1, ImmutableList.of()));

    apiLicenseLegalHdsService.getAnameComponentLegalComments(
        ImmutableSet.copyOf(aggregageFileGroups),
        ImmutableMap.of(componentId1, "component_1_hash"));

    verify(mockHdsClient, never())
        .post(eq(ComponentLegalCommentDTO[].class),
            eq(ApiLicenseLegalHdsService.LEGAL_ANAME_COMMENT_URL),
            any());
  }

  private ComponentLegalCommentDTO createComponentCommentsDTO(
      final ComponentIdentifier componentIdentifier,
      final String componentHash,
      final LegalCommentDTO... comments)
  {
    final ComponentLegalCommentDTO componentLegalComment = new ComponentLegalCommentDTO();
    componentLegalComment.setComponentIdentifier(componentIdentifier);
    componentLegalComment.setHash(componentHash);
    componentLegalComment.setComments(Sets.newHashSet(comments));
    return componentLegalComment;
  }

  @Test
  public void testGetComponentLegalComments_Empty() {
    List<ComponentIdentifier> components = Collections.singletonList(
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));

    when(mockHdsClient
        .post(eq(ComponentLegalCommentDTO[].class), eq(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL), eq(components)))
            .thenReturn(new ComponentLegalCommentDTO[0]);

    Set<ComponentLegalCommentDTO> results = apiLicenseLegalHdsService.getComponentLegalComments(components);
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetComponentLegalFiles() {
    ComponentIdentifier component1 = ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");
    ComponentIdentifier component2 = ComponentIdentifier.createNpmCoordinates("npmPackageId", "npmVersion");
    List<ComponentIdentifier> components = Arrays.asList(component1, component2);

    ComponentLegalFileDTO componentLegalFile1 = new ComponentLegalFileDTO();
    componentLegalFile1.setComponentIdentifier(component1);
    componentLegalFile1.setHash("hash1");
    LegalFileDTO legalFile1 = new LegalFileDTO();
    legalFile1.setContent("Content 1");
    legalFile1.setContentHash("contentHash1");
    legalFile1.setRelPath("path/1");
    legalFile1.setType("Type 1");
    componentLegalFile1.setLegalFiles(Sets.newHashSet(legalFile1));

    ComponentLegalFileDTO componentLegalFile2 = new ComponentLegalFileDTO();
    componentLegalFile2.setComponentIdentifier(component2);
    componentLegalFile2.setHash("hash2");
    LegalFileDTO legalFile2 = new LegalFileDTO();
    legalFile2.setContent("Content 2");
    legalFile2.setContentHash("contentHash2");
    legalFile2.setRelPath("path/2");
    legalFile2.setType("Type 2");
    componentLegalFile2.setLegalFiles(Sets.newHashSet(legalFile2));

    Set<ComponentLegalFileDTO> expectedLegalFiles =
        new LinkedHashSet<>(Arrays.asList(componentLegalFile1, componentLegalFile2));

    when(mockHdsClient
        .post(eq(ComponentLegalFileDTO[].class), eq(ApiLicenseLegalHdsService.LEGAL_FILE_URL), eq(components)))
            .thenReturn(expectedLegalFiles.toArray(new ComponentLegalFileDTO[2]));

    Set<ComponentLegalFileDTO> results = apiLicenseLegalHdsService.getComponentLegalFiles(components);

    assertThat(results).isEqualTo(expectedLegalFiles);
  }

  @Test
  public void testGetComponentLegalFiles_Empty() {
    List<ComponentIdentifier> components =
        Collections.singletonList(ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));

    when(mockHdsClient
        .post(eq(ComponentLegalFileDTO[].class), eq(ApiLicenseLegalHdsService.LEGAL_FILE_URL), eq(components)))
            .thenReturn(new ComponentLegalFileDTO[0]);

    Set<ComponentLegalFileDTO> results = apiLicenseLegalHdsService.getComponentLegalFiles(components);
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetComponentLegalCommentsWithPaths() {
    ComponentIdentifier component1 = ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");

    ComponentLegalCommentFilePathsDTO componentLegalComment1 = new ComponentLegalCommentFilePathsDTO();
    componentLegalComment1.setComponentIdentifier(component1);
    componentLegalComment1.setHash("hash1");
    LegalCommentFilesDTO legalComment1 = new LegalCommentFilesDTO();
    legalComment1.setContent("Content 1");
    legalComment1.setCopyrightContentHashes(Sets.newHashSet("content1Hash", "content11Hash"));
    legalComment1.setFilePaths(Sets.newHashSet("path1/file1", "path2/file"));
    LegalCommentFilesDTO legalComment2 = new LegalCommentFilesDTO();
    legalComment2.setContent("Content 2");
    legalComment2.setCopyrightContentHashes(Sets.newHashSet("content2Hash"));
    legalComment1.setFilePaths(Sets.newHashSet("path1/file2", "path2/file2"));
    componentLegalComment1.setComments(Sets.newHashSet(legalComment1, legalComment2));

    Set<ComponentLegalCommentFilePathsDTO> expectedLegalComments =
        new LinkedHashSet<>(Collections.singletonList(componentLegalComment1));

    when(mockHdsClient
        .post(ComponentLegalCommentFilePathsDTO[].class,
            ApiLicenseLegalHdsService.LEGAL_COMMENT_FILE_PATHS_URL,
            ImmutableList.of(component1)))
                .thenReturn(expectedLegalComments.toArray(new ComponentLegalCommentFilePathsDTO[1]));

    Collection<ComponentLegalCommentFilePathsDTO> results = apiLicenseLegalHdsService
        .getComponentLegalCommentFilePaths(component1);

    assertThat(results).isEqualTo(expectedLegalComments);
  }

  @Test
  public void testGetSourceLinksFromComponentIdentifierSet() {
    SourceLinkOverride source1 = new SourceLinkOverride("www.sltest.net", ComponentLegalPartStatus.ENABLED, "slId1");
    ComponentIdentifier component1 = ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");
    ComponentSourceLinkDTO componentSourceLink1 = new ComponentSourceLinkDTO();
    componentSourceLink1.setComponentIdentifier(component1);
    componentSourceLink1.setSourceLinks(Collections.singletonList("www.sltest.net"));
    Set<ComponentSourceLinkDTO> expectedComponentSourceLinkDTO =
        new LinkedHashSet<>(Collections.singletonList(componentSourceLink1));

    when(mockHdsClient.post(ComponentSourceLinkDTO[].class, ApiLicenseLegalHdsService.SOURCE_LINK_URL,
        Collections.singletonList(component1)))
            .thenReturn(expectedComponentSourceLinkDTO.toArray(new ComponentSourceLinkDTO[1]));

    Map<ComponentIdentifier, Set<LegalSourceLinkDTO>> results =
        apiLicenseLegalHdsService.getSourceLinksFromComponentIdentifierSet(Sets.newHashSet(component1));
    Map<ComponentIdentifier, Set<LegalSourceLinkDTO>> expectedSourceLinkMap = new HashMap<>();
    expectedSourceLinkMap.put(component1, Sets.newHashSet(new LegalSourceLinkDTO(source1)));

    assertThat(results).isEqualTo(expectedSourceLinkMap);
  }
}
