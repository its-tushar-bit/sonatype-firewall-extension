/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSlice, ThunkAction, UnknownAction } from '@reduxjs/toolkit';
import {
  getDisplayTheme as getDisplayThemeOnStorage,
  setDisplayTheme as setDisplayThemeOnStorage,
  onDisplayThemeChange as onDisplayThemeStorageChange,
} from 'MainRoot/util/preferenceStore';

export type DisplayTheme = 'system' | 'dark' | 'light';

type AppThunk = ThunkAction<void, unknown, unknown, UnknownAction>;

const REDUCER_NAME = 'displayTheme';

export const validDisplayThemes: DisplayTheme[] = ['system', 'dark', 'light'];

function isDisplayTheme(value: string): value is DisplayTheme {
  return validDisplayThemes.includes(value as DisplayTheme);
}

// Function form prevents createSlice from narrowing state to the literal "system"
const initialState = (): DisplayTheme => 'system';

export const displayThemeSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: (create) => ({
    setDisplayThemeState: create.reducer<DisplayTheme>((_, action) => action.payload),
  }),
});

const { setDisplayThemeState: setDisplayThemeStateAction } = displayThemeSlice.actions;

export const setDisplayTheme =
  (theme: DisplayTheme): AppThunk =>
  (dispatch) => {
    if (!isDisplayTheme(theme)) {
      throw new Error(`Invalid theme: ${theme}`);
    }

    setDisplayThemeOnStorage(theme);
    dispatch(setDisplayThemeStateAction(theme));
  };

/**
 * Initialize the display theme state from local storage and listen for changes to it.
 */
const initialize = (): AppThunk => (dispatch) => {
  const storedTheme = getDisplayThemeOnStorage();
  if (storedTheme && isDisplayTheme(storedTheme)) {
    dispatch(setDisplayTheme(storedTheme));
  }

  onDisplayThemeStorageChange((theme: string) => {
    if (isDisplayTheme(theme)) {
      dispatch(setDisplayTheme(theme));
    }
  });
};

export default displayThemeSlice.reducer;

export const actions = { initialize, setDisplayTheme, setDisplayThemeState: setDisplayThemeStateAction };
