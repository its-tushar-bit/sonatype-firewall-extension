/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import registerDisplayThemeHandler from 'MainRoot/configuration/displayTheme/displayThemeHandler';
import * as displayThemeSelectors from 'MainRoot/configuration/displayTheme/displayThemeSelectors';

describe('registerDisplayThemeHandler', () => {
  let store, selectDisplayThemeSpy;

  beforeEach(() => {
    selectDisplayThemeSpy = jest.spyOn(displayThemeSelectors, 'selectDisplayTheme').mockReturnValue('light');
    store = {
      subscribe: jest.fn((callback) => callback()),
      getState: jest.fn(),
    };
    document.documentElement.classList.remove('nx-html--dark-mode', 'nx-html--light-mode');
  });

  it('should apply dark mode class when theme is dark', () => {
    selectDisplayThemeSpy.mockReturnValue('dark');
    registerDisplayThemeHandler(store);

    expect(document.documentElement.classList.contains('nx-html--dark-mode')).toBe(true);
    expect(document.documentElement.classList.contains('nx-html--light-mode')).toBe(false);
  });

  it('should apply light mode class when theme is light', () => {
    selectDisplayThemeSpy.mockReturnValue('light');
    registerDisplayThemeHandler(store);

    expect(document.documentElement.classList.contains('nx-html--dark-mode')).toBe(false);
    expect(document.documentElement.classList.contains('nx-html--light-mode')).toBe(true);
  });

  it('should not apply any mode class when theme is system', () => {
    selectDisplayThemeSpy.mockReturnValue('system');
    registerDisplayThemeHandler(store);

    expect(document.documentElement.classList.contains('nx-html--dark-mode')).toBe(false);
    expect(document.documentElement.classList.contains('nx-html--light-mode')).toBe(false);
  });
});
