/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Callout, Flex, IconButton, Theme } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
import { selectToastSlice } from 'MainRoot/toastContainer/toastSelectors';

const AUTO_DISMISS_MS = 6000;

type ToastType = 'success' | 'error' | 'info' | 'warning';

function calloutColor(type: ToastType): 'green' | 'red' | 'blue' | 'amber' {
  switch (type) {
    case 'success':
      return 'green';
    case 'error':
      return 'red';
    case 'warning':
      return 'amber';
    default:
      return 'blue';
  }
}

/**
 * Renders Redux toasts for the Nexus One shell (Radix Callouts).
 * Shares {@code toastSlice} with Classic so mutation code can dispatch once.
 *
 * Portaled to {@code document.body} with its own Theme so shell stacking /
 * washed-out soft Callouts cannot hide success/error feedback.
 */
export default function NoscToastHost(): JSX.Element | null {
  const dispatch = useDispatch();
  const { effectiveTheme } = useNoscTheme();
  const scheduledDismiss = useRef(new Map<number, number>());
  const { toasts } = useSelector(selectToastSlice) as {
    readonly toasts: ReadonlyArray<{
      readonly id: number;
      readonly type: ToastType;
      readonly message: string;
    }>;
  };

  useEffect(() => {
    toasts.forEach((toast) => {
      if (scheduledDismiss.current.has(toast.id)) return;
      const timerId = window.setTimeout(() => {
        scheduledDismiss.current.delete(toast.id);
        dispatch(toastActions.removeToast(toast.id));
      }, AUTO_DISMISS_MS);
      scheduledDismiss.current.set(toast.id, timerId);
    });
  }, [dispatch, toasts]);

  useEffect(() => () => {
    scheduledDismiss.current.forEach((timerId) => window.clearTimeout(timerId));
    scheduledDismiss.current.clear();
  }, []);

  if (toasts.length === 0 || typeof document === 'undefined') {
    return null;
  }

  return createPortal(
    <Theme
      appearance={effectiveTheme}
      accentColor={BRAND_ACCENT}
      grayColor="slate"
      radius="medium"
      scaling="100%"
      hasBackground={false}
    >
      <div className="nosc-toast-host" data-testid="nosc-toast-host" aria-live="polite">
        <Flex direction="column" gap="2">
          {toasts.map((toast) => (
            <Callout.Root
              key={toast.id}
              color={calloutColor(toast.type)}
              // soft ≈ 10% fill — reads as "no toast" on light chrome.
              variant="surface"
              highContrast
              role={toast.type === 'error' ? 'alert' : 'status'}
              data-testid={`nosc-toast-${toast.type}`}
              className="nosc-toast-item"
            >
              <Callout.Icon>
                {toast.type === 'error' || toast.type === 'warning' ? (
                  <ActionIcons.AlertCircle size={16} aria-hidden />
                ) : (
                  <ActionIcons.Save size={16} aria-hidden />
                )}
              </Callout.Icon>
              <Callout.Text weight="medium">{toast.message}</Callout.Text>
              <IconButton
                size="1"
                variant="ghost"
                color="gray"
                highContrast
                aria-label="Dismiss notification"
                onClick={() => dispatch(toastActions.removeToast(toast.id))}
                data-testid={`nosc-toast-dismiss-${toast.id}`}
                className="nosc-toast-dismiss"
              >
                <ActionIcons.Cancel size={14} aria-hidden />
              </IconButton>
            </Callout.Root>
          ))}
        </Flex>
      </div>
    </Theme>,
    document.body,
  );
}
