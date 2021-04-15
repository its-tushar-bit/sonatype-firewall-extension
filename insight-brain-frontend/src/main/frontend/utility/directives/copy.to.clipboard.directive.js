/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Directive to copy value of provided expression to clipboard when user clicks on containing element.
 * It should be used in conjunction with "copied-tooltip" directive, which displays "copy success" tooltip.
 *
 * attributes:
 *  copy-to-clipboard="expression to copy"
 *  copied-tooltip
 *
 * Example:
 * <a copy-to-clipboard="vm.owner.publicId" copied-tooltip>Copy app Id to clipboard</a>
 *
 */
export default function CopyToClipboard($window, $parse) {
  return {
    restrict: 'A',
    require: 'copiedTooltip',
    link: CopyToClipboardLink,
  };

  function CopyToClipboardLink(scope, element, attrs, copiedTooltipCtrl) {
    element.on('click', function (e) {
      e.stopPropagation();
      var textToCopy = $parse(attrs.copyToClipboard)(scope);
      try {
        copyText(textToCopy);
      } catch (err) {
        // expected in Safari browser
        showManualCopyPrompt();
      }
    });

    function copyText(text) {
      var node = createHiddenNode(text);
      $window.document.body.appendChild(node);
      copyFromNode(node);
      $window.document.body.removeChild(node);
    }

    function createHiddenNode(text) {
      var node = $window.document.createElement('textarea');

      // make it invisible but not hidden so that it still can be selected
      node.style.position = 'absolute';
      node.style.left = '-10000px';

      node.textContent = text;
      node.oncopy = function () {
        removeManualCopyPrompt();
        copiedTooltipCtrl.showTooltip();
      };
      return node;
    }

    function copyFromNode(node) {
      var selection = $window.document.getSelection();
      selection.removeAllRanges();
      node.select();
      if (!$window.document.execCommand('copy')) {
        throw 'failure copy';
      }
      selection.removeAllRanges();
    }

    function showManualCopyPrompt() {
      element
        .tooltip({
          title: 'Press ⌘-C to copy',
          trigger: 'manual',
          placement: 'bottom',
        })
        .tooltip('show');
    }

    function removeManualCopyPrompt() {
      element.tooltip('destroy');
    }
  }
}

CopyToClipboard.$inject = ['$window', '$parse'];
