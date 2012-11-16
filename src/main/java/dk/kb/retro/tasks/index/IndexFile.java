package dk.kb.retro.tasks.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jwat.arc.ArcRecordBase;
import org.jwat.common.UriProfile;
import org.jwat.gzip.GzipEntry;
import org.jwat.tools.core.ArchiveParser;
import org.jwat.tools.core.ArchiveParserCallback;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcRecord;

public class IndexFile implements ArchiveParserCallback {

	protected List<IndexEntry> entries = new ArrayList<IndexEntry>();

	protected String filemame;

	public IndexFile() {
	}

	public List<IndexEntry> processFile(File file) {
		filemame = file.getName();
		ArchiveParser archiveParser = new ArchiveParser();
		archiveParser.uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
		archiveParser.bBlockDigestEnabled = false;
		archiveParser.bPayloadDigestEnabled = false;
		long consumed = archiveParser.parse(file, this);
		return entries;
	}

	@Override
	public void apcFileId(File file, int fileId) {
	}

	@Override
	public void apcUpdateConsumed(long consumed) {
	}

	@Override
	public void apcGzipEntryStart(GzipEntry gzipEntry, long startOffset) {
	}

	@Override
	public void apcArcRecordStart(ArcRecordBase arcRecord, long startOffset,
			boolean compressed) throws IOException {
	}

	@Override
	public void apcWarcRecordStart(WarcRecord warcRecord, long startOffset,
			boolean compressed) throws IOException {
		if (warcRecord.header.warcTypeIdx == WarcConstants.RT_IDX_RESPONSE) {
			IndexEntry entry = new IndexEntry();
			WarcHeader header = warcRecord.header;
			entry.recordId = header.warcRecordIdStr;
	        entry.offset = startOffset;
	        entry.filename = filemame;
	        entries.add(entry);
		}
		warcRecord.close();
	}

	@Override
	public void apcRuntimeError(Throwable t, long offset, long consumed) {
	}

	@Override
	public void apcDone() {
	}

}
