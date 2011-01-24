/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace segments_slider_Plugin
 */
Opencast.segments_ui_slider_Plugin = (function ()
{
    // The Templates to process
    var templateSegmentsTable = '<tr>' +
                                '{for s in segment}' +
                                    '{if s.durationIncludingSegment >= currentTime}' +
                                        '<td class="segment-holder" style="width: 15px;" " ' +
                                             'id="segment${s.index}" ' +
                                             'onmouseover="Opencast.segments_ui.hoverSegment(${parseInt(s.index)})" ' +
                                             'onmouseout="Opencast.segments_ui.hoverOutSegment(${parseInt(s.index)})" ' +
                                             'alt="Slide ${parseInt(s.index) + 1} of ${segment.length}" ' +
                                             'onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})" ' +
                                              'style="width: ${parseInt(s.duration) / parseInt(s.durationIncludingSegment) * 100}" ' +
                                        '></td>' +
                                     '{/if}' +
                                 '{forelse}' +
                                     '<td style="width: 100%;" id="segment-holder-empty" class="segment-holder" />' +
                                 '{/for}' +
                                 '</tr>';
     
     var templateData1 = '{for t in track}' +
                             '{if t.type == "presenter/delivery"}' +
                                 '{if t.mimetype == "video/x-flv"}' +
                                     '{if t.url.substring(0, 4) == "http"}' +
                                         '<div id="oc-video-presenter-delivery-x-flv-http" style="display: none">' +
                                             '${t.url}' +
                                         '</div>' +
                                         '<div id="oc-mimetype-presenter-delivery-x-flv-http" style="display: none">' +
                                             '${t.mimetype}' +
                                         '</div>' +
                                     '{/if}' +
                                 '{/if}' +
                             '{/if}' +
                              '{if (t.type == "presenter/delivery")' +
                               '&& (t.precedingSiblingType == "presentation/delivery")' +
                               '&& (t.precedingSiblingMimetypeIsVideo == "true")' +
                               '&& (t.followingSiblingType == "presentation/delivery")' +
                               '&& (t.followingSiblingMimetypeIsVideo == "true")}' +
                                  '{if t.mimetype == "audio/x-adpcm"}' +
                                      '{if t.url.substring(0, 4) == "http"}' +
                                          '<div id="oc-video-presenter-delivery-x-flv-http" style="display: none">' +
                                              '${t.url}' +
                                          '</div>' +
                                          '<div id="oc-mimetype-presenter-delivery-x-flv-http" style="display: none">' +
                                              '${t.mimetype}' +
                                          '</div>' +
                                      '{/if}' +
                                  '{/if}' +
                              '{/if}' +
                              
                             '{if t.type == "presentation/delivery"}' +
                                 '{if t.mimetype == "video/x-flv"}' +
                                     '{if t.url.substring(0, 4) == "http"}' +
                                         '<div id="oc-video-presentation-delivery-x-flv-http" style="display: none">' +
                                             '${t.url}' +
                                         '</div>' +
                                         '<div id="oc-mimetype-presentation-delivery-x-flv-http" style="display: none">' +
                                             '${t.mimetype}' +
                                         '</div>' +
                                     '{/if}' +
                                 '{/if}' +
                             '{/if}' +
     
                              '{if t.type == "presenter/delivery"}' +
                                  '{if t.mimetype == "video/x-flv"}' +
                                      '{if t.url.substring(0, 4) == "rtmp"}' +
                                          '<div id="oc-video-presenter-delivery-x-flv-rtmp" style="display: none">' +
                                              '${t.url}' +
                                          '</div>' +
                                          '<div id="oc-mimetype-presenter-delivery-x-flv-rtmp" style="display: none">' +
                                              '${t.mimetype}' +
                                          '</div>' +
                                      '{/if}' +
                                  '{/if}' +
                              '{/if}' + 
     
                              '{if t.type == "presentation/delivery"}' +
                                  '{if t.mimetype == "video/x-flv"}' +
                                      '{if t.url.substring(0, 4) == "rtmp"}' +
                                          '<div id="oc-video-presentation-delivery-x-flv-rtmp" style="display: none">' +
                                              '${t.url}' +
                                          '</div>' +
                                          '<div id="oc-mimetype-presentation-delivery-x-flv-rtmp" style="display: none">' +
                                              '${t.mimetype}' +
                                          '</div>' +
                                      '{/if}' +
                                  '{/if}' +
                              '{/if}' +
                         '{forelse}' +
                             '' +
                         '{/for}';
                         
    var templateData2 = '{for a in attachment}' +
                            '{if a.type == "presenter/player+preview"}' +
                                '<div id="oc-cover-presenter" style="display: none">' +
                                    '${a.url}' +
                                '</div>' +
                            '{/if}' +
                            '{if a.type == "presentation/player+preview"}' +
                                '<div id="oc-cover-presentation" style="display: none">' +
                                    '${a.url}' +
                                '</div>' +
                            '{/if}' +
                        '{forelse}' +
                            '' +
                        '{/for}';

    // The Elements to put the div into
    var elementSegmentsTable, elementData1, elementData2;
    // Data to process == search-result.results.result
    var segments_ui_data,
        segments_ui_dataSegments,
        segments_ui_dataData1,
        segments_ui_dataData2;
    // Precessed Data
    var processedTemplateData;
    
    /**
     * @memberOf Opencast.segments_ui_slider_Plugin
     * @description Add As Plug-in
     * @param elemSegmentsTable Segment Table Element
     * @param elemData1 First Data Element
     * @param elemData2 Second Data Element
     * @param data Data to fill the Elements with
     * @param withSegments boolean Flag if parse with Segments or without
     */
    function addAsPlugin(elemSegmentsTable,
                         elemData1,
                         elemData2,
                         data,
                         withSegments)
    {
        elementSegmentsTable = elemData1;
        elementData1 = elemData1;
        elementData2 = elemData2;
        segments_ui_data = data;
        segments_ui_dataSegments = segments_ui_data.segments;
        segments_ui_dataData1 = segments_ui_data.mediapackage.media;
        segments_ui_dataData2 = segments_ui_data.mediapackage.attachments;
        createSegments(withSegments);
    }
    
    /**
     * @memberOf Opencast.segments_ui_slider_Plugin
     * @description Processes the Data and puts it into the Element
     */
    function createSegments(withSegments)
    {
        // Process Element Segments Table 1
        if (withSegments && (elementSegmentsTable !== undefined))
        {
            processedTemplateData = templateSegmentsTable.process(segments_ui_dataSegments);
            elementSegmentsTable.html(processedTemplateData);
        }
        // Process Element Data 1
        if (elementData1 !== undefined)
        {
            processedTemplateData = templateData1.process(segments_ui_dataData1);
            elementSegmentsTable.html(processedTemplateData);
        }
        // Process Element Data 2
        if (elementData2 !== undefined)
        {
            processedTemplateData = templateData2.process(segments_ui_dataData2);
            elementSegmentsTable.html(processedTemplateData);
        }
    }
    
    return {
        addAsPlugin: addAsPlugin
    };
}());
