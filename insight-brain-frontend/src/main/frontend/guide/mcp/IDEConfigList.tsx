/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Box, Table } from '@radix-ui/themes';
import { ExternalLink } from 'lucide-react';
import { BodyText } from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import { CopyToClipboardButton } from './CopyToClipboardButton';
import type { IDEConfig } from './types';

export function CodeSnippet({ code }: { code: string }) {
  return (
    <Box style={{ position: 'relative' }}>
      <Box
        p={tokens.space.item}
        style={{
          backgroundColor: 'var(--gray-2)',
          borderRadius: 'var(--radius-3)',
          fontFamily: 'var(--code-font-family)',
          fontSize: tokens.code.fontSize,
          paddingRight: '34px',
        }}
      >
        <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{code}</pre>
      </Box>
      <CopyToClipboardButton text={code} />
    </Box>
  );
}

export function IDEConfigList({ configs }: { configs: IDEConfig[] }) {
  return (
    <Box mt={tokens.space.section}>
      <ul style={{ margin: 0, paddingLeft: '1.5em', listStyleType: 'disc' }}>
        {configs.map((ideConfig, index) => (
          <li key={index} style={{ marginBottom: 'var(--space-6)' }}>
            <Box style={{ display: 'block' }}>
              <BodyText size="sm" weight="bold" mb={tokens.space.item}>
                {ideConfig.ideName}
              </BodyText>
            </Box>

            <Box style={{ display: 'block' }}>
              <BodyText size="sm" tone="subtle" mb={tokens.space.item}>
                {ideConfig.docsUrl ? (
                  <>
                    {ideConfig.description} For more detailed setup instructions, see the{' '}
                    <a
                      href={ideConfig.docsUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25em' }}
                    >
                      {ideConfig.docsUrlText || 'documentation'}
                      <ExternalLink size={12} />
                    </a>
                    .
                  </>
                ) : (
                  ideConfig.description
                )}
              </BodyText>
            </Box>

            {ideConfig.instructions && ideConfig.instructions.length > 0 && (
              <Box style={{ display: 'block' }} mb={tokens.space.item}>
                {ideConfig.instructions.map((instruction, i) => (
                  <BodyText
                    key={i}
                    size="sm"
                    tone="subtle"
                    mb={tokens.space.tight}
                    style={{ display: 'block' }}
                  >
                    {i + 1}. {instruction}
                  </BodyText>
                ))}
              </Box>
            )}

            {ideConfig.tableRows && (
              <Table.Root
                size="1"
                style={{
                  marginTop: 'var(--space-5)',
                  marginBottom: ideConfig.note ? tokens.space.inline : 0,
                  overflow: 'hidden',
                  borderRadius: 'var(--radius-3)',
                  border: '1px solid var(--gray-6)',
                }}
              >
                <Table.Header>
                  <Table.Row style={{ backgroundColor: 'var(--violet-5)' }}>
                    <Table.ColumnHeaderCell style={{ color: 'var(--gray-12)', fontWeight: 'bold' }}>
                      Field
                    </Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell style={{ color: 'var(--gray-12)', fontWeight: 'bold' }}>
                      Value
                    </Table.ColumnHeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {ideConfig.tableRows.map((row, rowIndex) => (
                    <Table.Row key={rowIndex}>
                      <Table.Cell>
                        <strong>{row.field}</strong>
                      </Table.Cell>
                      <Table.Cell
                        style={{
                          fontFamily: 'var(--code-font-family)',
                          fontSize: tokens.code.fontSize,
                          whiteSpace: 'pre-wrap',
                          wordBreak: 'break-word',
                          color: 'var(--gray-12)',
                        }}
                      >
                        {row.value}
                      </Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table.Root>
            )}

            {ideConfig.config && (
              <Box mb={ideConfig.note ? tokens.space.inline : '0'}>
                <CodeSnippet code={ideConfig.config} />
              </Box>
            )}

            {ideConfig.note && (
              <BodyText size="sm" tone="subtle" mt={tokens.space.inline}>
                {ideConfig.note}
              </BodyText>
            )}
          </li>
        ))}
      </ul>
    </Box>
  );
}
