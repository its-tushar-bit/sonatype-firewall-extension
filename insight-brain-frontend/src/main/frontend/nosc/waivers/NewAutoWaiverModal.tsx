/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { Button, Checkbox, Dialog, Flex, Select, Text, TextField } from '@radix-ui/themes';
import { useNoscToast } from 'MainRoot/nosc/toast/useNoscToast';
import {
  DEFAULT_AUTO_WAIVER_THREAT_LEVEL,
  createAutoPolicyWaiver,
  updateAutoPolicyWaiver,
  type AutoWaiverConfigPayload,
  type AutoWaiverOwnerType,
} from 'MainRoot/nosc/waivers/autoWaiversApi';

export interface NewAutoWaiverModalValues {
  readonly autoPolicyWaiverId?: string;
  readonly threatLevel: number;
  readonly reachability: boolean;
  readonly pathForward: boolean;
  readonly scopesOperatorAny: boolean;
}

export interface NewAutoWaiverModalProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly ownerType: AutoWaiverOwnerType;
  readonly ownerId: string;
  readonly canManage: boolean;
  readonly initial?: NewAutoWaiverModalValues | null;
  readonly onSaved: () => void;
}

const DEFAULT_VALUES: NewAutoWaiverModalValues = {
  threatLevel: DEFAULT_AUTO_WAIVER_THREAT_LEVEL,
  reachability: false,
  pathForward: false,
  scopesOperatorAny: true,
};

function toPayload(values: NewAutoWaiverModalValues): AutoWaiverConfigPayload {
  return {
    threatLevel: values.threatLevel,
    reachability: values.reachability,
    pathForward: values.pathForward,
    scopesOperatorAny: values.scopesOperatorAny,
    autoPolicyWaiverId: values.autoPolicyWaiverId,
  };
}

export default function NewAutoWaiverModal({
  open,
  onOpenChange,
  ownerType,
  ownerId,
  canManage,
  initial = null,
  onSaved,
}: NewAutoWaiverModalProps): JSX.Element {
  const toast = useNoscToast();
  const isEdit = Boolean(initial?.autoPolicyWaiverId);
  const [form, setForm] = useState<NewAutoWaiverModalValues>(DEFAULT_VALUES);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setForm(initial ?? DEFAULT_VALUES);
    setError(null);
    setSubmitting(false);
  }, [open, initial]);

  const submit = async (): Promise<void> => {
    if (!canManage) return;
    if (!form.reachability && !form.pathForward) {
      setError('Select at least one condition: Not reachable or No path forward.');
      return;
    }
    const threat = Number(form.threatLevel);
    if (!Number.isInteger(threat) || threat < 0 || threat > 10) {
      setError('Threat level must be an integer from 0 to 10.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const body = toPayload({ ...form, threatLevel: threat });
      if (isEdit && form.autoPolicyWaiverId) {
        await updateAutoPolicyWaiver({
          ownerType,
          ownerId,
          autoPolicyWaiverId: form.autoPolicyWaiverId,
          body,
        });
        toast.success('Auto-waiver updated');
      } else {
        await createAutoPolicyWaiver({ ownerType, ownerId, body });
        toast.success('Auto-waiver created');
      }
      onOpenChange(false);
      onSaved();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to save auto-waiver';
      setError(message);
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Content maxWidth="480px" data-testid="new-auto-waiver-modal">
        <Dialog.Title>{isEdit ? 'Edit auto-waiver' : 'New auto-waiver'}</Dialog.Title>
        <Dialog.Description size="2" mb="3">
          Automatically waive policy violations when the threat level is at or below the threshold
          and the selected conditions are met.
        </Dialog.Description>

        <Flex direction="column" gap="3">
          <Flex direction="column" gap="1">
            <Text as="label" size="2" weight="medium" htmlFor="new-auto-waiver-threat">
              Max threat level
            </Text>
            <TextField.Root
              id="new-auto-waiver-threat"
              type="number"
              min={0}
              max={10}
              value={String(form.threatLevel)}
              disabled={!canManage || submitting}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, threatLevel: Number(event.target.value) }))
              }
              data-testid="new-auto-waiver-threat"
            />
          </Flex>

          <Flex direction="column" gap="1">
            <Text as="label" size="2" weight="medium" htmlFor="new-auto-waiver-operator">
              Conditions operator
            </Text>
            <Select.Root
              value={form.scopesOperatorAny ? 'any' : 'all'}
              disabled={!canManage || submitting || !(form.reachability && form.pathForward)}
              onValueChange={(value) =>
                setForm((prev) => ({ ...prev, scopesOperatorAny: value === 'any' }))
              }
            >
              <Select.Trigger id="new-auto-waiver-operator" data-testid="new-auto-waiver-operator" />
              <Select.Content>
                <Select.Item value="any">Any of the following</Select.Item>
                <Select.Item value="all">All of the following</Select.Item>
              </Select.Content>
            </Select.Root>
          </Flex>

          <Text as="label" size="2">
            <Flex gap="2" align="center">
              <Checkbox
                checked={form.pathForward}
                disabled={!canManage || submitting}
                onCheckedChange={(checked) =>
                  setForm((prev) => ({ ...prev, pathForward: checked === true }))
                }
                data-testid="new-auto-waiver-path-forward"
              />
              No path forward (no newer non-violating version)
            </Flex>
          </Text>

          <Text as="label" size="2">
            <Flex gap="2" align="center">
              <Checkbox
                checked={form.reachability}
                disabled={!canManage || submitting}
                onCheckedChange={(checked) =>
                  setForm((prev) => ({ ...prev, reachability: checked === true }))
                }
                data-testid="new-auto-waiver-reachability"
              />
              Vulnerability is not reachable
            </Flex>
          </Text>
        </Flex>

        {error && (
          <Text size="2" color="red" mt="3" data-testid="new-auto-waiver-error">
            {error}
          </Text>
        )}

        <Flex gap="3" mt="4" justify="end">
          <Dialog.Close>
            <Button variant="soft" color="gray" disabled={submitting}>
              Cancel
            </Button>
          </Dialog.Close>
          {canManage && (
            <Button
              disabled={submitting}
              onClick={() => void submit()}
              data-testid="new-auto-waiver-submit"
            >
              {isEdit ? 'Update' : 'Create'}
            </Button>
          )}
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}
