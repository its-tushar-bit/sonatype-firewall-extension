/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export interface TableRow {
  field: string;
  value: string;
}

export interface IDEConfig {
  ideName: string;
  description: string;
  config?: string;
  instructions?: string[];
  tableRows?: TableRow[];
  note?: string;
  docsUrl?: string;
  docsUrlText?: string;
}

export interface MCPTab {
  id: string;
  name: string;
  type: 'command' | 'json';
  command?: string;
  config?: string;
  needsPrereq: boolean;
  configTitle?: string;
  startUsingTitle?: string;
  rulesTitle?: string;
  rulesFile: string;
  rulesContent: string;
  note?: string;
  ideConfigs?: IDEConfig[];
}

export interface MCPTool {
  name: string;
  description: string;
  useCases: string[];
  examplePrompts: string[];
}

export interface TroubleshootingItem {
  issue: string;
  solution: string;
}

export interface AuthHelp {
  title: string;
  description: string;
  example: string;
}

export interface OptionalHeader {
  name: string;
  description: string;
  example: string;
}

export interface MCPData {
  tabs: MCPTab[];
  tools: MCPTool[];
  troubleshooting: TroubleshootingItem[];
  authHelp: AuthHelp;
  optionalHeaders?: OptionalHeader[];
}
