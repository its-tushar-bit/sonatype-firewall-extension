/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit } from 'ramda';

import reducer, {
  EXPORT_AND_DOWNLOAD_SBOM_SUBMIT_MASK_SUCCESS_MESSAGE,
  EXPORT_SBOM_FILE_FORMAT,
  EXPORT_SBOM_SPECIFICATION,
  exportAndDownloadSbomSubmitMaskInitialState,
  sbomAdditionalExportOptionsModalInitialState,
  initialState,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

const sbomMetadataInitialState = {
  author: [],
  manufacturer: [],
  supplier: [],
  person: [],
  organization: [],
  specification: null,
  specVersion: null,
  fileFormat: null,
  createdAt: null,
  validationSkipped: null,
};

const vulnerabilitiesSummaryInitialState = Object.freeze({
  critical: 0,
  high: 0,
  medium: 0,
  low: 0,
});

const policyViolationSummaryInitialState = Object.freeze({
  critical: 0,
  severe: 0,
  moderate: 0,
  low: 0,
});

const componentSummaryInitialState = Object.freeze({
  direct: 0,
  transitive: 0,
  unspecified: 0,
});

describe('billOfMaterialsPage reducers have the correct state when the following reducer is dispatched', function () {
  it('billOfMaterialsPage/setPublicAppId', () => {
    const state = {
      publicAppId: null,
    };

    const payload = 'app_123';

    const newState = reducer(state, {
      type: 'billOfMaterialsPage/setPublicAppId',
      payload: payload,
    });

    expect(newState.publicAppId).toBe('app_123');
  });

  describe('billOfMaterialsPage/loadSbomTableData', function () {
    it('/pending', () => {
      const state = {
        results: null,
        errorInternalAppId: null,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadInternalAppId/pending',
      });

      expect(newState.results).toBe(null);
      expect(newState.loadingInternalAppId).toBe(true);
    });

    it('/failed', () => {
      const state = {
        loadingInternalAppId: false,
        errorInternalAppId: null,
        applicationName: null,
        internalAppId: null,
        publicAppId: null,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadInternalAppId/rejected',
        payload: payload,
      });

      expect(newState.loadingInternalAppId).toBe(false);
      expect(newState.errorInternalAppId).toBe('payload error');
      expect(newState.applicationName).toBe(null);
      expect(newState.internalAppId).toBe(null);
      expect(newState.publicAppId).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loadingInternalAppId: false,
        errorInternalAppId: null,
        internalAppId: null,
        applicationName: null,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadInternalAppId/fulfilled',
        payload: { id: 'abc123', name: 'Alice' },
      });

      expect(newState.loadingInternalAppId).toBe(false);
      expect(newState.errorInternalAppId).toBe(null);
      expect(newState.internalAppId).toBe('abc123');
      expect(newState.applicationName).toBe('Alice');
    });
  });

  describe('billOfMaterialsPage/loadApplicationSbomVersions', function () {
    it('/pending', () => {
      const state = {
        loadingSbomVersions: true,
        errorSbomVersions: null,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadApplicationSbomVersions/pending',
      });

      expect(newState.loadingSbomVersions).toBe(true);
      expect(newState.errorSbomVersions).toBe(null);
    });

    it('/rejected', () => {
      const state = {
        loadingSbomVersions: false,
        errorSbomVersions: null,
        sbomVersions: null,
      };

      const payload = {
        response: {
          data: 'Error',
        },
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadApplicationSbomVersions/rejected',
        payload: payload,
      });

      expect(newState.loadingSbomVersions).toBe(false);
      expect(newState.errorSbomVersions).toBe(payload);
      expect(newState.sbomVersions).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loadingSbomVersions: false,
        errorSbomVersions: null,
        sbomVersions: null,
      };

      const payload = ['1.0-SNAPSHOT', '1.1-SNAPSHOT', '1.2-SNAPSHOT'];

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadApplicationSbomVersions/fulfilled',
        payload: payload,
      });

      expect(newState.loadingSbomVersions).toBe(false);
      expect(newState.errorSbomVersions).toBe(null);
      expect(newState.sbomVersions).toBe(payload);
    });
  });

  describe('billOfMaterialsPage/loadSbomMetadata', function () {
    it('/pending', () => {
      const state = {
        loadingSbomMetadata: true,
        errorSbomMetadata: null,
        sbomMetadata: { ...sbomMetadataInitialState },
        scanId: null,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadSbomMetadata/pending',
      });

      expect(newState.loadingSbomMetadata).toBe(true);
      expect(newState.errorSbomMetadata).toBe(null);
      expect(newState.sbomMetadata).toEqual(sbomMetadataInitialState);
      expect(newState.scanId).toBe(null);
    });

    it('/rejected', () => {
      const state = {
        loadingSbomMetadata: false,
        errorSbomMetadata: null,
        sbomMetadata: { ...sbomMetadataInitialState },
        scanId: null,
      };

      const payload = {
        response: {
          data: 'Error',
        },
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadSbomMetadata/rejected',
        payload: payload,
      });

      expect(newState.loadingSbomMetadata).toBe(false);
      expect(newState.errorSbomMetadata).toBe(payload);
      expect(newState.sbomMetadata).toEqual(sbomMetadataInitialState);
      expect(newState.scanId).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loadingSbomMetadata: false,
        errorSbomMetadata: null,
        sbomMetadata: { ...sbomMetadataInitialState },
        scanId: null,
      };

      const payload = {
        author: ['Alice', 'Bob'],
        manufacturer: ['Orange'],
        supplier: ['Apple'],
        person: ['John', 'Jane'],
        organization: ['Sonatype'],
        specification: 'SPDX',
        specVersion: '2.3',
        fileFormat: 'json',
        createdAt: '2024-01-12T20:11:22Z',
        validationSkipped: false,
        scanId: 'scan-id',
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadSbomMetadata/fulfilled',
        payload: payload,
      });

      expect(newState.loadingSbomMetadata).toBe(false);
      expect(newState.errorSbomMetadata).toBe(null);
      expect(newState.sbomMetadata).toEqual(omit(['scanId'], payload));
      expect(newState.scanId).toBe('scan-id');
    });
  });

  describe('billOfMaterialsPage/loadSbomSummary', function () {
    it('/pending', () => {
      const state = {
        loadingSbomSummary: true,
        errorSbomSummary: null,
        componentSummary: { ...componentSummaryInitialState },
        vulnerabilitiesSummary: { ...vulnerabilitiesSummaryInitialState },
        policyViolationSummary: { ...policyViolationSummaryInitialState },
        annotatedVulnerabilitesPercentage: null,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadSbomSummary/pending',
      });

      expect(newState.loadingSbomSummary).toBe(true);
      expect(newState.errorSbomSummary).toBe(null);
      expect(newState.componentSummary).toEqual(componentSummaryInitialState);
      expect(newState.vulnerabilitiesSummary).toEqual(vulnerabilitiesSummaryInitialState);
      expect(newState.policyViolationSummary).toEqual(policyViolationSummaryInitialState);
      expect(newState.annotatedVulnerabilitesPercentage).toBe(null);
    });

    it('/rejected', () => {
      const state = {
        loadingSbomSummary: true,
        errorSbomSummary: null,
        componentSummary: { ...componentSummaryInitialState },
        vulnerabilitiesSummary: { ...vulnerabilitiesSummaryInitialState },
        policyViolationSummary: { ...policyViolationSummaryInitialState },
        annotatedVulnerabilitesPercentage: null,
      };

      const payload = {
        response: {
          data: 'Error',
        },
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadSbomSummary/rejected',
        payload: payload,
      });

      expect(newState.loadingSbomSummary).toBe(false);
      expect(newState.errorSbomSummary).toBe(payload);
      expect(newState.componentSummary).toEqual(componentSummaryInitialState);
      expect(newState.vulnerabilitiesSummary).toEqual(vulnerabilitiesSummaryInitialState);
      expect(newState.policyViolationSummary).toEqual(policyViolationSummaryInitialState);
      expect(newState.annotatedVulnerabilitesPercentage).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loadingSbomSummary: true,
        errorSbomSummary: null,
        componentSummary: { ...componentSummaryInitialState },
        vulnerabilitiesSummary: { ...vulnerabilitiesSummaryInitialState },
        policyViolationSummary: { ...policyViolationSummaryInitialState },
        annotatedVulnerabilitesPercentage: null,
      };

      const payload = {
        applicationVersion: '123',
        none: 123,
        low: 1,
        medium: 2,
        high: 3,
        critical: 4,
        dependencyType: {
          direct: 5,
          transitive: 6,
          unspecified: 7,
        },
        policyViolationSummary: {
          low: 111,
          moderate: 222,
          severe: 333,
          critical: 444,
        },
        annotatedPercentage: 50,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadSbomSummary/fulfilled',
        payload: payload,
      });

      expect(newState.loadingSbomSummary).toBe(false);
      expect(newState.errorSbomSummary).toBe(null);
      expect(newState.componentSummary).toEqual({
        direct: 5,
        transitive: 6,
        unspecified: 7,
      });
      expect(newState.vulnerabilitiesSummary).toEqual({
        low: 1,
        medium: 2,
        high: 3,
        critical: 4,
      });
      expect(newState.policyViolationSummary).toEqual({
        low: 111,
        moderate: 222,
        severe: 333,
        critical: 444,
      });
      expect(newState.annotatedVulnerabilitesPercentage).toBe(50);
    });
  });

  describe('billOfMaterialsPage/exportAndDownloadSbom', function () {
    it('/pending', () => {
      const newState = reducer(initialState, {
        type: 'billOfMaterialsPage/exportAndDownloadSbom/pending',
      });

      expect(newState.sbomAdditionalExportOptionsModal.showModal).toBe(false);
      expect(newState.exportAndDownloadSbomSubmitMask.showSubmitMask).toBe(true);
    });

    it('/rejected', () => {
      const state = {
        ...initialState,
        sbomAdditionalExportOptionsModal: {
          showModal: true,
          sbomSpecification: EXPORT_SBOM_SPECIFICATION.spdx,
          sbomFileFormat: EXPORT_SBOM_FILE_FORMAT.xml,
        },
        exportAndDownloadSbomSubmitMask: {
          showSubmitMask: true,
          success: true,
          successMessage: 'hello',
        },
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/exportAndDownloadSbom/rejected',
      });

      expect(newState.sbomAdditionalExportOptionsModal).toEqual(sbomAdditionalExportOptionsModalInitialState);
      expect(newState.exportAndDownloadSbomSubmitMask).toEqual(exportAndDownloadSbomSubmitMaskInitialState);
    });

    it('/fulfilled', () => {
      const newState = reducer(initialState, {
        type: 'billOfMaterialsPage/exportAndDownloadSbom/fulfilled',
      });

      expect(newState.exportAndDownloadSbomSubmitMask.showSubmitMask).toBe(true);
      expect(newState.exportAndDownloadSbomSubmitMask.success).toBe(true);
      expect(newState.exportAndDownloadSbomSubmitMask.successMessage).toBe(
        EXPORT_AND_DOWNLOAD_SBOM_SUBMIT_MASK_SUCCESS_MESSAGE
      );
    });
  });

  describe('setShowSbomAdditionalExportOptionsModal', () => {
    const state = {
      sbomAdditionalExportOptionsModal: {
        showModal: false,
      },
    };

    const newState = reducer(state, {
      type: 'billOfMaterialsPage/setShowSbomAdditionalExportOptionsModal',
      payload: true,
    });

    expect(newState.sbomAdditionalExportOptionsModal.showModal).toBe(true);
  });

  describe('setExportSbomSpecification', () => {
    const state = {
      sbomAdditionalExportOptionsModal: {
        sbomSpecification: EXPORT_SBOM_SPECIFICATION.cyclonedx,
      },
    };

    const newState1 = reducer(state, {
      type: 'billOfMaterialsPage/setExportSbomSpecification',
      payload: 'invalid-specification',
    });

    expect(newState1.sbomAdditionalExportOptionsModal.sbomSpecification).toBe(EXPORT_SBOM_SPECIFICATION.cyclonedx);

    const newState2 = reducer(newState1, {
      type: 'billOfMaterialsPage/setExportSbomSpecification',
      payload: EXPORT_SBOM_SPECIFICATION.spdx,
    });

    expect(newState2.sbomAdditionalExportOptionsModal.sbomSpecification).toBe(EXPORT_SBOM_SPECIFICATION.spdx);
  });

  describe('setExportSbomFileFormat', () => {
    const state = {
      sbomAdditionalExportOptionsModal: {
        sbomFileFormat: EXPORT_SBOM_FILE_FORMAT.json,
      },
    };

    const newState1 = reducer(state, {
      type: 'billOfMaterialsPage/setExportSbomFileFormat',
      payload: 'invalid-file-format',
    });

    expect(newState1.sbomAdditionalExportOptionsModal.sbomFileFormat).toBe(EXPORT_SBOM_FILE_FORMAT.json);

    const newState2 = reducer(newState1, {
      type: 'billOfMaterialsPage/setExportSbomFileFormat',
      payload: EXPORT_SBOM_FILE_FORMAT.xml,
    });

    expect(newState2.sbomAdditionalExportOptionsModal.sbomFileFormat).toBe(EXPORT_SBOM_FILE_FORMAT.xml);
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('clears state on onFinish', () => {
      const state = Object.freeze({
        loadingInternalAppId: true,
        errorInternalAppId: 'some error',
        publicAppId: 'test-app-public',
        internalAppId: 'test-app-internal',
      });

      const newState = reducer(state, { type: UI_ROUTER_ON_FINISH });
      expect(newState).toEqual(initialState);
    });
  });
});
