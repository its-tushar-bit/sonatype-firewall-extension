/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { RejectType, TransitionService, Transition } from '@uirouter/core';
import { Store } from 'redux';
import { selectIsCurrentRouteDirty } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions as unsavedChangesModalActions } from 'MainRoot/modals/unsavedChangesModal/unsavedChangesModalSlice';

// TODO(CLM-42220): Classic's main.js reimplements this logic inline
// (unloadListener + handleTransitionStart + handleTransitionError). Extract
// Classic's toast-clear and GETTING_STARTED_STATE departed-telemetry
// side-effects as separate transition listeners and have both bundles call
// this helper.

const IGNORED_REJECT_TYPES: readonly RejectType[] = [RejectType.SUPERSEDED, RejectType.ABORTED, RejectType.IGNORED];

/**
 * Wires the unsaved-changes guard into a bundle's UI-Router + window:
 *
 *  - `transitionService.onStart` — opens the shared `UnsavedChangesModal` when
 *    the current route's `data.isDirty` metadata evaluates to true, awaits the
 *    user's Continue/Cancel, and returns true/false so UI-Router either
 *    proceeds with or aborts the transition.
 *  - `transitionService.onError` — swallows the benign
 *    `SUPERSEDED / ABORTED / IGNORED` rejections that UI-Router raises for
 *    superseded and user-cancelled transitions, so a normal Cancel does not
 *    surface as an unhandled promise rejection.
 *  - `window.beforeunload` — calls `event.preventDefault()` and sets
 *    `event.returnValue = ''` (the modern spec pattern) so the browser shows
 *    its native "leave site" prompt on hard navigation (refresh, close tab,
 *    bundle switch via `window.location.assign`). Modern browsers ignore any
 *    string content — the prompt copy is browser-controlled.
 *
 * The Classic bundle wires the same behaviour inline in `main.js` (see the
 * TODO above for the consolidation ticket). This helper installs it in the
 * Nexus One bundle so admin forms embedded via `mountClassicComponent` (e.g.
 * Success Metrics configuration) preserve the unsaved-changes prompt when the
 * user navigates away.
 *
 * Returns a cleanup function used only in test teardown; production callers
 * should ignore the return value since the page unload tears everything down.
 */
export function installDirtyGuard(transitionService: TransitionService, store: Store): () => void {
  // Re-entrance guard: a second `onStart` firing while the modal is still open
  // (rapid nav, keyboard-repeat) short-circuits to `undefined` so the modal is
  // opened at most once per prompt. Returning `undefined` here rather than
  // `false` is a faithful port of Classic `main.js:handleTransitionStart`:
  // browser back/forward and programmatic `stateService.go` can proceed
  // unguarded while the modal is pending, discarding unsaved changes
  // (click-nav is safe — the overlay blocks it). Tracked in CLM-42220
  // alongside the Classic consolidation.
  let isProcessingStateChange = false;

  function isPageDirty(): boolean {
    return Boolean(selectIsCurrentRouteDirty(store.getState()));
  }

  const unregisterOnStart = transitionService.onStart({}, () => {
    if (isProcessingStateChange || !isPageDirty()) {
      return undefined;
    }
    isProcessingStateChange = true;
    // `store.dispatch(open())` returns the thunk's Promise when
    // redux-thunk is wired up; wrap in Promise.resolve so a
    // misconfigured store (or a future refactor that turns `open` into
    // a plain action) degrades to a no-op instead of throwing
    // synchronously and leaving the guard stuck in "modal in flight"
    // mode.
    let openPromise: Promise<void>;
    try {
      openPromise = Promise.resolve(store.dispatch(unsavedChangesModalActions.open()) as Promise<void>);
    } catch {
      isProcessingStateChange = false;
      return undefined;
    }
    return openPromise
      .then(
        () => true,
        () => false
      )
      .finally(() => {
        isProcessingStateChange = false;
      });
  });

  const unregisterOnError = transitionService.onError({}, (transition: Transition) => {
    const err = transition.error() as { type?: RejectType } | undefined;
    if (err && err.type != null && IGNORED_REJECT_TYPES.includes(err.type)) {
      return;
    }
    // Intentional scope-limit: non-benign transition errors fall through and
    // are not escalated to the shell's error banner. Classic's `main.js`
    // `handleTransitionError` dispatches these to `setRootError` / `setError`;
    // porting that path is tracked under CLM-42220 alongside the broader
    // dirty-guard consolidation.
  });

  // beforeunload deliberately does NOT consult `isProcessingStateChange`:
  // if the user has the unsaved-changes modal open and then hits Cmd-W or
  // closes the tab, the page is still dirty and the browser prompt is the
  // last line of defence.
  const beforeUnloadHandler = (event: BeforeUnloadEvent): void => {
    if (!isPageDirty()) {
      return;
    }
    event.preventDefault();
    event.returnValue = '';
  };
  window.addEventListener('beforeunload', beforeUnloadHandler);

  return () => {
    unregisterOnStart();
    unregisterOnError();
    window.removeEventListener('beforeunload', beforeUnloadHandler);
  };
}
