javascript:

(function(global) {
    // Strict mode.
    "use strict";

    // CLASS //////////////////////////////////////////////////

    /**
     * TEMPLATE
     */
    function TEMPLATE() {
        // NOP.
    };

    // HEADER /////////////////////////////////////////////////

    /**
     * CONSTRUCTOR.
     */
    TEMPLATE.prototype.constructor = TEMPLATE;

    /**
     * TEMPLATE method.
     */
    TEMPLATE.prototype.method;

    // IMPLEMENTATION /////////////////////////////////////////

    function TEMPLATE_method() {

    };



    // EXPORTS ////////////////////////////////////////////////

    var isBrowser = "document" in global;
    var isWebWorkers = "WorkerLocation" in global;
    var isNode = "process" in global;

    if (isNode) {
        module.exports = TEMPLATE;
    }
    global.IngressIntelHacker = TEMPLATE;

})((this || 0).self || global);

