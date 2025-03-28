/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { isNilOrEmpty } from './jsUtil';

const preferences = {
  leftNavigation: {
    isOpen: 'leftNavigation.isOpen',
  },
  displayTheme: 'displayTheme',
};

const defaults = {
  [preferences.leftNavigation.isOpen]: true,
};

export const isLeftNavigationOpen = () => {
  const currentLocalValue = getItemFromStorageForKey(preferences.leftNavigation.isOpen);
  if (isNilOrEmpty(currentLocalValue)) {
    return defaults[preferences.leftNavigation.isOpen];
  }

  return currentLocalValue === 'true';
};

export const setLeftNavigationOpen = (newLeftNavigationOpenState) => {
  if (newLeftNavigationOpenState !== true && newLeftNavigationOpenState !== false) {
    return;
  }
  setItemInStorage(preferences.leftNavigation.isOpen, newLeftNavigationOpenState);
  window.dispatchEvent(new Event('storage'));
};

export const getDisplayTheme = () => {
  const currentLocalValue = getItemFromStorageForKey(preferences.displayTheme);
  if (isNilOrEmpty(currentLocalValue)) {
    return null;
  }

  return currentLocalValue;
};

export const setDisplayTheme = (displayTheme) => {
  setItemInStorage(preferences.displayTheme, displayTheme);
};

/**
 * Register a callback to be called when the display theme changes, potentially due to action in another window.
 * The callback will be passed the new display theme.
 */
export function onDisplayThemeChange(callback) {
  window.addEventListener('storage', (evt) => {
    if (evt.key === preferences.displayTheme && evt.newValue !== null && evt.newValue !== evt.oldValue) {
      callback(evt.newValue);
    }
  });
}

const getItemFromStorageForKey = (key) => localStorage.getItem(key);

const setItemInStorage = (key, value) => localStorage.setItem(key, value);
