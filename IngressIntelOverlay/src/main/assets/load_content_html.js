javascript:

(function(global) {

    // HTML
    var mainHtml = document.documentElement.outerHTML;

    // Java Script Native Interface test.
    window.jsni.onContentHtmlLoaded(mainHtml);

    // evaluateJavascript test.
    return mainHtml;

})((this || 0).self || global);
