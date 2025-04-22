CREATE TABLE tokens
(
    id           VARCHAR NOT NULL,
    user_id      BIGINT  NOT NULL,
    expired_date date    NOT NULL,
    CONSTRAINT tokens_pkey PRIMARY KEY (id)
);