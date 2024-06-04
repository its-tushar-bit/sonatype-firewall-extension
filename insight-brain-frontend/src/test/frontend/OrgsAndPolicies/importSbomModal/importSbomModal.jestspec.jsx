/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { nxFileUploadStateHelpers } from '@sonatype/react-shared-components';

import ImportSbomModal from 'MainRoot/OrgsAndPolicies/importSbomModal/ImportSbomModal';
import { getCommitImportedSbomUrl, getImportSbomUrl } from 'MainRoot/util/CLMLocation';

import { axiosMockAdapter, fireEvent, render, screen, waitFor } from 'TestRoot/SpecUtil';

const { initialState: rscInitialFileUploadState } = nxFileUploadStateHelpers;

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
            componentCount: null,
            vulnerabilityCount: null,
            versionId: '',
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
            componentCount: null,
            vulnerabilityCount: null,
            versionId: '',
            submitError: null,
            submitMaskState: null,
            submitMaskMessage: null,
          },
        },
      },
    });
    const title = screen.queryByText('Import SBOM for Application testApplicationName');
    expect(title).not.toBeInTheDocument();
  });

  it('shows modal with the correct title', () => {
    renderComponent();
    const title = screen.getByText('Import SBOM for Application testApplicationName');
    expect(title).toBeVisible();
  });

  it('shows uploadFile and buttons on first open', async () => {
    renderComponent();
    const fileUpload = await screen.findByTestId('import-sbom-modal-file-upload');
    expect(fileUpload).toBeInTheDocument();
    const submitButton = await screen.findByRole('button', { name: 'Finish Import' });
    expect(submitButton).toHaveClass('disabled');
    const cancelButton = await screen.findByRole('button', { name: 'Cancel' });
    expect(cancelButton).toBeVisible();
  });

  it('closes modal on cancel', async () => {
    renderComponent();

    const cancelButton = await screen.findByRole('button', { name: 'Cancel' });
    fireEvent.click(cancelButton);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
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

      const submitButton = await screen.findByRole('button', { name: 'Finish Import' });
      expect(submitButton).toHaveClass('disabled');
    });

    it('renders post upload details', async () => {
      axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
        requestId: 'requestId',
        sbomSummary: {
          componentCount: 123,
          vulnerabilityCount: 456,
          applicationVersion: '0.1',
        },
      });

      renderComponent();

      const fileUpload = await screen.findByTestId('import-sbom-modal-file-upload');
      setFileUploadValue(fileUpload, file);
      expect(screen.getByText('testFile.json')).toBeInTheDocument();

      const applicationNameInput = await screen.findByRole('textbox', { name: 'Application Name' });
      expect(applicationNameInput).toBeVisible();
      expect(applicationNameInput.value).toBe('testApplicationName');

      const versionIdInput = await screen.findByRole('textbox', { name: 'Version Id' });
      expect(versionIdInput).toBeVisible();
      expect(versionIdInput.value).toBe('0.1');

      expect(screen.getByTestId('import-sbom-modal-info-alert').textContent).toBe(
        '123 components and 456 vulnerabilities will be included with uploaded file.'
      );

      const submitButton = await screen.findByRole('button', { name: 'Finish Import' });
      expect(submitButton).not.toHaveClass('disabled');
    });

    it('shows error alert with upload issue', async () => {
      axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
        requestId: 'requestId',
        sbomSummary: null,
        errorMessage: 'Error Message',
      });
      renderComponent();
      const fileUpload = await screen.findByTestId('import-sbom-modal-file-upload');
      setFileUploadValue(fileUpload, file);
      expect(screen.getByText('testFile.json')).toBeInTheDocument();

      expect(await screen.findByRole('alert')).toBeVisible();
      expect(await screen.findByRole('button', { name: 'Retry' })).toBeVisible();

      expect(await screen.findByText('An error occurred while importing the SBOM file. Error Message')).toBeVisible();

      const submitButton = await screen.findByRole('button', { name: 'Finish Import' });
      expect(submitButton).toHaveClass('disabled');
    });

    it('shows error alert when max sboms limit reached', async () => {
      axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(402, {});
      renderComponent();
      const fileUpload = await screen.findByTestId('import-sbom-modal-file-upload');
      setFileUploadValue(fileUpload, file);
      expect(screen.getByText('testFile.json')).toBeInTheDocument();

      expect(await screen.findByRole('alert')).toBeVisible();
      expect(await screen.findByRole('button', { name: 'Retry' })).toBeVisible();

      expect(
        await screen.findByText(
          'An error occurred while importing the SBOM file. You have reached the maximum limit of SBOM imports allowed. Please delete existing SBOMs or contact support to increase your limit.'
        )
      ).toBeVisible();

      const submitButton = await screen.findByRole('button', { name: 'Finish Import' });
      expect(submitButton).toHaveClass('disabled');
    });
  });

  describe('submit import', () => {
    beforeEach(() => {
      const {
        orgsAndPolicies: {
          ownerActions: { importSbomModal },
        },
      } = defaultPreloadedState;
      importSbomModal.requestId = 'requestIdTest';
      importSbomModal.uploadState = 1;
      importSbomModal.uploadFileProgress = 100;
    });

    it('shows importing mask upon submitting an import', async () => {
      renderComponent();

      fireEvent.click(await screen.findByRole('button', { name: 'Finish Import' }));

      expect(screen.getByRole('status')).toBeVisible();
      expect(screen.getByText('Importing…')).toBeVisible();
    });

    it('shows success mask upon a successful import', async () => {
      axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', 'requestIdTest')).reply(201);
      renderComponent();

      fireEvent.click(await screen.findByRole('button', { name: 'Finish Import' }));

      expect(screen.getByRole('status')).toBeVisible();
      expect(await screen.findByText('Success!')).toBeVisible();
    });

    it('shows error alert with submit issue', async () => {
      axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', 'requestIdTest')).reply(500, 'Test error');
      renderComponent();

      fireEvent.click(await screen.findByRole('button', { name: 'Finish Import' }));

      const alert = await screen.findByRole('alert');
      expect(alert).toBeVisible();
      expect(alert.textContent).toContain('An error occurred while importing the SBOM file.');
      expect(await screen.findByRole('button', { name: 'Retry' })).toBeVisible();
    });

    it('shows error alert when max sboms limit reached', async () => {
      axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', 'requestIdTest')).reply(402, '');
      renderComponent();

      fireEvent.click(await screen.findByRole('button', { name: 'Finish Import' }));

      expect(await screen.findByRole('alert')).toBeVisible();
      expect(await screen.findByRole('button', { name: 'Retry' })).toBeVisible();
      expect(
        await screen.findByText(
          'An error occurred while importing the SBOM file. You have reached the maximum limit of SBOM imports allowed. Please delete existing SBOMs or contact support to increase your limit.'
        )
      ).toBeVisible();
    });
  });
});
