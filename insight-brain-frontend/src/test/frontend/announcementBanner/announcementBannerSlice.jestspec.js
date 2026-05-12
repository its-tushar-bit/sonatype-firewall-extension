/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  initialState,
  actions,
  DISMISS_STORAGE_KEY,
} from 'MainRoot/announcementBanner/announcementBannerSlice';

describe('announcementBannerSlice', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  describe('initial state', () => {
    it('starts with empty banner, no error, not loading, not suppressed', () => {
      expect(initialState).toEqual({
        loading: false,
        loadError: null,
        banner: null,
        dismissedWindowId: null,
        suppressedByLogout: false,
      });
    });
  });

  describe('loadAnnouncementBanner', () => {
    it('pending sets loading=true and clears prior error', () => {
      const state = { ...initialState, loadError: 'old error' };
      const next = reducer(state, { type: 'announcementBanner/load/pending' });
      expect(next.loading).toBe(true);
      expect(next.loadError).toBeNull();
    });

    it('fulfilled stores payload and stops loading', () => {
      const payload = { enabled: true, windowId: 'w1', message: 'hi' };
      const next = reducer({ ...initialState, loading: true }, {
        type: 'announcementBanner/load/fulfilled',
        payload,
      });
      expect(next.loading).toBe(false);
      expect(next.banner).toEqual(payload);
    });

    it('rejected records an error message and stops loading', () => {
      const next = reducer({ ...initialState, loading: true }, {
        type: 'announcementBanner/load/rejected',
        payload: { response: { status: 500, data: 'boom' } },
      });
      expect(next.loading).toBe(false);
      expect(next.loadError).toBeTruthy();
    });
  });

  describe('dismiss', () => {
    it('records the dismissed windowId in state and sessionStorage', () => {
      const next = reducer(initialState, actions.dismiss('w-2026-05-26'));
      expect(next.dismissedWindowId).toBe('w-2026-05-26');
      expect(sessionStorage.getItem(DISMISS_STORAGE_KEY)).toBe('w-2026-05-26');
    });

    it('does not persist when windowId is null or undefined', () => {
      // Guards against sessionStorage stringifying undefined to "undefined".
      expect(reducer(initialState, actions.dismiss(undefined)).dismissedWindowId).toBeNull();
      expect(sessionStorage.getItem(DISMISS_STORAGE_KEY)).toBeNull();

      expect(reducer(initialState, actions.dismiss(null)).dismissedWindowId).toBeNull();
      expect(sessionStorage.getItem(DISMISS_STORAGE_KEY)).toBeNull();
    });
  });

  describe('hydrateDismissed', () => {
    it('reads sessionStorage into dismissedWindowId', () => {
      sessionStorage.setItem(DISMISS_STORAGE_KEY, 'w-from-storage');
      const next = reducer(initialState, actions.hydrateDismissed());
      expect(next.dismissedWindowId).toBe('w-from-storage');
    });

    it('is a no-op when no value is stored', () => {
      const next = reducer(initialState, actions.hydrateDismissed());
      expect(next.dismissedWindowId).toBeNull();
    });
  });

  describe('cross-slice action handling', () => {
    it('clears dismissal and suppressedByLogout when the user logs in (userLogin/submitUserLogin/fulfilled)', () => {
      sessionStorage.setItem(DISMISS_STORAGE_KEY, 'w-old');
      const state = { ...initialState, dismissedWindowId: 'w-old', suppressedByLogout: true };
      const next = reducer(state, { type: 'userLogin/submitUserLogin/fulfilled' });
      expect(next.dismissedWindowId).toBeNull();
      expect(next.suppressedByLogout).toBe(false);
      expect(sessionStorage.getItem(DISMISS_STORAGE_KEY)).toBeNull();
    });

    it('sets suppressedByLogout when the logout thunk starts (userSession/logout/pending) without clearing dismissal', () => {
      sessionStorage.setItem(DISMISS_STORAGE_KEY, 'w-kept');
      const state = { ...initialState, dismissedWindowId: 'w-kept' };
      const next = reducer(state, { type: 'userSession/logout/pending' });
      expect(next.suppressedByLogout).toBe(true);
      // Dismissal stays put; suppressedByLogout hides the banner until next login.
      expect(next.dismissedWindowId).toBe('w-kept');
      expect(sessionStorage.getItem(DISMISS_STORAGE_KEY)).toBe('w-kept');
    });

    it('does NOT clear dismissal or toggle suppressedByLogout on userSession/logout/fulfilled', () => {
      const state = { ...initialState, dismissedWindowId: 'w-kept', suppressedByLogout: true };
      const next = reducer(state, { type: 'userSession/logout/fulfilled' });
      expect(next).toEqual(state);
    });

    it('clears suppressedByLogout when logout fails so the banner reappears (userSession/logout/rejected)', () => {
      // Dismissal is preserved — logout failing shouldn't un-dismiss a banner the user dismissed earlier.
      sessionStorage.setItem(DISMISS_STORAGE_KEY, 'w-kept');
      const state = { ...initialState, dismissedWindowId: 'w-kept', suppressedByLogout: true };
      const next = reducer(state, { type: 'userSession/logout/rejected' });
      expect(next.suppressedByLogout).toBe(false);
      expect(next.dismissedWindowId).toBe('w-kept');
      expect(sessionStorage.getItem(DISMISS_STORAGE_KEY)).toBe('w-kept');
    });

    it('leaves state alone for unrelated actions', () => {
      const state = { ...initialState, dismissedWindowId: 'w-keep' };
      const next = reducer(state, { type: 'some/other/action' });
      expect(next.dismissedWindowId).toBe('w-keep');
      expect(next.suppressedByLogout).toBe(false);
    });
  });
});
