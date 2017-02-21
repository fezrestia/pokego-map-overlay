javascript:

(function(global) {

    var footer = document.getElementById("area_footer");
    if (footer != null) {
        footer.style.display = "none";
        footer.style.height = "0px";
    }

    var map_frame = document.getElementById("area_map_frame");
    if (map_frame != null) {
        map_frame.style.marginBottom = "0px";
    }

    var buttonsearch = document.getElementById("area_buttonsearch");
    if (buttonsearch != null) {
        buttonsearch.style.bottom = "0px";
    }

})((this || 0).self || global);

