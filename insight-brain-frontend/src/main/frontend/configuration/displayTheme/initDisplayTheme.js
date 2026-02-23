/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from './displayThemeSlice';
import registerDisplayThemeHandler from './displayThemeHandler';
import store from 'MainRoot/reduxConfig/store';

/**
 * Initialize display theme handling:
 * - Register handler that changes actual rendered theme in response to stored theme changes
 * - Connect redux and localStorage for theme persistence
 */
export default function initDisplayTheme() {
  registerDisplayThemeHandler(store);
  store.dispatch(actions.initialize());
}
