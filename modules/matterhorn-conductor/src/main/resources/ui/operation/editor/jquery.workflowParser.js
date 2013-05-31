$.workflowParser = function(workflow) {
    this.workflow = workflow;
		
    if(workflow.workflow) {
	this.state = this.workflow.workflow.state;
	this.id = this.workflow.workflow.id;
	this.template = this.workflow.workflow.template;
	this.title = this.workflow.workflow.title;
	this.description = this.workflow.workflow.description;
	this.creator = this.workflow.workflow.creator;
	this.organization = this.workflow.workflow.organization;
	this.mediapackage = this.workflow.workflow.mediapackage;
	this.operations = this.workflow.workflow.operations;
	this.configurations = this.workflow.workflow.configurations;
	this.errors = this.workflow.workflow.errors;
    }
}
