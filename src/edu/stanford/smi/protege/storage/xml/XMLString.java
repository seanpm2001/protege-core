package edu.stanford.smi.protege.storage.xml;

/**
 * TODO Class Comment
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public interface XMLString {
    public interface ElementName {
        String KNOWLEDGE_BASE = "knowledge_base";
        String CLASS = "class";
        String SLOT = "slot";
        String FACET = "facet";
        String SIMPLE_INSTANCE = "simple_instance";
        String NAME = "name";
        String TYPE = "type";
        String SUPERCLASS = "superclass";
        String SUPERSLOT = "superslot";
        String TEMPLATE_SLOT = "template_slot";
        String OWN_SLOT_VALUE = "own_slot_value";
        String TEMPLATE_FACET_VALUE = "template_facet_value";
        String SLOT_REFERENCE = "slot_reference";
        String FACET_REFERENCE = "facet_reference";
        String REFERENCE_VALUE = "reference_value";
        String PRIMITIVE_VALUE = "primitive_value";
    }

    public interface AttributeName {
        String VALUE_TYPE = "type";
        String ID = "id";
    }

    public interface AttributeValue {
        String CLASS_TYPE = "class";
        String SLOT_TYPE = "slot";
        String FACET_TYPE = "facet";
        String SIMPLE_INSTANCE_TYPE = "simple_instance";
        String STRING_TYPE = "string";
        String INTEGER_TYPE = "integer";
        String FLOAT_TYPE = "float";
        String BOOLEAN_TYPE = "boolean";
    }
}
