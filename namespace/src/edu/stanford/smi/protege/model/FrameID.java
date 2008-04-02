package edu.stanford.smi.protege.model;

import java.io.Serializable;


/**
 
 */
public class FrameID implements Serializable, Localizable {
    private static final long serialVersionUID = -3804918126573053937L;
    private String name;
    private int hashCode;

    public FrameID(String name) {
        if (name != null) {
            name = name.intern();
            hashCode = name.hashCode() + 42;
        }
        else {
        	hashCode = 0;
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FrameID)) {
            return false;
        }
        FrameID other = (FrameID) o;
        return name == other.name;
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "FrameID(" + name + ")";
    }

    public void localize(KnowledgeBase kb) {
        if (name != null) {
            name = name.intern();
        }
    }
}