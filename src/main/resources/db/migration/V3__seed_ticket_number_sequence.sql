-- Single global row used to atomically allocate ticket numbers ('001', '002', ...).
CREATE TABLE tb_ticket_number_sequence (
    id                BIGINT PRIMARY KEY,
    last_issued_value BIGINT NOT NULL
);

INSERT INTO tb_ticket_number_sequence (id, last_issued_value) VALUES (1, 0);
