/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { isLeftNavigationOpen, setLeftNavigationOpen } from '../../../main/frontend/util/preferenceStore';

describe('preferenceStore', function () {
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
});
