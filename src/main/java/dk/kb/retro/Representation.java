package dk.kb.retro;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Representation {

	public int vaerkid = -1;

	public int reprid = -1;

	public String url;

	public static Representation fromXml(Node parentNode) {
		Node tmpNode;
		String tmpName;
		Representation representation = new Representation();
		NodeList representationChildren = parentNode.getChildNodes();
		for (int k=0; k<representationChildren.getLength(); ++k) {
			tmpNode = representationChildren.item(k);
			tmpName = tmpNode.getNodeName();
			if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
				if ("vaerkid".equals(tmpName)) {
					tmpNode = tmpNode.getFirstChild();
					if (tmpNode != null) {
						try {
							// debug
							//System.out.println("reprVaerkId: " + tmpNode.getNodeValue());
							representation.vaerkid = Integer.parseInt(tmpNode.getNodeValue());
						} catch (NumberFormatException e) {
							System.out.println("Epic fail!");
						}
					}
				} else if ("reprid".equals(tmpName)) {
					tmpNode = tmpNode.getFirstChild();
					if (tmpNode != null) {
						try {
							// debug
							//System.out.println("reprId: " + tmpNode.getNodeValue());
							representation.reprid = Integer.parseInt(tmpNode.getNodeValue());
						} catch (NumberFormatException e) {
							System.out.println("Epic fail!");
						}
					}
				} else if ("url".equals(tmpName)) {
					tmpNode = tmpNode.getFirstChild();
					if (tmpNode != null) {
						// debug
						//System.out.println("reprUrl: " + tmpNode.getNodeValue());
						representation.url = tmpNode.getNodeValue();
					}
				}
			}
		}
		return representation;
	}

}
