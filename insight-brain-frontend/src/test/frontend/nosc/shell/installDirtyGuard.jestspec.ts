/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { RejectType, Transition, TransitionService } from '@uirouter/core';
import { Store } from 'redux';
import { installDirtyGuard } from 'MainRoot/nosc/shell/installDirtyGuard';
import { selectIsCurrentRouteDirty } from 'MainRoot/reduxUiRouter/routerSelectors';

jest.mock('MainRoot/reduxUiRouter/routerSelectors', () => ({
  selectIsCurrentRouteDirty: jest.fn(),
}));

const mockedIsDirty = selectIsCurrentRouteDirty as jest.MockedFunction<typeof selectIsCurrentRouteDirty>;

interface Harness {
  cleanup: () => void;
  invokeOnStart: () => unknown;
  invokeOnError: (transition: Transition) => void;
  invokeBeforeUnload: (event: BeforeUnloadEvent) => void;
  unregisterOnStart: jest.Mock;
  unregisterOnError: jest.Mock;
  dispatch: jest.Mock;
  removeEventListenerSpy: jest.SpyInstance;
  beforeUnloadHandler: EventListener;
}

function setup(dirty: boolean, dispatch: jest.Mock = jest.fn()): Harness {
  mockedIsDirty.mockReturnValue(dirty);

  const unregisterOnStart = jest.fn();
  const unregisterOnError = jest.fn();
  let onStartCb: () => unknown = () => undefined;
  let onErrorCb: (transition: Transition) => void = () => undefined;

  const transitionService = ({
    onStart: jest.fn((_matchCriteria: unknown, cb: () => unknown) => {
      onStartCb = cb;
      return unregisterOnStart;
    }),
    onError: jest.fn((_matchCriteria: unknown, cb: (transition: Transition) => void) => {
      onErrorCb = cb;
      return unregisterOnError;
    }),
  } as unknown) as TransitionService;

  const store = ({
    getState: jest.fn(() => ({})),
    dispatch,
  } as unknown) as Store;

  const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
  const removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');

  const cleanup = installDirtyGuard(transitionService, store);

  const beforeUnloadCall = addEventListenerSpy.mock.calls.find(([evt]) => evt === 'beforeunload');
  const beforeUnloadHandler = beforeUnloadCall?.[1] as EventListener;

  return {
    cleanup,
    invokeOnStart: () => onStartCb(),
    invokeOnError: (transition) => onErrorCb(transition),
    invokeBeforeUnload: (event) => beforeUnloadHandler(event),
    unregisterOnStart,
    unregisterOnError,
    dispatch,
    removeEventListenerSpy,
    beforeUnloadHandler,
  };
}

describe('installDirtyGuard', () => {
  let activeCleanup: (() => void) | undefined;

  afterEach(() => {
    activeCleanup?.();
    activeCleanup = undefined;
    jest.restoreAllMocks();
    mockedIsDirty.mockReset();
  });

  describe('onStart hook', () => {
    it('returns undefined when the route is clean', () => {
      const h = setup(false);
      activeCleanup = h.cleanup;

      expect(h.invokeOnStart()).toBeUndefined();
      expect(h.dispatch).not.toHaveBeenCalled();
    });

    it('returns undefined re-entrantly when a modal is already in flight', () => {
      // Never-settling promise keeps `isProcessingStateChange` true for the second call.
      const dispatch = jest.fn(() => new Promise<void>(() => undefined));
      const h = setup(true, dispatch);
      activeCleanup = h.cleanup;

      const first = h.invokeOnStart();
      expect(first).toBeInstanceOf(Promise);
      expect(dispatch).toHaveBeenCalledTimes(1);

      const second = h.invokeOnStart();
      expect(second).toBeUndefined();
      expect(dispatch).toHaveBeenCalledTimes(1);
    });

    it('resolves to true when the modal open() Promise resolves (Continue)', async () => {
      const dispatch = jest.fn(() => Promise.resolve());
      const h = setup(true, dispatch);
      activeCleanup = h.cleanup;

      await expect(h.invokeOnStart() as Promise<boolean>).resolves.toBe(true);
      expect(dispatch).toHaveBeenCalledTimes(1);
      expect(dispatch).toHaveBeenCalledWith(expect.any(Function));
    });

    it('resolves to false when the modal open() Promise rejects (Cancel)', async () => {
      const dispatch = jest.fn(() => Promise.reject(new Error('cancelled')));
      const h = setup(true, dispatch);
      activeCleanup = h.cleanup;

      await expect(h.invokeOnStart() as Promise<boolean>).resolves.toBe(false);
    });

    it('releases the re-entrance guard after the prompt settles so the next dirty transition prompts again', async () => {
      const dispatch = jest
        .fn()
        .mockReturnValueOnce(Promise.resolve())
        .mockReturnValueOnce(Promise.resolve());
      const h = setup(true, dispatch);
      activeCleanup = h.cleanup;

      await h.invokeOnStart();
      await h.invokeOnStart();

      expect(dispatch).toHaveBeenCalledTimes(2);
    });

    it('returns undefined and resets the re-entrance guard when dispatch throws synchronously', () => {
      // Defensive branch at installDirtyGuard.ts:72-76: a misconfigured store
      // (or a future refactor that turns `open` into a plain action) can make
      // dispatch throw synchronously. The catch should clear
      // isProcessingStateChange so the guard doesn't wedge in "modal in flight"
      // mode forever.
      const dispatch = jest
        .fn()
        .mockImplementationOnce(() => {
          throw new Error('misconfigured store');
        })
        .mockReturnValueOnce(Promise.resolve());
      const h = setup(true, dispatch);
      activeCleanup = h.cleanup;

      expect(h.invokeOnStart()).toBeUndefined();
      expect(dispatch).toHaveBeenCalledTimes(1);

      // If the guard had wedged, the second invocation would short-circuit
      // without calling dispatch. Reaching call #2 proves the flag reset.
      h.invokeOnStart();
      expect(dispatch).toHaveBeenCalledTimes(2);
    });
  });

  describe('onError hook', () => {
    it.each([RejectType.SUPERSEDED, RejectType.ABORTED, RejectType.IGNORED])(
      'swallows benign RejectType %s without throwing',
      (type) => {
        const h = setup(false);
        activeCleanup = h.cleanup;

        const transition = ({ error: () => ({ type }) } as unknown) as Transition;
        expect(() => h.invokeOnError(transition)).not.toThrow();
      }
    );

    it('does not throw when the transition has no error', () => {
      const h = setup(false);
      activeCleanup = h.cleanup;

      const transition = ({ error: () => undefined } as unknown) as Transition;
      expect(() => h.invokeOnError(transition)).not.toThrow();
    });

    it('leaves non-benign errors alone (no shell-error dispatch — CLM-42220)', () => {
      const h = setup(false);
      activeCleanup = h.cleanup;

      // ERROR is the generic UI-Router failure type, distinct from the three
      // benign ones the handler intentionally swallows.
      const transition = ({ error: () => ({ type: RejectType.ERROR }) } as unknown) as Transition;
      expect(() => h.invokeOnError(transition)).not.toThrow();
      expect(h.dispatch).not.toHaveBeenCalled();
    });
  });

  describe('beforeUnloadHandler', () => {
    it('calls preventDefault and sets returnValue to empty string when dirty', () => {
      const h = setup(true);
      activeCleanup = h.cleanup;

      const event = ({
        preventDefault: jest.fn(),
        returnValue: 'unset',
      } as unknown) as BeforeUnloadEvent;
      h.invokeBeforeUnload(event);

      expect(event.preventDefault).toHaveBeenCalledTimes(1);
      expect((event as unknown as { returnValue: string }).returnValue).toBe('');
    });

    it('leaves the event untouched when clean', () => {
      const h = setup(false);
      activeCleanup = h.cleanup;

      const event = ({
        preventDefault: jest.fn(),
        returnValue: 'unset',
      } as unknown) as BeforeUnloadEvent;
      h.invokeBeforeUnload(event);

      expect(event.preventDefault).not.toHaveBeenCalled();
      expect((event as unknown as { returnValue: string }).returnValue).toBe('unset');
    });

    it('still prompts on hard navigation while the unsaved-changes modal is in flight', () => {
      // Documents the intentional decision at installDirtyGuard.ts:95–98 —
      // beforeunload does NOT consult isProcessingStateChange, so Cmd-W with
      // the modal open still surfaces the browser prompt.
      const dispatch = jest.fn(() => new Promise<void>(() => undefined));
      const h = setup(true, dispatch);
      activeCleanup = h.cleanup;

      h.invokeOnStart();
      const event = ({
        preventDefault: jest.fn(),
        returnValue: 'unset',
      } as unknown) as BeforeUnloadEvent;
      h.invokeBeforeUnload(event);

      expect(event.preventDefault).toHaveBeenCalledTimes(1);
    });
  });

  describe('cleanup fn', () => {
    it('unregisters both transition hooks and removes the beforeunload listener', () => {
      const h = setup(false);
      h.cleanup();
      activeCleanup = undefined;

      expect(h.unregisterOnStart).toHaveBeenCalledTimes(1);
      expect(h.unregisterOnError).toHaveBeenCalledTimes(1);
      expect(h.removeEventListenerSpy).toHaveBeenCalledWith('beforeunload', h.beforeUnloadHandler);
    });
  });
});
