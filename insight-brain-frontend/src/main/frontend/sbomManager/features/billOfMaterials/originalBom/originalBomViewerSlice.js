/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { always } from 'ramda';
import axios from 'axios';

import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { getDownloadSbomFileUrl } from 'MainRoot/util/CLMLocation';

import { processRawDataToTree } from './utils/treeDataUtils';
import { expandJsonChildren, findComponentInJson } from './utils/jsonTreeUtils';
import { expandXmlChildren, findComponentInXml, isXmlContent } from './utils/xmlTreeUtils';

const REDUCER_NAME = 'originalBomViewer';

export const initialState = Object.freeze({
  loading: false,
  error: null,
  treeData: [],
  openNodes: {},
  nodeChildren: {},
  visibleCounts: {},
  componentNotFound: false,
  searchValue: '',
  debouncedSearchValue: '',
});

const fetchOriginalBomRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const fetchOriginalBomFulfilled = (state, { payload }) => {
  state.loading = false;
  state.treeData = payload.treeData;
  state.openNodes = payload.openNodes;
  state.nodeChildren = payload.nodeChildren;
  state.componentNotFound = payload.componentNotFound || false;
};

const fetchOriginalBomFailed = (state, { payload }) => {
  state.loading = false;
  state.error = payload;
};

const fetchOriginalBom = createAsyncThunk(
  `${REDUCER_NAME}/fetchOriginalBom`,
  async ({ internalAppId, sbomVersion, componentPurl }, { rejectWithValue }) => {
    try {
      const response = await axios.get(getDownloadSbomFileUrl(internalAppId, sbomVersion));

      if (componentPurl) {
        let componentNode = null;

        if (typeof response.data === 'string' && isXmlContent(response.data)) {
          const parser = new DOMParser();
          const xmlDoc = parser.parseFromString(response.data, 'text/xml');
          componentNode = findComponentInXml(xmlDoc, componentPurl);
        } else {
          componentNode = findComponentInJson(response.data, componentPurl);
        }

        if (componentNode) {
          const componentId = componentNode.id;
          let componentChildren = null;

          if (componentNode.xmlNode) {
            componentChildren = expandXmlChildren(componentNode.xmlNode, componentId);
          } else if (componentNode.rawData) {
            componentChildren = expandJsonChildren(componentNode.rawData, componentId);
          }

          return {
            treeData: [componentNode],
            openNodes: { [componentId]: true },
            nodeChildren: componentChildren ? { [componentId]: componentChildren } : {},
            componentNotFound: false,
          };
        }
      }

      const processed = processRawDataToTree(response.data);

      const rootNode = processed.treeData[0];
      const hasRootWithChildren = rootNode && (rootNode.rawData || rootNode.xmlNode);

      if (!hasRootWithChildren) {
        return {
          treeData: processed.treeData,
          openNodes: {},
          nodeChildren: {},
          componentNotFound: !!componentPurl,
        };
      }

      const rootId = rootNode.id;
      let rootChildren = null;

      if (rootNode.xmlNode) {
        rootChildren = expandXmlChildren(rootNode.xmlNode, rootId);
      } else if (rootNode.rawData) {
        rootChildren = expandJsonChildren(rootNode.rawData, rootId);
      }

      return {
        treeData: processed.treeData,
        openNodes: { [rootId]: true },
        nodeChildren: rootChildren ? { [rootId]: rootChildren } : {},
        componentNotFound: !!componentPurl,
      };
    } catch (err) {
      return rejectWithValue(err);
    }
  }
);

const originalBomViewerSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setLoading: (state, { payload }) => {
      state.loading = payload;
    },
    setError: (state, { payload }) => {
      state.error = payload;
    },
    setTreeData: (state, { payload }) => {
      state.treeData = payload;
    },
    toggleNode: (state, { payload }) => {
      const { nodeId, node } = payload;
      const isOpen = state.openNodes[nodeId];

      if (isOpen) {
        state.openNodes[nodeId] = false;
        return;
      }

      state.openNodes[nodeId] = true;

      if (!state.nodeChildren[nodeId]) {
        const hasChildren = node.rawData || node.xmlNode;
        if (hasChildren) {
          let children = null;
          if (node.xmlNode) {
            children = expandXmlChildren(node.xmlNode, node.id);
          } else if (node.rawData) {
            children = expandJsonChildren(node.rawData, node.id);
          }
          if (children) {
            state.nodeChildren[nodeId] = children;
          }
        }
      }
    },
    loadMoreChildren: (state, { payload }) => {
      const { nodeId, batchSize } = payload;
      const currentCount = state.visibleCounts[nodeId] || batchSize;
      state.visibleCounts[nodeId] = currentCount + batchSize;
    },
    setSearchValue: (state, { payload }) => {
      state.searchValue = payload;
    },
    setDebouncedSearchValue: (state, { payload }) => {
      state.debouncedSearchValue = payload;
    },
    resetState: always(initialState),
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchOriginalBom.pending, fetchOriginalBomRequested)
      .addCase(fetchOriginalBom.fulfilled, fetchOriginalBomFulfilled)
      .addCase(fetchOriginalBom.rejected, fetchOriginalBomFailed)
      .addCase(UI_ROUTER_ON_FINISH, always(initialState));
  },
});

export const actions = {
  ...originalBomViewerSlice.actions,
  fetchOriginalBom,
};

export default originalBomViewerSlice.reducer;
