package edu.stanford.smi.protege.ui;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import edu.stanford.smi.protege.event.*;
import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.resource.*;
import edu.stanford.smi.protege.util.*;

/**
 * TODO Class Comment
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class DirectTypesList extends SelectableContainer {
    private SelectableList list;
    private Instance instance;
    private KnowledgeBase knowledgeBase;
    private AbstractAction addAction;

    private InstanceListener instanceListener = new InstanceListener() {

        public void directTypeAdded(InstanceEvent event) {
            ComponentUtilities.addListValue(list, event.getCls());
        }

        public void directTypeRemoved(InstanceEvent event) {
            ComponentUtilities.removeListValue(list, event.getCls());
        }

    };

    public DirectTypesList(Project project) {
        this.knowledgeBase = project.getKnowledgeBase();
        list = ComponentFactory.createSelectableList(null);
        list.setCellRenderer(new FrameRenderer());
        setSelectable(list);

        LabeledComponent c = new LabeledComponent("Direct Types", new JScrollPane(list));
        c.addHeaderButton(createAddTypeAction());
        c.addHeaderButton(createRemoteTypeAction());
        setLayout(new BorderLayout());
        add(c);
        setPreferredSize(new Dimension(0, 100));
    }

    public void setInstance(Instance newInstance) {
        if (instance != null) {
            instance.removeInstanceListener(instanceListener);
        }
        instance = newInstance;
        if (instance != null) {
            instance.addInstanceListener(instanceListener);
        }
        updateModel();
        updateAddButton();
    }

    public void updateModel() {
        ListModel model;
        if (instance == null) {
            model = new DefaultListModel();
        } else {
            Collection types = instance.getDirectTypes();
            model = new SimpleListModel(types);
        }
        list.setModel(model);
    }

    public void updateAddButton() {
        addAction.setEnabled(instance != null);
    }

    private Action createAddTypeAction() {
        addAction = new AddAction(ResourceKey.CLASS_ADD) {
            public void onAdd() {
                Collection clses = DisplayUtilities.pickClses(DirectTypesList.this, knowledgeBase);
                Iterator i = clses.iterator();
                while (i.hasNext()) {
                    Cls cls = (Cls) i.next();
                    instance.addDirectType(cls);
                }
            }
        };
        return addAction;
    }

    private Action createRemoteTypeAction() {
        return new RemoveAction(ResourceKey.CLASS_REMOVE, list) {
            public void onRemove(Object o) {
                Cls cls = (Cls) o;
                instance.removeDirectType(cls);
            }
        };
    }
}