/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import NoticesModal from '../../../../../main/frontend/legal/files/notices/NoticesModal';
import { NxButton, NxForm, NxTextInput, NxToggle } from '@sonatype/react-shared-components';

describe('NoticesModal', function () {
  let getShallowComponent,
    minimalProps,
    cancelNoticesModalSpy,
    setNoticeContentSpy,
    setNoticeStatusSpy,
    addNoticeSpy,
    setNoticesScopeSpy,
    saveNoticesSpy;

  beforeEach(function () {
    cancelNoticesModalSpy = jasmine.createSpy('cancelNoticesModalSpy');
    setNoticeContentSpy = jasmine.createSpy('setNoticeContentSpy');
    setNoticeStatusSpy = jasmine.createSpy('setNoticeStatusSpy');
    addNoticeSpy = jasmine.createSpy('addNoticeSpy');
    setNoticesScopeSpy = jasmine.createSpy('setNoticesScopeSpy');
    saveNoticesSpy = jasmine.createSpy('saveNoticesSpy');
    minimalProps = {
      cancelNoticesModal: cancelNoticesModalSpy,
      setNoticeContent: setNoticeContentSpy,
      setNoticeStatus: setNoticeStatusSpy,
      addNotice: addNoticeSpy,
      setNoticesScope: setNoticesScopeSpy,
      saveNotices: saveNoticesSpy,
      scope: 'ROOT_ORGANIZATION_ID',
      originalScope: 'ROOT_ORGANIZATION_ID',
      availableScopes: {
        values: [
          { id: 'appId', name: 'app', label: 'Application' },
          { id: 'orgId', name: 'org', label: 'Organization' },
          {
            id: 'ROOT_ORGANIZATION_ID',
            name: 'Root Organization',
            label: 'Organization',
          },
        ],
      },
      notices: [
        {
          id: null,
          originalContentHash: 'originalContentHash1',
          relPath: 'some/path',
          originalContent: 'content1',
          content: 'content1',
          originalStatus: 'enabled',
          status: 'enabled',
          isPristine: true,
        },
        {
          id: null,
          originalContentHash: 'originalContentHash2',
          relPath: 'some/path',
          originalContent: '',
          content: '',
          originalStatus: 'disabled',
          status: 'disabled',
          isPristine: false,
        },
      ],
      error: 'error',
      submitMaskState: 'submitMaskState',
    };
    getShallowComponent = enzymeUtils.getShallowComponent(NoticesModal, minimalProps);
  });

  it('renders No notice files found found if there are no notices', function () {
    const wrapper = getShallowComponent({ notices: [] });
    const noNoticeTextsRow = wrapper.find('tbody tr');
    expect(noNoticeTextsRow).toHaveText('No notice files found');
  });

  it('renders notice contents', function () {
    const wrapper = getShallowComponent();
    const noticeContents = wrapper.find(NxTextInput);
    expect(noticeContents.length).toBe(2);
    expect(noticeContents.at(0).prop('value')).toEqual('content1');
    expect(noticeContents.at(0).prop('disabled')).toBeFalsy();
    expect(noticeContents.at(0).prop('isPristine')).toBeTruthy();
    expect(noticeContents.at(1).prop('value')).toEqual('');
    expect(noticeContents.at(1).prop('disabled')).toBeTruthy();
    expect(noticeContents.at(1).prop('isPristine')).toBeFalsy();
  });

  it('sets a notice content to the input value when typed', function () {
    const wrapper = getShallowComponent();
    const noticeContents = wrapper.find(NxTextInput);
    noticeContents.at(0).simulate('change', '');
    expect(setNoticeContentSpy.calls.mostRecent().args[0]).toEqual({
      index: 0,
      value: '',
    });
    noticeContents.at(1).simulate('change', 'content2');
    expect(setNoticeContentSpy.calls.mostRecent().args[0]).toEqual({
      index: 1,
      value: 'content2',
    });
  });

  it('renders notice statuses', function () {
    const wrapper = getShallowComponent();
    const noticeStatuses = wrapper.find(NxToggle);
    expect(noticeStatuses.length).toBe(2);
    expect(noticeStatuses.at(0).prop('isChecked')).toBeTruthy();
    expect(noticeStatuses.at(0)).toHaveText('Included');
    expect(noticeStatuses.at(1).prop('isChecked')).toBeFalsy();
    expect(noticeStatuses.at(1)).toHaveText('Excluded');
  });

  it('sets a notice status to its opposite value when the toggle is clicked', function () {
    const wrapper = getShallowComponent();
    const noticeStatuses = wrapper.find(NxToggle);
    noticeStatuses.at(0).simulate('change');
    expect(setNoticeStatusSpy.calls.mostRecent().args[0]).toEqual({
      index: 0,
      value: 'disabled',
    });
    noticeStatuses.at(1).simulate('change');
    expect(setNoticeStatusSpy.calls.mostRecent().args[0]).toEqual({
      index: 1,
      value: 'enabled',
    });
  });

  it('adds a notice when the add notice button is clicked', function () {
    const wrapper = getShallowComponent();
    const addNoticeButton = wrapper.find(NxButton);
    addNoticeButton.simulate('click');
    expect(addNoticeSpy).toHaveBeenCalled();
  });

  it('selects the given notices scope', function () {
    const wrapper = getShallowComponent();
    const noticesScope = wrapper.find('select');
    expect(noticesScope.length).toBe(1);
    expect(noticesScope.at(0).prop('value')).toEqual('ROOT_ORGANIZATION_ID');
  });

  it('has the notices scope options', function () {
    const wrapper = getShallowComponent();
    const noticesScopeOptions = wrapper.find('option');
    expect(noticesScopeOptions.length).toBe(3);
    expect(noticesScopeOptions.at(0).prop('value')).toEqual('appId');
    expect(noticesScopeOptions.at(0)).toHaveText('Application - app');
    expect(noticesScopeOptions.at(1).prop('value')).toEqual('orgId');
    expect(noticesScopeOptions.at(1)).toHaveText('Organization - org');
    expect(noticesScopeOptions.at(2).prop('value')).toEqual('ROOT_ORGANIZATION_ID');
    expect(noticesScopeOptions.at(2)).toHaveText('Organization - Root Organization');
  });

  it('sets the notices scope to the selected value when changed', function () {
    const wrapper = getShallowComponent();
    const noticesScope = wrapper.find('select');
    noticesScope.simulate('change', { currentTarget: { value: 'appId' } });
    expect(setNoticesScopeSpy).toHaveBeenCalledWith('appId');
  });

  it('has a validation error if a custom notice has no content', function () {
    const wrapper = getShallowComponent({
      notices: [
        ...minimalProps.notices,
        {
          id: null,
          originalContentHash: null,
          originalContent: '',
          content: '',
          status: 'disabled',
          isPristine: true,
        },
      ],
    });
    const form = wrapper.find(NxForm);
    expect(form.prop('validationErrors')).toBe('A custom notice must have text.');
  });

  it('has a validation error if there has been no changes', function () {
    const wrapper = getShallowComponent({
      notices: [
        {
          id: null,
          originalContentHash: 'originalContentHash1',
          relPath: 'some/path',
          originalContent: 'content1',
          content: 'content1',
          originalStatus: 'enabled',
          status: 'enabled',
          isPristine: true,
        },
      ],
    });
    const form = wrapper.find(NxForm);
    expect(form.prop('validationErrors')).toBe('Must add a new notice or change the content or status of a notice.');
  });

  it('has no validation error if the scope has changed', function () {
    const wrapper = getShallowComponent({ scope: 'appId' });
    const form = wrapper.find(NxForm);
    expect(form.prop('validationErrors')).toBeUndefined();
  });

  it('has no validation error if a custom notice was added with content', function () {
    const wrapper = getShallowComponent({
      notices: [
        {
          id: null,
          originalContentHash: null,
          originalContent: '',
          content: 'content',
          originalStatus: 'enabled',
          status: 'enabled',
        },
      ],
    });
    const form = wrapper.find(NxForm);
    expect(form.prop('validationErrors')).toBeUndefined();
  });

  it('has no validation error if the content has changed', function () {
    const wrapper = getShallowComponent({
      notices: [
        {
          id: null,
          originalContentHash: 'originalContentHash1',
          relPath: 'some/path',
          originalContent: 'content1',
          content: 'updatedContent1',
          originalStatus: 'enabled',
          status: 'enabled',
        },
      ],
    });
    const form = wrapper.find(NxForm);
    expect(form.prop('validationErrors')).toBeUndefined();
  });

  it('has no validation error if the status has changed', function () {
    const wrapper = getShallowComponent({
      notices: [
        {
          id: null,
          originalContentHash: 'originalContentHash1',
          relPath: 'some/path',
          originalContent: 'content1',
          content: 'content1',
          originalStatus: 'enabled',
          status: 'disabled',
        },
      ],
    });
    const form = wrapper.find(NxForm);
    expect(form.prop('validationErrors')).toBeUndefined();
  });
});
