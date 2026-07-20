/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Badge, Button, Dialog, Flex, Text, Tooltip } from '@radix-ui/themes';
import { tokens } from '@guide/ui-core/utils';
import { usePolicyContext } from './PolicyContext';
import { PolicyContextModal } from './PolicyContextModal';

/**
 * Top-navigation trigger for the policy-context picker. Shows the current selection's last path
 * segment plus an org/app type badge (or "Root Organization" when nothing is selected) and opens
 * the {@link PolicyContextModal} in a fixed 520×480 dialog.
 */
export function PolicyContextPicker() {
  const { activeOwner, activePath, isPickerOpen, setIsPickerOpen } = usePolicyContext();

  const pathParts = activePath.map((segment) => segment.name);
  const displayLabel = pathParts.length > 0 ? pathParts[pathParts.length - 1] : 'Root Organization';
  const tooltipLabel = pathParts.length > 0 ? pathParts.join(' / ') : 'Root Organization';
  const typeLabel = activeOwner?.type ?? null;

  return (
    <Flex align="center" gap={tokens.space.item} style={{ minWidth: 0 }}>
      <Text size={tokens.sizes.body.sm} color="gray" style={{ flexShrink: 0 }}>
        Policy context
      </Text>
      <Dialog.Root open={isPickerOpen} onOpenChange={setIsPickerOpen}>
        <Tooltip content={tooltipLabel} side="bottom" align="center">
          <Dialog.Trigger>
            <Button
              variant="surface"
              color="gray"
              size={tokens.sizes.body.sm}
              aria-label="Policy context — open picker"
            >
              <Text
                size={tokens.sizes.body.sm}
                style={{ maxWidth: 160, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
              >
                {displayLabel}
              </Text>
              {typeLabel && (
                <Badge color="gray" variant="soft" size={tokens.sizes.body.xs}>
                  {typeLabel}
                </Badge>
              )}
            </Button>
          </Dialog.Trigger>
        </Tooltip>
        <Dialog.Content
          aria-describedby={undefined}
          // Delegate Escape to the modal so it clears an active search before closing (WCAG-friendly,
          // matches the prototype). The modal's document-level handler performs the clear-then-close.
          onEscapeKeyDown={(e) => e.preventDefault()}
          style={{ maxWidth: 520, height: 480, padding: 0, overflow: 'hidden' }}
        >
          <PolicyContextModal onClose={() => setIsPickerOpen(false)} />
        </Dialog.Content>
      </Dialog.Root>
    </Flex>
  );
}
