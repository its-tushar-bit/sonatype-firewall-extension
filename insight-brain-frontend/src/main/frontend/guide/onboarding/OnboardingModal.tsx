/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useState } from 'react';
import { Dialog, Flex, Button, Box, Text } from '@radix-ui/themes';
import { tokens } from '@guide/ui-core/utils';
import { useOnboarding } from './OnboardingProvider';
import { WelcomeStep } from './steps/WelcomeStep';
import { PolicyStep } from './steps/PolicyStep';
import { ScopeStep } from './steps/ScopeStep';
import { ChangeScopeStep } from './steps/ChangeScopeStep';
import styles from './OnboardingModal.module.css';

const STEPS = [
  { title: 'Welcome to AI Developer', Component: WelcomeStep },
  { title: 'Policy compliance in AI Developer', Component: PolicyStep },
  { title: 'Choose your scope', Component: ScopeStep },
  { title: 'Change scope anytime', Component: ChangeScopeStep },
] as const;

const LAST_STEP_INDEX = STEPS.length - 1;

export function OnboardingModal() {
  const { isOpen, dismiss } = useOnboarding();
  const [stepIndex, setStepIndex] = useState(0);

  // Every open (first-run or replay from the picker's "Need help?") starts at Welcome —
  // otherwise a replay after a prior "Get started" would resume on the last step.
  useEffect(() => {
    if (isOpen) setStepIndex(0);
  }, [isOpen]);

  const { title, Component } = STEPS[stepIndex];
  const isFirst = stepIndex === 0;
  const isLast = stepIndex === LAST_STEP_INDEX;

  const handleContinue = () => {
    if (isLast) {
      dismiss();
      return;
    }
    setStepIndex((i) => i + 1);
  };

  const handleBack = () => {
    if (isFirst) return;
    setStepIndex((i) => i - 1);
  };

  const handleOpenChange = (open: boolean) => {
    // Radix fires this for backdrop click, Escape, and programmatic close.
    // All three paths mean "user is done with the tour" — persist and close.
    if (!open) dismiss();
  };

  return (
    <Dialog.Root open={isOpen} onOpenChange={handleOpenChange}>
      <Dialog.Content maxWidth="640px" aria-describedby={undefined}>
        <Dialog.Title size={tokens.sizes.itemTitle} mb={tokens.space.item}>
          {title}
        </Dialog.Title>

        <StepDots current={stepIndex} total={STEPS.length} />

        <Box mt={tokens.space.section} mb={tokens.space.section}>
          <Component />
        </Box>

        <Flex justify="between" align="center" mt={tokens.space.section}>
          <Button variant="ghost" color="gray" onClick={dismiss}>
            Skip tutorial
          </Button>
          <Flex gap={tokens.space.inline}>
            {!isFirst && (
              <Button variant="soft" color="gray" onClick={handleBack}>
                Back
              </Button>
            )}
            <Button onClick={handleContinue}>{isLast ? 'Get started' : 'Continue'}</Button>
          </Flex>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}

interface StepDotsProps {
  current: number;
  total: number;
}

function StepDots({ current, total }: StepDotsProps) {
  return (
    <Flex gap="2" align="center">
      <Flex gap="2" align="center" aria-hidden>
        {Array.from({ length: total }).map((_, i) => (
          <span
            key={i}
            className={styles.dot}
            data-active={i === current || undefined}
            data-complete={i < current || undefined}
          />
        ))}
      </Flex>
      <Text size="1" color="gray" ml="2" aria-live="polite">
        Step {current + 1} of {total}
      </Text>
    </Flex>
  );
}
