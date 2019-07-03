/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular*/

import AngularCommonModule from '../../util/AngularCommon';
import CommonServicesModule from '../../util/CommonServices';
import copiedTooltip from './copied.tooltip.directive';
import copyToClipboard from './copy.to.clipboard.directive';
import detectScrollbar from './detect.scrollbar.directive';
import enterKeyCall from './enter.key.call.directive';
import formMask from './form.mask.directive';
import hasWhitespaceValidator from './has.whitespace.validator.directive';
import loadWrapper from './load.wrapper.directive';
import middleClick from './middle.click.directive';
import padToTop from './pad.to.top.directive';
import SortController from './sort.controller';
import sort from './sort.directive';
import submitValidation from './submit.validation.directive';

export default angular.module('utility.directives', [AngularCommonModule.name, CommonServicesModule.name])
    .directive('copiedTooltip', copiedTooltip)
    .directive('copyToClipboard', copyToClipboard)
    .directive('detectScrollbar', detectScrollbar)
    .directive('enterKeyCall', enterKeyCall)
    .directive('formMask', formMask)
    .directive('hasWhitespaceValidator', hasWhitespaceValidator)
    .directive('loadWrapper', loadWrapper)
    .directive('middleClick', middleClick)
    .directive('padToTop', padToTop)
    .controller('sort.controller', SortController)
    .directive('sort', sort)
    .directive('submitValidation', submitValidation);
