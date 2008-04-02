package edu.stanford.smi.protege.event;

import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protege.widget.*;

/**
 * Dispatcher to send project events to their listeners. 
 *
 * @author    Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class ProjectEventDispatcher implements EventDispatcher {

    public void postEvent(Collection listeners, Object source, int type, Object arg1, Object arg2, Object arg3) {
        ProjectEvent event = new ProjectEvent((Project) source, type, (ClsWidget) arg1);
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            ProjectListener listener = (ProjectListener) i.next();
            switch (type) {
                case ProjectEvent.PROJECT_CLOSED:
                    listener.projectClosed(event);
                    break;
                case ProjectEvent.PROJECT_SAVED:
                    listener.projectSaved(event);
                    break;
                case ProjectEvent.FORM_CHANGED:
                    listener.formChanged(event);
                    break;
                case ProjectEvent.RUNTIME_CLS_WIDGET_CREATED:
                    listener.runtimeClsWidgetCreated(event);
                    break;
                default:
                    Assert.fail("bad type: " + type);
                    break;
            }
        }
    }
}
