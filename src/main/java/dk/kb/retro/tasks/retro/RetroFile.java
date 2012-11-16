package dk.kb.retro.tasks.retro;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jwat.arc.ArcRecordBase;
import org.jwat.common.ContentType;
import org.jwat.common.HeaderLine;
import org.jwat.common.UriProfile;
import org.jwat.gzip.GzipEntry;
import org.jwat.tools.core.ArchiveParser;
import org.jwat.tools.core.ArchiveParserCallback;
import org.jwat.tools.validators.XmlValidatorPlugin;
import org.jwat.tools.validators.XmlValidatorPlugin.XmlValidator;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcRecord;
import org.w3c.dom.Node;

public class RetroFile implements ArchiveParserCallback {

	public static XmlValidatorPlugin xmlValidatorPlugin = new XmlValidatorPlugin();

	public XmlValidator xmlValidator = xmlValidatorPlugin.getValidator();

	public Set<String> uris = new HashSet<String>();

	public Set<String> refUris = new HashSet<String>();

	public Periodica periodica; 

	public Vaerk vaerk;

	public Startside startside;

	public RetroFile() {
	}

	protected File file;

	public RetroFile processFile(File file) {
		this.file = file;
		ArchiveParser archiveParser = new ArchiveParser();
		archiveParser.uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
		archiveParser.bBlockDigestEnabled = false;
		archiveParser.bPayloadDigestEnabled = false;
		long consumed = archiveParser.parse(file, this);
		return this;
	}

	@Override
	public void apcFileId(File file, int fileId) {
		//System.out.println(fileId);
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
		try {
			WarcHeader header = warcRecord.header;
			ContentType contentType = header.contentType;
			if (contentType != null && "text".equalsIgnoreCase(contentType.contentType) && "xml".equalsIgnoreCase(contentType.mediaType)) {
				//System.out.println(contentType.toString());
				//System.out.println(warcRecord.header.warcRecordIdStr);
				if (warcRecord.hasPayload()) {
					xmlValidator.parse(warcRecord.getPayload().getInputStream(), null);
					if (xmlValidator.document != null) {
						//System.out.println(xmlValidator.document);
						//System.out.println(xmlValidator.document.getDocumentElement().getNodeName());
						Node rootNode = xmlValidator.document.getDocumentElement();
						String rootName = rootNode.getNodeName();
						if ("periodica".equals(rootName)) {
							periodica = Periodica.fromXml(rootNode);
							periodica.targetURI = header.warcTargetUriStr;
							periodica.date = header.warcDate;
							periodica.contentLength = warcRecord.header.contentLength;
							// debug
							//System.out.println(periodica.perId);
						} else if ("vaerk".equals(rootName)) {
							vaerk = Vaerk.fromXml(rootNode);
							vaerk.targetURI = header.warcTargetUriStr;
							vaerk.date = header.warcDate;
							vaerk.contentLength = warcRecord.header.contentLength;
							// debug
							//System.out.println(vaerk.id);
						}
					}
				}
			}
			warcRecord.close();
			if (header.warcRecordIdStr != null) {
				uris.add(header.warcRecordIdStr);
			}
			if (header.warcWarcinfoIdStr != null) {
				refUris.add(header.warcWarcinfoIdStr);
			}
			if (header.warcRefersToStr != null) {
				refUris.add(header.warcRefersToStr);
			}
			for (int i=0; i<header.warcConcurrentToList.size(); ++i) {
				refUris.add(header.warcConcurrentToList.get(i).warcConcurrentToStr);
			}

			/*
			 * Startsider.
			 */

			/*
			WARC-Target-URI: http://www.plantedir.dk/publ/VR97/index.htm
			X-Original-Date: Mon, 13 Sep 1999 09:37:09 GMT
			X-Original-Vaerkid: 2947
			X-Original-Reprid: 0
			X-Original-Filid: 0
			*/

			Integer vaerkId = null;
			Integer reprId = null;
			Integer filId = null;

			HeaderLine hl;
			hl = warcRecord.getHeader("X-Original-Vaerkid");
			if (hl != null && hl.value != null && hl.value.length() > 0) {
				vaerkId = Integer.parseInt(hl.value);
			}
			hl = warcRecord.getHeader("X-Original-Reprid");
			if (hl != null && hl.value != null && hl.value.length() > 0) {
				reprId = Integer.parseInt(hl.value);
			}
			hl = warcRecord.getHeader("X-Original-Filid");
			if (hl != null && hl.value != null && hl.value.length() > 0) {
				filId = Integer.parseInt(hl.value);
			}

			if (filId != null && filId == 0) {
				startside = new Startside();
				startside.targetURI = header.warcTargetUriStr;
				startside.date = header.warcDate;
				startside.vaerkId = vaerkId;
				startside.reprId = reprId;
				startside.filId = filId;
			}
		} catch (Throwable t) {
			System.out.println("Exception while processing: " + file.getPath());
		}
	}

	@Override
	public void apcRuntimeError(Throwable t, long offset, long consumed) {
	}

	@Override
	public void apcDone() {
	}

}
