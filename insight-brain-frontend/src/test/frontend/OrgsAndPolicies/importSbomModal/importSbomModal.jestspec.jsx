/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import {
  nxFileUploadStateHelpers,
  SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS,
  nxTextInputStateHelpers,
} from '@sonatype/react-shared-components';

import ImportSbomModal from 'MainRoot/OrgsAndPolicies/importSbomModal/ImportSbomModal';
import ToastContainer from 'MainRoot/toastContainer/ToastContainer';
import { IMPORT_STATE } from 'MainRoot/OrgsAndPolicies/importSbomModal/importSbomModalSlice';
import { getCommitImportedSbomUrl, getImportSbomUrl, getSbomSummaryUrl } from 'MainRoot/util/CLMLocation';

import { axiosMockAdapter, fireEvent, render, screen } from 'TestRoot/SpecUtil';
import { BASE_URL } from 'MainRoot/util/urlUtil';

const EVALUATION_POLLING_FREQUENCY = 500;

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
            publicId: 'testApplicationPublicId',
            name: 'testApplicationName',
          },
        },
        ownerActions: {
          importSbomModal: {
            isModalOpen: true,
            importState: IMPORT_STATE.INITIAL,
            uploadProgress: 0,
            errorMessage: null,
            fileInputState: nxFileUploadStateHelpers.initialState(null),
            sbomSummary: {
              totalComponents: null,
              totalVulnerabilities: null,
            },
          },
        },
      },
      toast: {
        toasts: [],
        toastIdInc: 0,
      },
    };

    renderComponent = (preloadedState) =>
      render(
        <>
          <ToastContainer />
          <ImportSbomModal />
        </>,
        { preloadedState: { ...defaultPreloadedState, ...preloadedState } }
      );
  });

  describe('show/hide Modal', () => {
    it('should hide modal when it is not open', () => {
      renderComponent({
        orgsAndPolicies: {
          ownerActions: {
            importSbomModal: {
              isModalOpen: false,
            },
          },
        },
      });
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('should show the modal when it is open', () => {
      renderComponent({
        orgsAndPolicies: {
          ownerActions: {
            importSbomModal: {
              isModalOpen: true,
            },
          },
        },
      });
      expect(screen.queryByRole('dialog')).toBeInTheDocument();
    });

    it('closes the modal on cancel', () => {
      async () => {
        renderComponent();
        expect(screen.queryByRole('dialog')).toBeInTheDocument();
        const cancelButton = await screen.findByRole('button', { name: 'Cancel' });
        fireEvent.click(cancelButton);
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      };
    });
  });

  describe('Modal Content', () => {
    const createTestFile = () => {
      const blob = new Blob([JSON.stringify([{ name: 'file-content' }])]);
      return new File([blob], 'test-file.json', {
        type: 'application/JSON',
      });
    };

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

    describe('Initial State', () => {
      it('shows the correct content', () => {
        renderComponent();
        expect(screen.getByText('Import File for Application testApplicationName')).toBeInTheDocument();
        expect(document.querySelector('input[type=file]')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Import/i })).toBeInTheDocument();
      });
    });

    describe('Upload form validation', () => {
      it('shows an error if the Import button is clicked when a file is not selected', async () => {
        const user = userEvent.setup();
        renderComponent();
        const importButton = screen.getByRole('button', { name: /Import/i });
        const fileUpload = document.querySelector('input[type=file]');

        expect(fileUpload).not.toHaveAccessibleErrorMessage();
        expect(importButton).toBeEnabled();

        await user.click(importButton);

        expect(fileUpload).toHaveAccessibleErrorMessage('This field is required!');
        expect(fileUpload).toHaveAttribute('aria-invalid', 'true');
        expect(screen.getByRole('alert', { name: /validation error/ })).toHaveTextContent(
          'There were validation errors. Please select a file to upload.'
        );

        setFileUploadValue(fileUpload, createTestFile());

        expect(fileUpload).not.toHaveAccessibleErrorMessage();
        expect(fileUpload).not.toHaveAttribute('aria-invalid', 'true');
        expect(screen.getByRole('button', { name: /Import/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Import/i })).toBeEnabled();
      });
    });

    describe('Uploading', () => {
      it('shows the correct content', async () => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply((config) => {
          const total = 1024; // Mocked file size
          const progress = 0.5;
          if (config.onUploadProgress) {
            config.onUploadProgress({ loaded: total * progress, total });
          }
          return new Promise(() => {});
        });

        renderComponent();

        setFileUploadValue(document.querySelector('input[type=file]'), createTestFile());
        const importButton = screen.getByRole('button', { name: /Import/i });
        fireEvent.click(importButton);

        expect(await screen.findByRole('progressbar')).toBeVisible();
        expect(screen.getByText('Import in progress…')).toBeVisible();
        expect(screen.getByText('Importing [test-file.json]…')).toBeVisible();

        expect(screen.queryByRole('button', { name: /Cancel/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Close/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Import/i })).not.toBeInTheDocument();
      });
    });

    describe('VERSION_CONFIRM/COMMITTING', () => {
      beforeEach(() => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
          sbomSummary: {
            specification: 'CycloneDx',
            format: 'json',
            version: '1.4',
            componentCount: 1,
            vulnerabilityCount: 2,
            applicationName: null,
            applicationVersion: '1.2.3',
            serialNumber: 'urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79',
            creationDetails: null,
          },
          savedVersion: '1.2.3_2024',
          scanType: 'SBOM',
          errorMessage: null,
        });

        renderComponent();

        // Set up initial state by uploading file
        setFileUploadValue(document.querySelector('input[type=file]'), createTestFile());
        const initialImportButton = screen.getByRole('button', { name: /import/i });
        fireEvent.click(initialImportButton);
      });

      it('shows the version confirmation page with correct content', async () => {
        expect(screen.getByRole('dialog')).toHaveAccessibleName('File Uploaded. Import in Progress…');

        const applicationNameLabel = (await screen.findByText('Application Name')).closest('dt');
        expect(applicationNameLabel).toBeVisible();
        expect(applicationNameLabel.nextElementSibling.tagName).toBe('DD');
        expect(applicationNameLabel.nextElementSibling).toHaveTextContent('testApplicationName');

        const versionInput = screen.getByRole('textbox', { name: 'Application Version' });
        expect(versionInput).toHaveValue('1.2.3_2024');

        expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Import/i })).toBeInTheDocument();
      });

      it('shows validation error when trying to import with empty version', async () => {
        const versionInput = screen.getByPlaceholderText('Enter version');
        const importButton = screen.getByRole('button', { name: /Import/i });

        fireEvent.change(versionInput, { target: { value: '' } });
        fireEvent.click(importButton);

        expect(
          screen.getByText('There were validation errors. Invalid version input. Please enter a valid version format.')
        );
        expect(screen.getByText('Must be non-empty')).toBeInTheDocument();
      });

      it('allows retry with new version after conflict', async () => {
        axiosMock
          .onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024'))
          .replyOnce(409, 'Version 1.2.3_2024 already exists');

        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024', '2.0.0')).replyOnce(201, {});

        let versionInput = screen.getByRole('textbox', { name: 'Application Version' });
        expect(versionInput).toHaveValue('1.2.3_2024');

        let importButton = screen.getByRole('button', { name: /Import/i });

        fireEvent.click(importButton);
        expect(
          await screen.findByText('An error occurred saving data. Version 1.2.3_2024 already exists')
        ).toBeInTheDocument();

        versionInput = screen.getByRole('textbox', { name: 'Application Version' });
        fireEvent.change(versionInput, { target: { value: '2.0.0' } });
        importButton = screen.getByRole('button', { name: /Import/i });
        fireEvent.click(importButton);

        expect(
          screen.queryByText('An error occurred saving data. Version 1.2.3_2024 already exists')
        ).not.toBeInTheDocument();

        //ensure we are on the evaluation in progress page
        expect(await screen.findByText('File Imported')).toBeInTheDocument();
        expect(await screen.findByText('Evaluating…')).toBeInTheDocument();
      });

      it('allows user to override version', async () => {
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024', '2.0.0')).replyOnce(201, {});

        await screen.findByText('File Uploaded. Import in Progress…');

        const versionInput = screen.getByPlaceholderText('Enter version');
        expect(versionInput).toHaveValue('1.2.3_2024');

        fireEvent.change(versionInput, { target: { value: '2.0.0' } });
        expect(versionInput).toHaveValue('2.0.0');

        const importButton = screen.getByRole('button', { name: 'Import' });
        fireEvent.click(importButton);

        //ensure we are on the evaluation in progress page
        expect(await screen.findByText('File Imported')).toBeInTheDocument();
        expect(await screen.findByText('Evaluating…')).toBeInTheDocument();

        expect(axiosMock.history.post).toHaveLength(2);
        const commitFileCall = axiosMock.history.post[1];
        expect(commitFileCall.url).toEqual(
          '/rest/sbom/commit/testApplicationId/1.2.3_2024?applicationVersionOverride=2.0.0'
        );
      });

      it('closes modal when Cancel is clicked', () => {
        const cancelButton = screen.getByRole('button', { name: /Cancel/i });

        fireEvent.click(cancelButton);

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });

    describe('Evaluation in Progress', () => {
      beforeEach(async () => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
          sbomSummary: {
            specification: 'CycloneDx',
            format: 'json',
            version: '1.4',
            componentCount: 1,
            vulnerabilityCount: 2,
            applicationName: 'testApplicationName',
            applicationVersion: '1.2.3',
            serialNumber: 'urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79',
            creationDetails: null,
          },
          savedVersion: '1.2.3_2024',
          scanType: 'SBOM',
          errorMessage: null,
        });
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024')).replyOnce(201, {
          statusUrl: 'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020',
        });

        renderComponent();

        // uploading the file
        setFileUploadValue(document.querySelector('input[type=file]'), createTestFile());
        const initialImportButton = await screen.findByRole('button', { name: /Import/i });
        fireEvent.click(initialImportButton);

        // confirming the version
        const importButton = await screen.findByRole('button', { name: /Import/i });
        fireEvent.click(importButton);
      });

      it('shows the evaluation in progress page with the correct content', async () => {
        expect(await screen.findByRole('dialog', { name: 'File Imported' })).toBeVisible();

        const evaluatingText = await screen.findByText('Evaluating…');
        expect(evaluatingText).toBeVisible();

        const evaluatingDescText = await screen.findByText(
          'Feel free to close this modal and continue working.' +
            ' Your evaluation will continue to process in the background and should be done within a few minutes.'
        );
        expect(evaluatingDescText).toBeVisible();

        expect(screen.getByRole('button', { name: /Close/i })).toBeInTheDocument();
      });

      it('polls the evaluation status URL until it returns 200', async () => {
        const evaluationStatusUri =
          'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020';
        const url = BASE_URL + `/${evaluationStatusUri}`;

        // mocking 2 polling responses
        axiosMock.onGet(url).replyOnce(404);
        axiosMock.onGet(url).replyOnce(200, {});

        // call to sbom summary after polling is successful
        axiosMock.onGet(getSbomSummaryUrl('testApplicationId', '1.2.3_2024')).reply(200, {
          none: 0,
          low: 0,
          medium: 0,
          high: 4,
          critical: 2,
        });

        // shows evaluation in progress page
        expect(await screen.findByRole('dialog', { name: 'File Imported' })).toBeVisible();

        // shows evaluation complete page
        expect(
          await screen.findByRole(
            'dialog',
            { name: 'Evaluation Complete' },
            { timeout: EVALUATION_POLLING_FREQUENCY * 3 }
          )
        ).toBeVisible();
      });

      it('shows the upload page with an error alert on evaluation error', async () => {
        const evaluationStatusUri =
          'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020';
        const url = BASE_URL + `/${evaluationStatusUri}`;

        // mocking 2 polling responses
        axiosMock.onGet(url).replyOnce(404);
        axiosMock.onGet(url).replyOnce(500, {
          errorMessage: 'An error occurred in the evaluation process',
        });

        // shows evaluation in progress page
        expect(await screen.findByRole('dialog', { name: 'File Imported' })).toBeVisible();

        expect(
          await screen.findByRole(
            'dialog',
            { name: 'Import File for Application testApplicationName' },
            { timeout: EVALUATION_POLLING_FREQUENCY * 3 }
          )
        ).toBeVisible();

        expect(
          await screen.findByText(
            'We were unable to process your SBOM: An error occurred in the evaluation process. Please re-import your SBOM.'
          )
        ).toBeInTheDocument();
      });
    });

    describe('Evaluation Complete', () => {
      beforeEach(async () => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
          sbomSummary: {
            specification: 'CycloneDx',
            format: 'json',
            version: '1.4',
            componentCount: 1,
            vulnerabilityCount: 2,
            applicationName: null,
            applicationVersion: '1.2.3',
            serialNumber: 'urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79',
            creationDetails: null,
          },
          savedVersion: '1.2.3_2024',
          scanType: 'SBOM',
          errorMessage: null,
        });
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024')).replyOnce(201, {
          statusUrl: 'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020',
        });

        // mocking polling response
        axiosMock
          .onGet(BASE_URL + 'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020')
          .reply(200, {});

        // call to sbom summary after polling is successful
        axiosMock.onGet(getSbomSummaryUrl('testApplicationId', '1.2.3_2024')).reply(200, {
          none: 0,
          low: 0,
          medium: 0,
          high: 4,
          critical: 2,
        });

        renderComponent();

        // uploading the file
        setFileUploadValue(document.querySelector('input[type=file]'), createTestFile());
        const initialImportButton = await screen.findByRole('button', { name: /Import/i });
        fireEvent.click(initialImportButton);

        // confirming the version
        const importButton = await screen.findByRole('button', { name: /Import/i });
        fireEvent.click(importButton);

        // show the evaluation in progress page
        await screen.findByRole('dialog', { name: 'File Imported' });
      });

      it('shows the evaluation complete page with correct content', async () => {
        expect(
          await screen.findByRole(
            'dialog',
            { name: 'Evaluation Complete' },
            { timeout: EVALUATION_POLLING_FREQUENCY * 3 }
          )
        ).toBeVisible();
        expect(await screen.findByText('Success!')).toBeVisible();
        const evaluatingDescText = await screen.findByText('Your SBOM has been evaluated and is ready for viewing.');
        expect(evaluatingDescText).toBeVisible();
        expect(screen.getByRole('button', { name: /Close/i })).toBeInTheDocument();
      });

      it(`closes the evaluation complete page after ${SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS} ms`, async () => {
        expect(
          await screen.findByRole(
            'dialog',
            { name: 'Evaluation Complete' },
            { timeout: EVALUATION_POLLING_FREQUENCY * 3 }
          )
        ).toBeVisible();

        // shows import complete page
        expect(
          await screen.findByRole(
            'dialog',
            { name: 'Import Complete' },
            { timeout: SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS }
          )
        ).toBeVisible();
      });

      it('closes the modal when the close button is pressed', async () => {
        expect(
          await screen.findByRole(
            'dialog',
            { name: 'Evaluation Complete' },
            { timeout: EVALUATION_POLLING_FREQUENCY * 3 }
          )
        ).toBeVisible();
        const closeButton = await screen.findByRole('button', { name: 'Close' });
        expect(closeButton).toBeVisible();
        fireEvent.click(closeButton);
        expect(screen.queryByRole('dialog', { name: 'Evaluation Complete' })).not.toBeInTheDocument();
      });
    });

    describe('SBOM Summary', () => {
      beforeEach(async () => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
          sbomSummary: {
            specification: 'CycloneDx',
            format: 'json',
            version: '1.4',
            componentCount: 1,
            vulnerabilityCount: 2,
            applicationName: null,
            applicationVersion: '1.2.3',
            serialNumber: 'urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79',
            creationDetails: null,
          },
          savedVersion: '1.2.3_2024',
          scanType: 'SBOM',
          errorMessage: null,
        });
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024')).replyOnce(201, {
          statusUrl: 'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020',
        });

        // mocking polling response
        axiosMock
          .onGet(BASE_URL + 'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020')
          .reply(200, {});

        // call to sbom summary after polling is successful
        axiosMock.onGet(getSbomSummaryUrl('testApplicationId', '1.2.3_2024')).reply(200, {
          none: 0,
          low: 0,
          medium: 0,
          high: 4,
          critical: 2,
        });

        renderComponent();

        // uploading the file
        setFileUploadValue(document.querySelector('input[type=file]'), createTestFile());
        const initialImportButton = await screen.findByRole('button', { name: /Import/i });
        fireEvent.click(initialImportButton);

        // confirming the version
        const importButton = await screen.findByRole('button', { name: /Import/i });
        fireEvent.click(importButton);

        // show the evaluation in progress page
        await screen.findByRole('dialog', { name: 'File Imported' }, { timeout: EVALUATION_POLLING_FREQUENCY * 2 });

        // show the evaluation complete page
        expect(
          await screen.findByRole(
            'dialog',
            { name: 'Evaluation Complete' },
            { timeout: EVALUATION_POLLING_FREQUENCY * 3 }
          )
        ).toBeVisible();
      });

      it('shows post upload details for SBOM', async () => {
        expect(await screen.findByText('Import Complete')).toBeInTheDocument();
        const totalComponentsLabel = (await screen.findByText('Total Components:')).closest('dt');
        expect(totalComponentsLabel).toBeVisible();
        expect(totalComponentsLabel.nextElementSibling).toHaveTextContent('1');

        const totalVulnerabilitiesLabel = (await screen.findByText('Total Vulnerabilities:')).closest('dt');
        expect(totalVulnerabilitiesLabel).toBeVisible();

        const criticalLabel = (await screen.findByText('Critical')).closest('span');
        expect(criticalLabel).toBeVisible();
        expect(criticalLabel.nextElementSibling).toHaveTextContent('2');

        const severeLabel = (await screen.findByText('Severe')).closest('span');
        expect(severeLabel).toBeVisible();
        expect(severeLabel.nextElementSibling).toHaveTextContent('4');

        const moderateLabel = (await screen.findByText('Moderate')).closest('span');
        expect(moderateLabel).toBeVisible();
        expect(moderateLabel.nextElementSibling).toHaveTextContent('0');

        const lowLabel = (await screen.findByText('Low')).closest('span');
        expect(lowLabel).toBeVisible();
        expect(lowLabel.nextElementSibling).toHaveTextContent('0');

        const applicationNameLabel = screen.getByText('Application Name').closest('dt');
        expect(applicationNameLabel).toBeVisible();
        expect(applicationNameLabel.nextElementSibling).toHaveTextContent('testApplicationName');

        const applicationVersionLabel = screen.getByText('Application Version').closest('dt');
        expect(applicationVersionLabel).toBeVisible();
        expect(applicationVersionLabel.nextElementSibling).toHaveTextContent('1.2.3_2024');

        expect(screen.queryByText('View SBOM')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Close' })).toBeInTheDocument();
      });

      it('it closes the modal when the close button is pressed', async () => {
        expect(await screen.findByRole('dialog', { name: 'Import Complete' })).toBeVisible();
        const closeButton = await screen.findByRole('button', { name: 'Close' });
        expect(closeButton).toBeVisible();
        fireEvent.click(closeButton);
        expect(screen.queryByRole('dialog', { name: 'Import Complete' })).not.toBeInTheDocument();
      });
    });

    describe('Binary Summary', () => {
      beforeEach(async () => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
          savedVersion: '1.2.3_2024',
          sbomSummary: null,
          scanType: 'BINARY',
          errorMessage: null,
        });
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024')).replyOnce(201, {
          statusUrl: 'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020',
        });

        // mocking polling response
        axiosMock
          .onGet(BASE_URL + 'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020')
          .reply(200, {});

        // call to sbom summary after polling is successful
        axiosMock.onGet(getSbomSummaryUrl('testApplicationId', '1.2.3_2024')).reply(200, {
          none: 0,
          low: 0,
          medium: 0,
          high: 4,
          critical: 2,
        });

        renderComponent();

        // uploading the file
        setFileUploadValue(document.querySelector('input[type=file]'), createTestFile());
        const initialImportButton = await screen.findByRole('button', { name: /Import/i });
        fireEvent.click(initialImportButton);

        // confirming the version
        const importButton = await screen.findByRole('button', { name: /Import/i });
        fireEvent.click(importButton);

        // show the evaluation in progress page
        await screen.findByRole('dialog', { name: 'File Imported' }, { timeout: EVALUATION_POLLING_FREQUENCY * 2 });

        // show the evaluation complete page
        expect(
          await screen.findByRole(
            'dialog',
            { name: 'Evaluation Complete' },
            { timeout: EVALUATION_POLLING_FREQUENCY * 3 }
          )
        ).toBeVisible();
      });

      it('shows post upload details for Binary', async () => {
        expect(await screen.findByText('Import Complete')).toBeInTheDocument();

        const fileNameLabel = screen.getByText('File').closest('dt');
        expect(fileNameLabel).toBeVisible();
        expect(fileNameLabel.parentElement.querySelector('dd')).toHaveTextContent('test-file.json');

        const applicationNameLabel = screen.getByText('Application Name').closest('dt');
        expect(applicationNameLabel).toBeVisible();
        expect(applicationNameLabel.nextElementSibling).toHaveTextContent('testApplicationName');

        const applicationVersionLabel = screen.getByText('Application Version').closest('dt');
        expect(applicationVersionLabel).toBeVisible();
        expect(applicationVersionLabel.nextElementSibling).toHaveTextContent('1.2.3_2024');

        expect(screen.queryByText('View SBOM')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Close' })).toBeInTheDocument();
      });

      it('it closes the modal when the close button is pressed', async () => {
        expect(await screen.findByRole('dialog', { name: 'Import Complete' })).toBeVisible();
        const closeButton = await screen.findByRole('button', { name: 'Close' });
        expect(closeButton).toBeVisible();
        fireEvent.click(closeButton);
        expect(screen.queryByRole('dialog', { name: 'Import Complete' })).not.toBeInTheDocument();
      });
    });

    describe('Result toast', () => {
      beforeEach(async () => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
          sbomSummary: {
            specification: 'CycloneDx',
            format: 'json',
            version: '1.4',
            componentCount: 1,
            vulnerabilityCount: 2,
            applicationName: null,
            applicationVersion: '1.2.3',
            serialNumber: 'urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79',
            creationDetails: null,
          },
          savedVersion: '1.2.3_2024',
          scanType: 'SBOM',
          errorMessage: null,
        });

        // call to sbom summary after polling is successful
        axiosMock.onGet(getSbomSummaryUrl('testApplicationId', '1.2.3_2024')).reply(200, {
          none: 0,
          low: 0,
          medium: 0,
          high: 4,
          critical: 2,
        });

        renderComponent();

        // uploading the file
        setFileUploadValue(document.querySelector('input[type=file]'), createTestFile());
        const initialImportButton = await screen.findByRole('button', { name: /Import/i });
        fireEvent.click(initialImportButton);
      });

      it('shows successful result toast when modal is closed', async () => {
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024')).replyOnce(201, {
          statusUrl: 'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020',
        });

        const evaluationStatusUri =
          'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020';
        const url = BASE_URL + `/${evaluationStatusUri}`;

        // mocking polling response
        axiosMock.onGet(url).replyOnce(404);
        axiosMock.onGet(url).replyOnce(404);
        axiosMock.onGet(url).replyOnce(200);

        // confirming the version
        const importButton = await screen.findByRole('button', { name: /Import/i });
        fireEvent.click(importButton);

        // show the evaluation in progress page
        await screen.findByRole('dialog', { name: 'File Imported' }, { timeout: EVALUATION_POLLING_FREQUENCY * 2 });

        const closeButton = await screen.findByRole('button', { name: 'Close' });
        expect(closeButton).toBeVisible();
        fireEvent.click(closeButton);

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

        const toast = await screen.findByRole('alert', {}, { timeout: EVALUATION_POLLING_FREQUENCY * 4 });
        expect(toast).toBeVisible();
        expect(toast).toHaveTextContent(
          'SBOM 1.2.3_2024 from application testApplicationPublicId is now ready for review in the SBOM table.'
        );
      });

      it('shows commit error result toast when modal is closed', async () => {
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024')).reply(() => {
          return new Promise((resolve) => {
            setTimeout(() => {
              resolve([400], {});
            }, 1000); // 2-second delay
          });
        });

        // confirming the version
        const importButton = await screen.findByRole('button', { name: /Import/i });
        fireEvent.click(importButton);

        // show the evaluation in progress page
        await screen.findByRole('dialog', { name: 'File Imported' }, { timeout: EVALUATION_POLLING_FREQUENCY * 2 });

        const closeButton = await screen.findByRole('button', { name: 'Close' });
        expect(closeButton).toBeVisible();
        fireEvent.click(closeButton);
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

        const toast = await screen.findByRole('alert', {}, { timeout: EVALUATION_POLLING_FREQUENCY * 4 });
        expect(toast).toBeVisible();
        expect(toast).toHaveTextContent(
          'SBOM 1.2.3_2024 evaluation from application testApplicationPublicId failed: Error 400.'
        );
      });

      it('shows evaluation error result toast when modal is closed', async () => {
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024')).replyOnce(201, {
          statusUrl: 'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020',
        });

        const evaluationStatusUri =
          'api/v2/sbom/applications/testApplicationId/status/2a16f12582ab4226acd883780f4f1020';
        const url = BASE_URL + `/${evaluationStatusUri}`;

        // mocking polling response
        axiosMock.onGet(url).replyOnce(404);
        axiosMock.onGet(url).replyOnce(404);
        axiosMock.onGet(url).replyOnce(400);

        // confirming the version
        const importButton = await screen.findByRole('button', { name: /Import/i });
        fireEvent.click(importButton);

        // show the evaluation in progress page
        await screen.findByRole('dialog', { name: 'File Imported' }, { timeout: EVALUATION_POLLING_FREQUENCY * 2 });

        const closeButton = await screen.findByRole('button', { name: 'Close' });
        expect(closeButton).toBeVisible();

        fireEvent.click(closeButton);
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

        const toast = await screen.findByRole('alert', {}, { timeout: EVALUATION_POLLING_FREQUENCY * 4 });
        expect(toast).toBeVisible();
        expect(toast).toHaveTextContent(
          'SBOM 1.2.3_2024 evaluation from application testApplicationPublicId failed: Error 400.'
        );
      });
    });

    describe('Error', () => {
      it('should show an error message when an error is generated prior to starting the validation', () => {
        // We're not testing mock responses here, due to js-dom FileList error with NxFileUpload.
        // When NxFileUpload is initially mounted with a fake "FileList" passed into the files prop, in js-dom, it throws an error:
        // Failed to set the 'files' property on 'HTMLInputElement': The provided value is not of type 'FileList'
        // This does not occur on other implementations because they were initially mounted with a null value.
        // So far there is no way to construct a proper mock "real" FileList object (DataTransfer is not supported) in js-dom.
        // Instead, we will be testing this in the functional tests.
        renderComponent({
          orgsAndPolicies: {
            ownerActions: {
              importSbomModal: {
                isModalOpen: true,
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
              },
            },
          },
        });

        expect(screen.getByRole('dialog', { name: 'Error Importing SBOM' })).toBeInTheDocument();
        const alert = screen.getByRole('alert');
        expect(alert).toBeInTheDocument();
        expect(alert).toHaveTextContent('Something went wrong.');
        expect(screen.queryByRole('button', { name: 'Copy to Clipboard' })).not.toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: 'Validation Error Details' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
        const importButton = screen.getByRole('button', { name: 'Import' });
        expect(importButton).toBeInTheDocument();
        expect(importButton).toBeDisabled();
      });

      it('should show an alert message of warning type when there are ignorable validation errors', () => {
        renderComponent({
          orgsAndPolicies: {
            ownerActions: {
              importSbomModal: {
                isModalOpen: true,
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
                validationErrors: ['error reason 1', 'error reason 2'],
                isSkipValidation: false,
                isValidationErrorIgnorable: true,
              },
            },
          },
        });

        expect(screen.getByRole('dialog', { name: 'Your SBOM failed validation' })).toBeInTheDocument();
        const alert = screen.getByRole('alert');
        expect(alert).toBeInTheDocument();
        expect(alert).toHaveTextContent('Something went wrong.');
        expect(alert).toHaveClass('nx-alert--warning');
        const skipCheckbox = screen.getByText('Skip validation and import anyway');
        expect(skipCheckbox).toBeInTheDocument();
        expect(skipCheckbox).not.toBeChecked();
        expect(screen.queryByRole('button', { name: 'Copy to Clipboard' })).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: 'Validation Error Details' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
        const importButton = screen.getByRole('button', { name: 'Import Anyway' });
        expect(importButton).toBeInTheDocument();
        expect(importButton).toBeDisabled();
      });

      it('should show an alert message of error type when validation errors are not ignorable', () => {
        renderComponent({
          orgsAndPolicies: {
            ownerActions: {
              importSbomModal: {
                isModalOpen: true,
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
                validationErrors: ['error reason 1', 'error reason 2'],
                isSkipValidation: false,
                isValidationErrorIgnorable: false,
              },
            },
          },
        });

        expect(screen.getByRole('dialog', { name: 'Your SBOM failed validation' })).toBeInTheDocument();
        const alert = screen.getByRole('alert');
        expect(alert).toBeInTheDocument();
        expect(alert).toHaveTextContent('Something went wrong.');
        expect(alert).toHaveClass('nx-alert--error');
        expect(screen.queryByRole('button', { name: 'Copy to Clipboard' })).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: 'Validation Error Details' })).toBeInTheDocument();
        expect(screen.queryByText('Skip validation and import anyway')).not.toBeInTheDocument();
        const cancelButton = screen.getByRole('button', { name: 'Cancel' });
        expect(cancelButton).toBeInTheDocument();
        expect(cancelButton).toBeEnabled();
        const importButton = screen.getByRole('button', { name: 'Import' });
        expect(importButton).toBeInTheDocument();
        expect(importButton).toHaveClass('disabled');
      });

      it('should show the list of errors', () => {
        renderComponent({
          orgsAndPolicies: {
            ownerActions: {
              importSbomModal: {
                isModalOpen: true,
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
                validationErrors: ['error reason 1', 'error reason 2'],
                isSkipValidation: false,
                isValidationErrorIgnorable: false,
              },
            },
          },
        });

        expect(screen.getByRole('dialog', { name: 'Your SBOM failed validation' })).toBeInTheDocument();
        const alert = screen.getByRole('alert');
        expect(alert).toBeInTheDocument();
        expect(alert).toHaveTextContent('Something went wrong.');
        expect(screen.getByRole('button', { name: 'Copy to Clipboard' })).toBeInTheDocument();
        const validationErrors = screen.getByRole('textbox', { name: 'Validation Error Details' });
        expect(validationErrors).toBeInTheDocument();
        expect(validationErrors).toHaveValue('• error reason 1\n• error reason 2');
        expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
      });

      it('should copy the list of errors when clicking the copy to clipboard button', async () => {
        renderComponent({
          orgsAndPolicies: {
            ownerActions: {
              importSbomModal: {
                isModalOpen: true,
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
                validationErrors: ['error reason 1', 'error reason 2'],
                isSkipValidation: false,
              },
            },
          },
        });
        const user = userEvent.setup();
        const copyToClipboardButton = screen.getByRole('button', { name: 'Copy to Clipboard' });
        expect(copyToClipboardButton).toBeInTheDocument();

        await user.click(copyToClipboardButton);

        const clipboardText = await navigator.clipboard.readText();
        expect(clipboardText).toEqual('• error reason 1\n• error reason 2');
      });

      it('should allow/disallow import if skipValidation selected when there are ignorable validation errors', () => {
        renderComponent({
          orgsAndPolicies: {
            ownerActions: {
              importSbomModal: {
                isModalOpen: true,
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
                validationErrors: ['error reason 1', 'error reason 2'],
                isSkipValidation: false,
                isValidationErrorIgnorable: true,
              },
            },
          },
        });

        const importButton = screen.getByRole('button', { name: 'Import Anyway' });
        expect(importButton).toBeInTheDocument();
        expect(importButton).toBeDisabled();

        const checkbox = screen.getByText('Skip validation and import anyway');
        expect(checkbox).toBeInTheDocument();
        expect(checkbox).not.toBeChecked();

        fireEvent.click(checkbox);

        const importButtonUpdate1 = screen.getByRole('button', { name: 'Import Anyway' });
        expect(importButtonUpdate1).toBeInTheDocument();
        expect(importButtonUpdate1).toBeEnabled();

        fireEvent.click(checkbox);

        const importButtonUpdate2 = screen.getByRole('button', { name: 'Import Anyway' });
        expect(importButtonUpdate2).toBeInTheDocument();
        expect(importButtonUpdate2).toBeDisabled();
      });

      it('should show a tooltip in import button when there are non ignorable validation errors', async () => {
        const user = userEvent.setup();
        renderComponent({
          orgsAndPolicies: {
            ownerActions: {
              importSbomModal: {
                isModalOpen: true,
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
                validationErrors: ['error reason 1', 'error reason 2'],
                isSkipValidation: false,
                isValidationErrorIgnorable: false,
              },
            },
          },
        });

        const importButton = screen.getByText('Import');
        expect(importButton).toBeInTheDocument();
        await user.hover(importButton);

        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent(
          'Import cannot proceed due to a critical error in the file. Please correct the file and try again.'
        );
      });

      it('should execute import if skipValidation selected when there are ignorable validation errors', async () => {
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024')).reply(201, {});

        const file = createTestFile();
        renderComponent({
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
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
                validationErrors: ['error reason 1', 'error reason 2'],
                isSkipValidation: false,
                isValidationErrorIgnorable: true,
                scanType: 'SBOM',
                isValid: false,
                fileInputState: nxFileUploadStateHelpers.initialState(fakeFileList(file)),
                uploadProgress: 0,
                sbomSummary: {
                  specification: 'CycloneDx',
                  format: 'json',
                  version: '1.4',
                  totalComponents: 1,
                  totalVulnerabilities: 2,
                  applicationName: 'Application Name',
                  applicationVersion: '1.2.3',
                  serialNumber: 'urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79',
                  creationDetails: null,
                },
                savedVersion: '1.2.3_2024',
                versionTextInput: nxTextInputStateHelpers.initialState('1.2.3_2024'),
              },
            },
          },
        });

        const checkbox = await screen.getByText('Skip validation and import anyway');
        expect(checkbox).toBeInTheDocument();
        expect(checkbox).not.toBeChecked();

        fireEvent.click(checkbox);

        const importButton = screen.getByRole('button', { name: 'Import Anyway' });
        expect(importButton).toBeInTheDocument();
        expect(importButton).toBeEnabled();

        fireEvent.click(importButton);

        //version confirm page
        expect(await screen.findByText('File Uploaded. Import in Progress…')).toBeInTheDocument();

        const confirmImportButton = screen.getByRole('button', { name: /import/i });
        fireEvent.click(confirmImportButton);

        expect(await screen.findByRole('dialog', { name: 'File Imported' })).toBeVisible();
      });
    });
  });
});
