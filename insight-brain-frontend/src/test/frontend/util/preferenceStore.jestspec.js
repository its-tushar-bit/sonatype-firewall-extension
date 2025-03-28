/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  isLeftNavigationOpen,
  setLeftNavigationOpen,
  getDisplayTheme,
  setDisplayTheme,
  onDisplayThemeChange,
} from 'MainRoot/util/preferenceStore';

describe('preferenceStore', function () {
  afterAll(function () {
    localStorage.clear();
  });

  const deleteFromLocalPreference = (key) => {
    localStorage.removeItem(key);
  };

  const setValueOnLocalPreference = (key, value) => {
    localStorage.setItem(key, value);
  };

  describe('isLeftNavigationOpen', function () {
    beforeEach(function () {
      deleteFromLocalPreference('leftNavigation.isOpen');
    });

    it('returns true if no preference has been set for the left navigation to be open', function () {
      const currentValue = isLeftNavigationOpen();
      expect(currentValue).toBe(true);
    });

    it('returns true if the left navigation open preference has been set to true', function () {
      setValueOnLocalPreference('leftNavigation.isOpen', true);

      const currentValue = isLeftNavigationOpen();
      expect(currentValue).toBe(true);
    });

    it('returns true if the left navigation open preference has been set to true', function () {
      setValueOnLocalPreference('leftNavigation.isOpen', false);

      const currentValue = isLeftNavigationOpen();
      expect(currentValue).toBe(false);
    });
  });

  describe('setLeftNavigationOpen', function () {
    beforeEach(function () {
      deleteFromLocalPreference('leftNavigation.isOpen');
    });

    it('sets the left navigation open to the received param if it is a boolean', function () {
      setValueOnLocalPreference('leftNavigation.isOpen', true);

      setLeftNavigationOpen(false);
      let currentValue = isLeftNavigationOpen();
      expect(currentValue).toBe(false);

      setLeftNavigationOpen(true);
      currentValue = isLeftNavigationOpen();
      expect(currentValue).toBe(true);
    });

    it('does not change the left navigation open if the received param is not a boolean', function () {
      setValueOnLocalPreference('leftNavigation.isOpen', false);
      let currentValue;

      setLeftNavigationOpen('');
      currentValue = isLeftNavigationOpen();
      expect(currentValue).toBe(false);

      setLeftNavigationOpen('test');
      currentValue = isLeftNavigationOpen();
      expect(currentValue).toBe(false);

      setLeftNavigationOpen(['test']);
      currentValue = isLeftNavigationOpen();
      expect(currentValue).toBe(false);

      setLeftNavigationOpen({ test: 'test' });
      currentValue = isLeftNavigationOpen();
      expect(currentValue).toBe(false);
    });

    it('dispatches a storage event on the window', () => {
      const eventSpy = jest.fn();
      window.addEventListener('storage', eventSpy);
      setLeftNavigationOpen(false);
      expect(eventSpy).toHaveBeenCalled();
    });
  });

  describe('getDisplayTheme', function () {
    beforeEach(function () {
      deleteFromLocalPreference('displayTheme');
    });

    it('returns null if no preference has been set for the display theme', function () {
      const currentValue = getDisplayTheme();
      expect(currentValue).toBe(null);
    });

    it('returns "dark" if the theme preference is set to dark', function () {
      setValueOnLocalPreference('displayTheme', 'dark');

      const currentValue = getDisplayTheme();
      expect(currentValue).toBe('dark');
    });

    it('returns "light" if the theme preference is set to light', function () {
      setValueOnLocalPreference('displayTheme', 'light');

      const currentValue = getDisplayTheme();
      expect(currentValue).toBe('light');
    });

    it('returns "system" if the theme preference is set to system', function () {
      setValueOnLocalPreference('displayTheme', 'system');

      const currentValue = getDisplayTheme();
      expect(currentValue).toBe('system');
    });
  });

  describe('setDisplayTheme', function () {
    beforeEach(function () {
      deleteFromLocalPreference('displayTheme');
    });

    it('sets the display theme to the received param', function () {
      setValueOnLocalPreference('displayTheme', 'dark');

      setDisplayTheme('dark');
      let currentValue = getDisplayTheme();
      expect(currentValue).toBe('dark');

      setDisplayTheme('light');
      currentValue = getDisplayTheme();
      expect(currentValue).toBe('light');

      setDisplayTheme('system');
      currentValue = getDisplayTheme();
      expect(currentValue).toBe('system');
    });
  });

  describe('onDisplayThemeChange', () => {
    it('should call the callback when the display theme changes', () => {
      const callback = jest.fn();
      onDisplayThemeChange(callback);

      const event = new StorageEvent('storage', {
        key: 'displayTheme',
        newValue: 'dark',
        oldValue: 'light',
      });
      window.dispatchEvent(event);

      expect(callback).toHaveBeenCalledWith('dark');
    });

    it('should not call the callback if the key is not displayTheme', () => {
      const callback = jest.fn();
      onDisplayThemeChange(callback);

      const event = new StorageEvent('storage', {
        key: 'otherKey',
        newValue: 'dark',
        oldValue: 'light',
      });
      window.dispatchEvent(event);

      expect(callback).not.toHaveBeenCalled();
    });

    it('should not call the callback if the newValue is null', () => {
      const callback = jest.fn();
      onDisplayThemeChange(callback);

      const event = new StorageEvent('storage', {
        key: 'displayTheme',
        newValue: null,
        oldValue: 'light',
      });
      window.dispatchEvent(event);

      expect(callback).not.toHaveBeenCalled();
    });

    it('should not call the callback if the newValue is the same as the oldValue', () => {
      const callback = jest.fn();
      onDisplayThemeChange(callback);

      const event = new StorageEvent('storage', {
        key: 'displayTheme',
        newValue: 'dark',
        oldValue: 'dark',
      });
      window.dispatchEvent(event);

      expect(callback).not.toHaveBeenCalled();
    });
  });
});
