$.mediapackageParser = function(mediapackage)
{
    var SMILTYPE = "smil/smil";
    var SMIL = "";

    this.mediapackage = mediapackage;
	
    if(this.mediapackage) {
	this.start = this.mediapackage.start;
	this.id = this.mediapackage.id;
	this.duration = this.mediapackage.duration;
	this.title = this.mediapackage.title;
	this.creators = this.mediapackage.creators;
	this.license = this.mediapackage.license;
	this.media = this.mediapackage.media;
	this.metadata = this.mediapackage.metadata;
	this.attachments = this.mediapackage.attachments;
	
	this.smil = "";
	this.smil_id = -1;
	this.smil_mimetype = "";
	this.smil_tags = "";
	this.smil_type = "";
	this.smil_url = "";

	if(this.metadata.catalog) {
	    $.each(this.metadata.catalog, function(index, value) {
		    if(value.type.indexOf(SMILTYPE) != -1) {
			SMIL = value;
		    }
		});
	    this.smil = SMIL;
	    this.smil_id = this.smil.id;
	    this.smil_mimetype = this.smil.mimetype;
	    this.smil_tags = this.smil.tags;
	    this.smil_type = this.smil.type;
	    this.smil_url = this.smil.url;
	}
    }
}
