package net.dorokhov.pony.web.domain;

import java.util.ArrayList;
import java.util.List;

public class AlbumSongsDto extends AlbumDto {

	private List<SongDto> songs;

	public List<SongDto> getSongs() {

		if (songs == null) {
			songs = new ArrayList<>();
		}

		return songs;
	}

	public void setSongs(List<SongDto> aSongs) {
		songs = aSongs;
	}
}
