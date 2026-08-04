/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Flex, Select, Text, TextArea, TextField } from '@radix-ui/themes';
import type {
  PolicyWaiverReason,
  WaiverMatcherStrategy,
  WaiverScopeTarget,
} from 'MainRoot/nosc/waivers/waiversMutationApi';

export type WaiverExpiryMode = 'never' | 'date' | 'remediation';

export interface WaiverMutationFormState {
  readonly scopeKey: string;
  readonly matcherStrategy: WaiverMatcherStrategy;
  readonly expiryMode: WaiverExpiryMode;
  readonly expiryDate: string;
  readonly comment: string;
  readonly waiverReasonId: string;
  readonly noteToReviewer: string;
}

export const DEFAULT_WAIVER_MUTATION_FORM: WaiverMutationFormState = {
  scopeKey: '',
  matcherStrategy: 'EXACT_COMPONENT',
  expiryMode: 'never',
  expiryDate: '',
  comment: '',
  waiverReasonId: '',
  noteToReviewer: '',
};

export function scopeKeyFor(target: WaiverScopeTarget): string {
  return `${target.ownerType}:${target.ownerId}`;
}

export function parseScopeKey(key: string): { ownerType: string; ownerId: string } | null {
  const idx = key.indexOf(':');
  if (idx <= 0) return null;
  return { ownerType: key.slice(0, idx), ownerId: key.slice(idx + 1) };
}

/** Classic-style scope label: "Application: My App", "Repository Container: …". */
export function formatScopeOwnerTypeLabel(ownerType: string): string {
  return ownerType
    .split(/[_-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(' ');
}

export interface WaiverMutationFormFieldsProps {
  readonly form: WaiverMutationFormState;
  readonly onChange: (next: WaiverMutationFormState) => void;
  readonly scopes: ReadonlyArray<WaiverScopeTarget>;
  readonly reasons: ReadonlyArray<PolicyWaiverReason>;
  readonly showNoteToReviewer?: boolean;
  readonly disabled?: boolean;
  readonly testIdPrefix: string;
}

export default function WaiverMutationFormFields({
  form,
  onChange,
  scopes,
  reasons,
  showNoteToReviewer = false,
  disabled = false,
  testIdPrefix,
}: WaiverMutationFormFieldsProps): JSX.Element {
  const set = <K extends keyof WaiverMutationFormState>(key: K, value: WaiverMutationFormState[K]) => {
    onChange({ ...form, [key]: value });
  };

  return (
    <Flex direction="column" gap="3">
      <Flex direction="column" gap="1">
        <Text as="label" size="2" weight="medium" htmlFor={`${testIdPrefix}-scope`}>
          Scope
        </Text>
        <Select.Root
          value={form.scopeKey || undefined}
          onValueChange={(value) => set('scopeKey', value)}
          disabled={disabled || scopes.length === 0}
        >
          <Select.Trigger
            id={`${testIdPrefix}-scope`}
            placeholder="Select scope"
            data-testid={`${testIdPrefix}-scope`}
          />
          <Select.Content>
            {scopes.map((scope) => (
              <Select.Item key={scopeKeyFor(scope)} value={scopeKeyFor(scope)}>
                {`${formatScopeOwnerTypeLabel(scope.ownerType)}: ${scope.ownerName}`}
              </Select.Item>
            ))}
          </Select.Content>
        </Select.Root>
      </Flex>

      <Flex direction="column" gap="1">
        <Text as="label" size="2" weight="medium" htmlFor={`${testIdPrefix}-matcher`}>
          Matcher
        </Text>
        <Select.Root
          value={form.matcherStrategy}
          onValueChange={(value) => {
            const matcher = value as WaiverMatcherStrategy;
            onChange({
              ...form,
              matcherStrategy: matcher,
              expiryMode:
                matcher !== 'EXACT_COMPONENT' && form.expiryMode === 'remediation'
                  ? 'never'
                  : form.expiryMode,
            });
          }}
          disabled={disabled}
        >
          <Select.Trigger id={`${testIdPrefix}-matcher`} data-testid={`${testIdPrefix}-matcher`} />
          <Select.Content>
            <Select.Item value="EXACT_COMPONENT">Exact component</Select.Item>
            <Select.Item value="ALL_VERSIONS">All versions</Select.Item>
            <Select.Item value="ALL_COMPONENTS">All components</Select.Item>
          </Select.Content>
        </Select.Root>
      </Flex>

      <Flex direction="column" gap="1">
        <Text as="label" size="2" weight="medium" htmlFor={`${testIdPrefix}-expiry-mode`}>
          Expiration
        </Text>
        <Select.Root
          value={form.expiryMode}
          onValueChange={(value) => set('expiryMode', value as WaiverExpiryMode)}
          disabled={disabled}
        >
          <Select.Trigger
            id={`${testIdPrefix}-expiry-mode`}
            data-testid={`${testIdPrefix}-expiry-mode`}
          />
          <Select.Content>
            <Select.Item value="never">Never</Select.Item>
            <Select.Item value="date">On date</Select.Item>
            {form.matcherStrategy === 'EXACT_COMPONENT' && (
              <Select.Item value="remediation">When remediation is available</Select.Item>
            )}
          </Select.Content>
        </Select.Root>
        {form.expiryMode === 'date' && (
          <TextField.Root
            type="date"
            value={form.expiryDate}
            onChange={(event) => set('expiryDate', event.target.value)}
            disabled={disabled}
            data-testid={`${testIdPrefix}-expiry-date`}
            mt="1"
          />
        )}
      </Flex>

      {reasons.length > 0 && (
        <Flex direction="column" gap="1">
          <Text as="label" size="2" weight="medium" htmlFor={`${testIdPrefix}-reason`}>
            Reason
          </Text>
          <Select.Root
            value={form.waiverReasonId || '__none__'}
            onValueChange={(value) => set('waiverReasonId', value === '__none__' ? '' : value)}
            disabled={disabled}
          >
            <Select.Trigger id={`${testIdPrefix}-reason`} data-testid={`${testIdPrefix}-reason`} />
            <Select.Content>
              <Select.Item value="__none__">Custom comment only</Select.Item>
              {reasons.map((reason) => (
                <Select.Item key={reason.id} value={reason.id}>
                  {reason.reasonText}
                </Select.Item>
              ))}
            </Select.Content>
          </Select.Root>
        </Flex>
      )}

      <Flex direction="column" gap="1">
        <Text as="label" size="2" weight="medium" htmlFor={`${testIdPrefix}-comment`}>
          Comment
        </Text>
        <TextArea
          id={`${testIdPrefix}-comment`}
          value={form.comment}
          onChange={(event) => set('comment', event.target.value)}
          disabled={disabled}
          placeholder="Why this waiver is justified"
          data-testid={`${testIdPrefix}-comment`}
        />
      </Flex>

      {showNoteToReviewer && (
        <Flex direction="column" gap="1">
          <Text as="label" size="2" weight="medium" htmlFor={`${testIdPrefix}-note`}>
            Note to reviewer
          </Text>
          <TextArea
            id={`${testIdPrefix}-note`}
            value={form.noteToReviewer}
            onChange={(event) => set('noteToReviewer', event.target.value)}
            disabled={disabled}
            placeholder="Optional context for the reviewer"
            data-testid={`${testIdPrefix}-note`}
          />
        </Flex>
      )}
    </Flex>
  );
}
