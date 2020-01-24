/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import IqOrgAppPicker from '../components/iqOrgAppPicker/IqOrgAppPicker';

export default angular.module('reactComponents', [])
    .component('iqOrgAppPicker', react2angular(IqOrgAppPicker))
    .component('nxFontAwesomeIcon', react2angular(NxFontAwesomeIcon, [
      'icon', 'mask', 'className', 'color', 'spin', 'pulse', 'border', 'fixedWidth', 'inverse', 'listItem', 'flip',
      'size', 'pull', 'rotation', 'transform', 'symbol', 'style', 'tabIndex', 'title']));
