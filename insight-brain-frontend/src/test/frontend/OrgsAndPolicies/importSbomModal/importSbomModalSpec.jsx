/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { nxFileUploadStateHelpers, nxTextInputStateHelpers } from '@sonatype/react-shared-components';

import ImportSbomModal from 'MainRoot/OrgsAndPolicies/importSbomModal/ImportSbomModal';
import { getImportSbomUrl } from 'MainRoot/util/CLMLocation';
import { validateNonEmpty } from 'MainRoot/util/validationUtil';

import { render, screen, axiosMockAdapter, fireEvent, waitFor } from 'TestRoot/SpecUtil';

const { initialState: rscInitialFileUploadState } = nxFileUploadStateHelpers;
const { initialState: rscInitialState } = nxTextInputStateHelpers;

describe('ImportSbomModal', () => {
  let renderComponent, axiosMock, defaultPreloadedState;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    defaultPreloadedState = {
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
        },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            id: 'testApplicationId',
            name: 'testApplicationName',
          },
        },
        ownerActions: {
          importSbomModal: {
            isModalOpen: true,
            file: rscInitialFileUploadState(null),
            uploadState: null,
            uploadFileProgress: 0,
            requestId: null,
            componentsCount: null,
            vulnerabilitiesCount: null,
            versionId: rscInitialState('', validateNonEmpty),
            submitError: null,
            submitMaskState: null,
            submitMaskMessage: null,
          },
        },
      },
    };

    renderComponent = (preloadedState) =>
      render(<ImportSbomModal />, { preloadedState: { ...defaultPreloadedState, ...preloadedState } });
  });

  it('doesn"t show modal without being open', () => {
    renderComponent({
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
        },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            id: 'testApplicationId',
            name: 'testApplicationName',
          },
        },
        ownerActions: {
          importSbomModal: {
            isModalOpen: false,
            file: rscInitialFileUploadState(null),
            uploadState: null,
            uploadFileProgress: 0,
            requestId: null,
            componentsCount: null,
            vulnerabilitiesCount: null,
            versionId: rscInitialState('', validateNonEmpty),
            submitError: null,
            submitMaskState: null,
            submitMaskMessage: null,
          },
        },
      },
    });
    const title = screen.queryByText('Import SBOM for testApplicationName');
    expect(title).not.toBeInTheDocument();
  });

  it('shows modal with the correct title', () => {
    renderComponent();
    const title = screen.getByText('Import SBOM for testApplicationName');
    expect(title).toBeVisible();
  });

  it('shows uploadFile and buttons on first open', async () => {
    renderComponent();
    const fileUpload = await screen.findByTestId('import-sbom-modal-file-upload');
    expect(fileUpload).toBeInTheDocument();
    const submitButton = await screen.findByRole('button', { name: 'Finish Import' });
    const cancelButton = await screen.findByRole('button', { name: 'Cancel' });
    expect(submitButton).toBeVisible();
    expect(cancelButton).toBeVisible();
  });

  describe('upload file', () => {
    const someFile = [{ name: 'test file' }];
    const str = JSON.stringify(someFile);
    const blob = new Blob([str]);
    const file = new File([blob], 'testFile.json', {
      type: 'application/JSON',
    });

    const fakeFileList = (...files) => {
      const retval = {
        ...files,
        item(i) {
          return files[i];
        },
        length: files.length,
      };

      Object.setPrototypeOf(retval, FileList.prototype);

      return retval;
    };

    const setFileUploadValue = (fileUpload, ...files) => {
      fireEvent.change(fileUpload, {
        target: {
          files: fakeFileList(...files),
        },
      });
    };

    beforeEach(() => {
      axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply((config) => {
        const total = 1024; // mocked file size
        const progress = 0.5;
        if (config.onUploadProgress) {
          config.onUploadProgress({ loaded: total * progress, total });
        }
        return new Promise(() => {});
      });
    });

    it('renders uploaded file progress', async () => {
      renderComponent();

      const fileUpload = await screen.findByTestId('import-sbom-modal-file-upload');
      setFileUploadValue(fileUpload, file);

      expect(screen.getByText('testFile.json')).toBeInTheDocument();
      expect(await screen.getByRole('progressbar')).toBeVisible();
      expect(screen.getByText('Uploading testFile.json file')).toBeVisible();
      await waitFor(async () => expect(screen.getByText('50%')).toBeVisible());
    });

    it('renders post upload details', async () => {
      axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
        requestId: 'requestId',
        componentsCount: 123,
        vulnerabilitiesCount: 456,
      });

      renderComponent();

      const fileUpload = await screen.findByTestId('import-sbom-modal-file-upload');
      setFileUploadValue(fileUpload, file);
      expect(screen.getByText('testFile.json')).toBeInTheDocument();

      await waitFor(async () => {
        expect(await screen.findByRole('textbox', { name: 'Application Name' })).toBeVisible();
        expect(await screen.findByRole('textbox', { name: 'Version Id' })).toBeVisible();
        expect(screen.getByTestId('import-sbom-modal-info-alert').textContent).toBe(
          '123 components and 456 vulnerabilitieswill be included with uploaded file.'
        );
      });
    });

    it('shows error alert with upload issue', async () => {
      axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(500, 'Error Message');
      renderComponent();
      const fileUpload = await screen.findByTestId('import-sbom-modal-file-upload');
      setFileUploadValue(fileUpload, file);
      expect(screen.getByText('testFile.json')).toBeInTheDocument();

      expect(await screen.findByRole('alert')).toBeVisible();
      expect(await screen.findByRole('button', { name: 'Retry' })).toBeVisible();

      expect(await screen.findByText('An error occurred saving data. Error Message')).toBeVisible();
    });
  });
});
