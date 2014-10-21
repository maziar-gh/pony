package net.dorokhov.pony.core.service.library;

import net.dorokhov.pony.core.common.PageProcessor;
import net.dorokhov.pony.core.dao.AlbumDao;
import net.dorokhov.pony.core.dao.ArtistDao;
import net.dorokhov.pony.core.dao.GenreDao;
import net.dorokhov.pony.core.dao.SongDao;
import net.dorokhov.pony.core.domain.*;
import net.dorokhov.pony.core.service.LogService;
import net.dorokhov.pony.core.service.audio.SongDataWritable;
import net.dorokhov.pony.core.service.file.StoredFileService;
import net.dorokhov.pony.core.service.library.common.LibraryFolder;
import net.dorokhov.pony.core.service.library.common.LibrarySong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;

@Service
public class LibraryServiceImpl implements LibraryService {

	private static final int CLEANING_BUFFER_SIZE = 300;

	private static final String FILE_TAG_ARTWORK_EMBEDDED = "artworkEmbedded";
	private static final String FILE_TAG_ARTWORK_EXTERNAL = "artworkExternal";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private LogService logService;

	private SongDao songDao;

	private AlbumDao albumDao;

	private ArtistDao artistDao;

	private GenreDao genreDao;

	private StoredFileService storedFileService;

	@Autowired
	public void setLogService(LogService aLogService) {
		logService = aLogService;
	}

	@Autowired
	public void setSongDao(SongDao aSongDao) {
		songDao = aSongDao;
	}

	@Autowired
	public void setAlbumDao(AlbumDao aAlbumDao) {
		albumDao = aAlbumDao;
	}

	@Autowired
	public void setArtistDao(ArtistDao aArtistDao) {
		artistDao = aArtistDao;
	}

	@Autowired
	public void setGenreDao(GenreDao aGenreDao) {
		genreDao = aGenreDao;
	}

	@Autowired
	public void setStoredFileService(StoredFileService aStoredFileService) {
		storedFileService = aStoredFileService;
	}

	@Override
	@Transactional
	public long cleanSongs(LibraryFolder aLibrary, final ProgressDelegate aDelegate) {

		final Set<String> librarySongPaths = new HashSet<>();

		for (LibrarySong songFile : aLibrary.getChildSongs(true)) {
			librarySongPaths.add(songFile.getFile().getAbsolutePath());
		}

		final List<Long> itemsToDelete = new ArrayList<>();

		PageProcessor.Handler<Song> handler = new PageProcessor.Handler<Song>() {

			@Override
			public void process(Song aSong, Page<Song> aPage, int aIndexInPage, long aIndexInAll) {

				File file = new File(aSong.getPath());

				if (!librarySongPaths.contains(file.getAbsolutePath()) || !file.exists()) {

					itemsToDelete.add(aSong.getId());

					String message = "Song file not found [" + file.getAbsolutePath() + "], deleting song [" + aSong + "].";

					log.debug(message);
					logService.debug("libraryService.deletingSong", message, Arrays.asList(file.getAbsolutePath(), aSong.toString()));
				}

				if (aDelegate != null) {
					aDelegate.onProgress(aIndexInAll / (double) aPage.getTotalElements());
				}
			}

			@Override
			public Page<Song> getPage(Pageable aPageable) {
				return songDao.findAll(aPageable);
			}
		};
		new PageProcessor<>(CLEANING_BUFFER_SIZE, new Sort("id"), handler).run();

		for (Long id : itemsToDelete) {
			deleteSong(id);
		}

		if (itemsToDelete.size() > 0) {

			String message = "Deleted [" + itemsToDelete.size() + "] songs.";

			log.info(message);
			logService.info("libraryService.deletedSongs", message, Arrays.asList(String.valueOf(itemsToDelete.size())));
		}

		return itemsToDelete.size();
	}

	@Override
	@Transactional
	public long cleanArtworks(final ProgressDelegate aDelegate) {

		final List<Long> itemsToDelete = new ArrayList<>();

		PageProcessor.Handler<StoredFile> storedFileHandler = new PageProcessor.Handler<StoredFile>() {

			@Override
			public void process(StoredFile aStoredFile, Page<StoredFile> aPage, int aIndexInPage, long aIndexInAll) {

				File externalFile = null;

				if (aStoredFile.getUserData() != null) {
					externalFile = new File(aStoredFile.getUserData());
				}

				if (externalFile == null || !externalFile.exists()) {

					String filePath = (externalFile != null ? externalFile.getAbsolutePath() : null);
					String message = "Artwork file not found [" + filePath + "], deleting stored file [" + aStoredFile + "]";

					log.debug(message);
					logService.debug("libraryService.deletingNotFoundStoredFile", message, Arrays.asList(filePath, aStoredFile.toString()));

					itemsToDelete.add(aStoredFile.getId());
				}

				if (aDelegate != null) {
					aDelegate.onProgress(aIndexInAll / (double) aPage.getTotalElements());
				}
			}

			@Override
			public Page<StoredFile> getPage(Pageable aPageable) {
				return storedFileService.getByTag(FILE_TAG_ARTWORK_EXTERNAL, aPageable);
			}
		};
		new PageProcessor<>(CLEANING_BUFFER_SIZE, new Sort("id"), storedFileHandler).run();

		for (final Long id : itemsToDelete) {

			clearSongArtwork(id);
			clearAlbumArtwork(id);
			clearArtistArtwork(id);
			clearGenreArtwork(id);

			storedFileService.deleteById(id);
		}

		if (itemsToDelete.size() > 0) {

			String message = "Deleted [" + itemsToDelete.size() + "] stored files.";

			log.info(message);
			logService.info("libraryService.deletedStoredFiles", message, Arrays.asList(String.valueOf(itemsToDelete.size())));
		}

		return itemsToDelete.size();
	}

	@Override
	public SongImportResult importSong(LibrarySong aSongFile) {
		return null;
	}

	@Override
	public SongImportResult writeAndImportSong(Long aId, SongDataWritable aSongData) {
		return null;
	}

	@Override
	public long importArtworks(ProgressDelegate aDelegate) {
		return 0;
	}

	private void deleteSong(Long aId) {
		// TODO: implement
	}

	private void clearGenreArtwork(final Long aStoredFileId) {
		PageProcessor.Handler<Genre> handler = new PageProcessor.Handler<Genre>() {

			@Override
			public void process(Genre aGenre, Page<Genre> aPage, int aIndexInPage, long aIndexInAll) {

				aGenre.setArtwork(null);

				genreDao.save(aGenre);
			}

			@Override
			public Page<Genre> getPage(Pageable aPageable) {
				return genreDao.findByArtworkId(aStoredFileId, aPageable);
			}
		};
		new PageProcessor<>(CLEANING_BUFFER_SIZE, new Sort("id"), handler).run();
	}

	private void clearArtistArtwork(final Long aStoredFileId) {
		PageProcessor.Handler<Artist> handler = new PageProcessor.Handler<Artist>() {

			@Override
			public void process(Artist aArtist, Page<Artist> aPage, int aIndexInPage, long aIndexInAll) {

				aArtist.setArtwork(null);

				artistDao.save(aArtist);
			}

			@Override
			public Page<Artist> getPage(Pageable aPageable) {
				return artistDao.findByArtworkId(aStoredFileId, aPageable);
			}
		};
		new PageProcessor<>(CLEANING_BUFFER_SIZE, new Sort("id"), handler).run();
	}

	private void clearAlbumArtwork(final Long aStoredFileId) {
		PageProcessor.Handler<Album> handler = new PageProcessor.Handler<Album>() {

			@Override
			public void process(Album aAlbum, Page<Album> aPage, int aIndexInPage, long aIndexInAll) {

				aAlbum.setArtwork(null);

				albumDao.save(aAlbum);
			}

			@Override
			public Page<Album> getPage(Pageable aPageable) {
				return albumDao.findByArtworkId(aStoredFileId, aPageable);
			}
		};
		new PageProcessor<>(CLEANING_BUFFER_SIZE, new Sort("id"), handler).run();
	}

	private void clearSongArtwork(final Long aStoredFileId) {
		PageProcessor.Handler<Song> handler = new PageProcessor.Handler<Song>() {

			@Override
			public void process(Song aSong, Page<Song> aPage, int aIndexInPage, long aIndexInAll) {

				aSong.setArtwork(null);

				songDao.save(aSong);
			}

			@Override
			public Page<Song> getPage(Pageable aPageable) {
				return songDao.findByArtworkId(aStoredFileId, aPageable);
			}
		};
		new PageProcessor<>(CLEANING_BUFFER_SIZE, new Sort("id"), handler).run();
	}
}
