/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import ComponentLegalOverviewContainer from './ComponentLegalOverviewContainer';
import LegalApplicationDetailsContainer from './application/LegalApplicationDetailsContainer';
import AttributionReportForm from './application/AttributionReportFormContainer';
import AttributionReportTemplateForm from './application/AttributionReportTemplateFormContainer';
import componentCopyrightDetails from './copyright/componentCopyrightDetails';
import CopyrightDetailsHeaderContainer from './copyright/CopyrightDetailsHeaderContainer';
import CopyrightListContainer from './copyright/CopyrightListContainer';
import CopyrightDetailsContentsContainer from './copyright/CopyrightDetailsContentsContainer';
import NoticeDetailsHeaderContainer from './files/notices/NoticeDetailsHeaderContainer';
import NoticeDetailsListContainer from './files/notices/NoticeDetailsListContainer';
import NoticeDetailsContentsContainer from './files/notices/NoticeDetailsContentsContainer';
import LegalDashboardContainer from './dashboard/LegalDashboardContainer';
import ComponentLicenseDetailsContainer from './license/ComponentLicenseDetailsContainer';
import componentNoticeDetails from './files/notices/componentNoticeDetails';
import componentLicenseFilesDetails from './files/licenses/componentLicenseFilesDetails';
import LicenseFilesDetailsHeaderContainer from './files/licenses/LicenseFilesDetailsHeaderContainer';
import LicenseFilesDetailsListContainer from './files/licenses/LicenseFilesDetailsListContainer';
import LicenseFilesDetailsContentsContainer from './files/licenses/LicenseFilesDetailsContentsContainer';
import { createLegalRoutes } from 'MainRoot/legal/legalUtility';

export default angular
  .module('legalModule', [])
  .component('legalDashboard', iqReact2Angular(LegalDashboardContainer, ['isAuthorized'], ['$ngRedux', '$state']))
  .component('componentLegalOverview', iqReact2Angular(ComponentLegalOverviewContainer, [], ['$ngRedux', '$state']))
  .component('legalApplicationDetails', iqReact2Angular(LegalApplicationDetailsContainer, [], ['$ngRedux', '$state']))
  .component('attributionReportForm', iqReact2Angular(AttributionReportForm, [], ['$ngRedux', '$state']))
  .component(
    'attributionReportTemplateForm',
    iqReact2Angular(AttributionReportTemplateForm, [], ['$ngRedux', '$state'])
  )
  .component('componentCopyrightDetails', componentCopyrightDetails)
  .component('copyrightDetailsHeader', iqReact2Angular(CopyrightDetailsHeaderContainer, [], ['$ngRedux', '$state']))
  .component('copyrightList', iqReact2Angular(CopyrightListContainer, [], ['$ngRedux', '$state']))
  .component('copyrightDetailsContents', iqReact2Angular(CopyrightDetailsContentsContainer, [], ['$ngRedux', '$state']))
  .component('componentNoticeDetails', componentNoticeDetails)
  .component('noticeDetailsHeader', iqReact2Angular(NoticeDetailsHeaderContainer, [], ['$ngRedux', '$state']))
  .component('noticeDetailsList', iqReact2Angular(NoticeDetailsListContainer, [], ['$ngRedux', '$state']))
  .component('noticeDetailsContents', iqReact2Angular(NoticeDetailsContentsContainer, [], ['$ngRedux', '$state']))
  .component('componentLicenseDetails', iqReact2Angular(ComponentLicenseDetailsContainer, [], ['$ngRedux', '$state']))
  .component('componentLicenseFilesDetails', componentLicenseFilesDetails)
  .component(
    'licenseFilesDetailsHeader',
    iqReact2Angular(LicenseFilesDetailsHeaderContainer, [], ['$ngRedux', '$state'])
  )
  .component('licenseFilesDetailsList', iqReact2Angular(LicenseFilesDetailsListContainer, [], ['$ngRedux', '$state']))
  .component(
    'licenseFilesDetailsContents',
    iqReact2Angular(LicenseFilesDetailsContentsContainer, [], ['$ngRedux', '$state'])
  )
  .config(routes);

function routes($stateProvider) {
  createLegalRoutes($stateProvider);
}

routes.$inject = ['$stateProvider'];
