package edu.stanford.smi.protege.storage.xml;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;

/**
 * TODO Class Comment
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class XMLLoader {
    private KnowledgeBase kb;
    private Collection errors;
    private BufferedReader reader;

    public XMLLoader(KnowledgeBase kb, BufferedReader reader, boolean isInclude, Collection errors) {
        this.kb = kb;
        this.reader = reader;
        this.errors = errors;
        // System.setProperty("org.xml.sax.driver", "org.apache.crimson.parser.XMLReaderImpl");
    }

    public void load() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new InputSource(reader), new MyHandler(kb, errors));
        } catch (Exception e) {
            Log.getLogger().severe(Log.toString(e));
        }
    }
}

class MyHandler extends DefaultHandler {
    private KnowledgeBase kb;
    private Collection errors;

    public MyHandler(KnowledgeBase kb, Collection errors) {
        this.kb = kb;
        this.errors = errors;
        Log.getLogger().info("Reading " + this.kb);
    }

    public void error(SAXParseException exception) throws SAXException {
        handle(exception);
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        handle(exception);
    }

    public void warning(SAXParseException exception) throws SAXException {
        handle(exception);
    }

    private void handle(Exception e) {
        errors.add(e);
        Log.getLogger().severe(Log.toString(e));
    }

    private void output(String s) {
        Log.getLogger().info(s);
    }

    public void setDocumentLocator(Locator locator) {
        output("document locator: " + locator);
    }

    public void startDocument() throws SAXException {
        output("start document");
    }

    public void endDocument() throws SAXException {
        // output("end document");
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        output("start prefix mapping: " + prefix + " " + uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        // output("end prefix mapping: " + prefix, -1);
    }

    private LinkedList openElements = new LinkedList();

    private Element getCurrentElement() {
        return openElements.isEmpty() ? null : (Element) openElements.get(openElements.size() - 1);
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (!qName.equals(XMLString.ElementName.KNOWLEDGE_BASE)) {
            Element element = new Element(qName, atts);
            if (openElements.isEmpty()) {
                openElements.add(element);
            } else {
                getCurrentElement().addElement(element);
                openElements.add(element);
            }
        }
        // output("start element: " + uri + " " + localName + " " + qName + " " + atts, +1);

    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (!qName.equals(XMLString.ElementName.KNOWLEDGE_BASE)) {
            Element lastElement = (Element) openElements.remove(openElements.size() - 1);
            if (openElements.isEmpty()) {
                createFrame(lastElement);
            }
        }
        // output("end element: " + uri + " " + localName + " " + qName, -1);
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        // output("character: " + new String(ch, start, length), 0);
        Element currentElement = getCurrentElement();
        if (currentElement != null) {
            currentElement.addCharacters(new String(ch, start, length));
        }
    }

    private Collection getElementClsValues(Element root, String tag) {
        Collection clses = new ArrayList();
        Iterator i = root.getSubelementValues(tag).iterator();
        while (i.hasNext()) {
            String clsName = (String) i.next();
            clses.add(getCls(clsName));
        }
        return clses;
    }

    private Collection getElementSlotValues(Element root, String tag) {
        Collection slots = new ArrayList();
        Iterator i = root.getSubelementValues(tag).iterator();
        while (i.hasNext()) {
            String slotName = (String) i.next();
            slots.add(getSlot(slotName));
        }
        return slots;
    }

    private Cls getCls(String name) {
        Cls cls = kb.getCls(name);
        if (cls == null) {
            cls = kb.createCls(null, name, Collections.EMPTY_LIST, Collections.EMPTY_LIST, false);
        }
        return cls;
    }

    private Slot getSlot(String name) {
        Slot slot = kb.getSlot(name);
        if (slot == null) {
            slot = kb.createSlot(name, null, Collections.EMPTY_LIST, false);
        }
        return slot;
    }

    private Facet getFacet(String name) {
        Facet facet = kb.getFacet(name);
        if (facet == null) {
            facet = kb.createFacet(name, null, false);
        }
        return facet;
    }

    private SimpleInstance getSimpleInstance(String name) {
        SimpleInstance simpleInstance = kb.getSimpleInstance(name);
        if (simpleInstance == null) {
            simpleInstance = kb.createSimpleInstance(null, name, Collections.EMPTY_LIST, false);
        }
        return simpleInstance;
    }

    private void createFrame(Element root) {
        String kind = root.getName();
        String name = root.getSubelementValue(XMLString.ElementName.NAME);
        Collection types = getElementClsValues(root, XMLString.ElementName.TYPE);
        // Log.getLogger().info("create: " + name + ", types=" + types);

        Frame frame;
        if (kind.equals(XMLString.ElementName.CLASS)) {
            frame = createCls(name, types, root);
        } else if (kind.equals(XMLString.ElementName.SLOT)) {
            frame = createSlot(name, types, root);
        } else if (kind.equals(XMLString.ElementName.FACET)) {
            frame = createFacet(name, types, root);
        } else if (kind.equals(XMLString.ElementName.SIMPLE_INSTANCE)) {
            frame = createSimpleInstance(name, types, root);
        } else {
            Log.getLogger().warning("bad frame type: " + kind);
            frame = null;
        }

        if (frame != null) {
            addOwnSlotValues(frame, root);
        }
    }

    private void addTemplateFacetValues(Cls cls, Element root) {
        Iterator i = root.getSubelements(XMLString.ElementName.TEMPLATE_FACET_VALUE).iterator();
        while (i.hasNext()) {
            Element node = (Element) i.next();
            addTemplateFacetValue(cls, node);
        }

    }

    private void addTemplateFacetValue(Cls cls, Element node) {
        Slot slot = null;
        Facet facet = null;
        Collection values = new ArrayList();
        Iterator i = node.getSubelements().iterator();
        while (i.hasNext()) {
            Element element = (Element) i.next();
            String elementName = element.getName();
            if (elementName.equals(XMLString.ElementName.SLOT_REFERENCE)) {
                slot = getSlot(element.getValue());
            } else if (elementName.equals(XMLString.ElementName.FACET_REFERENCE)) {
                facet = getFacet(element.getValue());
            } else {
                Object value = getValue(element);
                if (value != null) {
                    values.add(value);
                }
            }
        }
        cls.setTemplateFacetValues(slot, facet, values);
    }

    private void addOwnSlotValues(Frame frame, Element root) {
        Iterator i = root.getSubelements(XMLString.ElementName.OWN_SLOT_VALUE).iterator();
        while (i.hasNext()) {
            Element node = (Element) i.next();
            addOwnSlotValue(frame, node);
        }
    }

    private void addOwnSlotValue(Frame frame, Element node) {
        Slot slot = null;
        Collection values = new ArrayList();
        Iterator i = node.getSubelements().iterator();
        while (i.hasNext()) {
            Element element = (Element) i.next();
            if (element.getName().equals(XMLString.ElementName.SLOT_REFERENCE)) {
                slot = getSlot(element.getValue());
            } else {
                Object value = getValue(element);
                if (value != null) {
                    values.add(value);
                }
            }
        }
        frame.setOwnSlotValues(slot, values);
    }

    private Object getValue(Element element) {
        Object value;
        if (element.getName().equals(XMLString.ElementName.REFERENCE_VALUE)) {
            value = getReferenceValue(element);
        } else if (element.getName().equals(XMLString.ElementName.PRIMITIVE_VALUE)) {
            value = getPrimitiveValue(element);
        } else {
            Log.getLogger().warning("Bad element: " + element);
            value = null;
        }
        return value;
    }

    private Object getReferenceValue(Element element) {
        Object value;
        String frameName = element.getValue();
        String type = element.getAttributeValue(XMLString.AttributeName.VALUE_TYPE);
        if (type.equals(XMLString.AttributeValue.CLASS_TYPE)) {
            value = getCls(frameName);
        } else if (type.equals(XMLString.AttributeValue.SLOT_TYPE)) {
            value = getSlot(frameName);
        } else if (type.equals(XMLString.AttributeValue.FACET_TYPE)) {
            value = getFacet(frameName);
        } else if (type.equals(XMLString.AttributeValue.SIMPLE_INSTANCE_TYPE)) {
            value = getSimpleInstance(frameName);
        } else {
            Log.getLogger().warning("bad reference value type: " + type);
            value = null;
        }
        return value;
    }

    private Object getPrimitiveValue(Element element) {
        Object value;
        String valueString = element.getValue();
        String type = element.getAttributeValue(XMLString.AttributeName.VALUE_TYPE);
        if (type.equals(XMLString.AttributeValue.STRING_TYPE)) {
            value = valueString;
        } else if (type.equals(XMLString.AttributeValue.BOOLEAN_TYPE)) {
            value = new Boolean(valueString);
        } else if (type.equals(XMLString.AttributeValue.INTEGER_TYPE)) {
            value = new Integer(valueString);
        } else if (type.equals(XMLString.AttributeValue.FLOAT_TYPE)) {
            value = new Float(valueString);
        } else {
            Log.getLogger().warning("bad primitive value type: " + type);
            value = null;
        }
        return value;

    }

    private void addSuperclasses(Cls cls, Collection superclasses) {
        if (!superclasses.isEmpty()) {
            Collection currentSuperclasses = cls.getDirectSuperclasses();
            Iterator i = superclasses.iterator();
            while (i.hasNext()) {
                Cls superclass = (Cls) i.next();
                if (!currentSuperclasses.contains(superclass)) {
                    cls.addDirectSuperclass(superclass);
                }
            }
        }
    }

    private void addTypes(Instance instance, Collection types) {
        if (!types.isEmpty()) {
            Collection currentTypes = instance.getDirectTypes();
            Iterator i = types.iterator();
            while (i.hasNext()) {
                Cls type = (Cls) i.next();
                if (!currentTypes.contains(type)) {
                    instance.addDirectType(type);
                }
            }
        }
    }

    private Cls createCls(String name, Collection types, Element root) {
        Collection superclasses = getElementClsValues(root, XMLString.ElementName.SUPERCLASS);
        Cls cls = kb.getCls(name);
        if (cls == null) {
            cls = kb.createCls(null, name, superclasses, types, false);
        } else {
            addTypes(cls, types);
            addSuperclasses(cls, superclasses);
        }
        Collection slots = getElementSlotValues(root, XMLString.ElementName.TEMPLATE_SLOT);
        Iterator i = slots.iterator();
        while (i.hasNext()) {
            Slot slot = (Slot) i.next();
            cls.addDirectTemplateSlot(slot);
        }
        addTemplateFacetValues(cls, root);
        return cls;
    }

    private Slot createSlot(String name, Collection types, Element root) {
        Slot slot = kb.getSlot(name);
        if (slot == null) {
            Cls type = (Cls) CollectionUtilities.getFirstItem(types);
            slot = kb.createSlot(name, type, Collections.EMPTY_LIST, false);
        } else {
            addTypes(slot, types);
        }
        return slot;
    }

    private Facet createFacet(String name, Collection types, Element root) {
        Facet facet = kb.getFacet(name);
        if (facet == null) {
            Cls type = (Cls) CollectionUtilities.getFirstItem(types);
            facet = kb.createFacet(name, type, false);
        } else {
            addTypes(facet, types);
        }
        return facet;
    }

    private SimpleInstance createSimpleInstance(String name, Collection types, Element root) {
        SimpleInstance simpleInstance = (SimpleInstance) kb.getInstance(name);
        if (simpleInstance == null) {
            simpleInstance = kb.createSimpleInstance(null, name, types, false);
        } else {
            addTypes(simpleInstance, types);
        }
        return simpleInstance;
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        //output("ignorable whitespace: " + new String(ch, start, length) + " " + start + " " + length);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        output("processing instruction: " + target + " " + data);
    }

    public void skippedEntity(String name) throws SAXException {
        output("skipped entity: " + name);
    }

}

class Element {
    private String name;
    private Attributes attributes;
    private String value;
    private List subelements;

    Element(String name, Attributes attributes) {
        this.name = name;
        this.attributes = new AttributesImpl(attributes);
    }

    public void addCharacters(String s) {
        if (value == null) {
            value = s;
        } else {
            value += s;
        }
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void addElement(Element element) {
        if (subelements == null) {
            subelements = new ArrayList();
        }
        subelements.add(element);
    }

    public String getAttributeValue(String type) {
        return attributes.getValue(type);
    }

    public Element getSubelement(int index) {
        return (Element) subelements.get(index);
    }

    public Collection getSubelements() {
        return subelements;
    }

    public String getSubelementValue(String tag) {
        return (String) CollectionUtilities.getFirstItem(getSubelementValues(tag));
    }

    public Collection getSubelementValues(String tag) {
        Collection values = new ArrayList();
        Iterator i = subelements.iterator();
        while (i.hasNext()) {
            Element element = (Element) i.next();
            if (element.getName().equals(tag)) {
                values.add(element.getValue());
            }
        }
        return values;
    }

    public Collection getSubelements(String tag) {
        Collection elements = new ArrayList();
        Iterator i = subelements.iterator();
        while (i.hasNext()) {
            Element element = (Element) i.next();
            if (element.getName().equals(tag)) {
                elements.add(element);
            }
        }
        return elements;

    }

}
