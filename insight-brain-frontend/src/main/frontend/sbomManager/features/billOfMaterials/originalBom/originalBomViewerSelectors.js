/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const selectLoading = (state) => state.originalBomViewer.loading;
export const selectError = (state) => state.originalBomViewer.error;
export const selectTreeData = (state) => state.originalBomViewer.treeData;
export const selectOpenNodes = (state) => state.originalBomViewer.openNodes;
export const selectNodeChildren = (state) => state.originalBomViewer.nodeChildren;
export const selectVisibleCounts = (state) => state.originalBomViewer.visibleCounts;
