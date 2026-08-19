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
import { expandJsonChildren, findComponentInJson, getJsonTotalDescendants } from './utils/jsonTreeUtils';
import { expandXmlChildren, findComponentInXml, isXmlContent, getXmlTotalDescendants } from './utils/xmlTreeUtils';
import { AUTO_EXPAND_THRESHOLD } from './utils/constants';

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

/**
 * Recursively expands all JSON nodes in a tree
 */
const expandAllJsonNodesRecursively = (nodes) => {
  if (!nodes || nodes.length === 0) return nodes;

  return nodes.map((node) => {
    if (!node.rawData) return node;

    const expandedNode = { ...node, isOpen: true };
    expandedNode.children = expandJsonChildren(node.rawData, node.id);

    if (expandedNode.children && expandedNode.children.length > 0) {
      expandedNode.children = expandAllJsonNodesRecursively(expandedNode.children);
    }

    return expandedNode;
  });
};

/**
 * Recursively expands all XML nodes in a tree
 */
const expandAllXmlNodesRecursively = (nodes) => {
  if (!nodes || nodes.length === 0) return nodes;

  return nodes.map((node) => {
    if (!node.xmlNode) return node;

    const expandedNode = { ...node, isOpen: true };
    expandedNode.children = expandXmlChildren(node.xmlNode, node.id);

    if (expandedNode.children && expandedNode.children.length > 0) {
      expandedNode.children = expandAllXmlNodesRecursively(expandedNode.children);
    }

    return expandedNode;
  });
};

/**
 * Recursively extracts openNodes and nodeChildren from an auto-expanded tree
 * where nodes have isOpen: true and children: [...] already set
 */
const extractOpenNodesAndChildren = (nodes) => {
  const openNodes = {};
  const nodeChildren = {};

  const traverse = (nodeArray) => {
    if (!nodeArray || nodeArray.length === 0) return;

    nodeArray.forEach((node) => {
      if (node.isOpen && node.children && node.children.length > 0) {
        openNodes[node.id] = true;
        nodeChildren[node.id] = node.children;
        // Recursively process children
        traverse(node.children);
      }
    });
  };

  traverse(nodes);
  return { openNodes, nodeChildren };
};

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
        console.log('[Slice] Component purl provided:', componentPurl);
        let componentNode = null;

        if (typeof response.data === 'string' && isXmlContent(response.data)) {
          console.log('[Slice] Detected XML content, searching in XML');
          const parser = new DOMParser();
          const xmlDoc = parser.parseFromString(response.data, 'text/xml');
          componentNode = findComponentInXml(xmlDoc, componentPurl);
        } else {
          console.log('[Slice] Detected JSON content, searching in JSON');
          componentNode = findComponentInJson(response.data, componentPurl);
        }

        console.log('[Slice] Component node found:', componentNode);

        if (componentNode) {
          const componentId = componentNode.id;
          let componentChildren = null;

          console.log('[Slice] Component ID:', componentId);
          console.log('[Slice] Has xmlNode:', !!componentNode.xmlNode);
          console.log('[Slice] Has rawData:', !!componentNode.rawData);

          // Expand immediate children first
          if (componentNode.xmlNode) {
            componentChildren = expandXmlChildren(componentNode.xmlNode, componentId);
            console.log('[Slice] Expanded XML children:', componentChildren?.length || 0, 'items');
          } else if (componentNode.rawData) {
            componentChildren = expandJsonChildren(componentNode.rawData, componentId);
            console.log('[Slice] Expanded JSON children:', componentChildren?.length || 0, 'items');
          }

          // Count total descendants to determine if we should auto-expand everything
          let totalNodes = 1; // Count the component node itself
          if (componentNode.xmlNode) {
            totalNodes += getXmlTotalDescendants(componentNode);
          } else if (componentNode.rawData) {
            totalNodes += getJsonTotalDescendants(componentNode);
          }

          console.log('[Slice] Total nodes in component tree:', totalNodes);
          const shouldAutoExpand = totalNodes <= AUTO_EXPAND_THRESHOLD;
          console.log('[Slice] Should auto-expand all nodes:', shouldAutoExpand);

          // If small enough, recursively expand everything
          if (shouldAutoExpand && componentChildren && componentChildren.length > 0) {
            if (componentNode.xmlNode) {
              componentChildren = expandAllXmlNodesRecursively(componentChildren);
            } else if (componentNode.rawData) {
              componentChildren = expandAllJsonNodesRecursively(componentChildren);
            }
            console.log('[Slice] Recursively expanded all nodes');
          }

          const componentTreeData = [
            {
              ...componentNode,
              isOpen: true,
              children: componentChildren || [],
            },
          ];

          const { openNodes, nodeChildren } = extractOpenNodesAndChildren(componentTreeData);

          const result = {
            treeData: componentTreeData,
            openNodes,
            nodeChildren,
            componentNotFound: false,
          };
          console.log('[Slice] Returning result:', result);
          return result;
        } else {
          console.log('[Slice] Component not found, will show componentNotFound');
        }
      }

      const processed = processRawDataToTree(response.data);

      // Check if ANY node in the tree has expandable content (for JSON, first node might be a leaf)
      const hasExpandableNodes = processed.treeData.some((node) => node.rawData || node.xmlNode);

      if (!hasExpandableNodes) {
        return {
          treeData: processed.treeData,
          openNodes: {},
          nodeChildren: {},
          componentNotFound: !!componentPurl,
        };
      }

      // Extract openNodes and nodeChildren from the tree
      // (works for both auto-expanded and partially-expanded trees)
      const { openNodes, nodeChildren } = extractOpenNodesAndChildren(processed.treeData);

      return {
        treeData: processed.treeData,
        openNodes,
        nodeChildren,
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
