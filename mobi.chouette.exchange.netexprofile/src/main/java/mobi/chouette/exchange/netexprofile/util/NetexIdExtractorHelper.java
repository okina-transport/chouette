package mobi.chouette.exchange.netexprofile.util;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.util.IdVersion;
import net.sf.saxon.s9api.*;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NetexIdExtractorHelper {
	public static List<IdVersion> collectEntityIdentificators(Context context, XPathCompiler xpath, XdmNode dom, Set<String> ignorableElementNames)
			throws XPathExpressionException, SaxonApiException {
		return collectIdOrRefWithVersion(context, xpath, dom, "id", ignorableElementNames);
	}

	public static List<IdVersion> collectEntityReferences(Context context, XPathCompiler xpath, XdmNode dom, Set<String> ignorableElementNames)
			throws XPathExpressionException, SaxonApiException {
		return collectIdOrRefWithVersion(context, xpath, dom, "ref", ignorableElementNames);
	}

	public static List<IdVersion> collectIdOrRefWithVersion(Context context, XPathCompiler xpath, XdmNode dom, String attributeName, Set<String> ignorableElementNames)
			throws XPathExpressionException, SaxonApiException {
		StringBuilder filterClause = new StringBuilder();
		filterClause.append("//n:*[");
		if (ignorableElementNames != null) {
			for (String elementName : ignorableElementNames) {
				filterClause.append("not(local-name(.)='").append(elementName).append("') and ");
			}
		}
		filterClause.append("@").append(attributeName).append("]");

		XPathSelector selector = xpath.compile(filterClause.toString()).load();
		selector.setContextItem(dom);
		XdmValue nodes = selector.evaluate();

		String filename = (String) context.get(Constant.FILE_NAME);
		QName versionQName = new QName("version");
		List<IdVersion> ids = new ArrayList<IdVersion>();
		for (XdmItem item : nodes) {
			XdmNode n = (XdmNode)item;
			String elementName = n.getNodeName().getLocalName();

			List<String> parentElementNames = new ArrayList<>();
			XdmNode p = n.getParent();
			while (p != null && p.getNodeName() != null) {
				parentElementNames.add(p.getNodeName().getLocalName());
				p = p.getParent();
			}
			String id = n.getAttributeValue(new QName(attributeName));
			String version = n.getAttributeValue(versionQName);

			ids.add(new IdVersion(id, version, elementName, parentElementNames, filename,
					n.getLineNumber(), n.getColumnNumber()));

		}
		return ids;
	}
	
	


}
