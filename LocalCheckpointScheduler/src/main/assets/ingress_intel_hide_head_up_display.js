javascript:

(function(f, s){
    s = document.createElement("script");
    s.src = "//ajax.googleapis.com/ajax/libs/jquery/2.0.2/jquery.min.js";
    s.onload = function() {
        f(jQuery.noConflict(true))
    };
	document.body.appendChild(s);
})

(function($){
	var eraseArr = [

            'clear_planned_links',
            'geotools',
            'loading_msg',
            'butterbar',
            'version_switcher',
            'zoom_level_data'];

	var i;
	for ( i = 0; i <= eraseArr.length; i++) {
		$('div#' + eraseArr[i]).css('display', 'none');
	}

    var target;
    target = document.getElementById('loading_msg');
    target.style.top = '2000px';

})

;


/*
(function(f, s) {
    s = document.createElement("script");
    s.src = "//ajax.googleapis.com/ajax/libs/jquery/2.0.2/jquery.min.js";
    s.onload = function() {
        f(jQuery.noConflict(true))
    };
    document.body.appendChild(s);
})

(function($) {
    var eraseArr = [
            'header',
            'comm',
            'clear_planned_links',
            'bottom_right_stack',
            'portal_filter_header',
            'player_stats',
            'game_stats',
            'bottom_right_stack',
            'zoom_level_data',
            'geotools',
            'geocode',
            'butterbar',
            'version_switcher',
            'gmnoprint',
            'loading_msg',
            'box_drop_shadow',
            'geocode',
            'footer'];

    var i;
    for (i = 0; i <= eraseArr.length; i++) {
        $('div#' + eraseArr[i]).css('display', 'none');
//        document.getElementById($(eraseArr[i])).css('display', 'none');
    }

//    document.getElementById('img_snap').css('display', 'none');
//    document.getElementById('img_passcode').css('display', 'none');



//    var elements = document.getElementsByClassName('box_drop_shadow');
//    for (i = 0; i <= elements.length; i++) {
//        elements[i].css('display', 'none');
//    }
    
//    document.getElementByName('img_snap').css('display', 'none');c

//    var target;
//    target = document.getElementById('loading_msg');
//    target.style.top = '2000px';

});

*/
