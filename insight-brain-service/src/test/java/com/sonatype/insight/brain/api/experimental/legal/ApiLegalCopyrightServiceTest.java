/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightFilePathDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightFilePathsDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.dto.model.AnameAggregateFileGroup;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentFilePathsDTO;
import com.sonatype.insight.license.dto.model.LegalCommentDTO;
import com.sonatype.insight.license.dto.model.LegalCommentFilesDTO;
import com.sonatype.insight.license.dto.model.LegalCopyrightDTO;
import com.sonatype.insight.license.model.LicensedFeature;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.mockito.Mock;

public class ApiLegalCopyrightServiceTest
    extends AbstractComponentTest
{
  @Mock
  private ApiLicenseLegalHdsService mockHdsService;

  @Inject
  private ApiLegalCopyrightService apiLegalCopyrightService;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testGetCopyrightFilePaths_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightFilePaths(
            OwnerType.APPLICATION, "1",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
            "hash", "copyright hash 2", 0, 10));
  }

  @Test
  public void testGetCopyrightFileCount_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightFileCount(
            OwnerType.APPLICATION, "1",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
            "hash"));
  }

  @Test
  public void testGetCopyrightContextContent_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightContextContent(
            OwnerType.APPLICATION, "1",
            ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
            "hash", "copyright hash 2", "path/file"));
  }

  @Test
  public void testGetCopyrightFilePathsInvalidComponentId() throws JsonProcessingException {
    final ComponentIdentifier componentIdentifier = createInvalidComponentIdentifier();
    assertThatExceptionOfType(InvalidComponentIdentifierException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightFilePaths(
            OwnerType.APPLICATION, "1",
            componentIdentifier, "hash", "copyright hash 2", 0, 10));
  }

  @Test
  public void testGetCopyrightContextContentInvalidPagination() {
    final ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightFilePaths(
            OwnerType.APPLICATION, "1",
            mavenIdentifier, "hash", "copyright hash 2", 0, 0));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightFilePaths(
            OwnerType.APPLICATION, "1",
            mavenIdentifier, "hash", "copyright hash 2", -1, 10));
  }

  @Test
  public void testGetCopyrightContextContentInvalidComponentId() throws JsonProcessingException {
    final ComponentIdentifier componentIdentifier = createInvalidComponentIdentifier();
    assertThatExceptionOfType(InvalidComponentIdentifierException.class)
        .isThrownBy(() -> apiLegalCopyrightService.getCopyrightContextContent(
            OwnerType.APPLICATION, "1",
            componentIdentifier, "hash", "copyright hash 2", "path"));
  }

  @Test
  public void testGetNonAnameCopyrightFilePaths() {
    final ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    final LegalCommentFilesDTO commentFilesDTO1 = new LegalCommentFilesDTO();
    commentFilesDTO1.setContent("Content 1");
    commentFilesDTO1.setCopyrightContentHashes(ImmutableSet.of("copyright hash 1", "copyright hash 2"));
    commentFilesDTO1.setFilePaths(ImmutableSet.of("path1/file1", "path2/file1"));

    final LegalCommentFilesDTO commentFilesDTO2 = new LegalCommentFilesDTO();
    commentFilesDTO2.setContent("Content 2");
    commentFilesDTO2.setCopyrightContentHashes(ImmutableSet.of("copyright hash 3", "copyright hash 2"));
    commentFilesDTO2.setFilePaths(ImmutableSet.of("path2/file2", "path1/file1", "path2/file1"));

    final ComponentLegalCommentFilePathsDTO hdsResponse = new ComponentLegalCommentFilePathsDTO();
    hdsResponse.setHash("hash");
    hdsResponse.setComponentIdentifier(mavenIdentifier);
    hdsResponse.setComments(ImmutableSet.of(commentFilesDTO1, commentFilesDTO2));

    doReturn(ImmutableSet.of(hdsResponse))
        .when(mockHdsService)
        .getComponentLegalCommentFilePaths(mavenIdentifier);

    final CopyrightFilePathsDTO copyrightContexts = apiLegalCopyrightService.getCopyrightFilePaths(
        OwnerType.APPLICATION, "1",
        mavenIdentifier, "hash", "copyright hash 2", 0, 10);

    assertThat(copyrightContexts.getTotalFileMatches()).isEqualTo(3);
    assertThat(copyrightContexts.getFilePaths()).hasSize(3)
        .containsExactly(
            filePath("path1/file1", 2),
            filePath("path2/file1", 2),
            filePath("path2/file2", 1));

    final CopyrightFilePathsDTO copyrightContextsPage1 = apiLegalCopyrightService.getCopyrightFilePaths(
        OwnerType.APPLICATION, "1",
        mavenIdentifier, "hash", "copyright hash 2", 0, 2);

    assertThat(copyrightContextsPage1.getTotalFileMatches()).isEqualTo(3);
    assertThat(copyrightContextsPage1.getFilePaths()).hasSize(2)
        .containsExactly(
            filePath("path1/file1", 2),
            filePath("path2/file1", 2));

    final CopyrightFilePathsDTO copyrightContextsPage2 = apiLegalCopyrightService.getCopyrightFilePaths(
        OwnerType.APPLICATION, "1",
        mavenIdentifier, "hash", "copyright hash 2", 1, 2);

    assertThat(copyrightContextsPage1.getTotalFileMatches()).isEqualTo(3);
    assertThat(copyrightContextsPage2.getFilePaths()).hasSize(2)
        .containsExactly(
            filePath("path2/file1", 2),
            filePath("path2/file2", 1));

    final CopyrightFilePathsDTO contextsHash3 = apiLegalCopyrightService.getCopyrightFilePaths(
        OwnerType.APPLICATION, "1",
        mavenIdentifier, "hash", "copyright hash 3", 0, 10);

    assertThat(contextsHash3.getTotalFileMatches()).isEqualTo(3);
    assertThat(contextsHash3.getFilePaths()).hasSize(3)
        .containsExactly(
            filePath("path1/file1", 1),
            filePath("path2/file1", 1),
            filePath("path2/file2", 1));
  }

  private CopyrightFilePathDTO filePath(final String filePath, final int count) {
    return new CopyrightFilePathDTO(filePath, count);
  }

  @Test
  public void testGetNonAnameCopyrightContextContent() {
    final ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    final LegalCommentFilesDTO commentFilesDTO1 = new LegalCommentFilesDTO();
    commentFilesDTO1.setContent("Content 1");
    commentFilesDTO1.setCopyrightContentHashes(ImmutableSet.of("copyright hash 1", "copyright hash 2"));
    commentFilesDTO1.setFilePaths(ImmutableSet.of("path1/file1", "path2/file1"));

    final LegalCommentFilesDTO commentFilesDTO2 = new LegalCommentFilesDTO();
    commentFilesDTO2.setContent("Content 2");
    commentFilesDTO2.setCopyrightContentHashes(ImmutableSet.of("copyright hash 3", "copyright hash 2"));
    commentFilesDTO2.setFilePaths(ImmutableSet.of("path2/file2", "path1/file1", "path2/file1"));

    final ComponentLegalCommentFilePathsDTO hdsResponse = new ComponentLegalCommentFilePathsDTO();
    hdsResponse.setHash("hash");
    hdsResponse.setComponentIdentifier(mavenIdentifier);
    hdsResponse.setComments(ImmutableSet.of(commentFilesDTO1, commentFilesDTO2));

    doReturn(ImmutableSet.of(hdsResponse))
        .when(mockHdsService)
        .getComponentLegalCommentFilePaths(mavenIdentifier);

    final List<String> copyrightContextContent1 = apiLegalCopyrightService.getCopyrightContextContent(
        OwnerType.APPLICATION, "1",
        mavenIdentifier, "hash", "copyright hash 2", "path2/file1");

    assertThat(copyrightContextContent1).containsExactlyInAnyOrder("Content 1", "Content 2");

    final List<String> copyrightContextContent2 = apiLegalCopyrightService.getCopyrightContextContent(
        OwnerType.APPLICATION, "1",
        mavenIdentifier, "hash", "copyright hash 1", "path1/file1");

    assertThat(copyrightContextContent2).containsExactly("Content 1");
  }

  @Test
  public void testGetAnameCopyrightFilePaths() {
    final ComponentIdentifier anameIdentifier = ComponentIdentifier.createAnameCoordinates("n", "q", "v");
    final String componentHash = "compHash";

    final Application app = tempEntity.newApplicationWithParent();
    final OwnerComponent appComp =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, componentHash, anameIdentifier);
    tempEntity.newAggregateFile(appComp.getId(), "aggregate_file_hash1",
        ImmutableSet.of("some/path", "other/path", "z/path"));
    tempEntity.newAggregateFile(appComp.getId(), "aggregate_file_hash2", ImmutableSet.of("path2/file", "some/path"));

    final LegalCommentDTO commentDTO1 = new LegalCommentDTO();
    commentDTO1.setContent("Content 1");
    commentDTO1.setCopyrights(ImmutableSet.of(new LegalCopyrightDTO("Content 1", "content1Hash", "Author 1", "Year1"),
        new LegalCopyrightDTO("Content 1.1", "content11Hash", "Author 1.1", "Year1.1")));

    final LegalCommentDTO commentDTO2 = new LegalCommentDTO();
    commentDTO2.setContent("Content 2");
    commentDTO2.setCopyrights(ImmutableSet.of(new LegalCopyrightDTO("Content 2", "content2Hash", "Author 2", "Year2"),
        new LegalCopyrightDTO("Content 2.1", "content1Hash", "Author 1.1", "Year1.1")));

    final ComponentLegalCommentDTO comment1 = new ComponentLegalCommentDTO();
    comment1.setHash("aggregate_file_hash1");
    comment1.setComponentIdentifier(anameIdentifier);
    comment1.setComments(ImmutableSet.of(commentDTO1));

    final ComponentLegalCommentDTO comment2 = new ComponentLegalCommentDTO();
    comment2.setHash("aggregate_file_hash2");
    comment2.setComponentIdentifier(anameIdentifier);
    comment2.setComments(ImmutableSet.of(commentDTO2));

    final AnameAggregateFileGroup aggregateFileGroup = new AnameAggregateFileGroup(
        anameIdentifier,
        ImmutableList.of("aggregate_file_hash1", "aggregate_file_hash2"));

    doReturn(ImmutableSet.of(comment1, comment2))
        .when(mockHdsService)
        .getAnameRawComponentLegalComments(ImmutableSet.of(aggregateFileGroup));

    final CopyrightFilePathsDTO copyrightContexts = apiLegalCopyrightService.getCopyrightFilePaths(
        OwnerType.APPLICATION, "1",
        anameIdentifier, componentHash, "content1Hash", 0, 10);

    assertThat(copyrightContexts.getTotalFileMatches()).isEqualTo(4);
    assertThat(copyrightContexts.getFilePaths()).hasSize(4)
        .containsExactly(
            filePath("other/path", 1),
            filePath("path2/file", 1),
            filePath("some/path", 2),
            filePath("z/path", 1));

    final CopyrightFilePathsDTO copyrightContextsPage1 = apiLegalCopyrightService.getCopyrightFilePaths(
        OwnerType.APPLICATION, "1",
        anameIdentifier, componentHash, "content1Hash", 0, 2);

    assertThat(copyrightContextsPage1.getTotalFileMatches()).isEqualTo(4);
    assertThat(copyrightContextsPage1.getFilePaths()).hasSize(2)
        .containsExactly(
            filePath("other/path", 1),
            filePath("path2/file", 1));

    final CopyrightFilePathsDTO copyrightContextsPage2 = apiLegalCopyrightService.getCopyrightFilePaths(
        OwnerType.APPLICATION, "1",
        anameIdentifier, componentHash, "content1Hash", 2, 4);

    assertThat(copyrightContextsPage2.getTotalFileMatches()).isEqualTo(4);
    assertThat(copyrightContextsPage2.getFilePaths()).hasSize(2)
        .containsExactly(
            filePath("some/path", 2),
            filePath("z/path", 1));
  }

  @Test
  public void testGetCopyrightFilePaths_NullHash() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createAnameCoordinates("n", "q", "v");

    CopyrightFilePathsDTO copyrightFilePathsDTO = apiLegalCopyrightService.getCopyrightFilePaths(OwnerType.APPLICATION,
        "ownerId", componentIdentifier, null, "copyrightContentHash", 0, 10);
    assertThat(copyrightFilePathsDTO).isNotNull();
    assertThat(copyrightFilePathsDTO.getFilePaths()).isEmpty();
    assertThat(copyrightFilePathsDTO.getTotalFileMatches()).isEqualTo(0);
  }

  @Test
  public void testGetAnameCopyrightContextContents() {
    final ComponentIdentifier anameIdentifier = ComponentIdentifier.createAnameCoordinates("n", "q", "v");
    final String componentHash = "compHash";

    final Application app = tempEntity.newApplicationWithParent();
    final OwnerComponent appComp =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, componentHash, anameIdentifier);
    tempEntity.newAggregateFile(appComp.getId(), "aggregate_file_hash1",
        ImmutableSet.of("some/path", "other/path", "z/path"));
    tempEntity.newAggregateFile(appComp.getId(), "aggregate_file_hash2", ImmutableSet.of("other/path"));

    final LegalCommentDTO commentDTO1 = new LegalCommentDTO();
    commentDTO1.setContent("Content 1");
    commentDTO1.setCopyrights(ImmutableSet.of(new LegalCopyrightDTO("Content 1", "content1Hash", "Author 1", "Year1"),
        new LegalCopyrightDTO("Content A", "contentAHash", "Author A", "Year A")));

    final LegalCommentDTO commentDTO2 = new LegalCommentDTO();
    commentDTO2.setContent("Content 2");
    commentDTO2.setCopyrights(ImmutableSet.of(new LegalCopyrightDTO("Content 2", "content2Hash", "Author 2", "Year2"),
        new LegalCopyrightDTO("Content A", "contentAHash", "Author A", "Year A")));

    final ComponentLegalCommentDTO comment1 = new ComponentLegalCommentDTO();
    comment1.setHash("aggregate_file_hash1");
    comment1.setComponentIdentifier(anameIdentifier);
    comment1.setComments(ImmutableSet.of(commentDTO1));

    final ComponentLegalCommentDTO comment2 = new ComponentLegalCommentDTO();
    comment2.setHash("aggregate_file_hash2");
    comment2.setComponentIdentifier(anameIdentifier);
    comment2.setComments(ImmutableSet.of(commentDTO2));

    final AnameAggregateFileGroup aggregateFileGroup = new AnameAggregateFileGroup(
        anameIdentifier,
        ImmutableList.of("aggregate_file_hash1", "aggregate_file_hash2"));

    doReturn(ImmutableSet.of(comment1, comment2))
        .when(mockHdsService)
        .getAnameRawComponentLegalComments(ImmutableSet.of(aggregateFileGroup));

    final List<String> copyrightContextContent1 = apiLegalCopyrightService.getCopyrightContextContent(
        OwnerType.APPLICATION, "1",
        anameIdentifier, componentHash, "contentAHash", "other/path");

    assertThat(copyrightContextContent1).containsExactlyInAnyOrder("Content 1", "Content 2");

    final List<String> copyrightContextContent2 = apiLegalCopyrightService.getCopyrightContextContent(
        OwnerType.APPLICATION, "1",
        anameIdentifier, componentHash, "content2Hash", "other/path");

    assertThat(copyrightContextContent2).containsExactly("Content 2");
  }

  @Test
  public void testGetCopyrightContextContent_NullHash() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createAnameCoordinates("n", "q", "v");

    assertThat(apiLegalCopyrightService.getCopyrightContextContent(OwnerType.APPLICATION, "ownerId",
        componentIdentifier, null, "copyrightContentHash", "some/path")).isEmpty();
  }

  @Test
  public void testGetNonAnameCopyrightFileCount() {
    final ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    final LegalCommentFilesDTO commentFilesDTO1 = new LegalCommentFilesDTO();
    commentFilesDTO1.setContent("Content 1");
    commentFilesDTO1.setCopyrightContentHashes(ImmutableSet.of("copyright hash 1", "copyright hash 2"));
    commentFilesDTO1.setFilePaths(ImmutableSet.of("path1/file1", "path2/file1"));

    final LegalCommentFilesDTO commentFilesDTO2 = new LegalCommentFilesDTO();
    commentFilesDTO2.setContent("Content 2");
    commentFilesDTO2.setCopyrightContentHashes(ImmutableSet.of("copyright hash 3", "copyright hash 2"));
    commentFilesDTO2.setFilePaths(ImmutableSet.of("path2/file2", "path1/file1", "path2/file1"));

    final ComponentLegalCommentFilePathsDTO hdsResponse = new ComponentLegalCommentFilePathsDTO();
    hdsResponse.setHash("hash");
    hdsResponse.setComponentIdentifier(mavenIdentifier);
    hdsResponse.setComments(ImmutableSet.of(commentFilesDTO1, commentFilesDTO2));

    doReturn(ImmutableSet.of(hdsResponse))
        .when(mockHdsService)
        .getComponentLegalCommentFilePaths(mavenIdentifier);

    final Map<String, Integer> copyrightFileCount = apiLegalCopyrightService.getCopyrightFileCount(
        OwnerType.APPLICATION, "1", mavenIdentifier, "hash");

    assertThat(copyrightFileCount).hasSize(3)
        .hasEntrySatisfying("copyright hash 1", new Condition<>(Predicate.isEqual(2), "hash 1"))
        .hasEntrySatisfying("copyright hash 2", new Condition<>(Predicate.isEqual(5), "hash 2"))
        .hasEntrySatisfying("copyright hash 3", new Condition<>(Predicate.isEqual(3), "hash 3"));
  }

  @Test
  public void testGetAnameCopyrightFileCount() {
    final ComponentIdentifier anameIdentifier = ComponentIdentifier.createAnameCoordinates("n", "q", "v");
    final String componentHash = "compHash";

    final Application app = tempEntity.newApplicationWithParent();
    final OwnerComponent appComp =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, componentHash, anameIdentifier);
    tempEntity.newAggregateFile(appComp.getId(), "aggregate_file_hash1",
        ImmutableSet.of("some/path", "other/path", "z/path"));
    tempEntity.newAggregateFile(appComp.getId(), "aggregate_file_hash2", ImmutableSet.of("other/path"));

    final LegalCommentDTO commentDTO1 = new LegalCommentDTO();
    commentDTO1.setContent("Content 1");
    commentDTO1.setCopyrights(ImmutableSet.of(new LegalCopyrightDTO("Content 1", "content1Hash", "Author 1", "Year1"),
        new LegalCopyrightDTO("Content A", "contentAHash", "Author A", "Year A")));

    final LegalCommentDTO commentDTO2 = new LegalCommentDTO();
    commentDTO2.setContent("Content 2");
    commentDTO2.setCopyrights(ImmutableSet.of(new LegalCopyrightDTO("Content 2", "content2Hash", "Author 2", "Year2"),
        new LegalCopyrightDTO("Content A", "contentAHash", "Author A", "Year A")));

    final ComponentLegalCommentDTO comment1 = new ComponentLegalCommentDTO();
    comment1.setHash("aggregate_file_hash1");
    comment1.setComponentIdentifier(anameIdentifier);
    comment1.setComments(ImmutableSet.of(commentDTO1));

    final ComponentLegalCommentDTO comment2 = new ComponentLegalCommentDTO();
    comment2.setHash("aggregate_file_hash2");
    comment2.setComponentIdentifier(anameIdentifier);
    comment2.setComments(ImmutableSet.of(commentDTO2));

    final AnameAggregateFileGroup aggregateFileGroup = new AnameAggregateFileGroup(
        anameIdentifier,
        ImmutableList.of("aggregate_file_hash1", "aggregate_file_hash2"));

    doReturn(ImmutableSet.of(comment1, comment2))
        .when(mockHdsService)
        .getAnameRawComponentLegalComments(ImmutableSet.of(aggregateFileGroup));

    final Map<String, Integer> copyrightFileCount = apiLegalCopyrightService.getCopyrightFileCount(
        OwnerType.APPLICATION, "1",
        anameIdentifier, componentHash);

    assertThat(copyrightFileCount).hasSize(3)
        .hasEntrySatisfying("content1Hash", new Condition<>(Predicate.isEqual(3), "content1Hash"))
        .hasEntrySatisfying("content2Hash", new Condition<>(Predicate.isEqual(1), "content2Hash"))
        .hasEntrySatisfying("contentAHash", new Condition<>(Predicate.isEqual(4), "contentAHash"));
  }

  @Test
  public void testGetCopyrightFileCount_NullHash() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createAnameCoordinates("n", "q", "v");

    assertThat(apiLegalCopyrightService.getCopyrightFileCount(OwnerType.APPLICATION, "ownerId", componentIdentifier,
        null)).isEmpty();
  }

  private ComponentIdentifier createInvalidComponentIdentifier() throws JsonProcessingException {
    return new ObjectMapper().readValue("{\n" +
        "            \"format\": \"maven\",\n" +
        "            \"coordinates\": {\n" +
        "                \"name\": \"@carbon/ibmdotcom-react\",\n" +
        "                \"version\":\"1.16.0-canary.535625119.0\"\n" +
        "            }}", ComponentIdentifier.class);
  }
}
