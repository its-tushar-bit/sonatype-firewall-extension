/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import selectedComponentServiceModule from '../../services/selectedComponentService';
import componentInformationPanelDirective from './componentInformationPanelDirective';
import cipTabPaneDirective from './cipTabPaneDirective';

export default angular
  .module('cipTabsWidgetModule', [selectedComponentServiceModule.name])
  .directive('componentInformationPanel', componentInformationPanelDirective)
  .directive('cipTabPane', cipTabPaneDirective);
