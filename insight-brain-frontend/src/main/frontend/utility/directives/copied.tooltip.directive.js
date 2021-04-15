/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function CopiedTooltipDirective($parse) {
  return {
    restrict: 'A',
    controller: CopiedTooltipController,
    require: 'copiedTooltip',
    link: CopiedTooltipLink,
  };

  function CopiedTooltipLink(scope, element, attrs, tooltipController) {
    if (attrs.copiedTooltip.length) {
      $parse(attrs.copiedTooltip).assign(scope, tooltipController);
    }
  }
}

CopiedTooltipDirective.$inject = ['$parse'];

function CopiedTooltipController($element) {
  this.showTooltip = showTooltip;

  $element.on('mouseleave', removeTooltip);

  function showTooltip() {
    $element
      .tooltip({
        title: 'Copied!',
        trigger: 'manual',
        placement: 'bottom',
        template:
          '<div class="tooltip copied-tooltip"><div class="tooltip-arrow"></div>' +
          '<div class="tooltip-content"><i class="fa fa-check-circle"></i><span class="tooltip-inner"></span></div></div>',
      })
      .tooltip('show');
  }

  function removeTooltip() {
    $element.tooltip('destroy');
  }
}

CopiedTooltipController.$inject = ['$element'];
