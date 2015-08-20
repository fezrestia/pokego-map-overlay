javascript:

(function(global) {

    var gmnoprintArray = document.getElementsByClassName("gmnoprint");
    if (gmnoprintArray != null) {
        for (i = 0; i < gmnoprintArray.length; ++i) {
            console.log("gmnoprint Elm = " + gmnoprintArray[i].className);
            gmnoprintArray[i].style.display = "inline";
        }
    } else {
        console.log("gmnoprint is NULL");
    }

    var snapControl = document.getElementById("snapcontrol");
    if (snapControl != null) {
        snapControl.style.display = "inline";
    } else {
        console.log("snapcontrol is NULL");
    }

    var passcodeIcon = document.getElementById("passcode_icon");
    if (passcodeIcon != null) {
        passcodeIcon.style.display = "inline";
    } else {
        console.log("passcode_icon is NULL");
    }

    var versionSwitcher = document.getElementById("version_switcher");
    if (versionSwitcher != null) {
        versionSwitcher.style.display = "inline";
    } else {
        console.log("version_switcher is NULL");
    }

    var geoTools = document.getElementById("geotools");
    if (geoTools != null) {
        geoTools.style.display = "inline";
    } else {
        console.log("geotools is NULL");
    }

    var gmStyleArray = document.getElementsByClassName("gm-style");
    if (gmStyleArray != null) {
        for (i = 0; i < gmStyleArray.length; ++i) {
            var gmStyleChildArray = gmStyleArray[i].childNodes;
            if (gmStyleChildArray != null) {
                for (j = 0; j < gmStyleChildArray.length; ++j) {
                    console.log("MapCanvas child Z-Index = " + gmStyleChildArray[j].style.zIndex);
                    if (gmStyleChildArray[j].style.zIndex == 1000000) {
                        console.log("Do HIDE");
                        gmStyleChildArray[j].style.display = "inline";
                    }
                }
            } else {
                console.log("gmStyleChildArray is NULL");
            }
        }
    } else {
        console.log("gm-style is NULL");
    }

    var msgText = document.getElementById('loading_msg_text');
    if (msgText != null) {
        msgText.style.textShadow = "0 0 3px #0b0c0d";
        msgText.style.opacity = 1.0;
    } else {
        console.log("loading_msg_text is NULL");
    }

})((this || 0).self || global);