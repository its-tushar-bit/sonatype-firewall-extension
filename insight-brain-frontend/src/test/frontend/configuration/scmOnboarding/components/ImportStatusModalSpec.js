/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import ImportStatusModal from '../../../../../main/frontend/configuration/scmOnboarding/components/ImportStatusModal';
import { NxButton, NxErrorAlert, NxModal, NxSuccessAlert } from '@sonatype/react-shared-components';
import ReportsCta from '../../../../../main/frontend/configuration/scmOnboarding/components/ReportsCta';
import { createOrg, createRepo } from './utils';

describe('ImportStatusModal', () => {
  let getShallowComponent;

  beforeEach(() => {
    const minimalProps = {
      isImportStatusDialogVisible: true,
      failedRepos: [],
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ImportStatusModal, minimalProps);
  });

  describe('displays modal', () => {
    it('modal is present', () => {
      const component = getShallowComponent(),
        modal = component.find(NxModal);

      // expect dialog to be rendered
      expect(modal).toExist();
    });

    it('modal has reports CTA and continue button', () => {
      const component = getShallowComponent(),
        modal = component.find(NxModal),
        reportsCta = modal.find(ReportsCta);

      // expect continue button to be rendered
      expect(reportsCta).toExist();
    });
  });

  describe('can be closed', () => {
    it('continue button closes modal', () => {
      // given a continue button
      const setIsImportStatusDialogVisibleSpy = jasmine.createSpy('setIsImportStatusDialogVisible');
      const component = getShallowComponent({ setIsImportStatusDialogVisible: setIsImportStatusDialogVisibleSpy }),
        modal = component.find(NxModal),
        footer = modal.find('footer'),
        continueButton = footer.find(NxButton);

      // when onClick is invoked
      continueButton.invoke('onClick')();

      // then redux action to change visibility is called
      expect(setIsImportStatusDialogVisibleSpy).toHaveBeenCalledWith(false);
    });
  });

  describe('displays status message', () => {
    it('shows an error message if failed repos are present', () => {
      // given an import failure
      const component = getShallowComponent({ failedImportCount: 1 }),
        statusDiv = component.find('.nx-modal-content'),
        error = statusDiv.find(NxErrorAlert),
        success = statusDiv.find(NxSuccessAlert);

      // expect error message to be rendered
      expect(error).toExist();
      expect(success).not.toExist();
      expect(error.text()).toEqual('1 repository had an error. See details below.');
    });

    it('shows an error message if failed repos are present pluralized', () => {
      // given multiple import failures
      const component = getShallowComponent({ failedImportCount: 2 }),
        statusDiv = component.find('.nx-modal-content'),
        error = statusDiv.find(NxErrorAlert),
        success = statusDiv.find(NxSuccessAlert);

      // expect error message to pluralized
      expect(error).toExist();
      expect(success).not.toExist();
      expect(error.text()).toEqual('2 repositories had an error. See details below.');
    });

    it('shows an success message if no failed repos are present', () => {
      // given no failed imports
      const component = getShallowComponent({
          failedImportCount: 0,
          newlyImportedRepos: [createRepo('repo')],
          selectedOrganization: createOrg('org'),
        }),
        statusDiv = component.find('.nx-modal-content'),
        error = statusDiv.find(NxErrorAlert),
        success = statusDiv.find(NxSuccessAlert);

      // expect success message to be rendered
      expect(error).not.toExist();
      expect(success).toExist();
      expect(success.text()).toEqual('All repositories were successfully imported. See details below.');
    });
  });

  describe('displays success details', () => {
    it('shows an success message if no failed repos are present', () => {
      // given no failed imports and a single successful import
      const component = getShallowComponent({
          failedImportCount: 0,
          newlyImportedRepos: [createRepo('repo')],
          selectedOrganization: createOrg('org'),
        }),
        detailsDiv = component.find('.scm-import-details');

      // expect success details to be rendered
      expect(detailsDiv).toExist();
      expect(detailsDiv.text()).toEqual(
        '1 repository was successfully imported to IQ Server as applications under the org-org Organization.'
      );
    });

    it('shows an success message if no failed repos are present pluralized', () => {
      // given no failures and multiple successful imports
      const component = getShallowComponent({
          failedImportCount: 0,
          newlyImportedRepos: [createRepo('repoa'), createRepo('repob')],
          selectedOrganization: createOrg('org'),
        }),
        detailsDiv = component.find('.scm-import-details');

      // expect success message to be pluralized
      expect(detailsDiv).toExist();
      expect(detailsDiv.text()).toEqual(
        '2 repositories were successfully imported to IQ Server as applications under the org-org Organization.'
      );
    });
  });

  describe('displays error details', () => {
    it('not shown when no failed imports present', () => {
      // given no errors
      const component = getShallowComponent({ failedImportCount: 0 }),
        detailsDiv = component.find('.scm-import-details');

      // expect error details not to be rendered
      expect(detailsDiv).toExist();
      expect(detailsDiv.text()).toEqual('');
    });

    it('shows failed repo', () => {
      // given a single failed import
      const component = getShallowComponent({
          failedImportCount: 1,
          failedRepos: [{ repository: createRepo('repoa'), errorMessage: 'BOOM' }],
        }),
        detailsDiv = component.find('.scm-import-details'),
        list = component.find('.scm-import-error-detail-list'),
        listItem = list.find('li');

      // expect an error message to be rendered
      expect(detailsDiv).toExist();
      expect(detailsDiv.text()).toContain('1 repository had an error');
      expect(listItem.text()).toEqual('ns-repoa/prj-repoa failed with BOOM');
    });

    it('shows failed repos pluralized', () => {
      // given multiple failed imports
      const component = getShallowComponent({
          failedImportCount: 2,
          failedRepos: [{ repository: createRepo('repoa') }, { repository: createRepo('repob'), errorMessage: 'BANG' }],
        }),
        detailsDiv = component.find('.scm-import-details'),
        errorList = component.find('.scm-import-error-detail-list'),
        errorListItems = errorList.find('li');

      // expect a separate list item for each error
      expect(detailsDiv).toExist();
      expect(detailsDiv.text()).toContain('2 repositories had an error');
      expect(errorListItems.first().text()).toEqual('ns-repoa/prj-repoa');
      expect(errorListItems.last().text()).toEqual('ns-repob/prj-repob failed with BANG');
    });
  });
});
