package edu.stanford.smi.protege.model.framestore;

import java.lang.reflect.*;
import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;

public class DefaultFrameFactory implements FrameFactory {
    private static final int DEFAULT_CLS_JAVA_CLASS_ID = 6;
    private static final int DEFAULT_SLOT_JAVA_CLASS_ID = 7;
    private static final int DEFAULT_FACET_JAVA_CLASS_ID = 8;
    private static final int DEFAULT_SIMPLE_INSTANCE_JAVA_CLASS_ID = 5;

    private static final Class[] CONSTRUCTOR_PARAMETERS = { KnowledgeBase.class, FrameID.class };
    private KnowledgeBase _kb;
    private Collection _packages = new ArrayList();
    private Map _typesToImplementationClassMap = new HashMap();

    public DefaultFrameFactory(KnowledgeBase kb) {
        _kb = kb;
    }

    public KnowledgeBase getKnowledgeBase() {
        return _kb;
    }

    public void addJavaPackage(String packageName) {
        _packages.add(packageName);
    }

    public void removeJavaPackage(String packageName) {
        _packages.remove(packageName);
    }

    public Cls createCls(FrameID id, Collection directTypes) {
        Class implementationClass = getImplementationClass(directTypes, DefaultCls.class);
        return createCls(id, implementationClass);
    }

    protected Cls createCls(FrameID id, Class implementationClass) {
        Cls cls;
        if (implementationClass.equals(DefaultCls.class)) {
            cls = new DefaultCls(_kb, id);
        } else {
            cls = (Cls) createInstance(id, implementationClass);
        }
        configureCls(cls);
        return cls;
    }

    public Slot createSlot(FrameID id, Collection directTypes) {
        Class implementationClass = getImplementationClass(directTypes, DefaultSlot.class);
        return createSlot(id, implementationClass);
    }

    protected Slot createSlot(FrameID id, Class implementationClass) {
        Slot slot;
        if (implementationClass.equals(DefaultSlot.class)) {
            slot = new DefaultSlot(_kb, id);
        } else {
            slot = (Slot) createInstance(id, implementationClass);
        }
        configureSlot(slot);
        return slot;

    }

    public Facet createFacet(FrameID id, Collection directTypes) {
        Class implementationClass = getImplementationClass(directTypes, DefaultFacet.class);
        return createFacet(id, implementationClass);
    }

    protected Facet createFacet(FrameID id, Class implementationClass) {
        Facet facet;
        if (implementationClass.equals(DefaultFacet.class)) {
            facet = new DefaultFacet(_kb, id);
        } else {
            facet = (Facet) createInstance(id, implementationClass);
        }
        configureFacet(facet);
        return facet;
    }

    public SimpleInstance createSimpleInstance(FrameID id, Collection directTypes) {
        Class implementationClass = getImplementationClass(directTypes, DefaultSimpleInstance.class);
        return createSimpleInstance(id, implementationClass);
    }

    protected SimpleInstance createSimpleInstance(FrameID id, Class implementationClass) {
        SimpleInstance instance;
        if (implementationClass.equals(DefaultSimpleInstance.class)) {
            instance = new DefaultSimpleInstance(_kb, id);
        } else {
            instance = (SimpleInstance) createInstance(id, implementationClass);
        }
        configureSimpleInstance(instance);
        return instance;
    }

    protected void configureFacet(Facet facet) {
        if (facet.isSystem()) {
            Facet cachedSystemFacet = (Facet) getCachedSystemFrame(facet.getFrameID());
            if (cachedSystemFacet != null) {
                facet.setConstraint(cachedSystemFacet.getConstraint());
            }
        }
    }

    protected void configureSimpleInstance(SimpleInstance simpleInstance) {
        // do nothing (for now)
    }

    protected void configureCls(Cls cls) {
        // do nothing (for now)
    }

    protected void configureSlot(Slot slot) {
        // do nothing (for now)
    }

    private Frame getCachedSystemFrame(FrameID id) {
        return _kb.getSystemFrames().getFrame(id);
    }

    private Class getImplementationClass(Collection directTypes, Class defaultClass) {
        Class implementationClass;
        if (_packages.isEmpty()) {
            implementationClass = defaultClass;
        } else {
            directTypes = new ArrayList(directTypes);
            implementationClass = (Class) _typesToImplementationClassMap.get(directTypes);
            if (implementationClass == null) {
                implementationClass = getJavaImplementationClass(directTypes, defaultClass);
            }
            _typesToImplementationClassMap.put(directTypes, implementationClass);
        }
        return implementationClass;
    }

    public boolean isCorrectJavaImplementationClass(FrameID id, Collection types, Class clas) {
        return getImplementationClass(types, clas).equals(clas);
    }

    private Instance createInstance(FrameID id, Class type) {
        Instance instance = null;
        try {
            Constructor constructor = type.getConstructor(CONSTRUCTOR_PARAMETERS);
            instance = (Instance) constructor.newInstance(new Object[] { _kb, id });
        } catch (Exception e) {
            Log.getLogger().severe(Log.toString(e));
        }
        return instance;
    }

    private Class getJavaImplementationClass(Collection types, Class baseClass) {
        Class implementationClass = null;
        Iterator i = _packages.iterator();
        while (i.hasNext() && implementationClass == null) {
            String packageName = (String) i.next();
            implementationClass = getJavaImplementationClass(packageName, types);
        }
        if (implementationClass == null) {
            implementationClass = baseClass;
        } else if (!isValidImplementationClass(implementationClass, baseClass)) {
            Log.getLogger().warning(
                    "Java implementation class of wrong type: " + implementationClass);
            implementationClass = baseClass;
        }
        return implementationClass;
    }

    private boolean isValidImplementationClass(Class implementationClass, Class defaultClass) {
        return defaultClass.isAssignableFrom(implementationClass);
    }

    private Class getJavaImplementationClass(String packageName, Collection types) {
        Class implementationClass = null;
        Iterator i = types.iterator();
        while (i.hasNext() && implementationClass == null) {
            Cls type = (Cls) i.next();
            implementationClass = getJavaImplementationClass(packageName, type);
        }
        return implementationClass;
    }

    private Class getJavaImplementationClass(String packageName, Cls type) {
        String typeName = getJavaClassName(type);
        String className = packageName + "." + typeName;
        return SystemUtilities.forName(className, true);
    }

    protected String getJavaClassName(Cls type) {
        StringBuffer className = new StringBuffer();
        String typeName = type.getName();
        for (int i = 0; i < typeName.length(); ++i) {
            char c = typeName.charAt(i);
            if (isValidCharacter(c, className.length())) {
                className.append(c);
            }
        }
        return className.toString();
    }

    protected boolean isValidCharacter(char c, int i) {
        return (i == 0) ? Character.isJavaIdentifierStart(c) : Character.isJavaIdentifierPart(c);
    }

    public int getJavaClassId(Frame frame) {
        int javaClassId;
        if (frame instanceof Cls) {
            javaClassId = DEFAULT_CLS_JAVA_CLASS_ID;
        } else if (frame instanceof Slot) {
            javaClassId = DEFAULT_SLOT_JAVA_CLASS_ID;
        } else if (frame instanceof Facet) {
            javaClassId = DEFAULT_FACET_JAVA_CLASS_ID;
        } else {
            javaClassId = DEFAULT_SIMPLE_INSTANCE_JAVA_CLASS_ID;
        }
        return javaClassId;
    }

    public Frame createFrameFromClassId(int javaClassId, FrameID id) {
        Frame frame;
        switch (javaClassId) {
        case DEFAULT_CLS_JAVA_CLASS_ID:
            frame = createCls(id, DefaultCls.class);
            break;
        case DEFAULT_SLOT_JAVA_CLASS_ID:
            frame = createSlot(id, DefaultSlot.class);
            break;
        case DEFAULT_FACET_JAVA_CLASS_ID:
            frame = createFacet(id, DefaultFacet.class);
            break;
        case DEFAULT_SIMPLE_INSTANCE_JAVA_CLASS_ID:
            frame = createSimpleInstance(id, DefaultSimpleInstance.class);
            break;
        default:
            throw new RuntimeException("Invalid java class id: " + javaClassId);
        }
        return frame;
    }

    private Collection createRange(int value) {
        Collection c = new ArrayList();
        c.add(new Integer(value));
        return c;
    }

    public Collection getClsJavaClassIds() {
        return createRange(DEFAULT_CLS_JAVA_CLASS_ID);
    }

    public Collection getSlotJavaClassIds() {
        return createRange(DEFAULT_SLOT_JAVA_CLASS_ID);
    }

    public Collection getFacetJavaClassIds() {
        return createRange(DEFAULT_FACET_JAVA_CLASS_ID);
    }

    public Collection getSimpleInstanceJavaClassIds() {
        return createRange(DEFAULT_SIMPLE_INSTANCE_JAVA_CLASS_ID);
    }
}