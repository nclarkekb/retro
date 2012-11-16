package dk.kb.retro.tasks.retro;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.arc.ArcDateParser;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.CommandLine.Arguments;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.core.Task;

import dk.kb.retro.Retro;

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

		threadpool_feeder_lifecycle(filesList, this);

		resultThread.bExit = true;
		while (!resultThread.bClosed) {
			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		cout.println("uris: " + uris.size());
		cout.println("refuris: " + refUris.size());
		cout.println("duplicate uris: " + duplicate_uris);
		cout.println("missing uris: " + missing_uris);
		cout.println("periodicas: " + periodicas.size());
		cout.println("duplicate periodica: " + duplicate_perdiodica);
		cout.println("duplicate periodica length: " + duplicate_periodica_length);
		cout.println("vaerk: " + vaerks.size());
		cout.println("duplicate vaerk: " + duplicate_vaerk);
		cout.println("startsider: " + startsider.size());
		cout.println("null vaerk: " + null_vaerk);
		cout.println("null startside url: " + null_startside_url);
		cout.println("missing startside: " + missing_startside);
		cout.println("url trimmed: " + url_trimmed);
		cout.println("null repr urls: " + null_repr_urls);
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

	public int null_vaerk = 0;

	public int null_repr_urls = 0;

	public int null_startside_url = 0;

	public int missing_startside = 0;

	public int url_trimmed = 0;

	public Map<Integer, Vaerk> alm_vaerk = new TreeMap<Integer, Vaerk>();

	public Map<Integer, Vaerk> periodica_vaerk = new TreeMap<Integer, Vaerk>();

	public int incorrect_monograph = 0;

	public int incorrect_periodica = 0;

	public List<Vaerk> incorrectVaerkList = new LinkedList<Vaerk>();

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			RetroFile result;
			Iterator<String> iter;
			String uri;
			String key;
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
							key = result.startside.vaerkId + ":" + result.startside.reprId;
							// debug
							//System.out.println("key: " + key);
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

			Periodica periodica;
			Issue issue;
			Vaerk vaerk;
			Representation representation;
			Startside startside;

			cout.println("Output Thread stopped.");
			cout.println("Checking for missing URI referenced in warc headers.");
			iter = refUris.iterator();
			while (iter.hasNext()) {
				uri = iter.next();
				if (!uris.contains(uri)) {
					++missing_uris;
				}
			}

			cout.println("Checking for missing urls and startsider.");
			Iterator<Vaerk> vaerkIter = vaerks.values().iterator();
			while (vaerkIter.hasNext()) {
				vaerk = vaerkIter.next();
				if (vaerk == null) {
					++null_vaerk;
				} else {
					for (int i=0; i<vaerk.representations.size(); ++i) {
						representation = vaerk.representations.get(i);
						if (representation.url == null) {
							// debug
							System.out.println("null repr url for vaerk " + vaerk.id + " repr " + representation.reprId);
							++null_repr_urls;
						}
						key = representation.vaerkId + ":" + representation.reprId;
						// debug
						//System.out.println("key: " + key);
						startside = startsider.get(key);
						representation.startside = startside;
						if (startside != null) {
							if (startside.targetURI == null) {
								// debug
								System.out.println("null startside url for vaerk " + vaerk.id + " repr " + representation.reprId);
								++null_startside_url;
							}
							String url = startside.targetURI.trim();
							if (url.compareTo(startside.targetURI) != 0) {
								representation.url = url;
								++url_trimmed;
							}
						} else {
							// debug
							System.out.println("null startside for vaerk " + vaerk.id + " repr " + representation.reprId);
							++missing_startside;
						}
						if (representation.url != null) {
							String url = representation.url.trim();
							if (url.compareTo(representation.url) != 0) {
								representation.url = url;
								++url_trimmed;
							}
						}
					}
				}
			}

			alm_vaerk.putAll( vaerks );
			periodica_vaerk.clear();

			cout.println("Checking for missing vaerk referenced in issues.");
			Iterator<Map.Entry<Integer, Periodica>> pIter = periodicas.entrySet().iterator();
			Map.Entry<Integer, Periodica> pEntry;
			while (pIter.hasNext()) {
				pEntry = pIter.next();
				periodica = pEntry.getValue();
				for (int i=0; i<periodica.issues.size(); ++i) {
					issue = periodica.issues.get(i);
					if (!vaerks.containsKey(issue.vaerkId)) {
						System.out.println("Missing vaerk: " + issue.vaerkId);
					} else {
						vaerk = alm_vaerk.remove(issue.vaerkId);
						if (vaerk != null) {
							issue.vaerk = vaerk;
							periodica_vaerk.put(issue.vaerkId, vaerk);
						} else {
						}
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
					incorrectVaerkList.add(vaerk);
					++incorrect_monograph;
				}
			}

			cout.println("Checking periodica.");
			vIter = periodica_vaerk.entrySet().iterator();
			while (vIter.hasNext()) {
				vEntry = vIter.next();
				vaerk = vEntry.getValue();
				if (vaerk == null || vaerk.periodica != 1) {
					incorrectVaerkList.add(vaerk);
					++incorrect_periodica;
				}
			}

			List<Vaerk> vaerkList = new ArrayList<Vaerk>(alm_vaerk.values());
			Collections.sort(vaerkList);

			for (int i=0; i<vaerkList.size(); ++i) {
				vaerk = vaerkList.get(i);
				// debug
				//System.out.println(vaerk.id + " - " + vaerk.titel);
				for (int k=0; k<vaerk.representations.size(); ++k) {
					representation = vaerk.representations.get(k);
					// debug
					//System.out.println(" (" + representation.reprid + "): " + representation.url);
				}
			}

			List<Periodica> periodicaList = new ArrayList<Periodica>(periodicas.values());
			Collections.sort(periodicaList);

			for (int i=0; i<periodicaList.size(); ++i) {
				periodica = periodicaList.get(i);
				// debug
				//System.out.println(periodica.perId);
				for (int j=0; j<periodica.issues.size(); ++j) {
					issue = periodica.issues.get(j);
					if (issue.vaerk != null) {
						// debug
						//System.out.println(issue.numid + " - " + issue.vaerkid + " - " + issue.vaerk.titel);
						for (int k=0; k<issue.vaerk.representations.size(); ++k) {
							representation = issue.vaerk.representations.get(k);
							// debug
							//System.out.println(" (" + representation.reprid + "): " + representation.url);
						}
					} else {
						// debug
						System.out.println(issue.numId + " - " + issue.vaerkId + " - " + "null!");
					}
				}
			}

			/*
			for (int i=0; i<incorrectVaerkList.size(); ++i) {
				vaerk = incorrectVaerkList.get(i);
				System.out.println("incorrect vaerk: " + vaerk.id);
			}
			*/

			html_monografier(vaerkList);
			html_periodica(periodicaList);

			bClosed = true;
		}
	}

	protected static DateFormat arcDateFormat = ArcDateParser.getDateFormat();

	public void html_monografier(List<Vaerk> vaerkList) {
		Vaerk vaerk;
		Representation representation;
		String url;
		Date date;

		StringBuilder sb = new StringBuilder();
		try {
			sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
			sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
			sb.append("<head>\n");
			sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n");
			sb.append("<meta name=\"vs_defaultClientScript\" content=\"JavaScript\" />\n");
			sb.append("<meta name=\"vs_targetSchema\" content=\"http://schemas.microsoft.com/intellisense/ie5\" />\n");
			sb.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=EmulateIE7\" />\n");
			sb.append("<title>Monografier</title>");
			sb.append("</head>\n");
			sb.append("<body>\n");
			for (int i=0; i<vaerkList.size(); ++i) {
				vaerk = vaerkList.get(i);
				sb.append("<div>");
				sb.append(vaerk.id + ": " + vaerk.titel);
				sb.append("<br />\n");

				sb.append("<ul>\n");
				for (int k=0; k<vaerk.representations.size(); ++k) {
					representation = vaerk.representations.get(k);
					sb.append("<li>\n");
					sb.append(" (" + representation.reprId + "): ");
					if (representation.startside != null) {
						url = representation.startside.targetURI;
						date = representation.startside.date;
						sb.append("<a href=\"");
						sb.append("http://dia-prod-udv-01.kb.dk:8080/jsp/QueryUI/Redirect.jsp?url=");
						//sb.append("http%3A%2F%2Fwww.1899.dk%2Fartikler%2Fimages%2Fpol_hm01.jpg");
						// debug
						//System.out.println(representation.url.trim());
						url = URLEncoder.encode(url, "UTF-8");
						sb.append(url);
						url.replaceAll(":", "%3A");
						url.replaceAll("/", "%2F");
						sb.append("&time=");
						//sb.append("20041005122705");
						sb.append(arcDateFormat.format(date));
						sb.append("\">");
					} else {
						sb.append("Mangler startside!");
					}
					sb.append(representation.url);
					sb.append("</a>");
					sb.append("</li>\n");
				}
				sb.append("</ul>\n");

				sb.append("</div>");
			}
			sb.append("</body>\n");
			sb.append("</html>\n");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		try {
			byte[] bytes = sb.toString().getBytes("UTF-8");
			RandomAccessFile raf = new RandomAccessFile(new File("mono.html"), "rw");
			raf.seek(0);
			raf.setLength(0);
			raf.write(bytes);
			raf.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void html_periodica(List<Periodica> periodicaList) {
		Periodica periodica;
		List<Issue> issues;
		Issue issue;
		Vaerk vaerk;
		Representation representation;
		String url;
		Date date;

		StringBuilder sb = new StringBuilder();
		try {
			sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
			sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
			sb.append("<head>\n");
			sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n");
			sb.append("<meta name=\"vs_defaultClientScript\" content=\"JavaScript\" />\n");
			sb.append("<meta name=\"vs_targetSchema\" content=\"http://schemas.microsoft.com/intellisense/ie5\" />\n");
			sb.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=EmulateIE7\" />\n");
			sb.append("<title>Periodica</title>");
			sb.append("</head>\n");
			sb.append("<body>\n");
			for (int i=0; i<periodicaList.size(); ++i) {
				periodica = periodicaList.get(i);
				issues = periodica.issues;
				vaerk = issues.get(0).vaerk;
				sb.append("<div>");
				sb.append(vaerk.id + ": " + vaerk.titel);
				sb.append("<br />\n");

				sb.append("<ul>\n");
				for (int j=0; j<issues.size(); ++j) {
					issue = issues.get(0);
					vaerk = issue.vaerk;
					sb.append("<li>\n");
					sb.append(vaerk.id + ": " + vaerk.titel);
					sb.append("</li>\n");

					sb.append("<ul>\n");
					for (int k=0; k<vaerk.representations.size(); ++k) {
						representation = vaerk.representations.get(k);
						sb.append("<li>\n");
						sb.append(" (" + representation.reprId + "): ");
						if (representation.startside != null) {
							url = representation.startside.targetURI;
							date = representation.startside.date;
							sb.append("<a href=\"");
							sb.append("http://dia-prod-udv-01.kb.dk:8080/jsp/QueryUI/Redirect.jsp?url=");
							//sb.append("http%3A%2F%2Fwww.1899.dk%2Fartikler%2Fimages%2Fpol_hm01.jpg");
							// debug
							//System.out.println(representation.url.trim());
							url = URLEncoder.encode(url, "UTF-8");
							sb.append(url);
							url.replaceAll(":", "%3A");
							url.replaceAll("/", "%2F");
							sb.append("&time=");
							//sb.append("20041005122705");
							sb.append(arcDateFormat.format(date));
							sb.append("\">");
						} else {
							sb.append("Mangler startside!");
						}
						sb.append(representation.url);
						sb.append("</a>");
						sb.append("</li>\n");
					}
					sb.append("</ul>\n");

				}
				sb.append("</ul>\n");

				sb.append("</div>");
			}
			sb.append("</body>\n");
			sb.append("</html>\n");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		try {
			byte[] bytes = sb.toString().getBytes("UTF-8");
			RandomAccessFile raf = new RandomAccessFile(new File("periodica.html"), "rw");
			raf.seek(0);
			raf.setLength(0);
			raf.write(bytes);
			raf.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
