package dk.kb.retro;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.CommandLine.Arguments;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.core.Task;

public class RetroTask extends Task {

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed validation results list. */
	private ConcurrentLinkedQueue<RetroFile> results = new ConcurrentLinkedQueue<RetroFile>();

	@Override
	public void command(Arguments arguments) {
		CommandLine.Argument argument;
		argument = arguments.idMap.get( Retro.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				threads = Integer.parseInt(argument.value);
			} catch (NumberFormatException e) {
			}
		}
		argument = arguments.idMap.get( Retro.A_FILES );
		List<String> filesList = argument.values;

		ResultThread resultThread = new ResultThread();
		Thread thread = new Thread(resultThread);
		thread.start();

		init_threadpool(filesList);

		resultThread.bExit = true;
		while (!resultThread.bClosed) {
			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		cout.println("uris: " + uris.size() );
		cout.println("refuris: " + refUris.size());
		cout.println("duplicate uris: " + duplicate_uris);
		cout.println("missing uris: " + missing_uris);
		cout.println("periodicas: " + periodicas.size());
		cout.println("duplicate periodica: " + duplicate_perdiodica);
		cout.println("duplicate periodica length: " + duplicate_periodica_length);
		cout.println("vaerk: " + vaerks.size());
		cout.println("duplicate vaerk: " + duplicate_vaerk);
		cout.println("startsider: " + startsider.size());
		cout.println("duplicate startsider: " + duplicate_startsider);
		cout.println("alm vaerk: " + alm_vaerk.size());
		cout.println("periodica vaerk: " + periodica_vaerk.size());
		cout.println("incorrect monograph: " + incorrect_monograph);
		cout.println("incorrect periodica: " + incorrect_periodica);
	}

	@Override
	public void process(File file) {
		if (file.length() > 0) {
			int fileId = FileIdent.identFile(file);
			if (fileId > 0) {
				executor.submit(new TestRunnable(file));
				++queued;
			} else {
			}
		}
	}

	class TestRunnable implements Runnable {
		File file;
		TestRunnable(File file) {
			this.file = file;
		}
		@Override
		public void run() {
			RetroFile retroFile = new RetroFile();
			//TestFileResult result = retroFile.processFile(file);
			retroFile.processFile(file);
			results.add(retroFile);
			resultsReady.release();
		}
	}

	public Set<String> uris = new HashSet<String>();

	public int duplicate_uris = 0;

	public Set<String> refUris = new HashSet<String>();

	public int missing_uris = 0;

	public Map<Integer, Periodica> periodicas = new TreeMap<Integer, Periodica>();

	public int duplicate_perdiodica = 0;

	public long duplicate_periodica_length = 0;

	public Map<Integer, Vaerk> vaerks = new TreeMap<Integer, Vaerk>();

	public int duplicate_vaerk = 0;

	public Map<String, Startside> startsider = new TreeMap<String, Startside>();

	public int duplicate_startsider = 0;

	public Map<Integer, Vaerk> alm_vaerk = new TreeMap<Integer, Vaerk>();

	public Map<Integer, Vaerk> periodica_vaerk = new TreeMap<Integer, Vaerk>();

	public int incorrect_monograph = 0;

	public int incorrect_periodica = 0;

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			RetroFile result;
			Iterator<String> iter;
			String uri;
			//cout.println("Output Thread started.");
			boolean bLoop = true;
			while (bLoop) {
				try {
					if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
						result = results.poll();
						iter = result.uris.iterator();
						while (iter.hasNext()) {
							uri = iter.next();
							if (!uris.contains(uri)) {
								uris.add(uri);
							} else {
								++duplicate_uris;
							}
						}
						refUris.addAll( result.refUris );
						if (result.periodica != null) {
							if (!periodicas.containsKey(result.periodica.perId)) {
								periodicas.put(result.periodica.perId, result.periodica);
							} else {
								++duplicate_perdiodica;
								duplicate_periodica_length += result.periodica.contentLength;
							}
						}
						if (result.vaerk != null) {
							if (!vaerks.containsKey(result.vaerk.id)) {
								vaerks.put(result.vaerk.id, result.vaerk);
							} else {
								++duplicate_vaerk;
							}
						}
						if (result.startside != null) {
							String key = result.startside.vaerkId + ":" + result.startside.reprId;
							if (!startsider.containsKey(key)) {
								startsider.put(key, result.startside);
							} else {
								++duplicate_startsider;
							}
						}
						++processed;
						cout.print_progress("Queued: " + queued + " - Processed: " + processed + ".");
					} else if (bExit) {
						bLoop = false;
					}
				} catch (InterruptedException e) {
					bLoop = false;
				}
			}

			cout.println("Output Thread stopped.");
			cout.println("Checking for missing URI referenced in warc headers.");
			iter = refUris.iterator();
			while (iter.hasNext()) {
				uri = iter.next();
				if (!uris.contains(uri)) {
					++missing_uris;
				}
			}

			alm_vaerk.putAll( vaerks );
			periodica_vaerk.clear();

			cout.println("Checking for missing vaerk referenced in issues.");
			Iterator<Map.Entry<Integer, Periodica>> pIter = periodicas.entrySet().iterator();
			Map.Entry<Integer, Periodica> pEntry;
			Periodica periodica;
			Issue issue;
			Vaerk vaerk;
			while (pIter.hasNext()) {
				pEntry = pIter.next();
				periodica = pEntry.getValue();
				for (int i=0; i<periodica.issues.size(); ++i) {
					issue = periodica.issues.get(i);
					if (!vaerks.containsKey(issue.vaerkid)) {
						System.out.println("Missing vaerk: " + issue.vaerkid);
					} else {
						vaerk = alm_vaerk.remove(issue.vaerkid);
						issue.vaerk = vaerk;
						periodica_vaerk.put(issue.vaerkid, vaerk);
					}
				}
			}

			Iterator<Map.Entry<Integer, Vaerk>> vIter;
			Map.Entry<Integer, Vaerk> vEntry;

			cout.println("Checking monographs.");
			vIter = alm_vaerk.entrySet().iterator();
			while (vIter.hasNext()) {
				vEntry = vIter.next();
				vaerk = vEntry.getValue();
				if (vaerk == null || vaerk.periodica != 0) {
					++incorrect_monograph;
				}
			}

			cout.println("Checking periodica.");
			vIter = periodica_vaerk.entrySet().iterator();
			while (vIter.hasNext()) {
				vEntry = vIter.next();
				vaerk = vEntry.getValue();
				if (vaerk == null || vaerk.periodica != 1) {
					++incorrect_periodica;
				}
			}

			List<Vaerk> vaerkList = new ArrayList<Vaerk>(alm_vaerk.values());
			Collections.sort(vaerkList);
			Representation representation;

			for (int i=0; i<vaerkList.size(); ++i) {
				vaerk = vaerkList.get(i);
				System.out.println(vaerk.id + " - " + vaerk.titel);
				for (int k=0; k<vaerk.representations.size(); ++k) {
					representation = vaerk.representations.get(k);
					System.out.println(" (" + representation.reprid + "): " + representation.url);
				}
			}

			List<Periodica> periodicaList = new ArrayList<Periodica>(periodicas.values());
			Collections.sort(periodicaList);

			for (int i=0; i<periodicaList.size(); ++i) {
				periodica = periodicaList.get(i);
				System.out.println(periodica.perId);
				for (int j=0; j<periodica.issues.size(); ++j) {
					issue = periodica.issues.get(j);
					if (issue.vaerk != null) {
						System.out.println(issue.numid + " - " + issue.vaerkid + " - " + issue.vaerk.titel);
						for (int k=0; k<issue.vaerk.representations.size(); ++k) {
							representation = issue.vaerk.representations.get(k);
							System.out.println(" (" + representation.reprid + "): " + representation.url);
						}
					} else {
						System.out.println(issue.numid + " - " + issue.vaerkid + " - " + "null!");
					}
				}
			}

			bClosed = true;
		}
	}

}
