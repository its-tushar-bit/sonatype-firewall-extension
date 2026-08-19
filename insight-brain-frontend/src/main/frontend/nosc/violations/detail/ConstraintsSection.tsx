/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { type ReactElement } from 'react';
import { Box, Card, Flex, Heading, Text } from '@radix-ui/themes';
import type { ConstraintViolationDTO } from 'MainRoot/nosc/violations/detail/violationDetailTypes';

export interface ConstraintsSectionProps {
  readonly constraintViolations: ReadonlyArray<ConstraintViolationDTO> | undefined;
}

export function ConstraintsSection({ constraintViolations }: ConstraintsSectionProps): ReactElement | null {
  if (!constraintViolations?.length) {
    return null;
  }

  return (
    <Card data-testid="nosc-violation-detail-constraints">
      <Flex direction="column" gap="3">
        <Heading as="h2" size="3">
          Policy Constraints
        </Heading>
        {constraintViolations.map((constraint, constraintIndex) => (
          <Box key={`${constraint.constraintName}-${constraintIndex}`}>
            <Text as="p" size="2" weight="medium">
              {constraint.constraintName}
            </Text>
            {constraint.reasons?.length ? (
              <Box asChild mt="2">
                <ul>
                  {constraint.reasons.map((reason, reasonIndex) => (
                    <li key={`${reason.reason}-${reason.reference?.value ?? reasonIndex}`}>
                      <Text size="2" color="gray">
                        {reason.reason}
                      </Text>
                    </li>
                  ))}
                </ul>
              </Box>
            ) : (
              <Text as="p" size="2" color="gray">
                No condition specified.
              </Text>
            )}
          </Box>
        ))}
      </Flex>
    </Card>
  );
}
