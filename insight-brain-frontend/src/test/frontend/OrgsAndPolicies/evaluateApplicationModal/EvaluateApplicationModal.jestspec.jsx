/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter, fireEvent } from 'TestRoot/SpecUtil';
import EvaluateApplicationModal from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/EvaluateApplicationModal';
import { nxFileUploadStateHelpers } from '@sonatype/react-shared-components';
import { getCliStageUrl, getBundleUploadUrl } from 'MainRoot/util/CLMLocation';

const { initialState: rscInitialFileUploadState } = nxFileUploadStateHelpers;

describe('EvaluateApplicationModal', () => {
  let renderComponent, axiosMock, defaultPreloadedState;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    defaultPreloadedState = {
      productFeatures: {
        productFeatures: {
          notifications: true,
        },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            publicId: 'testApplicationPublicID',
          },
        },
        ownerActions: {
          evaluateApplication: {
            isEvaluationModalOpen: true,
            isStatusModalOpen: false,
            isValid: false,
            loading: false,
            loadError: null,
            submitError: null,
            stages: [],
            selectedStageId: null,
            notify: 'true',
            uploadFileProgress: 0,
            isUploadingFile: false,
            file: rscInitialFileUploadState(null),
            evaluationStatus: {
              currentStep: 1,
              totalSteps: 1,
              currentStepName: 'Uploading',
              scanId: '',
              error: null,
            },
          },
        },
      },
    };

    renderComponent = (preloadedState) =>
      render(<EvaluateApplicationModal />, { preloadedState: { ...defaultPreloadedState, ...preloadedState } });
  });

  it('doesn"t show modal without being open', () => {
    renderComponent({
      productFeatures: {
        productFeatures: {
          notifications: true,
        },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            publicId: 'testApplicationPublicID',
          },
        },
        ownerActions: {
          evaluateApplication: {
            isEvaluationModalOpen: false,
            isStatusModalOpen: false,
            isValid: false,
            loading: false,
            loadError: null,
            submitError: null,
            stages: [],
            selectedStageId: null,
            notify: 'true',
            file: rscInitialFileUploadState(null),
            uploadFileProgress: 0,
            isUploadingFile: false,
            evaluationStatus: {
              currentStep: 1,
              totalSteps: 1,
              currentStepName: 'Uploading',
              scanId: '',
              error: null,
            },
          },
        },
      },
    });
    const evaluateAppModalTitle = screen.queryByText('Evaluate a File');
    expect(evaluateAppModalTitle).not.toBeInTheDocument();
  });

  it('shows modal with the correct title and loading spinner', () => {
    renderComponent();
    const evaluateAppModalTitle = screen.getByText('Evaluate a File');
    expect(evaluateAppModalTitle).toBeVisible();
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('fetches stages, when opening modal', () => {
    renderComponent();
    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getCliStageUrl());
  });

  describe('successfully fetched stages', () => {
    beforeEach(() => {
      axiosMock.onGet(getCliStageUrl()).reply(200, [
        { stageTypeId: 'develop', stageName: 'Develop' },
        { stageTypeId: 'source', stageName: 'Source' },
        { stageTypeId: 'build', stageName: 'Build' },
        { stageTypeId: 'stage-release', stageName: 'Stage Release' },
        { stageTypeId: 'release', stageName: 'Release' },
        { stageTypeId: 'operate', stageName: 'Operate' },
      ]);
    });

    it('shows all inputs and control buttons', async () => {
      renderComponent();
      const fileUpload = await screen.findByTestId('evaluate-application-upload-file');
      const select = await screen.findAllByRole('option');
      expect(fileUpload).toBeInTheDocument();
      expect(select.length).toBe(5);
      expect(select[0].value).toBe('');
      const radios = await screen.findAllByRole('radio');
      expect(radios.length).toBe(2);
      const submitButton = await screen.findByRole('button', { name: 'Upload' });
      const cancelButton = await screen.findByRole('button', { name: 'Cancel' });
      expect(submitButton).toBeVisible();
      expect(cancelButton).toBeVisible();
    });

    it('does not show notification radio buttons block if notifications are not supported by the license', async () => {
      renderComponent({
        productFeatures: {
          productFeatures: {
            notifications: false,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              publicId: 'testApplicationPublicID',
            },
          },
          ownerActions: {
            evaluateApplication: {
              isEvaluationModalOpen: true,
              isStatusModalOpen: false,
              isValid: false,
              loading: false,
              loadError: null,
              submitError: null,
              stages: [],
              selectedStageId: null,
              notify: 'true',
              file: rscInitialFileUploadState(null),
              uploadFileProgress: 0,
              isUploadingFile: false,
              evaluationStatus: {
                currentStep: 1,
                totalSteps: 1,
                currentStepName: 'Uploading',
                scanId: '',
                error: null,
              },
            },
          },
        },
      });
      const notificationsLabel = await screen.queryByText(
        'Should notifications be sent if this application violates any policies?'
      );
      expect(notificationsLabel).not.toBeInTheDocument();
    });
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
      axiosMock.onGet(getCliStageUrl()).reply(200, [
        { stageTypeId: 'develop', stageName: 'Develop' },
        { stageTypeId: 'source', stageName: 'Source' },
        { stageTypeId: 'build', stageName: 'Build' },
        { stageTypeId: 'stage-release', stageName: 'Stage Release' },
        { stageTypeId: 'release', stageName: 'Release' },
        { stageTypeId: 'operate', stageName: 'Operate' },
      ]);
      axiosMock.onPost(getBundleUploadUrl('testApplicationPublicID', 'build', true)).reply((config) => {
        const total = 1024; // mocked file size
        const progress = 0.5;
        if (config.onUploadProgress) {
          config.onUploadProgress({ loaded: total * progress, total });
        }
        return new Promise(() => {});
      });
    });

    it('renders uploaded file porcentage and file name', async () => {
      renderComponent();

      const fileUpload = await screen.findByTestId('evaluate-application-upload-file');
      setFileUploadValue(fileUpload, file);
      expect(screen.getByText('testFile.json')).toBeInTheDocument();

      const select = await screen.findByRole('combobox');
      await fireEvent.change(select, { target: { value: 'build' } });

      const submitButton = await screen.findByRole('button', { name: 'Upload' });
      expect(submitButton).toHaveTextContent('Upload');
      await fireEvent.click(submitButton);

      expect(await screen.getByRole('progressbar')).toBeVisible();
      expect(screen.getByRole('dialog')).toHaveTextContent('Evaluate a File');
      expect(screen.getByText('50%')).toBeVisible();
      expect(screen.getByText('Uploading testFile.json file')).toBeVisible();
    });
  });

  describe('failed to fetch stages', () => {
    it('shows error alert with issue', async () => {
      axiosMock.onGet(getCliStageUrl()).reply(500, 'Error Message');
      renderComponent();
      expect(await screen.findByRole('alert')).toBeVisible();
      expect(await screen.findByRole('button', { name: 'Retry' })).toBeVisible();
      expect(await screen.findByText('An error occurred loading data. Error Message')).toBeVisible();
    });
  });
});
