package dk.kb.retro;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Periodica {

	public int perId = -1;

	public long contentLength = 0;

	public static Periodica fromXml(Node parentNode) {
		Node tmpNode;
		String tmpName;
		Periodica periodica = new Periodica();
		NodeList children = parentNode.getChildNodes();
		for (int i=0; i<children.getLength(); ++i) {
			tmpNode = children.item(i);
			tmpName = tmpNode.getNodeName();
			if (tmpNode.getNodeType() == Node.ELEMENT_NODE && "perid".equals(tmpName)) {
				tmpNode = tmpNode.getFirstChild();
				if (tmpNode != null) {
					try {
						System.out.println(tmpNode.getNodeValue());
						periodica.perId = Integer.parseInt(tmpNode.getNodeValue());
					} catch (NumberFormatException e) {
						System.out.println("Epic fail!");
					}
				}
			}
		}
		return periodica;
	}

}
