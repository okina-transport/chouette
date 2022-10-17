package mobi.chouette.exchange.netexprofile.parser.xml;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.namespace.QName;
import java.util.Set;

public class SkippingElementContentHandler extends DefaultHandler {

    private XMLReader xmlReader;

    private Set<QName> elementsToSkip;
    
    public SkippingElementContentHandler(XMLReader xmlReader, Set<QName> elementsToSkip) {
        this.xmlReader = xmlReader;
        this.elementsToSkip = elementsToSkip;
    }

    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException {
        
    	if (skipElement(uri, localName, qName, atts)) {
            xmlReader.setContentHandler(new IgnoringContentHandler(xmlReader,
                    this));
        } else {
            super.startElement(uri, localName, qName, atts);
        }
    }
	private boolean skipElement(String uri, String localName, String qName, Attributes attributes) {
		return elementsToSkip.contains(new QName(uri, localName));
	}

	private static class IgnoringContentHandler extends DefaultHandler {

	    private int depth = 1;
	    private XMLReader xmlReader;
	    private ContentHandler contentHandler;

	    public IgnoringContentHandler(XMLReader xmlReader, ContentHandler contentHandler) {
	        this.contentHandler = contentHandler;
	        this.xmlReader = xmlReader;
	    }

	    public void startElement(String uri, String localName, String qName,
	            Attributes atts) throws SAXException {
	        depth++;
	    }

	    public void endElement(String uri, String localName, String qName)
	            throws SAXException {
	        depth--;
	        if(0 == depth) {
	           xmlReader.setContentHandler(contentHandler);
	        }
	    }

	}

}