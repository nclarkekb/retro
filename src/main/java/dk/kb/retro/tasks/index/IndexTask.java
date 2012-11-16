package dk.kb.retro.tasks.index;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.core.SynchronizedOutput;
import org.jwat.tools.core.Task;

public class IndexTask extends Task {

	/** Valid results output stream. */
	private SynchronizedOutput indexOutput;

	public IndexTask() {
	}

	@Override
	public void command(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;
		argument = arguments.idMap.get( JWATTools.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				threads = Integer.parseInt(argument.value);
			} catch (NumberFormatException e) {
			}
		}
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		indexOutput = new SynchronizedOutput("index-unsorted.out");

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
	}

	@Override
	public void process(File srcFile) {
		if (srcFile.length() > 0) {
			int fileId = FileIdent.identFile(srcFile);
			if (fileId > 0) {
				executor.submit(new TaskRunnable(srcFile));
				++queued;
			} else {
			}
		}
	}

	class TaskRunnable implements Runnable {
		File srcFile;
		TaskRunnable(File srcFile) {
			this.srcFile = srcFile;
		}
		@Override
		public void run() {
			IndexFile indexFile = new IndexFile();
			//testFile.callback = null;
			List<IndexEntry> entries = indexFile.processFile(srcFile);
			results.add(entries);
			resultsReady.release();
		}
	}

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed validation results list. */
	private ConcurrentLinkedQueue<List<IndexEntry>> results = new ConcurrentLinkedQueue<List<IndexEntry>>();

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			List<IndexEntry> entries;
			IndexEntry entry;
			boolean bLoop = true;
			while (bLoop) {
				try {
					if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
						entries = results.poll();
						indexOutput.acquire();
						for (int i=0; i<entries.size(); ++i) {
							entry = entries.get(i);
							indexOutput.out.println(indexEntry(entry));
						}
						indexOutput.release();
						++processed;
						cout.print_progress("Queued: " + queued + " - Processed: " + processed + ".");
					} else if (bExit) {
						bLoop = false;
					}
				} catch (InterruptedException e) {
					bLoop = false;
				}
			}
			bClosed = true;
		}

		public String indexEntry(IndexEntry indexEntry) {
			StringBuilder sb = new StringBuilder();
			sb.setLength(0);
			sb.append(indexEntry.recordId);
			sb.append(" ");
			sb.append(indexEntry.offset);
			sb.append(" ");
			sb.append(indexEntry.filename);
			return sb.toString();
		}

	}

}
