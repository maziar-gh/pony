package net.dorokhov.pony.web.shared;

import net.dorokhov.pony.web.client.util.ObjectUtils;

import java.util.Date;

public class SongDto extends AbstractDto<Long> implements Comparable<SongDto> {

	private Date updateDate;

	private String url;

	private Long size;

	private Integer duration;

	private Integer discNumber;

	private Integer trackNumber;

	private String artistName;

	private String name;

	private AlbumDto album;

	private GenreDto genre;

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date aUpdateDate) {
		updateDate = aUpdateDate;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String aUrl) {
		url = aUrl;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long aSize) {
		size = aSize;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer aDuration) {
		duration = aDuration;
	}

	public Integer getDiscNumber() {
		return discNumber;
	}

	public void setDiscNumber(Integer aDiscNumber) {
		discNumber = aDiscNumber;
	}

	public Integer getTrackNumber() {
		return trackNumber;
	}

	public void setTrackNumber(Integer aTrackNumber) {
		trackNumber = aTrackNumber;
	}

	public String getArtistName() {
		return artistName;
	}

	public void setArtistName(String aArtistName) {
		artistName = aArtistName;
	}

	public String getName() {
		return name;
	}

	public void setName(String aName) {
		name = aName;
	}

	public GenreDto getGenre() {
		return genre;
	}

	public void setGenre(GenreDto aGenre) {
		genre = aGenre;
	}

	public AlbumDto getAlbum() {
		return album;
	}

	public void setAlbum(AlbumDto aAlbum) {
		album = aAlbum;
	}

	@Override
	public String toString() {
		return "SongDto{" +
				"name='" + name + '\'' +
				", album=" + album +
				'}';
	}

	@Override
	@SuppressWarnings("NullableProblems")
	public int compareTo(SongDto aSong) {

		int result = 0;

		if (!equals(aSong)) {

			result = ObjectUtils.compare(getArtistName(), aSong.getArtistName());

			if (result == 0) {
				result = ObjectUtils.compare(getAlbum().getYear(), aSong.getAlbum().getYear());
			}
			if (result == 0) {
				result = ObjectUtils.compare(getAlbum().getName(), aSong.getAlbum().getName());
			}

			if (result == 0) {

				Integer discNumber1 = getDiscNumber() != null ? getDiscNumber() : 1;
				Integer discNumber2 = aSong.getDiscNumber() != null ? aSong.getDiscNumber() : 1;

				result = ObjectUtils.compare(discNumber1, discNumber2);
			}
			if (result == 0) {

				Integer trackNumber1 = getTrackNumber() != null ? getTrackNumber() : 1;
				Integer trackNumber2 = aSong.getTrackNumber() != null ? aSong.getTrackNumber() : 1;

				result = ObjectUtils.compare(trackNumber1, trackNumber2);
			}
			if (result == 0) {
				result = ObjectUtils.compare(getName(), aSong.getName());
			}
		}

		return result;
	}
}
