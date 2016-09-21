package no.difi.webservice.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class XmlUtil {
    private static final Logger logger = LoggerFactory.getLogger(XmlUtil.class);

    public static String asString(Source source) {
        StringWriter sw = new StringWriter();
        try {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StreamResult sr = new StreamResult(sw);
            transformer.transform(source, sr);
            return sw.toString();
        } catch (TransformerException e) {
            logger.error("Failed to marshall webservice response to XML string", e);
        }
        return "";
    }
}
