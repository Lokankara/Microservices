CREATE TABLE IF NOT EXISTS songs (
    id       BIGINT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    artist   VARCHAR(100) NOT NULL,
    album    VARCHAR(100) NOT NULL,
    duration VARCHAR(5)   NOT NULL,
    "year"   VARCHAR(4)   NOT NULL
);

INSERT INTO songs (id, name, artist, album, duration, "year")
VALUES (100001, 'Midnight City', 'M83', 'Hurry Up, We''re Dreaming', '04:03', '2011');

INSERT INTO songs (id, name, artist, album, duration, "year")
VALUES (100002, 'Harder, Better, Faster, Stronger', 'Daft Punk', 'Discovery', '03:44', '2001');
