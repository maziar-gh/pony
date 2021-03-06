package net.dorokhov.pony.core.storage;

import net.dorokhov.pony.core.common.PonyUtils;
import net.dorokhov.pony.core.dao.StoredFileDao;
import net.dorokhov.pony.core.domain.StoredFile;
import net.dorokhov.pony.core.file.FileTypeService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;

@Service
public class StoredFileServiceImpl implements StoredFileService {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Object lock = new Object();

	private StoredFileDao storedFileDao;

	private FileTypeService fileTypeService;

	private String storagePath;

	private File filesFolder;

	@Autowired
	public void setStoredFileDao(StoredFileDao aStoredFileDao) {
		storedFileDao = aStoredFileDao;
	}

	@Autowired
	public void setFileTypeService(FileTypeService aFileTypeService) {
		fileTypeService = aFileTypeService;
	}

	@Value("${storage.path}")
	public void setStoragePath(String aStoragePath) {
		storagePath = aStoragePath;
	}

	@PostConstruct
	public void postConstruct() {
		createFilesFolder();
	}

	@Override
	@Transactional(readOnly = true)
	public long getCount() {
		return storedFileDao.count();
	}

	@Override
	@Transactional(readOnly = true)
	public long getCountByTag(String aTag) {
		return storedFileDao.countByTag(aTag);
	}

	@Override
	@Transactional(readOnly = true)
	public long getCountByTagAndMinimalDate(String aTag, Date aMinimalDate) {
		return storedFileDao.countByTagAndDateGreaterThan(aTag, aMinimalDate);
	}

	@Override
	@Transactional(readOnly = true)
	public long getSizeByTag(String aTag) {

		Long size = storedFileDao.sumSizeByTag(aTag);

		return size != null ? size : 0;
	}

	@Override
	@Transactional(readOnly = true)
	public StoredFile getById(Long aId) {
		return storedFileDao.findOne(aId);
	}

	@Override
	@Transactional(readOnly = true)
	public StoredFile getByTagAndChecksum(String aTag, String aChecksum) {
		return storedFileDao.findByTagAndChecksum(aTag, aChecksum);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<StoredFile> getAll(Pageable aPageable) {
		return storedFileDao.findAll(aPageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<StoredFile> getByTag(String aTag, Pageable aPageable) {
		return storedFileDao.findByTag(aTag, aPageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredFile> getByChecksum(String aChecksum) {
		return storedFileDao.findByChecksum(aChecksum);
	}

	@Override
	@Transactional(readOnly = true)
	public File getFile(Long aId) {
		return getFile(getById(aId));
	}

	@Override
	@Transactional(readOnly = true)
	public File getFile(StoredFile aStoredFile) {
		return new File(filesFolder, aStoredFile.getPath());
	}

	@Override
	@Transactional
	public StoredFile save(StoreFileCommand aCommand) {

		if (!aCommand.getFile().exists()) {
			throw new RuntimeException(new FileNotFoundException("File [" + aCommand.getFile().getAbsolutePath() + "] not found."));
		}
		if (aCommand.getFile().isDirectory()) {
			throw new RuntimeException("File [" + aCommand.getFile().getAbsolutePath() + "] is directory.");
		}

		File targetFile = null;

		try {

			String relativePath;

			synchronized (lock) {

				relativePath = commandToPath(aCommand);

				targetFile = new File(filesFolder, relativePath);

				switch (aCommand.getType()) {

					case COPY:
						FileUtils.copyFile(aCommand.getFile(), targetFile);
						break;

					case MOVE:
						FileUtils.moveFile(aCommand.getFile(), targetFile);
						break;

					default:
						throw new RuntimeException("Storage command type cannot be null.");
				}
			}

			final File fileToDeleteOnRollback = targetFile;

			StoredFile storedFile = new StoredFile();

			storedFile.setName(aCommand.getName());
			storedFile.setMimeType(aCommand.getMimeType());
			storedFile.setChecksum(aCommand.getChecksum());
			storedFile.setSize(targetFile.length());
			storedFile.setTag(aCommand.getTag());
			storedFile.setUserData(aCommand.getUserData());
			storedFile.setPath(relativePath);

			storedFile = storedFileDao.save(storedFile);

			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void afterCompletion(int aStatus) {
					if (aStatus != STATUS_COMMITTED) {
						fileToDeleteOnRollback.delete();
					}
				}
			});

			return storedFile;

		} catch (Exception e) {

			if (targetFile != null) {
				targetFile.delete();
			}

			throw new RuntimeException(e);
		}
	}

	@Override
	@Transactional
	public void delete(Long aId) {

		StoredFile storedFile = getById(aId);

		if (storedFile != null) {
			delete(storedFile);
		}
	}

	@Override
	@Transactional
	public void deleteAll() {

		storedFileDao.deleteAll();

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
			@Override
			public void afterCommit() {
				try {
					FileUtils.cleanDirectory(filesFolder);
				} catch (Exception e) {
					log.warn("could not clean storage folder", e);
				}
			}
		});
	}

	private void createFilesFolder() {

		File userHome = FileUtils.getUserDirectory();

		filesFolder = new File(userHome, storagePath + "/files");

		if (!filesFolder.exists()) {
			if (!filesFolder.mkdirs()) {
				throw new RuntimeException("Could not create directory [" + filesFolder.getAbsolutePath() + "] for storing files.");
			}
		}
	}

	private String commandToPath(StoreFileCommand aCommand) {

		File file;

		int attempt = 0;

		do {

			StringBuilder buf = new StringBuilder(StringUtils.hasText(aCommand.getTag()) ? aCommand.getTag().trim() + "/" : "");

			// Don't put too many files into one folder
			buf.append(RandomStringUtils.random(2, false, true)).append("/")
					.append(RandomStringUtils.random(2, false, true)).append("/");

			// Append task name or file name
			if (aCommand.getName() != null) {
				buf.append(PonyUtils.sanitizeFileName(aCommand.getName()));
			} else {
				buf.append(aCommand.getFile().getName());
			}

			// Append attempt number (to guarantee file name to be unique)
			if (attempt > 0) {
				buf.append(attempt);
			}

			// Append type extension
			String fileExtension = fileTypeService.getFileExtension(aCommand.getMimeType());
			if (fileExtension != null) {
				buf.append(".").append(fileExtension);
			}

			file = new File(buf.toString());

			attempt++;

		} while (file.exists());

		return file.getPath();
	}

	private void delete(StoredFile aStoredFile) {

		final File file = new File(filesFolder, aStoredFile.getPath());

		storedFileDao.delete(aStoredFile);

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
			@Override
			public void afterCommit() {
				if (!file.delete()) {
					log.warn("Could not delete file [{}] from file system.", file.getAbsolutePath());
				}
			}
		});
	}

}
