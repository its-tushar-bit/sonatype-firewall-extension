/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import type { KeyboardEvent } from 'react';
import { Box, Card, Flex, Heading, Separator, Text } from '@radix-ui/themes';
import { ChevronDown, ChevronUp } from 'lucide-react';
import { BodyText, ItemTitle } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import * as Accordion from '@radix-ui/react-accordion';
import type { MCPTool, TroubleshootingItem } from './types';

function AccordionChevron({ isOpen }: { isOpen?: boolean }) {
  return (
    <ChevronDown
      size={20}
      style={{
        transform: isOpen ? 'rotate(180deg)' : 'rotate(0deg)',
        transition: 'transform 0.2s ease',
      }}
    />
  );
}

interface ToolsAndTroubleshootingProps {
  tools: MCPTool[];
  troubleshooting: TroubleshootingItem[];
  expandedTool: string | null;
  onToolToggle: (toolName: string | null) => void;
  openTroubleshootingAccordion: string;
  onTroubleshootingAccordionChange: (value: string) => void;
}

export function ToolsAndTroubleshooting({
  tools,
  troubleshooting,
  expandedTool,
  onToolToggle,
  openTroubleshootingAccordion,
  onTroubleshootingAccordionChange,
}: ToolsAndTroubleshootingProps) {
  return (
    <Flex direction="column" gap={tokens.space.item}>
      {/* Available MCP Tools */}
      <Box mb={tokens.space.tight}>
        <Heading as="h2" size={tokens.sizes.sectionTitle} weight="medium">
          Available MCP Tools
        </Heading>
      </Box>
      <Flex direction="column" gap={tokens.space.inline}>
        {tools.map((tool, index) => {
          const isExpanded = expandedTool === tool.name;
          return (
            <Card
              key={index}
              size={tokens.card.small}
              role="button"
              tabIndex={0}
              aria-expanded={isExpanded}
              style={{
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
              onClick={() => onToolToggle(isExpanded ? null : tool.name)}
              onKeyDown={(e: KeyboardEvent) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  onToolToggle(isExpanded ? null : tool.name);
                }
              }}
            >
              <Box p={tokens.space.item}>
                <Flex align="center" justify="between">
                  <Flex
                    direction="column"
                    gap={tokens.space.tight}
                    style={{ flex: 1, minWidth: 0 }}
                  >
                    <Text
                      size={{ initial: tokens.sizes.body.sm, sm: tokens.sizes.body.md }}
                      weight="medium"
                      style={{
                        fontFamily: 'var(--code-font-family)',
                        wordBreak: 'break-word',
                        overflowWrap: 'break-word',
                      }}
                    >
                      {tool.name}
                    </Text>
                    <Text size={tokens.sizes.body.sm} color="gray">
                      {tool.description}
                    </Text>
                  </Flex>
                  <Box style={{ flexShrink: 0, marginLeft: tokens.space.inline }}>
                    {isExpanded ? (
                      <ChevronUp size={tokens.icon.large} />
                    ) : (
                      <ChevronDown size={tokens.icon.large} />
                    )}
                  </Box>
                </Flex>
                {isExpanded && (
                  <>
                    <Separator my={tokens.space.item} size="4" />
                    <Flex direction="column" gap={tokens.space.item}>
                      {/* Use Cases */}
                      <Box>
                        <Text
                          size="2"
                          weight="bold"
                          style={{ display: 'block', marginBottom: 'var(--space-2)' }}
                        >
                          Use Cases
                        </Text>
                        <Flex direction="column" gap={tokens.space.tight}>
                          {tool.useCases.map((useCase, useCaseIndex) => (
                            <Text key={useCaseIndex} size="2" color="gray">
                              {'•'} {useCase}
                            </Text>
                          ))}
                        </Flex>
                      </Box>
                      {/* Example Prompts */}
                      <Box>
                        <Text
                          size="2"
                          weight="bold"
                          style={{ display: 'block', marginBottom: 'var(--space-2)' }}
                        >
                          Example Prompts:
                        </Text>
                        <Flex direction="column" gap={tokens.space.inline}>
                          {tool.examplePrompts.map((prompt, promptIndex) => (
                            <Box
                              key={promptIndex}
                              p={tokens.space.inline}
                              style={{
                                backgroundColor: 'var(--blue-2)',
                                border: '1px solid var(--blue-6)',
                                borderRadius: 'var(--radius-2)',
                                fontFamily: 'var(--code-font-family)',
                                fontSize: tokens.code.fontSize,
                              }}
                            >
                              &ldquo;{prompt}&rdquo;
                            </Box>
                          ))}
                        </Flex>
                      </Box>
                    </Flex>
                  </>
                )}
              </Box>
            </Card>
          );
        })}
      </Flex>

      {/* Troubleshooting */}
      <Box mt={tokens.space.section} mb={tokens.space.tight}>
        <Heading as="h2" size={tokens.sizes.sectionTitle} weight="medium">
          Troubleshooting
        </Heading>
      </Box>
      <Accordion.Root
        type="single"
        collapsible
        value={openTroubleshootingAccordion}
        onValueChange={onTroubleshootingAccordionChange}
      >
        {troubleshooting.map((item, index) => {
          const itemValue = `troubleshooting-${index}`;
          const isOpen = openTroubleshootingAccordion === itemValue;
          return (
            <Accordion.Item key={index} value={itemValue}>
              <Card
                size={tokens.card.small}
                style={{
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                  marginBottom: tokens.space.inline,
                }}
              >
                <Accordion.Trigger asChild>
                  <Box p={tokens.space.item}>
                    <Flex align="center" justify="between">
                      <ItemTitle>{item.issue}</ItemTitle>
                      <AccordionChevron isOpen={isOpen} />
                    </Flex>
                  </Box>
                </Accordion.Trigger>
                <Accordion.Content>
                  <Box p={tokens.space.item} pt="0">
                    <BodyText size="sm" tone="subtle">
                      {item.solution}
                    </BodyText>
                  </Box>
                </Accordion.Content>
              </Card>
            </Accordion.Item>
          );
        })}
      </Accordion.Root>
    </Flex>
  );
}
