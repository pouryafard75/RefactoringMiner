
package io;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Abstract class for writing filtered XML streams. This class provides methods
 * that merely delegate to the contained stream. Subclasses should override some
 * of these methods, and may also provide additional methods and fields.
 *
 * @author <a href="mailto:jk2006@engineer.com">John Kristian</a>
 */
public abstract class StreamWriterDelegate implements XMLStreamWriter {

    protected StreamWriterDelegate(XMLStreamWriter out) {
        this.out = out;
    }

    protected XMLStreamWriter out;

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return out.getProperty(name);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return out.getNamespaceContext();
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        out.setNamespaceContext(context);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        out.setDefaultNamespace(uri);
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        out.writeStartDocument();
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        out.writeStartDocument(version);
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        out.writeStartDocument(encoding, version);
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        out.writeDTD(dtd);
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        out.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        out.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        out.writeComment(data);
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        out.writeEmptyElement(localName);
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        out.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI)
            throws XMLStreamException {
        out.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        out.writeStartElement(localName);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        out.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI)
            throws XMLStreamException {
        out.writeStartElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        out.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        out.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return out.getPrefix(uri);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        out.setPrefix(prefix, uri);
    }

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        out.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName, String value)
            throws XMLStreamException {
        out.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
            throws XMLStreamException {
        out.writeAttribute(prefix, namespaceURI, localName, value);
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        out.writeCharacters(text);
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        out.writeCharacters(text, start, len);
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        out.writeCData(data);
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        out.writeEntityRef(name);
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        out.writeEndElement();
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        out.writeEndDocument();
    }

    @Override
    public void flush() throws XMLStreamException {
        out.flush();
    }

    @Override
    public void close() throws XMLStreamException {
        out.close();
    }

}
