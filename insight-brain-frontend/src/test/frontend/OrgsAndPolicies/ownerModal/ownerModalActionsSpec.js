/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getApplicationsUrl, getNLevelOrgUrl, getAddIconUrl } from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/OrgsAndPolicies/ownerModal/ownerModalSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { nxTextInputStateHelpers, nxFileUploadStateHelpers } from '@sonatype/react-shared-components';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

const { initialState: rscInitialState } = nxTextInputStateHelpers;
const { initialState: rscInitialFileUploadState } = nxFileUploadStateHelpers;

const APPS = [
  {
    id: 'applicationOneID',
    publicId: 'applicationOnePublicID',
    organizationId: 'organizationOneID',
    name: 'ApplicationOneName',
  },
  {
    id: 'applicationTwoID',
    publicId: 'applicationTwoPublicID',
    organizationId: 'organizationOneID',
    name: 'ApplicationTwoName',
  },
  {
    id: 'applicationThreeID',
    publicId: 'applicationThreePublicID',
    organizationId: 'organizationTwoID',
    name: 'ApplicationThreeName',
  },
];

const ORGS = [
  {
    id: 'organizationOneID',
    name: 'OrganizationOneName',
  },
  {
    id: 'organizationTwoID',
    name: 'OrganizationTwoName',
  },
];

const createOrgState = {
  router: {
    currentState: {
      name: 'management.view.organization',
    },
    currentParams: {
      organizationId: 'ROOT_ORGANIZATION_ID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: {
        id: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
      },
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isEditMode: false,
        ownerIconType: '',
        isApplication: false,
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState(''),
        appId: rscInitialState(''),
      },
    },
  },
};

const createAppState = {
  router: {
    currentState: {
      name: 'management.view.organization',
    },
    currentParams: {
      organizationId: 'organizationOneID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: {
        id: 'organizationOneID',
        name: 'OrganizationOneName',
      },
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isApplication: true,
        isEditMode: false,
        ownerIconType: '',
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState(''),
        appId: rscInitialState(''),
      },
    },
  },
};

const editOrgState = {
  router: {
    currentState: {
      name: 'management.view.organization',
    },
    currentParams: {
      organizationId: 'organizationOneID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: {
        id: 'organizationOneID',
        name: 'OrganizationOneName',
      },
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isEditMode: true,
        ownerIconType: '',
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState('newOrgName'),
        appId: rscInitialState(''),
      },
    },
  },
};

const editAppState = {
  router: {
    currentState: {
      name: 'management.view.application',
    },
    currentParams: {
      applicationPublicId: 'applicationOneID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: {
        id: 'applicationOneID',
        publicId: 'applicationOnePublicID',
        organizationId: 'organizationOneID',
        name: 'ApplicationOneName',
      },
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isEditMode: true,
        ownerIconType: '',
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState('newAppName'),
        appId: rscInitialState(''),
      },
    },
  },
};

describe('ownerModal actions', () => {
  let mock;

  beforeEach(() => {
    jasmine.clock().install();
    mock = axiosMockAdapter();
  });

  afterEach(function () {
    jasmine.clock().uninstall();
  });

  it('handles create new organization', (done) => {
    const store = SpecUtil.mockReduxStore(createOrgState);
    mock.onPost(getNLevelOrgUrl()).reply(200, {
      data: {
        id: 'organizationThreeID',
        name: 'OrganizationThreeName',
      },
    });

    store.dispatch(actions.createNewOwner()).then(() => {
      expect(mock.history.post.length).toBe(1);
      expect(mock.history.post[0].url).toBe(getNLevelOrgUrl());

      const actions = store.getActions();

      expect(actions.length).toBe(6);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/createOwner/pending',
        'ownerActions/updateApplication/pending',
        'organizations/updateOrganization',
        'ownerActions/updateApplication/fulfilled',
        'ownerSideNav/updateOwnersMapWithNewEntry',
        'ownerActions/ownerModal/createOwner/fulfilled',
      ]);

      jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      expect(actions.length).toBe(7);
      expect(actions).toHaveActionType('ownerActions/ownerModal/closeModal');

      done();
    });
  });

  it('handles create new application', (done) => {
    const store = SpecUtil.mockReduxStore(createAppState);
    mock.onPost(getApplicationsUrl()).reply(200, {
      data: {
        id: 'applicationFourID',
        name: 'applicationFourName',
        publicId: 'applicationFourPublicId',
        organizationId: 'organizationOneID',
      },
    });

    store.dispatch(actions.createNewOwner()).then(() => {
      expect(mock.history.post.length).toBe(1);
      expect(mock.history.post[0].url).toBe(getApplicationsUrl());

      const actions = store.getActions();

      expect(actions.length).toBe(6);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/createOwner/pending',
        'ownerActions/updateApplication/pending',
        'applications/updateApplication',
        'ownerActions/updateApplication/fulfilled',
        'ownerSideNav/updateOwnersMapWithNewEntry',
        'ownerActions/ownerModal/createOwner/fulfilled',
      ]);

      jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      expect(actions.length).toBe(7);
      expect(actions).toHaveActionType('ownerActions/ownerModal/closeModal');

      done();
    });
  });

  it('handles edit current organization', (done) => {
    const store = SpecUtil.mockReduxStore(editOrgState);
    mock.onPut(getNLevelOrgUrl()).reply(200, {
      data: {
        id: 'organizationOneID',
        name: 'newOwnerName',
      },
    });
    mock.onPost(getAddIconUrl(false, 'organizationOneID')).reply(200, {});

    store.dispatch(actions.editCurrentOwner()).then(() => {
      expect(mock.history.put.length).toBe(1);
      expect(mock.history.put[0].url).toBe(getNLevelOrgUrl());

      const actions = store.getActions();
      expect(actions.length).toBe(5);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/editCurrentOwner/pending',
        'ownerActions/updateApplication/pending',
        'organizations/updateOrganization',
        'ownerActions/updateApplication/fulfilled',
        'ownerActions/ownerModal/editCurrentOwner/fulfilled',
      ]);

      jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      expect(actions.length).toBe(6);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/editCurrentOwner/pending',
        'ownerActions/updateApplication/pending',
        'organizations/updateOrganization',
        'ownerActions/updateApplication/fulfilled',
        'ownerActions/ownerModal/editCurrentOwner/fulfilled',
        'ownerActions/ownerModal/closeModal',
      ]);

      done();
    });
  });

  it('handles edit current application', (done) => {
    const store = SpecUtil.mockReduxStore(editAppState);
    mock.onPut(getApplicationsUrl()).reply(200, {
      id: 'applicationOneID',
      publicId: 'applicationOnePublicID',
      organizationId: 'organizationOneID',
      name: 'newAppName',
    });
    mock.onPost(getAddIconUrl(true, 'applicationOneID')).reply(200, {});

    store.dispatch(actions.editCurrentOwner()).then(() => {
      expect(mock.history.put.length).toBe(1);
      expect(mock.history.put[0].url).toBe(getApplicationsUrl());

      const actions = store.getActions();

      expect(actions.length).toBe(5);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/editCurrentOwner/pending',
        'ownerActions/updateApplication/pending',
        'applications/updateApplication',
        'ownerActions/updateApplication/fulfilled',
        'ownerActions/ownerModal/editCurrentOwner/fulfilled',
      ]);

      jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      expect(actions.length).toBe(6);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/editCurrentOwner/pending',
        'ownerActions/updateApplication/pending',
        'applications/updateApplication',
        'ownerActions/updateApplication/fulfilled',
        'ownerActions/ownerModal/editCurrentOwner/fulfilled',
        'ownerActions/ownerModal/closeModal',
      ]);

      done();
    });
  });
});
