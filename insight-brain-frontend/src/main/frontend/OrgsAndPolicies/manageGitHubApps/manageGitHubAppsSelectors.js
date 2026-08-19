/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const selectSlice = (state) => state.manageGitHubApps;

export const selectGitHubApps = (state) => selectSlice(state).githubApps;
export const selectLoading = (state) => selectSlice(state).loading;
export const selectError = (state) => selectSlice(state).error;
export const selectDeleteModal = (state) => selectSlice(state).deleteModal;
export const selectHasEditPermission = (state) => selectSlice(state).hasEditPermission;
