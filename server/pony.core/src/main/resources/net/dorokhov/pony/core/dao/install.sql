CREATE TABLE installation (

	id BIGINT IDENTITY,

	creation_date TIMESTAMP NOT NULL,
	update_date TIMESTAMP NOT NULL,

	version VARCHAR(255) NOT NULL
);

CREATE TABLE config (

	id VARCHAR(255) NOT NULL,

	creation_date TIMESTAMP NOT NULL,
	update_date TIMESTAMP NOT NULL,

	value LONGVARCHAR,

	PRIMARY KEY (id)
);

CREATE TABLE stored_file (

	id BIGINT IDENTITY,

	creation_date TIMESTAMP NOT NULL,
	update_date TIMESTAMP NOT NULL,

	name VARCHAR(255) NOT NULL,
	mime_type VARCHAR(255) NOT NULL,
	checksum VARCHAR(255) NOT NULL,
	tag VARCHAR(255),
	relative_path VARCHAR(255) NOT NULL,
	user_data LONGVARCHAR,

	UNIQUE (relative_path),
	UNIQUE (tag, checksum)
);

CREATE INDEX index_stored_file_checksum ON stored_file(checksum);
CREATE INDEX index_stored_file_tag ON stored_file(tag);

CREATE TABLE artist (

	id BIGINT IDENTITY,

	creation_date TIMESTAMP NOT NULL,
	update_date TIMESTAMP NOT NULL,

	name VARCHAR(255) NOT NULL,
	artwork_stored_file_id BIGINT,

	FOREIGN KEY (artwork_stored_file_id) REFERENCES stored_file(id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE INDEX index_artist_name ON artist(name);

CREATE TABLE album (

	id BIGINT IDENTITY,

	creation_date TIMESTAMP NOT NULL,
	update_date TIMESTAMP NOT NULL,

	name VARCHAR(255) NOT NULL,
	year INT,
	artwork_stored_file_id BIGINT,

	artist_id BIGINT NOT NULL,

	FOREIGN KEY (artist_id) REFERENCES artist(id) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (artwork_stored_file_id) REFERENCES stored_file(id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE INDEX index_album_name_artist_id ON album(name, artist_id);
CREATE INDEX index_album_artist_id_year_name ON album(artist_id, year, name);

CREATE TABLE song (

	id BIGINT IDENTITY,

	creation_date TIMESTAMP NOT NULL,
	update_date TIMESTAMP NOT NULL,

	path VARCHAR(255) NOT NULL,
	format VARCHAR(255) NOT NULL,
	mime_type VARCHAR(255) NOT NULL,
	size BIGINT NOT NULL,

	duration INT NOT NULL,
	bit_rate BIGINT NOT NULL,

	disc_number INT,
	disc_count INT,

	track_number INT,
	track_count INT,

	name VARCHAR(255),
	artist_name VARCHAR(255),
	album_artist_name VARCHAR(255),
	album_name VARCHAR(255),

	year INT,

	artwork_stored_file_id BIGINT,

	album_id BIGINT NOT NULL,

	FOREIGN KEY (album_id) REFERENCES album(id) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (artwork_stored_file_id) REFERENCES stored_file(id) ON DELETE SET NULL ON UPDATE CASCADE,

	UNIQUE (path)
);

CREATE INDEX index_song_track_number_name ON song(disc_number, track_number, name);

INSERT INTO installation (creation_date, update_date, version) VALUES (NOW(), NOW(), '1.0');