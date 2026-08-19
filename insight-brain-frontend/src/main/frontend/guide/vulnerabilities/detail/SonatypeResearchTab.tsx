/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Box, Text } from '@radix-ui/themes';
import { tokens } from '@guide/ui-core/utils';
import { SonatypeResearchCard, MarkdownContent, BodyText, useVulnerability } from '@guide/ui-core';

const PLACEHOLDER_EXPLANATION = 'Explanation data is not yet available for this vulnerability.';
const PLACEHOLDER_DETECTION = 'Detection guidance is not yet available for this vulnerability.';
const PLACEHOLDER_RECOMMENDATION = 'Remediation guidance is not yet available for this vulnerability.';

export function SonatypeResearchTab() {
  const vulnerability = useVulnerability();

  const explanationContent =
    vulnerability.explanation && vulnerability.explanation.length > 0
      ? <MarkdownContent content={vulnerability.explanation} size="sm" />
      : <Text size={tokens.sizes.body.sm}>{PLACEHOLDER_EXPLANATION}</Text>;

  const detectionContent =
    vulnerability.detection && vulnerability.detection.length > 0
      ? <MarkdownContent content={vulnerability.detection} size="sm" />
      : <Text size={tokens.sizes.body.sm}>{PLACEHOLDER_DETECTION}</Text>;

  const recommendationContent =
    vulnerability.recommendation && vulnerability.recommendation.length > 0
      ? <MarkdownContent content={vulnerability.recommendation} size="sm" />
      : <Text size={tokens.sizes.body.sm}>{PLACEHOLDER_RECOMMENDATION}</Text>;

  return (
    <Box mt={tokens.space.section}>
      <SonatypeResearchCard
        vulnerability={vulnerability}
        title="Sonatype Research Data"
        explanationContent={explanationContent}
        detectionContent={detectionContent}
        recommendationContent={recommendationContent}
      />
    </Box>
  );
}
