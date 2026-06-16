/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit';
import axios from 'axios';
import { extractAxiosMessage } from 'MainRoot/nosc/util/extractAxiosMessage';
import {
  getWaiversAndAutoWaiversUrl,
  getWaiversUrl,
  getWaiverDetailsUrl,
} from 'MainRoot/util/CLMLocation';
import type {
  PolicyWaiverDTO,
  PolicyWaiverDetailDTO,
  WaiversListResponse,
} from './waiverTypes';

export type NoscWaiversFetchStatus = 'idle' | 'loading' | 'ready' | 'error';

export interface WaiversListRequest {
  readonly applicationInternalId?: string;
  readonly pageSize?: number;
  readonly page?: number;
  readonly includeAutoWaivers?: boolean;
}

export const DEFAULT_WAIVERS_PAGE_SIZE = 100;

export function waiversListKey(request: WaiversListRequest): string {
  const pageSize = request.pageSize ?? DEFAULT_WAIVERS_PAGE_SIZE;
  const page = request.page ?? 0;
  const includeAutoWaivers = request.includeAutoWaivers ?? true;
  return `${request.applicationInternalId ?? '*'}|${pageSize}|${page}|${includeAutoWaivers}`;
}

export function waiverDetailKey(
  ownerType: string | null,
  ownerId: string | null,
  waiverId: string | null,
): string | null {
  if (!ownerType || !ownerId || !waiverId) return null;
  return `${ownerType}|${ownerId}|${waiverId}`;
}

interface WaiversListState {
  readonly status: NoscWaiversFetchStatus;
  readonly waivers: ReadonlyArray<PolicyWaiverDTO>;
  readonly hasNextPage: boolean;
  readonly error: string | null;
  readonly activeKey: string | null;
}

interface WaiverDetailState {
  readonly status: NoscWaiversFetchStatus;
  readonly waiver: PolicyWaiverDetailDTO | null;
  readonly error: string | null;
  readonly activeKey: string | null;
}

export interface NoscWaiversState {
  /** One list entry per {@link waiversListKey} so co-mounted consumers do not clobber each other. */
  readonly listsByKey: Record<string, WaiversListState>;
  readonly detail: WaiverDetailState;
}

/**
 * Cap on cached list entries. Entries are no longer evicted on unmount (that clobbered
 * co-mounted consumers sharing a key), so without a cap `listsByKey` would grow once per
 * distinct request tuple for the lifetime of the session. The cap bounds memory by evicting
 * the oldest non-active entries (insertion order) when the limit is exceeded.
 */
const MAX_LIST_ENTRIES = 32;

/**
 * Evict entries when the cache exceeds MAX_LIST_ENTRIES. Uses FIFO (oldest
 * *inserted*, not LRU least-recently-*used*) because it's simpler and sufficient
 * for a bounded cache — a hot key inserted early may be evicted before a cold
 * key inserted later, but the bound prevents unbounded growth either way.
 */
function evictOldestListEntries(
  listsByKey: Record<string, WaiversListState>,
  keepKey: string,
): void {
  const keys = Object.keys(listsByKey);
  let overflow = keys.length - MAX_LIST_ENTRIES;
  // FIFO eviction: drop the oldest non-active entries (insertion order).
  for (const key of keys) {
    if (overflow <= 0) break;
    if (key === keepKey) continue;
    delete listsByKey[key];
    overflow -= 1;
  }
}

const emptyListState = (): WaiversListState => ({
  status: 'idle',
  waivers: [],
  hasNextPage: false,
  error: null,
  activeKey: null,
});

/** Singleton for missing-key selector returns — prevents unnecessary re-renders.
 *  When `selectNoscWaiversListState` is called for a key that doesn't exist yet,
 *  returning the same object reference allows `useSelector` to skip re-renders
 *  until the key is actually written.
 */
const MISSING_LIST_STATE: WaiversListState = emptyListState();

const emptyDetailState = (): WaiverDetailState => ({
  status: 'idle',
  waiver: null,
  error: null,
  activeKey: null,
});

const initialState: NoscWaiversState = {
  listsByKey: {},
  detail: emptyDetailState(),
};

export const fetchNoscWaiversList = createAsyncThunk(
  'noscWaivers/fetchList',
  async (request: WaiversListRequest, { signal, rejectWithValue }) => {
    const pageSize = request.pageSize ?? DEFAULT_WAIVERS_PAGE_SIZE;
    const page = request.page ?? 0;
    const includeAutoWaivers = request.includeAutoWaivers ?? true;
    const url = includeAutoWaivers ? getWaiversAndAutoWaiversUrl() : getWaiversUrl();
    const body: Record<string, unknown> = { pageSize, page };
    if (request.applicationInternalId) {
      body.applicationIds = [request.applicationInternalId];
    }
    try {
      const { data } = await axios.post<WaiversListResponse>(url, body, { signal });
      return {
        waivers: data.dashboardResults ?? [],
        hasNextPage: Boolean(data.hasNextPage),
        listKey: waiversListKey(request),
      };
    } catch (err) {
      return rejectWithValue(extractAxiosMessage(err));
    }
  },
  {
    condition: (request, { getState }) => {
      const key = waiversListKey(request);
      const entry = (getState() as NoscWaiversRootState).noscWaivers.listsByKey[key];
      return !(entry?.status === 'loading' && entry.activeKey === key);
    },
  },
);

export interface FetchNoscWaiverDetailArgs {
  readonly ownerType: string;
  readonly ownerId: string;
  readonly waiverId: string;
}

export const fetchNoscWaiverDetail = createAsyncThunk(
  'noscWaivers/fetchDetail',
  async (args: FetchNoscWaiverDetailArgs, { signal, rejectWithValue }) => {
    try {
      const { data } = await axios.get<PolicyWaiverDetailDTO>(
        getWaiverDetailsUrl(args.ownerType, args.ownerId, args.waiverId),
        { signal },
      );
      return {
        waiver: data,
        detailKey: waiverDetailKey(args.ownerType, args.ownerId, args.waiverId),
      };
    } catch (err) {
      return rejectWithValue(extractAxiosMessage(err));
    }
  },
  {
    condition: (args, { getState }) => {
      const detail = (getState() as NoscWaiversRootState).noscWaivers.detail;
      const key = waiverDetailKey(args.ownerType, args.ownerId, args.waiverId);
      return !(detail.status === 'loading' && detail.activeKey === key);
    },
  },
);

const noscWaiversSlice = createSlice({
  name: 'noscWaivers',
  initialState,
  reducers: {
    resetNoscWaiversList: (state, action: PayloadAction<string | undefined>) => {
      const key = action.payload;
      if (key) {
        delete state.listsByKey[key];
      } else {
        state.listsByKey = {};
      }
    },
    resetNoscWaiverDetail: (state) => {
      state.detail = emptyDetailState();
    },
    resetNoscWaivers: () => initialState,
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchNoscWaiversList.pending, (state, action) => {
        const key = waiversListKey(action.meta.arg);
        state.listsByKey[key] = {
          ...(state.listsByKey[key] ?? emptyListState()),
          status: 'loading',
          error: null,
          activeKey: key,
        };
        evictOldestListEntries(state.listsByKey, key);
      })
      // No activeKey stale guard here (unlike fetchNoscWaiverDetail.fulfilled): each
      // list response writes to its own listsByKey slot, so a slow response for key
      // "A" cannot clobber key "B". A ghost-write after resetNoscWaiversList is benign.
      .addCase(fetchNoscWaiversList.fulfilled, (state, action) => {
        const key = action.payload.listKey;
        state.listsByKey[key] = {
          status: 'ready',
          waivers: action.payload.waivers,
          hasNextPage: action.payload.hasNextPage,
          error: null,
          activeKey: key,
        };
      })
      .addCase(fetchNoscWaiversList.rejected, (state, action) => {
        if (action.meta.aborted) return;
        const key = waiversListKey(action.meta.arg);
        state.listsByKey[key] = {
          ...(state.listsByKey[key] ?? emptyListState()),
          status: 'error',
          waivers: [],
          hasNextPage: false,
          error:
            (typeof action.payload === 'string' ? action.payload : null) ??
            action.error.message ??
            'Failed to load waivers',
          activeKey: key,
        };
      })
      .addCase(fetchNoscWaiverDetail.pending, (state, action) => {
        state.detail.status = 'loading';
        state.detail.error = null;
        state.detail.activeKey = waiverDetailKey(
          action.meta.arg.ownerType,
          action.meta.arg.ownerId,
          action.meta.arg.waiverId,
        );
      })
      .addCase(fetchNoscWaiverDetail.fulfilled, (state, action) => {
        if (action.payload.detailKey !== state.detail.activeKey) return;
        state.detail.status = 'ready';
        state.detail.waiver = action.payload.waiver;
      })
      .addCase(fetchNoscWaiverDetail.rejected, (state, action) => {
        if (action.meta.aborted) return;
        const detailKey = waiverDetailKey(
          action.meta.arg.ownerType,
          action.meta.arg.ownerId,
          action.meta.arg.waiverId,
        );
        if (detailKey !== state.detail.activeKey) return;
        state.detail.status = 'error';
        state.detail.waiver = null;
        state.detail.error =
          (typeof action.payload === 'string' ? action.payload : null) ??
          action.error.message ??
          'Failed to load waiver detail';
      });
  },
});

export const { resetNoscWaiversList, resetNoscWaiverDetail, resetNoscWaivers } =
  noscWaiversSlice.actions;

export default noscWaiversSlice.reducer;

interface NoscWaiversRootState {
  readonly noscWaivers: NoscWaiversState;
}

export const selectNoscWaiversListState = (
  state: NoscWaiversRootState,
  listKey: string,
): WaiversListState => state.noscWaivers.listsByKey[listKey] ?? MISSING_LIST_STATE;

export const selectNoscWaiversListHasEntry = (
  state: NoscWaiversRootState,
  listKey: string,
): boolean => listKey in state.noscWaivers.listsByKey;

export const selectNoscWaiverDetailState = (state: NoscWaiversRootState): WaiverDetailState =>
  state.noscWaivers.detail;
