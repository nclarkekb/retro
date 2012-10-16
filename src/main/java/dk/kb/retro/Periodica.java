package dk.kb.retro;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Periodica {

	public int perId = -1;

	public List<Issue> issues = new LinkedList<Issue>();

	public long contentLength = 0;

	public static Periodica fromXml(Node parentNode) {
		Node tmpNode;
		String tmpName;
		Periodica periodica = new Periodica();
		Issue issue;
		NodeList children = parentNode.getChildNodes();
		for (int i=0; i<children.getLength(); ++i) {
			tmpNode = children.item(i);
			tmpName = tmpNode.getNodeName();
			if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
				if ("perid".equals(tmpName)) {
					tmpNode = tmpNode.getFirstChild();
					if (tmpNode != null) {
						try {
							System.out.println("perId: " + tmpNode.getNodeValue());
							periodica.perId = Integer.parseInt(tmpNode.getNodeValue());
						} catch (NumberFormatException e) {
							System.out.println("Epic fail!");
						}
					}
				} else if ("issues".equals(tmpName)) {
					NodeList issuesChildren = tmpNode.getChildNodes();
					for (int j=0; j<issuesChildren.getLength(); ++j) {
						tmpNode = issuesChildren.item(j);
						tmpName = tmpNode.getNodeName();
						if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
							if ("issue".equals(tmpName)) {
								issue = Issue.fromXml(tmpNode);
								periodica.issues.add(issue);
							}
						}
					}
				}
			}
		}
		return periodica;
	}

}
