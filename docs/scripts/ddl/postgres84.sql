CREATE TABLE "sequence" (
  "seq_name" character varying(50) NOT NULL,
  "seq_count" numeric(38,0),
  PRIMARY KEY ("seq_name")
);

INSERT INTO sequence (seq_name, seq_count) values ('SEQ_GEN', 0);

CREATE TABLE "mh_organization" (
  "id" character varying(128) NOT NULL,
  "anonymous_role" character varying(255),
  "name" character varying(255),
  "admin_role" character varying(255),
  PRIMARY KEY ("id")
);

CREATE TABLE "mh_organization_node" (
  "organization" character varying(128) NOT NULL,
  "port" integer,
  "name" character varying(255),
  PRIMARY KEY ("organization", "port", "name"),
  CONSTRAINT "FK_mh_organization_node_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE INDEX "IX_mh_organization_node_pk" ON "mh_organization_node" ("organization");
CREATE INDEX "IX_mh_organization_node_name" ON "mh_organization_node" ("name");
CREATE INDEX "IX_mh_organization_node_port" ON "mh_organization_node" ("port");

CREATE TABLE "mh_organization_property" (
  "organization" character varying(128) NOT NULL,
  "name" character varying(255) NOT NULL,
  "value" character varying(255),
  PRIMARY KEY ("organization", "name"),
  CONSTRAINT "FK_mh_organization_property_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE INDEX "IX_mh_organization_property_pk" ON "mh_organization_property" ("organization");

CREATE TABLE "mh_annotation" (
  "id" bigint NOT NULL,
  "inpoint" bigint,
  "outpoint" bigint,
  "mediapackage" character varying(128),
  "session" character varying(128),
  "created" timestamp,
  "user"  character varying(255),
  "length" bigint,
  "type" character varying(128),
  "value" text,
  "private" boolean,
  PRIMARY KEY ("id")
);

CREATE INDEX "IX_mh_annotation_created" ON "mh_annotation" ("created");
CREATE INDEX "IX_mh_annotation_inpoint" ON "mh_annotation" ("inpoint");
CREATE INDEX "IX_mh_annotation_outpoint" ON "mh_annotation" ("outpoint");
CREATE INDEX "IX_mh_annotation_mediapackage" ON "mh_annotation" ("mediapackage");
CREATE INDEX "IX_mh_annotation_private" ON "mh_annotation" ("private");
CREATE INDEX "IX_mh_annotation_user" ON "mh_annotation" ("user");
CREATE INDEX "IX_mh_annotation_session" ON "mh_annotation" ("session");
CREATE INDEX "IX_mh_annotation_type" ON "mh_annotation" ("type");

CREATE TABLE "mh_capture_agent_role" (
  "id" character varying(128) NOT NULL,
  "organization" character varying(128) NOT NULL,
  "role" character varying(255),
  PRIMARY KEY ("id", "organization", "role"),
  CONSTRAINT "FK_mh_capture_agent_role_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE INDEX "IX_mh_capture_agent_role" ON "mh_capture_agent_role" ("id", "organization");

CREATE TABLE "mh_capture_agent_state" (
  "id" character varying(128) NOT NULL,
  "organization" character varying(128) NOT NULL,
  "configuration" text,
  "state" text NOT NULL,
  "last_heard_from" bigint NOT NULL,
  "url" text,
  PRIMARY KEY ("id", "organization"),
  CONSTRAINT "FK_mh_capture_agent_state_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE TABLE "mh_dictionary" (
  "text" character varying(255) NOT NULL,
  "language" character varying(5) NOT NULL,
  "weight" numeric(8,2),
  "count" bigint,
  "stop_word" boolean,
  PRIMARY KEY ("text", "language")
);

CREATE INDEX "IX_mh_dictionary_weight" ON "mh_dictionary" ("weight");

CREATE TABLE "mh_host_registration" (
  "id" bigint NOT NULL,
  "host" character varying(255) NOT NULL,
  "maintenance" boolean NOT NULL,
  "online" boolean NOT NULL DEFAULT TRUE,
  "active" boolean NOT NULL DEFAULT TRUE,
  "max_jobs" bigint NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "UNQ_mh_host_registration_0" UNIQUE ("host")
);

CREATE INDEX "IX_mh_host_registration_online" ON "mh_host_registration" ("online");
CREATE INDEX "IX_mh_host_registration_active" ON "mh_host_registration" ("active");

CREATE TABLE "mh_service_registration" (
  "id" bigint NOT NULL,
  "path" character varying(255) NOT NULL,
  "job_producer" boolean NOT NULL,
  "service_type" character varying(255) NOT NULL,
  "online" boolean NOT NULL DEFAULT TRUE,
  "active" boolean NOT NULL DEFAULT TRUE,
  "online_from" timestamp,
  "service_state" integer NOT NULL,
  "state_changed" timestamp,
  "warning_state_trigger" bigint,
  "error_state_trigger" bigint,
  "host_registration" bigint NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "UNQ_mh_service_registration_0" UNIQUE ("host_registration", "service_type"),
  CONSTRAINT "FK_service_registration_host_registration" FOREIGN KEY ("host_registration") REFERENCES "mh_host_registration" ("id")
);

CREATE INDEX "IX_mh_service_registration_service_type" ON "mh_service_registration" ("service_type");
CREATE INDEX "IX_mh_service_registration_service_state" ON "mh_service_registration" ("service_state");
CREATE INDEX "IX_mh_service_registration_active" ON "mh_service_registration" ("active");
CREATE INDEX "IX_mh_service_registration_host_registration" ON "mh_service_registration" ("host_registration");

CREATE TABLE "mh_job" (
  "id" bigint NOT NULL,
  "status" integer,
  "payload" text,
  "date_started" timestamp,
  "run_time" bigint,
  "creator" character varying(128) NOT NULL,
  "instance_version" bigint,
  "date_completed" timestamp,
  "operation" character varying(128),
  "dispatchable" boolean DEFAULT TRUE,
  "organization" character varying(128) NOT NULL,
  "date_created" timestamp,
  "queue_time" bigint,
  "creator_service" bigint,
  "processor_service" bigint,
  "parent" bigint,
  "root" bigint,
  PRIMARY KEY ("id"),
  CONSTRAINT "FK_mh_job_creator_service" FOREIGN KEY ("creator_service") REFERENCES "mh_service_registration" ("id"),
  CONSTRAINT "FK_mh_job_processor_service" FOREIGN KEY ("processor_service") REFERENCES "mh_service_registration" ("id"),
  CONSTRAINT "FK_mh_job_parent" FOREIGN KEY ("parent") REFERENCES "mh_job" ("id"),
  CONSTRAINT "FK_mh_job_root" FOREIGN KEY ("root") REFERENCES "mh_job" ("id"),
  CONSTRAINT "FK_mh_job_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE INDEX "IX_mh_job_parent" ON "mh_job" ("parent");
CREATE INDEX "IX_mh_job_root" ON "mh_job" ("root");
CREATE INDEX "IX_mh_job_creator_service" ON "mh_job" ("creator_service");
CREATE INDEX "IX_mh_job_processor_service" ON "mh_job" ("processor_service");
CREATE INDEX "IX_mh_job_status" ON "mh_job" ("status");
CREATE INDEX "IX_mh_job_date_created" ON "mh_job" ("date_created");
CREATE INDEX "IX_mh_job_date_completed" ON "mh_job" ("date_completed");
CREATE INDEX "IX_mh_job_dispatchable" ON "mh_job" ("dispatchable");
CREATE INDEX "IX_mh_job_operation" ON "mh_job" ("operation");

CREATE TABLE "mh_job_argument" (
  "id" bigint NOT NULL,
  "argument" text,
  "argument_index" bigint,
  CONSTRAINT "UNQ_mh_job_argument_0" UNIQUE ("id", "argument_index"),
  CONSTRAINT "FK_mh_job_argument_id" FOREIGN KEY ("id") REFERENCES "mh_job" ("id")
);

CREATE INDEX "IX_mh_job_argument_id" ON "mh_job_argument" ("id");

CREATE TABLE "mh_job_context" (
  "id" bigint NOT NULL,
  "name" character varying(255) NOT NULL,
  "value" text,
  CONSTRAINT "UNQ_mh_job_context_name" UNIQUE ("id", "name"),
  CONSTRAINT "FK_mh_job_context_id" FOREIGN KEY ("id") REFERENCES "mh_job" ("id")
);

CREATE INDEX "IX_mh_job_context_id" ON "mh_job_context" ("id");

CREATE TABLE "mh_user" (
  "username" character varying(128) NOT NULL,
  "organization" character varying(128) NOT NULL,
  "password" text,
  PRIMARY KEY ("username", "organization"),
  CONSTRAINT "FK_mh_user_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE TABLE "mh_role" (
  "username" character varying(128) NOT NULL,
  "organization" character varying(128) NOT NULL,
  "role" text,
  CONSTRAINT "FK_mh_role_username" FOREIGN KEY ("username", "organization") REFERENCES "mh_user" ("username", "organization"),
  CONSTRAINT "FK_mh_role_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE INDEX "IX_mh_role_pk" ON "mh_role" ("username", "organization");

CREATE TABLE "mh_scheduled_event" (
  "id" bigint NOT NULL,
  "capture_agent_metadata" text,
  "dublin_core" text,
  PRIMARY KEY ("id")
);

CREATE TABLE "mh_search" (
  "id" character varying(128) NOT NULL,
  "organization" character varying(128),
  "deletion_date" timestamp,
  "access_control" text,
  "mediapackage_xml" text,
  "modification_date" timestamp,
  PRIMARY KEY ("id"),
  CONSTRAINT "FK_mh_search_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE INDEX "IX_mh_search_organization" ON "mh_search" ("organization");

CREATE TABLE "mh_series" (
  "id" character varying(128) NOT NULL,
  "organization" character varying(128) NOT NULL,
  "access_control" text,
  "dublin_core" text,
  PRIMARY KEY ("id", "organization"),
  CONSTRAINT "FK_mh_series_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE TABLE "mh_upload" (
  "id" character varying(128) NOT NULL,
  "total" bigint NOT NULL,
  "received" bigint NOT NULL,
  "filename" text NOT NULL,
  PRIMARY KEY ("id")
);

CREATE TABLE "mh_user_action" (
  "id" bigint NOT NULL,
  "user_ip" text,
  "inpoint" bigint,
  "outpoint" bigint,
  "mediapackage" character varying(128),
  "session" character varying(128),
  "created" timestamp,
  "user" character varying(255),
  "length" bigint,
  "type" character varying(128),
  "playing" boolean DEFAULT FALSE,
  PRIMARY KEY ("id")
);

CREATE INDEX "IX_mh_user_action_created" ON "mh_user_action" ("created");
CREATE INDEX "IX_mh_user_action_inpoint" ON "mh_user_action" ("inpoint");
CREATE INDEX "IX_mh_user_action_outpoint" ON "mh_user_action" ("outpoint");
CREATE INDEX "IX_mh_user_action_mediapackage" ON "mh_user_action" ("mediapackage");
CREATE INDEX "IX_mh_user_action_user" ON "mh_user_action" ("user");
CREATE INDEX "IX_mh_user_action_session" ON "mh_user_action" ("session");
CREATE INDEX "IX_mh_user_action_type" ON "mh_user_action" ("type");

CREATE TABLE "mh_oaipmh_harvesting" (
  "url" character varying(255) NOT NULL,
  "last_harvested" timestamp,
  PRIMARY KEY (url)
);

CREATE TABLE "mh_episode_episode" (
  "id" character varying(128) NOT NULL,
  "version" bigint NOT NULL,
  "organization" character varying(128),
  "deletion_date" timestamp,
  "access_control" text,
  "mediapackage_xml" text,
  "modification_date" timestamp,
  PRIMARY KEY ("id", "version", "organization"),
  CONSTRAINT "FK_mh_episode_episode_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE INDEX "IX_mh_episode_episode_mediapackage" ON "mh_episode_episode" ("id");
CREATE INDEX "IX_mh_episode_episode_version" ON "mh_episode_episode" ("version");

CREATE TABLE "mh_episode_asset" (
  "id" bigint NOT NULL,
  "mediapackageelement" character varying(128) NOT NULL,
  "mediapackage" character varying(128) NOT NULL,
  "organization" character varying(128) NOT NULL,
  "checksum" character varying(255) NOT NULL,
  "uri" character varying(255) NOT NULL,
  "version" bigint NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "UNQ_mh_episode_asset_0" UNIQUE ("organization", "mediapackage", "mediapackageelement", "version"),
  CONSTRAINT "FK_mh_episode_asset_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id"),
  CONSTRAINT "FK_mh_episode_asset_mediapackage" FOREIGN KEY ("mediapackage", "version", "organization") REFERENCES "mh_episode_episode" ("id", "version", "organization")
);

CREATE INDEX "IX_mh_episode_asset_mediapackage" ON "mh_episode_asset" ("mediapackage");
CREATE INDEX "IX_mh_episode_asset_checksum" ON "mh_episode_asset" ("checksum");
CREATE INDEX "IX_mh_episode_asset_uri" ON "mh_episode_asset" ("uri");

CREATE TABLE "mh_episode_version_claim" (
 "mediapackage" character varying(128) NOT NULL,
 "last_claimed" bigint NOT NULL,
 PRIMARY KEY ("mediapackage")
);

CREATE INDEX "IX_mh_episode_version_claim_mediapackage" ON "mh_episode_version_claim" ("mediapackage");
CREATE INDEX "IX_mh_episode_version_claim_last_claimed" ON "mh_episode_version_claim" ("last_claimed");