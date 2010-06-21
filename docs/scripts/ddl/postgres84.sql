CREATE TABLE annotation (
    id integer NOT NULL,
    outpoint integer,
    inpoint integer,
    media_package_id character varying(255),
    session_id character varying(255),
    created timestamp without time zone,
    length integer,
    annotation_val character varying(255),
    annotation_key character varying(255)
);

CREATE TABLE capture_agent (
    name character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    capabilities bytea,
    lastheardfrom bigint NOT NULL,
    url character varying(255)
);

CREATE TABLE job (
    id character varying(255) NOT NULL,
    host character varying(255),
    status integer,
    element_xml text,
    type character varying(255)
);

CREATE TABLE sched_event (
    id character varying(128) NOT NULL
);

CREATE TABLE sched_event_metadata (
    event_id character varying(128) NOT NULL,
    metadata_id bigint NOT NULL
);

CREATE TABLE sched_metadata (
    id bigint NOT NULL,
    md_val character varying(255),
    md_key character varying(255)
);

CREATE TABLE sched_r_event (
    id character varying(128) NOT NULL,
    recurrence character varying(255)
);

CREATE TABLE sched_r_event_item (
    rec_event_id character varying(128) NOT NULL,
    event_id character varying(128) NOT NULL
);

CREATE TABLE sched_r_event_metadata (
    rec_event_id character varying(128) NOT NULL,
    md_id bigint NOT NULL
);

CREATE TABLE sequence (
    seq_name character varying(50) NOT NULL,
    seq_count numeric(38,0)
);

INSERT INTO SEQUENCE VALUES('SEQ_GEN', 50);

CREATE TABLE series (
    id character varying(128) NOT NULL
);

CREATE TABLE series_metadata (
    metadata_key character varying(128) NOT NULL,
    metadata_val character varying(256),
    series_id character varying(128) NOT NULL
);

CREATE TABLE service_registration (
    id bigint NOT NULL,
    host character varying(255),
    job_type character varying(255)
);

CREATE TABLE upload (
    id character varying(255) NOT NULL,
    total bigint NOT NULL,
    received bigint NOT NULL,
    filename character varying(255) NOT NULL
);

ALTER TABLE ONLY annotation
    ADD CONSTRAINT annotation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY capture_agent
    ADD CONSTRAINT capture_agent_pkey PRIMARY KEY (name);

ALTER TABLE ONLY job
    ADD CONSTRAINT job_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sched_event_metadata
    ADD CONSTRAINT sched_event_metadata_pkey PRIMARY KEY (event_id, metadata_id);

ALTER TABLE ONLY sched_event
    ADD CONSTRAINT sched_event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sched_metadata
    ADD CONSTRAINT sched_metadata_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sched_r_event_item
    ADD CONSTRAINT sched_r_event_item_pkey PRIMARY KEY (rec_event_id, event_id);

ALTER TABLE ONLY sched_r_event_metadata
    ADD CONSTRAINT sched_r_event_metadata_pkey PRIMARY KEY (rec_event_id, md_id);

ALTER TABLE ONLY sched_r_event
    ADD CONSTRAINT sched_r_event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sequence
    ADD CONSTRAINT sequence_pkey PRIMARY KEY (seq_name);

ALTER TABLE ONLY series_metadata
    ADD CONSTRAINT series_metadata_pkey PRIMARY KEY (metadata_key, series_id);

ALTER TABLE ONLY series
    ADD CONSTRAINT series_pkey PRIMARY KEY (id);

ALTER TABLE ONLY service_registration
    ADD CONSTRAINT service_registration_pkey PRIMARY KEY (id);

ALTER TABLE ONLY upload
    ADD CONSTRAINT upload_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sched_event_metadata
    ADD CONSTRAINT fk_sched_event_metadata_event_id FOREIGN KEY (event_id) REFERENCES sched_event(id);

ALTER TABLE ONLY sched_event_metadata
    ADD CONSTRAINT fk_sched_event_metadata_metadata_id FOREIGN KEY (metadata_id) REFERENCES sched_metadata(id);

ALTER TABLE ONLY sched_r_event_item
    ADD CONSTRAINT fk_sched_r_event_item_event_id FOREIGN KEY (event_id) REFERENCES sched_event(id);

ALTER TABLE ONLY sched_r_event_item
    ADD CONSTRAINT fk_sched_r_event_item_rec_event_id FOREIGN KEY (rec_event_id) REFERENCES sched_r_event(id);

ALTER TABLE ONLY sched_r_event_metadata
    ADD CONSTRAINT fk_sched_r_event_metadata_md_id FOREIGN KEY (md_id) REFERENCES sched_metadata(id);

ALTER TABLE ONLY sched_r_event_metadata
    ADD CONSTRAINT fk_sched_r_event_metadata_rec_event_id FOREIGN KEY (rec_event_id) REFERENCES sched_r_event(id);

ALTER TABLE ONLY series_metadata
    ADD CONSTRAINT fk_series_metadata_series_id FOREIGN KEY (series_id) REFERENCES series(id);
