(function($) {
    $.widget("ui.timefield", {
	// **CHANGE** set default values
	options : {
	    value : 0,
	    seconds : 0
	},

	/**
	 * create the timefield
	 */
	_create : function() {
	    var self = this;
	    this.inputItem = $('<input type="text" />');
	    this.inputItem.val(this._format(this.options.value));
	    this.inputItem.focusout(function(e) {
		val = self.inputItem.val();
		val = val.split(':');
		newVal = parseInt(val[0]) * 3600;
		newVal += parseInt(val[1]) * 60;
		newVal += parseInt(val[2]);
		newVal += parseInt(val[2].split(".")[1]) / 1000;
		self._setOption('value', newVal);
	    });
	    this.inputItem.keyup(function(evt) {
		if(evt.keyCode == 13) {
		    val = self.inputItem.val();
		    val = val.split(':');
		    newVal = parseInt(val[0]) * 3600;
		    newVal += parseInt(val[1]) * 60;
		    newVal += parseInt(val[2]);
		    newVal += parseInt(val[2].split(".")[1]) / 1000;
		    self._setOption('value', newVal);
		}
	    });
	    this.element.append(this.inputItem);
	},

	/**
	 * format the number of seconds and milliseconds to something like
	 * hh:MM:ss.mmmm
	 */
	_format : function(seconds) {
	    if (typeof seconds == "string") {
		seconds = seconds.replace("s", "");
		seconds = parseFloat(seconds);
	    }

	    var h = "00";
            var m = "00";
            var s = "00";
            if (!isNaN(seconds) && (seconds >= 0)) {
		var tmpH = Math.floor(seconds / 3600);
		var tmpM = Math.floor((seconds - (tmpH * 3600)) / 60);
		var tmpS = seconds - (tmpH * 3600) - (tmpM * 60);
		var tmpMS = tmpS + "";
		h = (tmpH < 10) ? "0" + tmpH : Math.floor(seconds / 3600);
		m = (tmpM < 10) ? "0" + tmpM : tmpM;
		s = (tmpS < 10) ? "0" + tmpS : tmpS;
		s = s + "";
		var indexOfSDot = s.indexOf(".");
		if(indexOfSDot != -1) {
		    s = s.substr(0, indexOfSDot + 5);
		}
            }
            return h + ":" + m + ":" + s;
	},

	/**
	 * format the seconds to something like 00:00:12 from ocUtils
	 */
	_formatSeconds : function(seconds) {
	    var tmp = _format(seconds);
	    return tmp.substr(0, tmp.indexOf('.'));
	},

	/**
	 * set an option
	 */
	_setOption : function(key, value) {
	    switch (key) {

	    case "value":
		value += "";
		if (value.indexOf("s") == -1) {
		    value += "s";
		}
		this.inputItem.val(this._format(value));
		this.options.value = value;
		this.options.seconds = parseFloat(value.replace("s", ""));
		break;
	    default:
		this.options[key] = value;
	    }
	},

    });
})(jQuery);