/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import RepositoryPane from '../../../../../main/frontend/configuration/scmOnboarding/components/RepositoryPane';
import LoadWrapper from '../../../../../main/frontend/react/LoadWrapper';
import ResultsTable from '../../../../../main/frontend/configuration/scmOnboarding/components/ResultsTable';
import {createOrg, createRepo} from './utils';
import {NxButton, NxSubmitMask} from '@sonatype/react-shared-components';
import GitHostModal from '../../../../../main/frontend/configuration/scmOnboarding/components/GitHostModal';
import {shallow} from 'enzyme';
import React from 'react';

describe('RepositoryPane', function () {
  let minimalProps,
      getShallowComponent,
      mock$State;

  beforeEach(() => {
    mock$State = jasmine.createSpyObj('$state', ['get', 'href']);
    mock$State.href.and.returnValue('routerUrl');

    minimalProps = {$state: mock$State};

    getShallowComponent = enzymeUtils.getShallowComponent(RepositoryPane, minimalProps);
  });

  it('displays add org button', () => {
    const component = getShallowComponent(),
        button = component.find('#repository-pane-add-org');

    expect(button).toExist();
  });

  describe('new organization modal', () => {

    it('shows modal when clicking add org button', () => {
      const props = {
        setIsNewOrganizationModalVisible: jasmine.createSpy('setIsNewOrganizationModalVisible'),
        isNewOrganizationModalVisible: false
      };
      const component = getShallowComponent(props),
          button = component.find('#repository-pane-add-org');

      button.simulate('click');

      expect(props.setIsNewOrganizationModalVisible).toHaveBeenCalled();
    });
  });

  describe('git host modal', () => {
    it('passes URL-missing error message as property', () => {
      const component = getShallowComponent({scmProvider: 'myprovider'}),
          gitHostModal = component.find(GitHostModal);

      // wrap react node passed as prop
      const errorText = gitHostModal.props().errorText;
      const errorTextWrapper = shallow(<div>{errorText}</div>);

      expect(errorTextWrapper.text()).toEqual('IQ Server was unable to identify the URL for your ' +
          'myprovider host. You need to provide a SCM URL in order to proceed.');
    });

    it('uses friendly provider name', () => {
      const component = getShallowComponent({scmProvider: 'github'}),
          gitHostModal = component.find(GitHostModal);

      // wrap react node passed as prop
      const errorText = gitHostModal.props().errorText;
      const errorTextWrapper = shallow(<div>{errorText}</div>);

      // then github is replaced with GitHub
      expect(errorTextWrapper.text()).toEqual('IQ Server was unable to identify the URL for your GitHub ' +
          'host. You need to provide a SCM URL in order to proceed.');
    });
  });

  describe('load wrapper flags are propagated correctly', () => {

    const propsData = [
      {loadingRepositories: true},
      {loadingRepositories: false, isSelectingOrganization: true}
    ];

    propsData.forEach(props => {
      it('shows load wrapper when loadingRepositories: ' + props.loadingRepositories, () => {
        const component = getShallowComponent(props),
            loadWrapper = component.find('#scm-repo-table').find(LoadWrapper),
            resultsTable = component.find(ResultsTable);

        // then loading flags are propagated correctly
        expect(loadWrapper.props().loading).toEqual(true);
        expect(resultsTable.props().loadingRepositories).toEqual(props.loadingRepositories);
      });
    });

    it('displays error message with correct links', () => {
      // given properties indicating no SCM token is configured
      const props = {
        isScmTokenConfigured: false,
        selectedOrganization: createOrg('errorOrg')
      };
      const component = getShallowComponent(props),
          loadWrapper = component.find('#scm-repo-table').find(LoadWrapper);

      // when the component is rendered
      const error = loadWrapper.props().error;
      const errorWrapper = shallow(<div>{error}</div>);
      const errorUrl1 = errorWrapper.find('a').first();
      const errorUrl2 = errorWrapper.find('a').last();

      // then error message is rendered
      expect(errorWrapper.text()).toEqual('We could not find a token. You can configure a token to be ' +
          'shared across organizations in the Root Organization\'s Source Control Configuration page, or you can ' +
          'provide a custom token for the org-errorOrg Organization.');

      // and URLs from router are inserted
      expect(errorUrl1.props().href).toEqual('routerUrl');
      expect(errorUrl2.props().href).toEqual('routerUrl');
    });
  });

  describe('footer', () => {
    const repositories = ['aaaa', 'bbbb', 'aabb'].map(prefix => createRepo(prefix));

    it('hides when repositories empty', () => {
      const props = {
        repositories: []
      };

      const component = getShallowComponent(props),
          footer = component.find('footer');

      expect(footer).not.toExist();
    });

    it('displays when repositories present', () => {
      const props = {
        repositories: repositories
      };

      const component = getShallowComponent(props),
          footer = component.find('footer'),
          count = component.find('#scm-repo-to-import-count'),
          importButton = footer.find(NxButton);

      // then footer is displayed
      expect(footer).toExist();
      expect(count.text()).toEqual('0 of 3 repositories');
      expect(importButton).toExist();
      expect(importButton.props().disabled).toEqual(true);
    });

    it('text is updated when repositories are selected', () => {
      const props = {
        repositories: repositories
      };

      const component = getShallowComponent(props),
          footer = component.find('footer'),
          resultsTable = component.find(ResultsTable);

      // when repository is selected
      resultsTable.prop('setSelectedRepositories')([repositories[0]]);

      // then repository count is updated
      expect(footer).toExist();
      expect(component.find('#scm-repo-to-import-count').text()).toEqual('1 of 3 repositories');
    });
  });

  describe('submit mask', () => {
    it('displays submit mask while importing', () => {
      const component = getShallowComponent({isImporting: true}),
          submitMask = component.find(NxSubmitMask);

      expect(submitMask).toExist();
    });

    it('hides submit mask when not importing', () => {
      const component = getShallowComponent({isImporting: false}),
          submitMask = component.find(NxSubmitMask);

      expect(submitMask).not.toExist();
    });
  });
});
