/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../enzymeUtils';
import ScmOnboarding from '../../../../main/frontend/configuration/scmOnboarding/ScmOnboarding';
import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';
import { shallow } from 'enzyme';
import React from 'react';
import ImportStatusModal from '../../../../main/frontend/configuration/scmOnboarding/components/ImportStatusModal';
import ReportsCta from '../../../../main/frontend/configuration/scmOnboarding/components/ReportsCta';
import { createRepo } from './components/utils';
import RepositoryPane from '../../../../main/frontend/configuration/scmOnboarding/components/RepositoryPane';

describe('ScmOnboarding', function () {
  let minimalProps, getShallowComponent, mock$State;

  beforeEach(() => {
    mock$State = jasmine.createSpyObj('$state', ['get', 'href']);
    mock$State.href.and.returnValue('routerUrl');
    mock$State.get.and.returnValue({ data: { title: 'title' } });

    minimalProps = { $state: mock$State };

    getShallowComponent = enzymeUtils.getShallowComponent(ScmOnboarding, minimalProps);
  });

  describe('load wrapper', () => {
    const propsData = [{ loadingPage: true }, { loadingPage: false }];

    propsData.forEach((props) => {
      it('receives loadingPage prop: ' + props.loadingPage, () => {
        const component = getShallowComponent(props),
          loadWrapper = component.find(LoadWrapper);

        // expect loading prop to have expected value
        expect(loadWrapper.props().loading).toEqual(props.loadingPage);
      });
    });

    it('receives pageError prop', () => {
      // given authN failed
      const component = getShallowComponent({
          loadingPermissionsError: 'It appears you do not have permission to access this page.',
        }),
        loadWrapper = component.find(LoadWrapper);

      // when error is rendered
      const error = loadWrapper.props().error;
      const errorWrapper = shallow(<div>{error}</div>);

      // then error message is matches expected value
      expect(errorWrapper.text()).toContain('It appears you do not have permission to access this page.');
    });

    it('retry handler triggers reload', () => {
      // given a failure
      const loadPageMock = jasmine.createSpy('loadPage');
      const component = getShallowComponent({
          isAuthorized: false,
          loadPage: loadPageMock,
        }),
        loadWrapper = component.find(LoadWrapper);

      // when retry is requested
      loadWrapper.props().retryHandler();

      // then loadPage action is triggered
      expect(loadPageMock).toHaveBeenCalled();
    });
  });

  describe('Import status modal', () => {
    it('is present', () => {
      // expect import status modal component always to be present
      expect(getShallowComponent().find(ImportStatusModal)).toExist();
    });
  });

  describe('Page title', () => {
    it('contains title with human readable SCM provider name', () => {
      // expect title to contain camelcase provider name
      expect(getShallowComponent({ scmProvider: 'github' }).find('.iq-scmonboarding-title').text()).toEqual(
        'Import Applications from GitHub'
      );
    });
  });

  describe('Reports CTA', () => {
    const createRepos = (count) => Array.from(Array(count).keys()).map((i) => createRepo(i));

    // given repos list with [0] none imported repos and [1] 5 imported repos
    const propsData = [
      {
        totalRepositories: 10,
        repositories: createRepos(10),
        scmProvider: 'provider',
      },
      {
        totalRepositories: 10,
        repositories: createRepos(5),
        scmProvider: 'provider',
      },
    ];

    propsData.forEach((props) => {
      it('with alreadyImportedCount: ' + (props.totalRepositories - props.repositories.length), () => {
        // given the provided number of repos
        const reportsCta = getShallowComponent(props).find(ReportsCta);

        // then reports CTA is only rendered when there are already imported repos
        if (props.totalRepositories - props.repositories.length > 0) {
          expect(reportsCta).toExist();
        } else {
          expect(reportsCta).not.toExist();
        }
      });
    });
  });

  describe('Repository pane', () => {
    it('is present', () => {
      // expect repository pane component to always exist
      expect(getShallowComponent().find(RepositoryPane)).toExist();
    });
  });
});
