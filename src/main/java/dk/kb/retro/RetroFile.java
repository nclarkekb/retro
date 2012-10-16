package dk.kb.retro;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jwat.arc.ArcRecordBase;
import org.jwat.common.ContentType;
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

	public RetroFile() {
	}

	public RetroFile processFile(File file) {
		ArchiveParser archiveParser = new ArchiveParser();
		archiveParser.uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
		archiveParser.bBlockDigestEnabled = false;
		archiveParser.bPayloadDigestEnabled = false;
		long consumed = archiveParser.parse(file, this);
		return this;
	}

	@Override
	public void apcFileId(int fileId) {
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
		WarcHeader header = warcRecord.header;
		ContentType contentType = header.contentType;
		if (contentType != null && "text".equalsIgnoreCase(contentType.contentType) && "xml".equalsIgnoreCase(contentType.mediaType)) {
			//System.out.println(contentType.toString());
			//System.out.println(warcRecord.header.warcRecordIdStr);
			if (warcRecord.hasPayload()) {
				xmlValidator.parse(warcRecord.getPayload().getInputStream());
				if (xmlValidator.document != null) {
					//System.out.println(xmlValidator.document);
					//System.out.println(xmlValidator.document.getDocumentElement().getNodeName());
					Node rootNode = xmlValidator.document.getDocumentElement();
					String rootName = rootNode.getNodeName();
					if ("periodica".equals(rootName)) {
						periodica = Periodica.fromXml(rootNode);
						periodica.contentLength = warcRecord.header.contentLength;
						// debug
						//System.out.println(periodica.perId);
					} else if ("vaerk".equals(rootName)) {
						vaerk = Vaerk.fromXml(rootNode);
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
		//header.warcTargetUriStr;
	}

	@Override
	public void apcRuntimeError(Throwable t, long offset, long consumed) {
	}

}
