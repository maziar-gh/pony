package net.dorokhov.pony.core.upgrade;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UpgradeWorkerLookupServiceImpl implements UpgradeWorkerLookupService, ApplicationContextAware {

	private static final String VERSION_SEPARATOR_REGEX = "\\.";
	private static final Comparator<int[]> VERSION_COMPARATOR = new VersionComparator();

	private ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext aContext) throws BeansException {
		context = aContext;
	}

	@Override
	public List<UpgradeWorker> lookupUpgradeWorkers(String aFromVersion, String aToVersion) {

		int[] fromVersion = stringToVersion(aFromVersion);
		int[] toVersion = stringToVersion(aToVersion);

		List<UpgradeWorker> allWorkers = new ArrayList<>();
		for (Map.Entry<String, UpgradeWorker> entry : context.getBeansOfType(UpgradeWorker.class).entrySet()) {
			allWorkers.add(entry.getValue());
		}

		Collections.sort(allWorkers, new Comparator<UpgradeWorker>() {
			@Override
			public int compare(UpgradeWorker o1, UpgradeWorker o2) {
				return VERSION_COMPARATOR.compare(stringToVersion(o1.getVersion()), stringToVersion(o2.getVersion()));
			}
		});

		List<UpgradeWorker> workersToPerform = new ArrayList<>();
		for (UpgradeWorker worker : allWorkers) {

			int[] workerVersion = stringToVersion(worker.getVersion());

			if (VERSION_COMPARATOR.compare(workerVersion, toVersion) <= 0 && VERSION_COMPARATOR.compare(workerVersion, fromVersion) > 0) {
				workersToPerform.add(worker);
			}
		}

		return workersToPerform;
	}

	private int[] stringToVersion(String aVersion) throws IllegalArgumentException {

		List<Integer> version = new ArrayList<>();

		for (String part : aVersion.split(VERSION_SEPARATOR_REGEX)) {

			String buf = "";

			for (char symbol : part.toCharArray()) {
				if (Character.isDigit(symbol)) {
					buf += symbol;
				} else {
					break;
				}
			}

			if (buf.length() > 0) {
				version.add(Integer.valueOf(buf));
			} else {
				version.add(0);
			}
		}

		return ArrayUtils.toPrimitive(version.toArray(new Integer[version.size()]));
	}

	private static class VersionComparator implements Comparator<int[]> {

		public int compare(int[] version1, int[] version2) {

			int length = version1.length;

			if (version2.length > version1.length) {
				length = version2.length;
			}

			for (int i = 0; i < length; i++) {

				Integer number1 = version1[i];
				Integer number2 = version2[i];

				if (number1.compareTo(number2) < 0) {
					return -1;
				} else if (number2.compareTo(number1) < 0) {
					return 1;
				}
			}

			return 0;
		}
	}

}