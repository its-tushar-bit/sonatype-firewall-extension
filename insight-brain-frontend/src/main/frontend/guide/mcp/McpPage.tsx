/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useState } from 'react';
import {
  Box,
  Flex,
  Card,
  Code,
  Tabs,
  Grid,
  Heading,
  Text,
  DropdownMenu,
  IconButton,
} from '@radix-ui/themes';
import { CircleCheck, ExternalLink, MoreVertical } from 'lucide-react';
import { SectionHeading, BodyText, PageLayout } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import { CopyToClipboardButton } from './CopyToClipboardButton';
import { CodeSnippet, IDEConfigList } from './IDEConfigList';
import { ToolsAndTroubleshooting } from './ToolsAndTroubleshooting';
import type { MCPData } from './types';
import mcpData from '../../../resources/mcp.json';

const data = mcpData as MCPData;

export function McpPage() {
  const [activeTab, setActiveTab] = useState(data.tabs[0].id);
  const [expandedTool, setExpandedTool] = useState<string | null>(null);
  const [openTroubleshootingAccordion, setOpenTroubleshootingAccordion] = useState<string>('');

  const currentTab = data.tabs.find((tab) => tab.id === activeTab) || data.tabs[0];

  const handleTabChange = (value: string) => {
    setActiveTab(value);
  };

  // Split tabs for different screen sizes
  const firstThreeTabs = data.tabs.slice(0, 3);
  const activeTabIndex = data.tabs.findIndex((tab) => tab.id === activeTab);
  const isActiveTabBeyondThree = activeTabIndex >= 3;

  const visibleTabsXsToSm = isActiveTabBeyondThree
    ? [...firstThreeTabs, currentTab]
    : data.tabs.slice(0, 4);

  const remainingTabsXsToSm = data.tabs.filter((tab) => !visibleTabsXsToSm.includes(tab));

  return (
    <PageLayout>
      <Grid columns={{ initial: '1', lg: '2fr 1fr' }} gap={tokens.space.section} align="start">
        {/* Header - spans full width */}
        <Flex direction="column" gap="2" style={{ gridColumn: '1 / -1' }}>
          <Heading as="h1" size={tokens.sizes.heroTitle} color="gray" highContrast>
            Sonatype MCP
          </Heading>
          <Text size="3" color="gray">
            Get real-time security scanning, version guidance, and compliance checks right in your
            AI chat.
          </Text>
        </Flex>

        {/* Left Column: Auth + Optional Headers + Connection Guides */}
        <Flex direction="column" gap={tokens.space.section}>
          <Box
            p={tokens.space.item}
            style={{
              backgroundColor: 'var(--blue-2)',
              border: '1px solid var(--blue-6)',
              borderRadius: 'var(--radius-3)',
            }}
          >
            <Flex direction="column" gap={tokens.space.inline}>
              <Text size="2" weight="bold">
                {data.authHelp.title}
              </Text>
              <Text size="2" color="gray">
                {data.authHelp.description}
              </Text>
              <Box
                p={tokens.space.inline}
                style={{
                  backgroundColor: 'var(--gray-2)',
                  borderRadius: 'var(--radius-2)',
                  fontFamily: 'var(--code-font-family)',
                  fontSize: tokens.code.fontSize,
                }}
              >
                {data.authHelp.example}
              </Box>
              {data.authHelp.learnMoreUrl && (
                <Text size="2">
                  <a
                    href={data.authHelp.learnMoreUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25em' }}
                  >
                    {data.authHelp.learnMoreText || 'Learn more'}
                    <ExternalLink size={12} />
                  </a>
                </Text>
              )}
            </Flex>
          </Box>
          {data.optionalHeaders && data.optionalHeaders.length > 0 && (
            <Box
              p={tokens.space.item}
              style={{
                backgroundColor: 'var(--gray-2)',
                border: '1px solid var(--gray-6)',
                borderRadius: 'var(--radius-3)',
              }}
            >
              <Flex direction="column" gap={tokens.space.item}>
                <Text size="2" weight="bold">
                  Optional Headers
                </Text>
                <Text size="2" color="gray">
                  Pass these headers to enable policy evaluation for specific IQ applications.
                  These can also be provided as parameters in tool calls.
                </Text>
                {data.optionalHeaders.map((header, index) => (
                  <Box key={index} mt={tokens.space.inline}>
                    <Flex direction="column" gap={tokens.space.tight}>
                      <Text size="2" weight="medium" style={{ fontFamily: 'var(--code-font-family)' }}>
                        {header.name}
                      </Text>
                      <Text size="2" color="gray">
                        {header.description}
                      </Text>
                      <Box
                        p={tokens.space.tight}
                        style={{
                          backgroundColor: 'var(--gray-3)',
                          borderRadius: 'var(--radius-2)',
                          fontFamily: 'var(--code-font-family)',
                          fontSize: tokens.code.fontSize,
                        }}
                      >
                        {header.example}
                      </Box>
                    </Flex>
                  </Box>
                ))}
              </Flex>
            </Box>
          )}

          {/* Connection Guides */}
          <Card size={tokens.card.large}>
          <Box p={tokens.space.section}>
            <Flex direction="column" gap={tokens.space.page}>
              <SectionHeading>Connection Guides</SectionHeading>

              {/* IDE Tabs */}
              <Tabs.Root value={activeTab} onValueChange={handleTabChange}>
                <Flex align="center" gap={tokens.space.inline}>
                  <Tabs.List size="2" style={{ flexGrow: 1 }}>
                    {/* Below XS: Show only current tab */}
                    <Box display={{ initial: 'block', xs: 'none' }}>
                      <Tabs.Trigger value={currentTab.id}>{currentTab.name}</Tabs.Trigger>
                    </Box>

                    {/* XS to SM: Show first 3 tabs + active tab if beyond index 3 */}
                    {visibleTabsXsToSm.map((tab) => (
                      <Box key={tab.id} display={{ initial: 'none', xs: 'block', sm: 'none' }}>
                        <Tabs.Trigger value={tab.id}>{tab.name}</Tabs.Trigger>
                      </Box>
                    ))}

                    {/* SM+ screens: Show all tabs */}
                    {data.tabs.map((tab) => (
                      <Box key={tab.id} display={{ initial: 'none', sm: 'block' }}>
                        <Tabs.Trigger value={tab.id}>{tab.name}</Tabs.Trigger>
                      </Box>
                    ))}
                  </Tabs.List>

                  {/* Overflow menu - Below XS: show all tabs */}
                  <Box display={{ initial: 'block', xs: 'none' }}>
                    <DropdownMenu.Root>
                      <DropdownMenu.Trigger>
                        <IconButton
                          variant="soft"
                          color="gray"
                          size={tokens.button.medium}
                          aria-label="More tabs"
                        >
                          <MoreVertical size={tokens.icon.menu} />
                        </IconButton>
                      </DropdownMenu.Trigger>
                      <DropdownMenu.Content align="end">
                        {data.tabs.map((tab) => (
                          <DropdownMenu.Item key={tab.id} onClick={() => handleTabChange(tab.id)}>
                            {tab.name}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                    </DropdownMenu.Root>
                  </Box>

                  {/* Overflow menu - XS to SM: show remaining tabs */}
                  <Box display={{ initial: 'none', xs: 'block', sm: 'none' }}>
                    <DropdownMenu.Root>
                      <DropdownMenu.Trigger>
                        <IconButton
                          variant="soft"
                          color="gray"
                          size={tokens.button.medium}
                          aria-label="More tabs"
                        >
                          <MoreVertical size={tokens.icon.menu} />
                        </IconButton>
                      </DropdownMenu.Trigger>
                      <DropdownMenu.Content align="end">
                        {remainingTabsXsToSm.map((tab) => (
                          <DropdownMenu.Item key={tab.id} onClick={() => handleTabChange(tab.id)}>
                            {tab.name}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                    </DropdownMenu.Root>
                  </Box>
                </Flex>
              </Tabs.Root>

              {/* Step 1: Add configuration */}
              <Box>
                <Box style={{ display: 'block' }}>
                  <BodyText size="sm" weight="bold" mb={tokens.space.item}>
                    {currentTab.configTitle || '1. Add configuration'}
                  </BodyText>
                </Box>

                {/* Render IDE Configs if available (GitHub Copilot) */}
                {currentTab.ideConfigs ? (
                  <IDEConfigList configs={currentTab.ideConfigs} />
                ) : (
                  /* Standard Configuration Code Block */
                  <CodeSnippet
                    code={(currentTab.type === 'command' ? currentTab.command : currentTab.config) ?? ''}
                  />
                )}

                {currentTab.note && (
                  <BodyText size="sm" tone="subtle" mt={tokens.space.inline}>
                    {currentTab.note}
                  </BodyText>
                )}
              </Box>

              {/* Step 2: Start Using */}
              <Box>
                <Box style={{ display: 'block' }}>
                  <BodyText size="sm" weight="bold" mb={tokens.space.item}>
                    {currentTab.startUsingTitle || '2. Start Using'}
                  </BodyText>
                </Box>
                <Flex direction="column" gap={tokens.space.inline}>
                  <BodyText size="sm" tone="subtle">
                    Your IQ Server credentials authenticate all requests automatically. Simply start
                    using the MCP tools in your AI assistant - no additional login required.
                  </BodyText>
                </Flex>
              </Box>

              {/* Step 3: Configure AI Assistant Rules */}
              <Box>
                <Box style={{ display: 'block' }}>
                  <BodyText size="sm" weight="bold" mb={tokens.space.item}>
                    {currentTab.rulesTitle || '3. Configure AI Assistant Rules (Recommended)'}
                  </BodyText>
                </Box>
                <Flex direction="column" gap={tokens.space.item}>
                  <BodyText size="sm" tone="subtle">
                    Maximize effectiveness by configuring your AI assistant to prioritize Sonatype
                    MCP tools for dependency-related tasks.
                  </BodyText>
                  <Flex direction="column" gap={tokens.space.inline}>
                    <BodyText size="sm" weight="medium">
                      Create file: <Code>{currentTab.rulesFile}</Code>
                    </BodyText>
                    <Box style={{ position: 'relative' }}>
                      <Box
                        p={tokens.space.item}
                        style={{
                          backgroundColor: 'var(--gray-2)',
                          borderRadius: 'var(--radius-3)',
                          fontFamily: 'var(--default-font-family)',
                          fontSize: tokens.code.fontSize,
                          lineHeight: 1.6,
                          whiteSpace: 'pre-wrap',
                          wordBreak: 'break-word',
                          paddingRight: '34px',
                        }}
                      >
                        {currentTab.rulesContent}
                      </Box>
                      <CopyToClipboardButton text={currentTab.rulesContent} />
                    </Box>
                  </Flex>
                </Flex>
              </Box>

              {/* Success Message */}
              <Box
                p={tokens.space.item}
                style={{
                  backgroundColor: 'var(--green-2)',
                  border: '1px solid var(--green-6)',
                  borderRadius: 'var(--radius-3)',
                }}
              >
                <Flex align="center" gap={tokens.space.inline}>
                  <CircleCheck size={tokens.icon.menu} color="var(--green-11)" />
                  <BodyText size="sm" tone="subtle">
                    That&apos;s it! The MCP tools will now be available in your AI agent.
                  </BodyText>
                </Flex>
              </Box>
            </Flex>
          </Box>
        </Card>

        </Flex>

        {/* Right Column: Tools and Troubleshooting */}
        <ToolsAndTroubleshooting
          tools={data.tools}
          troubleshooting={data.troubleshooting}
          expandedTool={expandedTool}
          onToolToggle={setExpandedTool}
          openTroubleshootingAccordion={openTroubleshootingAccordion}
          onTroubleshootingAccordionChange={setOpenTroubleshootingAccordion}
        />
      </Grid>
    </PageLayout>
  );
}
