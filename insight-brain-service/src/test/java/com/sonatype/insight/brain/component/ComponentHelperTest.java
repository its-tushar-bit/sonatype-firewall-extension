/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Optional;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.git.PullRequestCommentingRemediationService;
import com.sonatype.insight.brain.git.RemediationVersionDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING;
import static com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ComponentHelperTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES =
      ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0");

  private static final String APP_ID = "app123";

  @Mock
  private PullRequestCommentingRemediationService mockRemediationService;

  private ComponentHelper componentHelper;

  @BeforeEach
  public void setUp() {
    componentHelper = new ComponentHelper(mockRemediationService);
  }

  @Test
  public void testIsGolden_whenRemediationVersionIsEmptyVersion() {
    // Given
    when(mockRemediationService.getRemediationVersion(any(), any())).thenReturn(Optional.empty());

    // When
    boolean result = componentHelper.isGoldenVersion(MAVEN_COORDINATES, APP_ID);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void testIsGolden_whenRemediationTypeIsNotRecommendedNonBreakingWithDependenciesVersion() {
    // Given
    var remediationVersionDTO = new RemediationVersionDTO("1.1", RECOMMENDED_NON_BREAKING);
    when(mockRemediationService.getRemediationVersion(eq(MAVEN_COORDINATES), eq(APP_ID)))
        .thenReturn(Optional.of(remediationVersionDTO));

    // When
    boolean result = componentHelper.isGoldenVersion(MAVEN_COORDINATES, APP_ID);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void testIsGolden_whenRemediationTypeIsRecommendedNonBreakingWithDependenciesVersion() {
    // Given
    var remediationVersionDTO = new RemediationVersionDTO("1.1", RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES);
    when(mockRemediationService.getRemediationVersion(eq(MAVEN_COORDINATES), eq(APP_ID)))
        .thenReturn(Optional.of(remediationVersionDTO));

    // When
    boolean result = componentHelper.isGoldenVersion(MAVEN_COORDINATES, APP_ID);

    // Then
    assertThat(result).isTrue();
  }
}
