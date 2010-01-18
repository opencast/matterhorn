/**
 * Scheduler service implementation classes
 * 
 * The scheduler creates events that should be recorded on a Matterhorn Capture Agent at the specified time and date.
 * It is intended that the user adds the events with the scheduler web interface that is provided in the admin tools 
 * (currently http://localhost:8080/admin/scheduler.html).
 * 
 * The service communicates with the UI over some methods that allow to add an new event, update an event, 
 * delete an event and to list the events in the database in various ways. 
 * The Capture Agent get the events belonging to him as an iCalendar, in which the Dublin Core Metadata and 
 * Capture agent specific technical metadata is included as Base64 encoded files.
 * 
 * This implementation stores the events in a database that is provided to the scheduler service as an javax.sql.DataSource 
 * object that should be available over the OSGI server.
 * 
 * Nearly any metadata key-value pairs will be stored in the scheduler database. So the UI can adopt indepentend of the service 
 * on the metadata-fields that schould be used.
 * How the metadata will be mapped on the exported formats (Dublin Pore and properties for the capture agent) can be defined in  
 * the two properties files under src/main/resources/config. 
 * 
 * There is the possibility to define other ingestion endpoints that the capture agent might use. This is defined in the 
 * felix confing with the key "capture.ingest.enpoint.url". If this is missing, the value will be constructed from the 
 * "serverURL" that is in the felix-config too.  
 */
@javax.xml.bind.annotation.XmlSchema(elementFormDefault=XmlNsForm.QUALIFIED)
package org.opencastproject.scheduler.impl;

import javax.xml.bind.annotation.XmlNsForm;
