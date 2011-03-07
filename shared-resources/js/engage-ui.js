/*global $, Videodisplay, Opencast, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 @namespace the global Opencast namespace engage
 */
Opencast.engage = (function ()
{
    var loadProgressPercent = -1;
    
    /**
     * @memberOf Opencast.engage
     * @description Gets player type ("watch" or "embed")
     * @return the player type
     */
    function getPlayerType()
    {
        var pathname = window.location.pathname;
        return pathname;
    }
    
    /**
     * @memberOf Opencast.engage
     * @description Gets the url to the search service;
     * @return the search service endpoint url
     */
    function getSearchServiceEpisodeIdURL()
    {
        var restEndpoint = "../../search/episode.xml?id="; // Production 
        //var restEndpoint = "xml/episode.xml?id="; // Activate for testing purposes
        //var restEndpoint = "episode-segments.xml?id="; // Activate for testing purposes
        return restEndpoint;
    }
    
    /**
     * @memberOf Opencast.engage
     * @description Gets the current load progress
     * @return The current load progress
     */
    function getLoadProgress()
    {
        if (loadProgressPercent === -1) return -1;
        else
        {
            var duration = Opencast.Player.getDuration();
            return duration * loadProgressPercent / 100;
        }
    }
    
    /**
     * @memberOf Opencast.engage
     * @description Sets the current load progress
     * @param The current load progress
     */
    function setLoadProgressPercent(value)
    {
        if (0 <= value && value <= 100)
        {
            loadProgressPercent = value;
        }
    }
    
    /**
     * @memberOf Opencast.engage
     * @description Returns a specific Cookie
     * @param name Name of the Cookie to return
     * @return a specific Cookie with the Name 'name'
     */
    function getCookie(name)
    {
        var start = document.cookie.indexOf(name + "=");
        var len = start + name.length + 1;
        if ((!start) && (name != document.cookie.substring(0, name.length)))
        {
            return null;
        }
        if (start == -1) return null;
        var end = document.cookie.indexOf(';', len);
        if (end == -1) end = document.cookie.length;
        return unescape(document.cookie.substring(len, end));
    }
    
    return {
        getCookie: getCookie,
        getPlayerType: getPlayerType,
        getLoadProgress: getLoadProgress,
        setLoadProgressPercent: setLoadProgressPercent,
        getSearchServiceEpisodeIdURL: getSearchServiceEpisodeIdURL
    };
}());
