/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from '../test-utils';

// Mock the mcp.json import
jest.mock('../../../../main/resources/mcp.json', () => ({
  tabs: [
    {
      id: 'github-copilot',
      name: 'GitHub Copilot',
      type: 'json',
      needsPrereq: false,
      configTitle: 'Supported IDEs and Configuration',
      startUsingTitle: 'Start Using',
      rulesTitle: 'Configure AI Assistant Rules (Recommended)',
      rulesFile: '.github/copilot-instructions.md',
      rulesContent: 'Test rules content',
      ideConfigs: [
        {
          ideName: 'Visual Studio Code',
          description: 'Add the following to the global VS Code mcp.json',
          docsUrl: 'https://code.visualstudio.com/docs/copilot/customization/mcp-servers',
          docsUrlText: 'VS Code MCP documentation',
          config: '{\n  "servers": {\n    "sonatype-mcp": {\n      "url": "https://YOUR-IQ-SERVER/mcp"\n    }\n  }\n}',
          note: 'Replace YOUR-IQ-SERVER with your IQ Server hostname.',
        },
      ],
    },
    {
      id: 'claude-code',
      name: 'Claude Code',
      type: 'command',
      command: 'claude mcp add sonatype-mcp https://YOUR-IQ-SERVER/mcp',
      needsPrereq: false,
      rulesFile: '~/.claude/CLAUDE.md',
      rulesContent: 'Test rules for Claude Code',
    },
    {
      id: 'gemini',
      name: 'Gemini',
      type: 'json',
      config: '{\n  "mcpServers": {\n    "sonatype-mcp": {\n      "httpUrl": "https://YOUR-IQ-SERVER/mcp"\n    }\n  }\n}',
      needsPrereq: false,
      rulesFile: '~/.gemini/GEMINI.md',
      rulesContent: 'Test rules for Gemini',
    },
  ],
  tools: [
    {
      name: 'getComponentVersion',
      description: 'Get comprehensive information for a specific package version',
      useCases: ['Security analysis', 'License checking'],
      examplePrompts: ['Is express 4.17.1 vulnerable?'],
    },
    {
      name: 'getLatestComponentVersion',
      description: 'Get information about the latest version',
      useCases: ['Find current version'],
      examplePrompts: ['What is the latest lodash?'],
    },
  ],
  troubleshooting: [
    {
      issue: 'MCP server not showing',
      solution: 'Make sure you have added the configuration.',
    },
    {
      issue: 'Authentication fails',
      solution: 'Verify your credentials are correct.',
    },
  ],
  authHelp: {
    title: 'Authentication',
    description: 'Use your IQ Server credentials.',
    example: 'echo -n "admin:password" | base64',
  },
}));

// Import after mock
import { McpPage } from 'GuideRoot/mcp/McpPage';

describe('McpPage', () => {
  it('renders page with correct header', () => {
    render(<McpPage />);

    expect(screen.getByRole('heading', { name: 'Sonatype MCP' })).toBeInTheDocument();
    expect(screen.getByText(/Get real-time security scanning, version guidance/)).toBeInTheDocument();
  });

  it('renders all IDE tabs', () => {
    render(<McpPage />);

    // Check that tab names are in the document (they may appear multiple times due to responsive design)
    expect(screen.getAllByText('GitHub Copilot').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Claude Code').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Gemini').length).toBeGreaterThan(0);
  });

  it('displays authentication help section', () => {
    render(<McpPage />);

    expect(screen.getByText('Authentication')).toBeInTheDocument();
    expect(screen.getByText('Use your IQ Server credentials.')).toBeInTheDocument();
    expect(screen.getByText(/echo -n "admin:password" \| base64/)).toBeInTheDocument();
  });

  it('tab switching works correctly', async () => {
    const user = userEvent.setup();
    render(<McpPage />);

    // Click on Claude Code tab (use getAllByText since there are multiple elements)
    const claudeTabs = screen.getAllByText('Claude Code');
    await user.click(claudeTabs[0]);

    // The Claude Code tab config should now be visible (it uses command type)
    expect(screen.getByText(/claude mcp add sonatype-mcp/)).toBeInTheDocument();
  });

  it('renders available MCP tools section', () => {
    render(<McpPage />);

    expect(screen.getByRole('heading', { name: 'Available MCP Tools' })).toBeInTheDocument();
    expect(screen.getByText('getComponentVersion')).toBeInTheDocument();
    expect(screen.getByText('getLatestComponentVersion')).toBeInTheDocument();
  });

  it('tools expand/collapse correctly', async () => {
    const user = userEvent.setup();
    render(<McpPage />);

    expect(screen.queryByText(/Security analysis/)).not.toBeInTheDocument();

    await user.click(screen.getByText('getComponentVersion'));

    expect(screen.getByText(/Security analysis/)).toBeInTheDocument();
    expect(screen.getByText(/Is express 4\.17\.1 vulnerable/)).toBeInTheDocument();

    await user.click(screen.getByText('getComponentVersion'));

    expect(screen.queryByText(/Security analysis/)).not.toBeInTheDocument();
  });

  it('tool expands on keyboard Enter', async () => {
    const user = userEvent.setup();
    render(<McpPage />);

    const toolCard = screen.getByText('getComponentVersion').closest('[role="button"]')!;
    toolCard.focus();
    await user.keyboard('{Enter}');

    expect(screen.getByText(/Security analysis/)).toBeInTheDocument();
  });

  it('renders troubleshooting section', () => {
    render(<McpPage />);

    expect(screen.getByRole('heading', { name: 'Troubleshooting' })).toBeInTheDocument();
    expect(screen.getByText('MCP server not showing')).toBeInTheDocument();
    expect(screen.getByText('Authentication fails')).toBeInTheDocument();
  });

  it('troubleshooting accordion works', async () => {
    const user = userEvent.setup();
    render(<McpPage />);

    // Solution should not be visible initially
    expect(screen.queryByText('Make sure you have added the configuration.')).not.toBeInTheDocument();

    // Click on troubleshooting item to expand
    await user.click(screen.getByText('MCP server not showing'));

    await waitFor(() => {
      expect(screen.getByText('Make sure you have added the configuration.')).toBeInTheDocument();
    });
  });

  it('displays connection guides section', () => {
    render(<McpPage />);

    expect(screen.getByText('Connection Guides')).toBeInTheDocument();
    // Check that the steps are visible - the step titles come from mcp.json
    expect(screen.getByText('Supported IDEs and Configuration')).toBeInTheDocument();
    expect(screen.getByText('Start Using')).toBeInTheDocument();
    expect(screen.getByText('Configure AI Assistant Rules (Recommended)')).toBeInTheDocument();
  });

  it('shows success message', () => {
    render(<McpPage />);

    expect(screen.getByText(/The MCP tools will now be available in your AI agent/)).toBeInTheDocument();
  });
});
