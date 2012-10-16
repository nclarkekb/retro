package dk.kb.retro;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Vaerk {

	public int id = -1;

	public String titel;

	public List<Representation> representations = new LinkedList<Representation>();

	public long contentLength = 0;

	public static Vaerk fromXml(Node parentNode) {
		Node tmpNode;
		String tmpName;
		Vaerk vaerk = new Vaerk();
		Representation representation;
		NodeList children = parentNode.getChildNodes();
		for (int i=0; i<children.getLength(); ++i) {
			tmpNode = children.item(i);
			tmpName = tmpNode.getNodeName();
			if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
				if ("id".equals(tmpName)) {
					tmpNode = tmpNode.getFirstChild();
					if (tmpNode != null) {
						try {
							System.out.println("vaerkId: " + tmpNode.getNodeValue());
							vaerk.id = Integer.parseInt(tmpNode.getNodeValue());
						} catch (NumberFormatException e) {
							System.out.println("Epic fail!");
						}
					}
				} else if ("titel".equals(tmpName)) {
					tmpNode = tmpNode.getFirstChild();
					if (tmpNode != null) {
						System.out.println("vaerkTitel: " + tmpNode.getNodeValue());
						vaerk.titel = tmpNode.getNodeValue();
					}
				} else if ("representations".equals(tmpName)) {
					NodeList representationsChildren = tmpNode.getChildNodes();
					for (int j=0; j<representationsChildren.getLength(); ++j) {
						tmpNode = representationsChildren.item(j);
						tmpName = tmpNode.getNodeName();
						if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
							if ("representation".equals(tmpName)) {
								representation = Representation.fromXml(tmpNode);
								vaerk.representations.add(representation);
							}
						}
					}
				}
			}
		}
		return vaerk;
	}

}
