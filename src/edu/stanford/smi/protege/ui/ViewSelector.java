package edu.stanford.smi.protege.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.resource.*;
import edu.stanford.smi.protege.util.*;
import edu.stanford.smi.protege.widget.*;

/**
 * TODO Class Comment
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class ViewSelector extends JComponent {
    private JComboBox combobox;
    private ProjectView projectView;
    private ItemListener itemListener = createChangeViewAction();
    private JToolBar toolBar;
    private ButtonGroup buttonGroup = new ButtonGroup();
    private Map descriptorToButtonMap = new HashMap();

    public ViewSelector(ProjectView projectView) {
        this.projectView = projectView;
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setBorder(BorderFactory.createEmptyBorder(2, 20, 0, 0));
        combobox = ComponentFactory.createComboBox();
        ComboBoxModel model = createModel();
        combobox.setModel(model);
        combobox.addItemListener(itemListener);
        setOpaque(false);
        combobox.setRenderer(new TabRenderer(projectView));
        combobox.setPreferredSize(new Dimension(200, 10));
        add(new JLabel("View:  "));
        add(combobox);
        toolBar = ComponentFactory.createToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        add(toolBar);

        projectView.getTabbedPane().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                TabWidget widget = ViewSelector.this.projectView.getSelectedTab();
                if (widget != null) {
                    setSelection(widget);
                }
            }
        });

        createInitialButtons();
    }

    private ComboBoxModel createModel() {
        Project project = projectView.getProject();
        Collection tabs = project.getTabWidgetDescriptors();
        Collection modelElements = new ArrayList();

        modelElements.addAll(getCurrentDescriptors());
        modelElements.addAll(getPotentialDescriptors(tabs));

        return new DefaultComboBoxModel(modelElements.toArray());
    }

    private Collection getCurrentDescriptors() {
        Collection currentDescriptors = new ArrayList();
        Iterator i = projectView.getTabs().iterator();
        while (i.hasNext()) {
            TabWidget tab = (TabWidget) i.next();
            WidgetDescriptor d= tab.getDescriptor();
            currentDescriptors.add(d);
        }
        return currentDescriptors;
    }

    private Collection getPotentialDescriptors(Collection tabDescriptors) {
        Collection potentialTabs = new ArrayList();
        Iterator i = tabDescriptors.iterator();
        while (i.hasNext()) {
            WidgetDescriptor d = (WidgetDescriptor) i.next();
            String className = d.getWidgetClassName();
            TabWidget tab = projectView.getTabByClassName(className);
            if (tab == null) {
                Collection errors = new ArrayList();
                if (WidgetUtilities.isSuitableTab(className, projectView.getProject(), errors)) {
                    potentialTabs.add(d);
                }
            }
        }
        return potentialTabs;
    }

    private ItemListener createChangeViewAction() {
        return new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    WidgetDescriptor d = (WidgetDescriptor) e.getItem();
                    showView(d);
                }
            }
        };
    }

    private void showView(WidgetDescriptor descriptor) {
        AbstractButton button = (AbstractButton) descriptorToButtonMap.get(descriptor);
        if (button == null) {
            button = addButton(descriptor.getWidgetClassName());
        }
        button.setSelected(true);
    }

    /*
    private void rebuildModel() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Object selection = combobox.getSelectedItem();
                combobox.removeItemListener(itemListener);
                combobox.setModel(createModel());
                combobox.setSelectedItem(selection);
                combobox.addItemListener(itemListener);
            }
        });
    }
    */

    public AbstractButton addButton(String className) {
        final WidgetDescriptor d = projectView.getProject().getTabWidgetDescriptor(className);
        d.setVisible(true);
        final TabWidget widget = projectView.addTab(d);
        Icon icon = widget.getIcon();
        if (icon == null) {
            icon = Icons.getUglyIcon();
        }
        Action action = new AbstractAction(widget.getLabel(), icon) {
            public void actionPerformed(ActionEvent event) {
                projectView.setSelectedTab(widget);
            }
        };
        AbstractButton button = ComponentFactory.addToggleToolBarButton(toolBar, action);
        descriptorToButtonMap.put(d, button);
        buttonGroup.add(button);
        button.setRolloverEnabled(false);
        button.setBorderPainted(true);
        toolBar.setRollover(false);
        return button;
    }

    private void setSelection(TabWidget widget) {
        combobox.removeItemListener(itemListener);
        combobox.setSelectedItem(widget.getDescriptor());
        combobox.addItemListener(itemListener);

        AbstractButton button = (AbstractButton) descriptorToButtonMap.get(widget.getDescriptor());
        if (button != null) {
            button.setSelected(true);
        }
    }
    
    private void createInitialButtons() {
        
    }

}

class TabRenderer extends DefaultRenderer {
    private ProjectView projectView;

    public TabRenderer(ProjectView view) {
        // Log.enter(this, "TabRenderer");
        projectView = view;
    }

    public void load(Object o) {
        if (o instanceof WidgetDescriptor) {
            WidgetDescriptor d = (WidgetDescriptor) o;
            String longClassName = d.getWidgetClassName();
            TabWidget tab = projectView.getTabByClassName(longClassName);
            if (tab == null) {
                String shortClassName = StringUtilities.getShortClassName(longClassName);
                if (shortClassName.endsWith("Tab")) {
                    shortClassName = shortClassName.substring(0, shortClassName.length() - 3);
                }
                setMainText(shortClassName);
                setMainIcon(null);
            } else {
                setMainText(tab.getLabel());
                setMainIcon(tab.getIcon());
            }
        } else {
            setMainText(o.toString());
        }
    }

    public String toString() {
        return StringUtilities.getClassName(this);
    }
}