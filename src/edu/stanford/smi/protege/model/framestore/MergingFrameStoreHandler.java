package edu.stanford.smi.protege.model.framestore;

import java.lang.reflect.*;
import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;

/**
 * All queries go to all frame stores.  Writes go to the primary (delegate) frame store.
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class MergingFrameStoreHandler extends AbstractFrameStoreInvocationHandler {
    private Collection secondaryFrameStores = new ArrayList();

    public static FrameStore newInstance() {
        return newInstance(MergingFrameStoreHandler.class);
    }

    public void addSecondaryFrameStore(FrameStore fs) {
        if (getSecondaryFrameStore(fs.getName()) == null) {
            secondaryFrameStores.add(fs);
        } else {
            Log.getLogger().severe("Duplicate Frame Store: " + fs.getName());
        }
    }

    public void removeSecondaryFrameStore(FrameStore fs) {
        secondaryFrameStores.remove(fs);
    }
    
    private FrameStore getSecondaryFrameStore(String name) {
        FrameStore frameStore = null;
        Iterator i = secondaryFrameStores.iterator();
        while (i.hasNext()) {
            FrameStore fs = (FrameStore) i.next();
            if (fs.getName().equals(name)) {
                frameStore = fs;
                break;
            }
        }
        return frameStore;
    }
    
    public void setPrimaryFrameStore(String name) {
        FrameStore frameStore = getSecondaryFrameStore(name);
        if (frameStore == null) {
            if (!getDelegate().getName().equals(name)) {
                Log.getLogger().severe("Frame store not found: " + name);
            }
        } else {
            secondaryFrameStores.add(getDelegate());
            setDelegate(frameStore);
        }
    }

    protected Object handleInvoke(Method method, Object[] arguments) {
        Object returnValue;
        if (isQuery(method)) {
            returnValue = handleQuery(method, arguments);
        } else {
            returnValue = handleModification(method, arguments);
        }
        return returnValue;
    }

    protected Object handleQuery(Method method, Object[] arguments) {
        Object returnValue = invoke(method, arguments);
        Iterator i = secondaryFrameStores.iterator();
        while (i.hasNext()) {
            FrameStore frameStore = (FrameStore) i.next();
            Object secondaryReturnValue = invoke(method, arguments, frameStore);
            returnValue = merge(returnValue, secondaryReturnValue);
        }
        return returnValue;
    }

    protected Object merge(Object primary, Object secondary) {
        Object returnValue = null;
        if (primary == null) {
            returnValue = secondary;
        } else if (primary instanceof String) {
            returnValue = primary;
            if (secondary != null) {
                Log.getLogger().warning("discarding secondary framestore string: " + secondary);
            }
        } else if (primary instanceof Frame) {
            returnValue = primary;
            if (secondary != null) {
                Log.getLogger().warning("discarding secondary framestore frame: " + secondary);
            }
        } else if (primary instanceof Integer) {
            int r1 = ((Integer) primary).intValue();
            int r2 = ((Integer) secondary).intValue();
            returnValue = new Integer(r1 + r2);
        } else if (primary instanceof List) {
            returnValue = new ArrayList();
            addCollections(returnValue, primary, secondary);
        } else if (primary instanceof Collection) {
            returnValue = new LinkedHashSet();
            addCollections(returnValue, primary, secondary);
        } else {
            throw new UnsupportedOperationException(primary.toString());
        }
        return returnValue;
    }

    private void addCollections(Object sum, Object c1, Object c2) {
        Collection c = (Collection) sum;
        c.addAll((Collection) c1);
        Iterator i = ((Collection) c2).iterator();
        while (i.hasNext()) {
            Object o = i.next();
            if (!c.contains(o)) {
                c.add(o);
            }
        }
    }

    protected Object handleModification(Method method, Object[] arguments) {
        return invoke(method, arguments);
    }

}