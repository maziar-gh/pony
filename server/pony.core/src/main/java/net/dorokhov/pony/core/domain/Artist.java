package net.dorokhov.pony.core.domain;

import net.dorokhov.pony.core.domain.common.BaseEntity;
import net.dorokhov.pony.core.service.SearchAnalyzer;
import org.apache.commons.lang.ObjectUtils;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "artist")
@Indexed
public class Artist extends BaseEntity<Long> implements Comparable<Artist> {

	private String name;

	private Integer songCount = 0;

	private Long songSize = 0L;

	private StoredFile artwork;

	private List<Album> albums;

	@Column(name = "name")
	@Field(analyzer = @Analyzer(impl = SearchAnalyzer.class))
	public String getName() {
		return name;
	}

	public void setName(String aName) {
		name = aName;
	}

	@Column(name = "song_count", nullable = false)
	@NotNull
	public Integer getSongCount() {
		return songCount;
	}

	public void setSongCount(Integer aSongCount) {
		songCount = aSongCount;
	}

	@Column(name = "song_size", nullable = false)
	@NotNull
	public Long getSongSize() {
		return songSize;
	}

	public void setSongSize(Long aSongSize) {
		songSize = aSongSize;
	}

	@OneToOne(optional = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "artwork_stored_file_id")
	public StoredFile getArtwork() {
		return artwork;
	}

	public void setArtwork(StoredFile aArtwork) {
		artwork = aArtwork;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "artist")
	public List<Album> getAlbums() {

		if (albums == null) {
			albums = new ArrayList<>();
		}

		return albums;
	}

	public void setAlbums(List<Album> aAlbums) {
		albums = aAlbums;
	}

	@Override
	@SuppressWarnings("NullableProblems")
	public int compareTo(Artist aArtist) {

		int result = 0;

		if (!equals(aArtist)) {

			String regex = "^the\\s+";

			String name1 = getName() != null ? getName().toLowerCase().replaceAll(regex, "") : null;
			String name2 = aArtist.getName() != null ? aArtist.getName().toLowerCase().replaceAll(regex, "") : null;

			result = ObjectUtils.compare(name1, name2);
		}

		return result;
	}

	@Override
	public String toString() {
		return "Artist{" +
				"id=" + getId() +
				", name='" + name + '\'' +
				'}';
	}
}