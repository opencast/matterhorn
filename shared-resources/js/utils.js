/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace utils delegate.
 * @description Helper Functions used by more than 1 Plugin
 */
Opencast.Utils = (function ()
{
    /**
     * @memberOf Opencast.Utils
     * @description Returns the Input Time in Milliseconds
     * @param data Data in the Format ab:cd:ef
     * @return Time from the Data in Milliseconds
     */
    function getTimeInMilliseconds(data)
    {
        var values = data.split(':');
        // If the Format is correct
        if (values.length == 3)
        {
            // Try to convert to Numbers
            var val0 = values[0] * 1;
            var val1 = values[1] * 1;
            var val2 = values[2] * 1;
            // Check and parse the Seconds
            if (!isNaN(val0) && !isNaN(val1) && !isNaN(val2))
            {
                // Convert Hours, Minutes and Seconds to Milliseconds
                val0 *= 60 * 60 * 1000; // 1 Hour = 60 Minutes = 60 * 60 Seconds = 60 * 60 * 1000 Milliseconds
                val1 *= 60 * 1000; // 1 Minute = 60 Seconds = 60 * 1000 Milliseconds
                val2 *= 1000; // 1 Second = 1000 Milliseconds
                // Add the Milliseconds and return it
                return val0 + val1 + val2;
            }
            else
            {
                return 0;
            }
        }
        else
        {
            return 0;
        }
    }
    
    /**
     * @memberOf Opencast.Utils
     * @description Returns formatted Seconds
     * @param seconds Seconds to format
     * @return formatted Seconds
     */
    function formatSeconds(seconds)
    {
        var result = "";
        if (parseInt(seconds / 3600) < 10)
        {
            result += "0";
        }
        result += parseInt(seconds / 3600);
        result += ":";
        if ((parseInt(seconds / 60) - parseInt(seconds / 3600) * 60) < 10)
        {
            result += "0";
        }
        result += parseInt(seconds / 60) - parseInt(seconds / 3600) * 60;
        result += ":";
        if (seconds % 60 < 10)
        {
            result += "0";
        }
        result += seconds % 60;
        return result;
    }
    
    /**
     * @memberOf Opencast.Utils
     * @description Returns an Array of URL Arguments
     * @return an Array of URL Arguments
     */
    function parseURL()
    {
        var vars = [],
            hash;
        var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
        for (var i = 0; i < hashes.length; i++)
        {
            hash = hashes[i].split('=');
            vars.push(hash[0]);
            vars[hash[0]] = hash[1];
        }
        return vars;
    }
    
    /**
     * @memberOf Opencast.Utils
     * @description Returns the value of URL-Parameter 'name'
     *              Current used URL Parameters:
     *                  - id:           the current media package id
     *                  - user:         the user id
     *                  - play:         autoplay, true or false
     *                  - videoUrl:     the current url for video (1)
     *                  - videoUrl2:    the current url for video 2
     *                  - coverUrl:     the current url for cover (preview image)
     *                  - t:            jump to given time
     *                                  Valid Parameter Formats (as seen at Opencast.Utils.parseSeconds):
     *                                      - Minutes and seconds:  XmYs    or    YsXm    or    XmY
     *                                      - Minutes only:         Xm
     *                                      - Seconds only:         Ys      or    Y
     *                  - videomode:    the Video Mode
     *                                  Valid Parameters:
                                            - streaming (default)
                                            - progressive
     *                  - page
     *                  - q
     * @return the value of URL-Parameter 'name' or null if not defined
     */
    function getURLParameter(name)
    {
        var urlParam = parseURL()[name];
        if(urlParam === undefined)
        {
            return null;
        }
        return urlParam;
    }
    
    /**
     * @memberOf Opencast.Utils
     * @description Parses Seconds
     *
     * Format: Minutes and seconds:  XmYs    or    YsXm    or    XmY
     *         Minutes only:         Xm
     *         Seconds only:         Ys      or    Y
     *
     * @return parsed Seconds if parsing was successfully, 0 else
     */
    function parseSeconds(val)
    {
        if ((val !== undefined) && !(val == ""))
        {
            // Only Seconds given
            if (!isNaN(val))
            {
                return val;
            }
            var tmpVal = val + "";
            var min = -1,
                sec = -1;
            var charArr = tmpVal.split("");
            var tmp = "";
            for (var i = 0; i < charArr.length; ++i)
            {
                // If Minutes-Suffix detected
                if (charArr[i] == "m")
                {
                    if (!isNaN(tmp))
                    {
                        min = parseInt(tmp);
                    }
                    else
                    {
                        min = 0;
                    }
                    tmp = "";
                }
                // If Seconds-Suffix detected
                else if (charArr[i] == "s")
                {
                    if (!isNaN(tmp))
                    {
                        sec = parseInt(tmp);
                    }
                    else
                    {
                        sec = 0;
                    }
                    tmp = "";
                }
                // If any Number detected
                else if (!isNaN(charArr[i]))
                {
                    tmp += charArr[i];
                }
            }
            if (min < 0)
            {
                min = 0;
            }
            if (sec < 0)
            {
                // If Seconds without 's'-Suffix
                if (tmp != "")
                {
                    if (!isNaN(tmp))
                    {
                        sec = parseInt(tmp);
                    }
                    else
                    {
                        sec = 0;
                    }
                }
                else
                {
                    sec = 0;
                }
            }
            var ret = min * 60 + sec;
            if (!isNaN(ret))
            {
                return ret;
            }
        }
        return 0;
    }
    
    
    /**
     * @memberOf Opencast.Utils
     * @description create date in format MM/DD/YYYY
     * @param timeDate Time and Date
     */
    function getLocaleDate(timeDate)
    {
        return timeDate.substring(0, 10);
    }
    
    /**
     * @memberOf Opencast.Utils
     * @description Returns a random Number in between [min, max]
     * @param min Min Value
     * @param max Max Value
     * @return a random Number in between [min, max]
     */
    function getRandom(min, max) {
	    if(min > max)
	    {
		    return max;
	    }
	    if(min == max)
	    {
		    return min;
	    }
	    return(min + parseInt(Math.random() * (max - min + 1)));
	}
	
    /**
     * @memberOf Opencast.Utils
     * @description Returns if 'haystack' starts with 'start'
     * @param haystack String to search in
     * @param start String to search for
     * @return true if 'haystack' starts with 'start', false else
     */
	function startsWith(haystack, start)
	{
	    if((typeof(haystack) == 'string') && (typeof(start) == 'string'))
	    {
	        return (haystack.substring(0, start.length).indexOf(start) != -1);
	    }
	    return false;
	}
    
    return {
        getTimeInMilliseconds: getTimeInMilliseconds,
        formatSeconds: formatSeconds,
        parseURL: parseURL,
        getURLParameter: getURLParameter,
        parseSeconds: parseSeconds,
        getLocaleDate: getLocaleDate,
        startsWith: startsWith,
        getRandom: getRandom
    };
}());
