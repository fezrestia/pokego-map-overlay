javascript:

(function(global) {

    var msgText = document.getElementById('loading_msg');
    var msgTextDisplay = "null";

    if (msgText != null) {
        msgTextDisplay = msgText.style.display;
    }

    return msgTextDisplay;

})((this || 0).self || global);
