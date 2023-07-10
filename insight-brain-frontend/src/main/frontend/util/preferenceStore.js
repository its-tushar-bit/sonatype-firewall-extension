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

const getItemFromStorageForKey = (key) => localStorage.getItem(key);

const setItemInStorage = (key, value) => localStorage.setItem(key, value);
