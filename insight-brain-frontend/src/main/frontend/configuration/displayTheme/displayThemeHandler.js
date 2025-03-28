/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectDisplayTheme } from './displayThemeSelectors';

/**
 * Change the actual theme of the page whenever the theme stored in redux changes
 */
export default function registerDisplayThemeHandler(store) {
  store.subscribe(() => {
    const theme = selectDisplayTheme(store.getState());
    switch (theme) {
      case 'dark':
        updateThemeClasses(true, false);
        break;
      case 'light':
        updateThemeClasses(false, true);
        break;
      case 'system':
        updateThemeClasses(false, false);
        break;
    }
  });
}

const DARK_MODE_CLASS = 'nx-html--dark-mode';
const LIGHT_MODE_CLASS = 'nx-html--light-mode';

function updateThemeClasses(darkMode, lightMode) {
  const htmlRoot = document.documentElement;
  htmlRoot.classList.toggle(DARK_MODE_CLASS, darkMode);
  htmlRoot.classList.toggle(LIGHT_MODE_CLASS, lightMode);
}
