package edu.stanford.smi.protege.storage.clips;

import java.io.*;
import java.util.*;

import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.plugin.*;
import edu.stanford.smi.protege.resource.*;
import edu.stanford.smi.protege.ui.*;
import edu.stanford.smi.protege.util.*;

/**
 * Import and Export plugins for the CLIPS file format.
 * 
 * Note that this is not a good example of a plugin. An import plugin for clips
 * should be able to read any clips file. This plugin can only read a subset of
 * clips files, in particular those generated by Protege. An export plugin for
 * clips should write out "clean" clips. This output should have none of the
 * "comments" and other Protege encoded information in it. This plugin does
 * neither of these jobs well because it just delegates the calls to the CLIPS
 * backend which has entirely different constraints. Nevertheless this plugin
 * does illustrate how to implement the required calls as well as demonstrating
 * a sample interaction with the USER.
 * 
 * @author Ray Fergerson <fergerson@smi.stanford.edu>
 */
public class ClipsImportExportPlugin implements ImportPlugin, ExportPlugin {

    public String getName() {
        return "CLIPS";
    }

    public Project handleImportRequest() {
        Project project = Project.createNewProject(null, new ArrayList());
        handleImportRequest(project);
        return project;
    }

    private static void handleImportRequest(Project project) {
        ClipsFilePanel panel = new ClipsFilePanel();
        String title = LocalizedText.getText(ResourceKey.CLIPS_FILES_TO_IMPORT_DIALOG_TITLE);
        int rval = ModalDialog.showDialog(null, panel, title, ModalDialog.MODE_OK_CANCEL);
        if (rval == ModalDialog.OPTION_OK) {
            String classesFileName = panel.getClsesFileName();
            String instancesFileName = panel.getInstancesFileName();
            WaitCursor cursor = new WaitCursor(ProjectManager.getProjectManager().getMainPanel());
            try {
                importProject(project, classesFileName, instancesFileName);
            } finally {
                cursor.hide();
            }
        }
    }

    private static void importProject(Project project, String clsesFileName, String instancesFileName) {
        Collection errors = new ArrayList();
        KnowledgeBase kb = project.getKnowledgeBase();
        ClipsKnowledgeBaseFactory factory = new ClipsKnowledgeBaseFactory();
        Reader clsesReader = FileUtilities.getReader(clsesFileName);
        Reader instancesReader = FileUtilities.getReader(instancesFileName);
        factory.loadKnowledgeBase(kb, clsesReader, instancesReader, false, errors);
        handleErrors(errors);
    }

    public void handleExportRequest(Project project) {
        ClipsFilePanel panel = new ClipsFilePanel();
        String title = LocalizedText.getText(ResourceKey.CLIPS_FILES_TO_EXPORT_DIALOG_TITLE);
        int rval = ModalDialog.showDialog(null, panel, title, ModalDialog.MODE_OK_CANCEL);
        if (rval == ModalDialog.OPTION_OK) {
            String classesFileName = panel.getClsesFileName();
            String instancesFileName = panel.getInstancesFileName();
            WaitCursor cursor = new WaitCursor(ProjectManager.getProjectManager().getMainPanel());
            try {
                exportProject(project, classesFileName, instancesFileName);
            } finally {
                cursor.hide();
            }
        }
    }

    private static void exportProject(Project project, String clsesFileName, String instancesFileName) {
        Collection errors = new ArrayList();
        KnowledgeBase kb = project.getKnowledgeBase();
        ClipsKnowledgeBaseFactory factory = new ClipsKnowledgeBaseFactory();
        Writer clsesWriter = FileUtilities.getWriter(clsesFileName);
        Writer instancesWriter = FileUtilities.getWriter(instancesFileName);
        factory.saveKnowledgeBase(kb, clsesWriter, instancesWriter, errors);
        handleErrors(errors);
    }

    private static void handleErrors(Collection errors) {
        if (!errors.isEmpty()) {
            Iterator i = errors.iterator();
            while (i.hasNext()) {
                Object o = i.next();
                Log.getLogger().warning(o.toString());
            }
        }
    }

    public void dispose() {
        // do nothing
    }

}