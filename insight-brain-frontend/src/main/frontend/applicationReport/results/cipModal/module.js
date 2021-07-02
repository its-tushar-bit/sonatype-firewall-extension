/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import withStoreProvider from '../../../reactAdapter/StoreProvider';
import './cipGlobals';
import '../../../version-graph/appcheck';
import pv from '../../../lib/protovis/protovis.min';

import cipTabsWidgetModule from '../../../components/cipTabsWidget/module';
import versionGraphModule from '../../../version-graph/version.graph/version.graph.module';
import cipComponentUtilModule from '../../../cip/cip-component-util';
import proprietaryMatchersModule from '../../../cip/proprietary.matchers.modal/proprietary.matchers.module';
import cipPolicyViolationsModule from '../../../cip/cip.policy.violations/cip.policy.violations.module';
import cipLicenseEditorModule from '../../../cip/cip.license.editor/cip.license.editor.module';
import cipLabelEditorModule from '../../../cip/cip.label.editor/cip.label.editor.module';
import cipVulnerabilityEditorModule from '../../../audit-report/cip/cip.vulnerability.editor/cip.vulnerability.editor.module';
import CLMLocationModule from '../../../util/CLMLocation';

// needed for Vulnerabilities tab
import '../../../audit-report/lib/jquery/jquery.browser';
import '../../../audit-report/lib/slickgrid/jquery.event.drag-2.3.0';
import '../../../audit-report/insight';
import '../../../audit-report/table';
import '../../../audit-report/lib/slickgrid/slick.core';
import '../../../audit-report/lib/slickgrid/slick.grid';
import '../../../audit-report/lib/slickgrid/slick.dataview';
import '../../../audit-report/lib/slickgrid/slick.groupitemmetadataprovider';
import '../../../audit-report/lib/slickgrid/slick.pager';
import '../../../audit-report/lib/slickgrid/slick.rowselectionmodel';
import '../../../audit-report/lib/slickgrid/slick.checkboxselectcolumn';
import '../../../audit-report/slickgrid/column-grouping';
import '../../../audit-report/slickgrid/filter';
import '../../../audit-report/slickgrid/sort';

import cipModal from './cipModal';
import applicationReportCipModal from './applicationReportCipModal';
import cipOccurrences from './cipOccurrences/cipOccurrences';
import cipSimilar from './cipSimilar/cipSimilar';
import cipAudit from './cipAudit/cipAudit';
import cipTabPanel from './cipTabPanel/cipTabPanel';
import cipClaimComponent from './cipClaimComponent/cipClaimComponent';
import rootAncestors from './rootAncestors/rootAncestors';
import innerSourceProducerReportModalContainer from './cipTabPanel/innerSourceProducerReportModal/InnerSourceProducerReportModalContainer';

export default angular
  .module('cipModal', [
    cipTabsWidgetModule.name,
    versionGraphModule.name,
    cipComponentUtilModule.name,
    proprietaryMatchersModule.name,
    cipPolicyViolationsModule.name,
    cipLicenseEditorModule.name,
    cipLabelEditorModule.name,
    cipVulnerabilityEditorModule.name,
    CLMLocationModule.name,
  ])
  .component('cipModal', cipModal)
  .component('applicationReportCipModal', applicationReportCipModal)
  .component('cipOccurrences', cipOccurrences)
  .component('cipSimilar', cipSimilar)
  .component('cipAudit', cipAudit)
  .component('cipTabPanel', cipTabPanel)
  .component('cipClaimComponent', cipClaimComponent)
  .component('rootAncestors', rootAncestors)
  .component(
    'innerSourceProducerReportModal',
    react2angular(withStoreProvider(innerSourceProducerReportModalContainer), [], ['$ngRedux'])
  )
  .service('OwnerContext', OwnerContext);

// context service needed for CIP
function OwnerContext() {
  return {
    ownerType: null,
    ownerId: null,
    scanId: null,
    setOwnerId(id) {
      this.ownerId = id;
    },
    setScanId(scanId) {
      this.scanId = scanId;
    },
    setOwnerType(ownerType) {
      this.ownerType = ownerType;
    },
  };
}

window.pv = pv;
