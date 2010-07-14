/* @namespace Holds functions and properites related to all Admin UIs. */
var AdminUI = {} || AdminUI;
/* @namespace Holds functions and properites related to forms used in Admin UIs. */
var AdminForm = {} || AdminForm;

AdminUI.internationalize = function(obj, prefix){
  for(var i in obj){
    if(typeof obj[i] == 'object'){
      AdminUI.internationalize(obj[i], prefix + '_' + i);
    }else if(typeof obj[i] == 'string'){
      var id = '#' + prefix + '_' + i;
      if($(id).length){
        $(id).text(obj[i]);
      }
    }
  }
}

AdminUI.log = function(){
  if(window.console){
    try{
      window.console && console.log.apply(console,Array.prototype.slice.call(arguments));
    }catch(e){
      console.log(e);
    }
  }
}

/**
 *  Function parses the URL for parameters.
 *  @param {String} Optional. If a name is passed, that parameter's value is returned.
 *  @return {String|Boolean|Array} If optional parameter is left empty, an array of all params are returned.
 */
AdminUI.getURLParams = function(param) {
  var urlParams = {};
  if(document.location.search) {
    params = document.location.search.substr(1).split('&');
    for(var p in params) {
      eq = params[p].indexOf('=');
      if(eq != -1) {
        urlParams[params[p].substr(0, eq)] = params[p].substr(eq+1);
      } else {
        urlParams[params[p]] = true;
      }
    }
  }
  if(param && urlParams[param]) {
    return urlParams[param];
  } else if(urlParams.length > 0) {
    return urlParams;
  }
  return null;
};

AdminForm.components = {};

AdminForm.Manager = function(rootElm, rootNs, components){
  this.rootElm = rootElm;
  this.rootNs = rootNs;
  this.components = components;
};

$.extend(AdminForm.Manager.prototype, {
  serialize: function(){
    if(this.validate()){
      var doc = this.createDoc();
      var mdlist = doc.createElement('metadataList');
      for(var c in this.components){
        if(c === 'recurrence' || c === 'eventId'){
          this.components[c].toNode(doc.documentElement)
        } else {
          this.components[c].toNode(mdlist);
        }
      }
      doc.documentElement.appendChild(mdlist);
      if(typeof XMLSerializer != 'undefined') {
        return (new XMLSerializer()).serializeToString(doc);
      } else if(doc.xml) {
        return doc.xml;
      } else { 
        return false;
      }
    }
    return false;
  },
  populate: function(values){
    for(var e in this.components){
      if(values[e] != undefined){
        this.components[e].setValue(values[e]);
      }
    }
  },
  validate: function(){
    var error = false;
    $('#missingFields-container').hide();
    $('.missing-fields-item').hide();
    for(var k in this.components){
      if(this.components[k].required && !this.components[k].validate()){
        $('#' + this.components[k].errorField).show();
        $('#' + this.components[k].label).addClass('error');
        error = true;
      }else{
        if(this.components[k].errorField && this.components[k].label){
          $('#' + this.components[k].errorField).hide();
          $('#' + this.components[k].label).removeClass('error');
        }
      }
    }
    if(error){
      $('#missingFields-container').show();
    }
    return !error;
  },
  createDoc: function(){
    var doc = null;
    //Create a DOM Document, methods vary between browsers, e.g. IE and Firefox
    if(document.implementation && document.implementation.createDocument) { //Firefox, Opera, Safari, Chrome, etc.
      doc = document.implementation.createDocument(this.rootNs, this.rootElm, null);
    } else { // IE
      doc = new ActiveXObject('MSXML2.DOMDocument');
      doc.loadXML('<' + this.rootElm + ' xmlns="' + this.rootNs + '"></' + this.rootElm + '>');
    }
    return doc;
  }
});

/* @class The Component class is a collection of form elements and associated functions for use
 * with the AdminForm.Manager. It provides basic implementations for setting, getting, displaying,
 * and XMLifying the form elements.
 */
AdminForm.Component = function Component(fields, props, funcs){
  this.fields = [];
  this.errorField = "";
  this.label = "";
  this.properties = [];
  this.required = false;
  this.value = null;
  this.nodeKey = null;
  
  this.setFields(fields);
  this.setFunctions(funcs);
  this.setProperties(props);
};

$.extend(AdminForm.Component.prototype, {
  /* @lends AdminForm.Component.prototype */
  /** 
   *  Sets the fields from an array of element ids.
   *  @param {String[]} Array of element ids
   */
  setFields:  function(fields){
    if(typeof fields == 'string') { //If a single field is specified, wrap in an array.
      fields = [fields];
    }
    for(var k in fields) {
      var e = $('#' + fields[k]);
      if(e[0]){
        this.fields[fields[k]] = e;
        this.label = 'label-' + e[0].id;
        this.errorField = 'missing-' + e[0].id;
      }
    }
  },
  /** 
   *  Extends Component with additional methods and/or properties
   *  @param {Object} An object literal or instance with which to extend Component
   */
  setFunctions: function(funcs){
    if(funcs && typeof funcs == 'object'){
      $.extend(this, funcs);
    }
  },
  /** 
   *  Sets the Component properties, arbitrary properties are added to properties array
   *  @param {Object} Key/Value pair of properties
   */
  setProperties: function(props){
    if(typeof props == 'object') {
      for(var f in props) {
        switch(f){
          case 'errorField':
            this.errorField = props[f];
            break;
          case 'label':
            this.label = props[f];
            break;
          case 'required':
            this.required = props[f];
            break;
          case 'nodeKey':
            this.nodeKey = props[f];
            break;
          default:
            this.properties[f] = props[f];
        }
      }
    }
  },
  /** 
   *  Default getValue function
   *  @return A comma seperated string of all element values.
   *  @type String
   */
  getValue: function(){
    if(this.validate()){
      var values = [];
      for(var el in this.fields){
        var e = this.fields[el];
        if(e.length){
          values.push(e.val());
        }
        this.value = values.join(',');
      } 
    }
    return this.value;
  },
  /** 
   *  Default setValue function
   *  Sets all elements to specified value
   *  @param {String}
   */
  setValue: function(val){
    for(var el in this.fields){
      if(this.fields[el].length){
        this.fields[el].val(val);
      }
    }
  },
  /** 
   *  Default toString function
   *  @return A string of the Components value.
   *	@type String
   */
  toString:  function(){
    return this.getValue();
  },
  /** 
   *  Default toNode function
   *  @param {DOM Node} Node to which to attach this Components value
   *  @return DOM Node created from this Component.
   *	@type DOM Node
   */
  toNode: function(parent){
    var doc, container, value, key;
    for(var el in this.fields){
      if(parent){
        doc = parent.ownerDocument;
      }else{
        doc = document;
      }
      container = doc.createElement('metadata');
      value = doc.createElement('value');
      key = doc.createElement('key');
      value.appendChild(doc.createTextNode(this.getValue()));
      if(this.nodeKey !== null){
         key.appendChild(doc.createTextNode(this.nodeKey));
      }else{
         key.appendChild(doc.createTextNode(el));
      }
      container.appendChild(value);
      container.appendChild(key);
    }
    if(parent && parent.nodeType && container){
      parent.appendChild(container); //license bug
    }else{
      AdminUI.log('Unable to append node to document. ', parent, container);
    }
    return container;
  },
  /** 
   *  Default validation function, displays Component's error message
   *  @return True if Component is required and valid, otherwise false.
   *	@type Boolean
   */
  validate: function(){
    if(!this.required){
      return true;
    }else{
      var oneIsValid = false;
      for(var e in this.fields){
        if(this.fields[e][0].type == 'checkbox' || this.fields[e][0].type == 'radio'){
          if(this.fields[e][0].checked){
            oneIsValid = true;
            break;
          }
        }else{
          if(this.fields[e].val()){
            oneIsValid = true;
            break;
          }
        }
      }
      if(oneIsValid){
        return true;
      }
    }
    return false;
  }
});
/*
TODO: Create a container for components to handle those components that can repeat

AdminUI.ComponentSet = function ComponentSet(){

};

$.extend(AdminUI.ComponentSet.prototype, {
  components: []
});

*/