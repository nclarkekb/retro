package dk.kb.retro;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Issue {

	public int perid = 1;

	public int numid = -1;

	public int vaerkid = -1;

	// Set during validation!
	public Vaerk vaerk;

	public static Issue fromXml(Node parentNode) {
		Node tmpNode;
		String tmpName;
		Issue issue = new Issue();
		NodeList representationChildren = parentNode.getChildNodes();
		for (int k=0; k<representationChildren.getLength(); ++k) {
			tmpNode = representationChildren.item(k);
			tmpName = tmpNode.getNodeName();
			if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
				if ("perid".equals(tmpName)) {
					tmpNode = tmpNode.getFirstChild();
					if (tmpNode != null) {
						try {
							// debug
							//System.out.println("issuePerId: " + tmpNode.getNodeValue());
							issue.perid = Integer.parseInt(tmpNode.getNodeValue());
						} catch (NumberFormatException e) {
							System.out.println("Epic fail!");
						}
					}
				} else if ("numid".equals(tmpName)) {
					tmpNode = tmpNode.getFirstChild();
					if (tmpNode != null) {
						try {
							// debug
							//System.out.println("issueNumId: " + tmpNode.getNodeValue());
							issue.numid = Integer.parseInt(tmpNode.getNodeValue());
						} catch (NumberFormatException e) {
							System.out.println("Epic fail!");
						}
					}
				} else if ("vaerkid".equals(tmpName)) {
					tmpNode = tmpNode.getFirstChild();
					if (tmpNode != null) {
						try {
							// debug
							//System.out.println("issueVaerkId: " + tmpNode.getNodeValue());
							issue.vaerkid = Integer.parseInt(tmpNode.getNodeValue());
						} catch (NumberFormatException e) {
							System.out.println("Epic fail!");
						}
					}
				}
			}
		}
		return issue;
	}

}
