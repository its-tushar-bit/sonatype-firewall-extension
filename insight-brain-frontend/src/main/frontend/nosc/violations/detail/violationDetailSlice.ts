/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axios from 'axios';
import {
  getApplicationSummaryUrl,
  getPermissionContextTestUrl,
  getVulnerabilityJsonDetailUrl,
} from 'MainRoot/util/CLMLocation';
import {
  fetchApplicableWaivers,
  fetchCrossStageViolationDetails,
} from 'MainRoot/nosc/violations/detail/violationDetailApi';
import {
  ApplicableWaiverDTO,
  ComponentIdentifierDTO,
  ViolationDetailsDTO,
  VulnerabilitySummaryDTO,
} from 'MainRoot/nosc/violations/detail/violationDetailTypes';
import {
  getMostRecentScanId,
  isSecurityPolicyCategory,
} from 'MainRoot/nosc/violations/detail/violationDetailUtils';

export type FetchStatus = 'idle' | 'loading' | 'ready' | 'error';

export interface ViolationDetailSubState<T> {
  readonly status: FetchStatus;
  readonly data: T | null;
  readonly error: string | null;
}

export interface ViolationDetailWaiversState {
  readonly status: FetchStatus;
  readonly active: ReadonlyArray<ApplicableWaiverDTO>;
  readonly expired: ReadonlyArray<ApplicableWaiverDTO>;
  readonly error: string | null;
}

export interface ViolationDetailState {
  readonly violationId: string | null;
  readonly identity: ViolationDetailSubState<ViolationDetailsDTO>;
  readonly waivers: ViolationDetailWaiversState;
  readonly vulnerabilitySummary: ViolationDetailSubState<VulnerabilitySummaryDTO>;
  readonly hasPermissionForAppWaivers: boolean | null;
  readonly waiverPermissionError: string | null;
}

const emptySubState = <T>(): ViolationDetailSubState<T> => ({
  status: 'idle',
  data: null,
  error: null,
});

const emptyWaiversState = (): ViolationDetailWaiversState => ({
  status: 'idle',
  active: [],
  expired: [],
  error: null,
});

const createInitialState = (): ViolationDetailState => ({
  violationId: null,
  identity: emptySubState<ViolationDetailsDTO>(),
  waivers: emptyWaiversState(),
  vulnerabilitySummary: emptySubState<VulnerabilitySummaryDTO>(),
  hasPermissionForAppWaivers: null,
  waiverPermissionError: null,
});

const getErrorMessage = (message: string | undefined, fallback: string) => message ?? fallback;

interface ViolationDetailRootState {
  readonly violationDetail?: ViolationDetailState;
  readonly router?: {
    readonly currentParams?: {
      readonly hash?: string;
    };
  };
  readonly applicationReport?: {
    readonly selectedReport?: {
      readonly allEntries?: ReadonlyArray<{
        readonly hash?: string;
        readonly identificationSource?: string;
      }>;
    };
  };
}

function isCurrentViolation(state: ViolationDetailState, violationId: string): boolean {
  return state.violationId === violationId;
}

function isCurrentViolationInRootState(state: unknown, violationId: string): boolean {
  return (state as ViolationDetailRootState).violationDetail?.violationId === violationId;
}

function getClassicSelectedComponentIdentificationSource(state: unknown): string | undefined {
  const rootState = state as ViolationDetailRootState;
  const hash = rootState.router?.currentParams?.hash;
  if (!hash) return undefined;

  return rootState.applicationReport?.selectedReport?.allEntries?.find((component) => component.hash === hash)
    ?.identificationSource;
}

function getSecurityVulnerabilityRefId(details: ViolationDetailsDTO): string | null {
  for (const constraint of details.constraintViolations ?? []) {
    const reason = constraint.reasons?.find((item) => item.reference?.type === 'SECURITY_VULNERABILITY_REFID');
    if (reason?.reference?.value) {
      return reason.reference.value;
    }
  }
  return null;
}

function normalizeComponentIdentifier(
  componentIdentifier: ViolationDetailsDTO['componentIdentifier']
): ComponentIdentifierDTO | string | undefined {
  if (typeof componentIdentifier !== 'string') {
    return componentIdentifier;
  }
  try {
    return JSON.parse(componentIdentifier) as ComponentIdentifierDTO;
  } catch {
    return componentIdentifier;
  }
}

export const fetchViolationIdentity = createAsyncThunk(
  'violationDetail/fetchIdentity',
  async ({ violationId }: { readonly violationId: string }) => fetchCrossStageViolationDetails(violationId)
);

export const fetchViolationWaivers = createAsyncThunk(
  'violationDetail/fetchWaivers',
  async ({ violationId }: { readonly violationId: string }) => fetchApplicableWaivers(violationId)
);

export const fetchViolationVulnerabilitySummary = createAsyncThunk(
  'violationDetail/fetchVulnerabilitySummary',
  async ({ details }: { readonly violationId: string; readonly details: ViolationDetailsDTO }, { signal, getState }) => {
    const refId = getSecurityVulnerabilityRefId(details);
    if (!refId) return null;

    const scanId = getMostRecentScanId(details.stageData);
    const identificationSource =
      getClassicSelectedComponentIdentificationSource(getState()) ?? details.identificationSource;
    const extraQueryParameters: Record<string, string> = {
      ownerType: 'application',
      ownerId: details.applicationPublicId,
    };
    if (scanId) {
      extraQueryParameters.scanId = scanId;
    }
    if (identificationSource) {
      extraQueryParameters.identificationSource = identificationSource;
    }

    const { data } = await axios.get<VulnerabilitySummaryDTO>(
      getVulnerabilityJsonDetailUrl(
        refId,
        normalizeComponentIdentifier(details.componentIdentifier),
        extraQueryParameters
      ),
      { signal }
    );
    return data;
  }
);

export const fetchViolationWaiverPermission = createAsyncThunk(
  'violationDetail/fetchWaiverPermission',
  async (
    { applicationPublicId }: { readonly violationId: string; readonly applicationPublicId: string },
    { signal }
  ) => {
    const { data: appSummary } = await axios.get<{ readonly id: string }>(
      getApplicationSummaryUrl(applicationPublicId),
      { signal }
    );
    const { data: permissions } = await axios.put<ReadonlyArray<string>>(
      getPermissionContextTestUrl('application', appSummary.id),
      ['WAIVE_POLICY_VIOLATIONS'],
      { signal }
    );
    return permissions.includes('WAIVE_POLICY_VIOLATIONS');
  }
);

export const loadViolationDetail = createAsyncThunk(
  'violationDetail/load',
  async (violationId: string, { dispatch, getState }) => {
    dispatch(resetForViolation(violationId));
    const [identityResult] = await Promise.allSettled([
      dispatch(fetchViolationIdentity({ violationId })).unwrap(),
      dispatch(fetchViolationWaivers({ violationId })).unwrap(),
    ]);

    if (identityResult.status !== 'fulfilled') {
      return;
    }

    const details = identityResult.value;
    if (!isCurrentViolationInRootState(getState(), violationId)) {
      return;
    }

    await dispatch(fetchViolationWaiverPermission({ violationId, applicationPublicId: details.applicationPublicId }))
      .unwrap()
      .catch(() => null);

    if (!isCurrentViolationInRootState(getState(), violationId)) {
      return;
    }

    if (isSecurityPolicyCategory(details.policyThreatCategory)) {
      await dispatch(fetchViolationVulnerabilitySummary({ violationId, details }))
        .unwrap()
        .catch(() => null);
    }
  }
);

const violationDetailSlice = createSlice({
  name: 'violationDetail',
  initialState: createInitialState(),
  reducers: {
    reset: () => createInitialState(),
    resetForViolation: (_, action: { readonly payload: string }) => ({
      ...createInitialState(),
      violationId: action.payload,
    }),
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchViolationIdentity.pending, (state, action) => {
        state.violationId = action.meta.arg.violationId;
        state.identity.status = 'loading';
        state.identity.data = null;
        state.identity.error = null;
        state.hasPermissionForAppWaivers = null;
        state.waiverPermissionError = null;
      })
      .addCase(fetchViolationIdentity.fulfilled, (state, action) => {
        if (!isCurrentViolation(state, action.meta.arg.violationId)) return;
        return {
          ...state,
          identity: {
            status: 'ready',
            data: action.payload,
            error: null,
          },
        };
      })
      .addCase(fetchViolationIdentity.rejected, (state, action) => {
        if (!isCurrentViolation(state, action.meta.arg.violationId)) return;
        if (action.meta.aborted) return;
        state.identity.status = 'error';
        state.identity.data = null;
        state.identity.error = getErrorMessage(action.error.message, 'Failed to load violation details');
      })
      .addCase(fetchViolationWaivers.pending, (state, action) => {
        if (!isCurrentViolation(state, action.meta.arg.violationId)) return;
        state.waivers.status = 'loading';
        state.waivers.active = [];
        state.waivers.expired = [];
        state.waivers.error = null;
      })
      .addCase(fetchViolationWaivers.fulfilled, (state, action) => {
        if (!isCurrentViolation(state, action.meta.arg.violationId)) return;
        return {
          ...state,
          waivers: {
            status: 'ready',
            active: action.payload.activeWaivers,
            expired: action.payload.expiredWaivers,
            error: null,
          },
        };
      })
      .addCase(fetchViolationWaivers.rejected, (state, action) => {
        if (!isCurrentViolation(state, action.meta.arg.violationId)) return;
        if (action.meta.aborted) return;
        state.waivers.status = 'error';
        state.waivers.active = [];
        state.waivers.expired = [];
        state.waivers.error = getErrorMessage(action.error.message, 'Failed to load applicable waivers');
      })
      .addCase(fetchViolationVulnerabilitySummary.pending, (state, action) => {
        if (!isCurrentViolation(state, action.meta.arg.violationId)) return;
        state.vulnerabilitySummary.status = 'loading';
        state.vulnerabilitySummary.data = null;
        state.vulnerabilitySummary.error = null;
      })
      .addCase(fetchViolationVulnerabilitySummary.fulfilled, (state, action) => {
        if (!isCurrentViolation(state, action.meta.arg.violationId)) return;
        if (!action.payload) {
          return {
            ...state,
            vulnerabilitySummary: {
              status: 'ready',
              data: null,
              error: null,
            },
          };
        }
        return {
          ...state,
          vulnerabilitySummary: {
            status: 'ready',
            data: action.payload,
            error: null,
          },
        };
      })
      .addCase(fetchViolationVulnerabilitySummary.rejected, (state, action) => {
        if (!isCurrentViolation(state, action.meta.arg.violationId)) return;
        if (action.meta.aborted) return;
        state.vulnerabilitySummary.status = 'error';
        state.vulnerabilitySummary.data = null;
        state.vulnerabilitySummary.error = getErrorMessage(
          action.error.message,
          'Failed to load vulnerability summary'
        );
      })
      .addCase(fetchViolationWaiverPermission.pending, (state, action) => {
        if (!isCurrentViolation(state, action.meta.arg.violationId)) return;
        state.hasPermissionForAppWaivers = null;
        state.waiverPermissionError = null;
      })
      .addCase(fetchViolationWaiverPermission.fulfilled, (state, action) => {
        if (!isCurrentViolation(state, action.meta.arg.violationId)) return;
        state.hasPermissionForAppWaivers = action.payload;
        state.waiverPermissionError = null;
      })
      .addCase(fetchViolationWaiverPermission.rejected, (state, action) => {
        if (!isCurrentViolation(state, action.meta.arg.violationId)) return;
        if (action.meta.aborted) return;
        state.hasPermissionForAppWaivers = null;
        state.waiverPermissionError = getErrorMessage(
          action.error.message,
          'Failed to check waiver permission'
        );
      });
  },
});

export const { reset, resetForViolation } = violationDetailSlice.actions;

export default violationDetailSlice.reducer;
