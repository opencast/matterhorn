/*global $, Videodisplay, Opencast, fluid*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
@namespace the global Opencast namespace engage
*/
Opencast.engage = (function () {
  /**
   * @memberOf Opencast.engage
   * @description Gets the url to the search service;
   * @return the search service endpoint url
   */

  function getSearchServiceEpisodeIdURL() 
  {
    var restEndpoint = "../../search/rest/episode?id=";
    // var restEndpoint = "episode.xml?id="; // Activate for testing purposes
    //var restEndpoint = "xml/episode.xml?id="; // Activate for testing purposes

      return restEndpoint;
   }

  function getCookie(name) {
    var start = document.cookie.indexOf( name + "=" );
    var len = start + name.length + 1;
    if ( ( !start ) && ( name != document.cookie.substring( 0, name.length ) ) ) {
      return null;
    }
    if ( start == -1 ) return null;
    
    var end = document.cookie.indexOf( ';', len );
    if ( end == -1 ) 
      end = document.cookie.length;
    return unescape( document.cookie.substring( len, end ) );
  }
  
   /**
     * @memberOf Opencast.engage
     * @description Gets the current media package id
     * @return The current media package id
     */
    function getMediaPackageId() {
      var value = getGETParameter("id");
      return value;
    }

    /**
     * @memberOf Opencast.engage
     * @description Gets the current video url
     * @return The video url
     */
    function getVideoUrl() {
      var value = getGETParameter("videoUrl");
      return value;
    }

    /**
     * @memberOf Opencast.engage
     * @description Get the value of the GET parameter with the passed "name"
     * @param string name
     * @return The value of the GET parameter
     */
    function getGETParameter(name) {
      name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
      var regexS = "[\\?&]" + name + "=([^&#]*)";
      var regex = new RegExp(regexS);
      var results = regex.exec(window.location.href);
      if (results == null)
        return null;
      else
        return results[1];
    }

    return {
      getCookie : getCookie,
      getMediaPackageId : getMediaPackageId,
      getVideoUrl : getVideoUrl,
      getSearchServiceEpisodeIdURL :  getSearchServiceEpisodeIdURL
    };
}());