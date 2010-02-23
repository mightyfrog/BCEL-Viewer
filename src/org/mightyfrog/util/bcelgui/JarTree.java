package org.mightyfrog.util.bcelgui;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;

import com.sun.org.apache.bcel.internal.classfile.ClassParser;
import com.sun.org.apache.bcel.internal.classfile.Code;
import com.sun.org.apache.bcel.internal.classfile.ConstantPool;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.Utility;

/**
 *
 * @author Shigehiro Soejima
 */
class JarTree extends JTree implements TreeSelectionListener,
                                       TreeWillExpandListener {
    //
    private File fileInUse = null;

    //
    private static final DefaultMutableTreeNode ROOT_NODE =
        new DefaultMutableTreeNode("");

    //
    private Map<String, DefaultMutableTreeNode> map =
        new HashMap<String, DefaultMutableTreeNode>();

    //
    private final DefaultTreeModel MODEL = (DefaultTreeModel) getModel();

    /**
     *
     */
    public JarTree() {
        MODEL.setRoot(ROOT_NODE);
        setCellRenderer(new JarTree.CellRenderer());

        setTransferHandler(new JarFileTransferHandler());
        addTreeSelectionListener(this);
        addTreeWillExpandListener(this);
    }

    /** */
    @Override
    public void valueChanged(TreeSelectionEvent evt) {
        int[] rows = getSelectionRows();
        if (rows == null || rows[0] == 0) {
            return;
        }
        Object[] paths =  evt.getPath().getPath();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < paths.length; i++) {
            sb.append("" + paths[i] + "/");
        }
        String className = sb.toString().substring(0, sb.length() - 1);
        if (className.endsWith(".class")) {
            try {
                JavaClass jc = parse(className);
                //new com.sun.org.apache.bcel.internal.util.Class2HTML(jc, "");
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        }
    }

    /** */
    @Override
    public void treeWillExpand(TreeExpansionEvent evt) {
        // no-op
    }

    /** */
    @Override
    public void treeWillCollapse(TreeExpansionEvent evt)
        throws ExpandVetoException {
        // doesn't allow the root node to collapse
        if (evt.getPath().getPathCount() == 1) {
            throw(new ExpandVetoException(evt));
        }
    }

    //
    //
    //

    /**
     *
     * @param className
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    JavaClass parse(File file) throws FileNotFoundException,
                                 IOException {
        if (!isClassFile(file)) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                          I18N.get("dialog.3", file.getName()));
            return null;
        }
        JavaClass jc = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            jc = new ClassParser(fis, file.getName()).parse();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }

        StringBuilder sb = new StringBuilder();
        Method[] methods = jc.getMethods();
        ConstantPool cp = jc.getConstantPool();
        for (Method m : methods) {
            Code code = m.getCode();
            if (code != null) {
                String access = Utility.accessToString(m.getAccessFlags());
                sb.append(Utility.methodSignatureToString(m.getSignature(),
                                                          m.getName(),
                                                          access));
                sb.append("\n\n");
                sb.append(Utility.codeToString(code.getCode(), cp,
                                               0, -1, true) + "\n");
            }
        }

        ROOT_NODE.removeAllChildren();
        ROOT_NODE.setUserObject(jc.getClassName());
        MODEL.reload(ROOT_NODE);
        getOwner().setClassInfoText(jc + "\n");
        getOwner().setBytecodeText(sb.toString());
        getOwner().setConstantPoolText(cp.toString());

        return jc;
    }

    /**
     *
     * @param file
     */
    void buildTree(File file) {
        if (!isJarFile(file)) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                          I18N.get("dialog.2", file.getName()));
            return;
        }
        ROOT_NODE.removeAllChildren();
        ((DefaultTreeModel) getModel()).reload();
        this.map.clear();
        this.fileInUse = file;
        ROOT_NODE.setUserObject(getFileInUse().getName());

        JarInputStream jis = null;
        try {
            jis = new JarInputStream(new FileInputStream(getFileInUse()));
            JarEntry je = null;
            List<String> fileList = new ArrayList<String>();
            Set<String> set = new HashSet<String>();
            while ((je = jis.getNextJarEntry()) != null) {
                String name = je.getName();
                if (je.isDirectory()) {
                    set.add(name);
                } else {
                    if (!name.endsWith(".class")) {
                        continue;
                    }
                    int index = name.lastIndexOf("/");
                    if (index != -1) {
                        set.add(name.substring(0, index) + "/");
                        fileList.add(name);
                    }
                }
            }
            createDirectoryNodes(new ArrayList<String>(set));
            createFileNodes(fileList);
            for (int i = 0; i < getRowCount(); i++) {
                expandRow(i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (jis != null) {
                try {
                    jis.close();
                } catch (IOException e) {
                }
            }
        }
        getOwner().setClassInfoText(null);
        getOwner().setBytecodeText(null);
        getOwner().setConstantPoolText(null);
        if (ROOT_NODE.getChildCount() == 0) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                          I18N.get("dialog.0", file.getName()));
        }
    }

    /**
     * Creates directory nodes. This method must be called before
     * createFileNodes().
     *
     * @param list
     */
    private void createDirectoryNodes(List<String> list) {
        Collections.sort(list);
        int i = 0;
        while (i < list.size()) {
            String s = list.get(i);
            int index = s.lastIndexOf("/", s.length() - 2);
            i++;
            if (index != -1) {
                s = s.substring(0, index);
                if (!list.contains(s + "/")) {
                    if (i == 0) {
                        list.add(0, s + "/");
                    } else {
                        list.add(i - 1, s + "/");
                        i = i - 1;
                    }
                }
            }
        }
        for (String s : list) {
            s = s.substring(0, s.length() - 1);
            int index = s.lastIndexOf("/");
            DefaultMutableTreeNode node = null;
            if (index == -1) {
                node = new DefaultMutableTreeNode(s);
                ROOT_NODE.add(node);
                this.map.put(s, node);
            } else {
                String t = s.substring(0, index);
                node = this.map.get(t);
                DefaultMutableTreeNode tmp =
                    new DefaultMutableTreeNode(s.substring(index + 1));
                node.add(tmp);
                this.map.put(s, tmp);
            }
        }
    }

    /**
     * Creates file nodes.
     *
     * @param list
     */
    private void createFileNodes(List<String> list) {
        Collections.sort(list);
        for (String s : list) {
            int index = s.lastIndexOf("/");
            String t = s.substring(0, index);
            DefaultMutableTreeNode node = this.map.get(t);
            node.add(new DefaultMutableTreeNode(s.substring(index + 1)));
        }
    }

    /**
     *
     */
    private File getFileInUse() {
        return this.fileInUse;
    }

    /**
     *
     * @param className
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    private JavaClass parse(String className) throws FileNotFoundException,
                                                IOException {
        JavaClass jc = null;
        try {
            JarFile jf = new JarFile(getFileInUse());
            JarEntry je = jf.getJarEntry(className);
            jc = new ClassParser(jf.getInputStream(je), className).parse();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }

        StringBuilder sb = new StringBuilder();
        Method[] methods = jc.getMethods();
        ConstantPool cp = jc.getConstantPool();
        for (Method m : methods) {
            Code code = m.getCode();
            if (code != null) {
                String access = Utility.accessToString(m.getAccessFlags());
                sb.append(Utility.methodSignatureToString(m.getSignature(),
                                                          m.getName(),
                                                          access));
                sb.append("\n\n");
                sb.append(Utility.codeToString(code.getCode(), cp,
                                               0, -1, true) + "\n");
            }
        }

        getOwner().setClassInfoText(jc + "\n");
        getOwner().setBytecodeText(sb.toString());
        getOwner().setConstantPoolText(cp.toString());

        return jc;
    }

    /**
     *
     */
    private BcelGui getOwner() {
        return ((BcelGui) JOptionPane.getRootFrame());
    }

    /**
     *
     * @param fileName
     */
    private static final Icon createIcon(String fileName) {
        return new ImageIcon(JarTree.class.getResource(fileName));
    }

    /**
     *
     * @param file
     */
    private boolean isJarFile(File file) {
        DataInputStream dis = null;
        boolean isJarFile = false;
        try {
            dis = new java.io.DataInputStream(new FileInputStream(file));
            isJarFile = dis.readInt() == 0x504b0304;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                }
            }
        }

        return isJarFile;
    }

    /**
     *
     * @param file
     */
    private boolean isClassFile(File file) {
        DataInputStream dis = null;
        boolean isJarFile = false;
        try {
            dis = new java.io.DataInputStream(new FileInputStream(file));
            isJarFile = dis.readInt() == 0xCAFEBABE;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                }
            }
        }

        return isJarFile;
    }

    //
    //
    //

    /**
     *
     */
    private static class CellRenderer extends DefaultTreeCellRenderer {
        //
        private final Icon ROOT_ICON = createIcon("root.png");
        private final Icon LEAF_ICON = createIcon("leaf.png");
        private final Icon OPEN_ICON = createIcon("open.png");
        private final Icon CLOSED_ICON = createIcon("closed.png");

        /** */
        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean selected,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected,
                                               expanded, leaf, row, hasFocus);
            if (row == 0) {
                setIcon(ROOT_ICON);
            } else {
                String fileName =
                    (String) ((DefaultMutableTreeNode) value).getUserObject();
                if (leaf && fileName.endsWith(".class")) {
                    setIcon(LEAF_ICON);
                } else {
                    if (expanded) {
                        setIcon(OPEN_ICON);
                    } else {
                        setIcon(CLOSED_ICON);
                    }
                }
            }

            return this;
        }
    }

    /**
     *
     */
    private class JarFileTransferHandler extends TransferHandler {
        /** */
        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            support.setShowDropLocation(false);

            return true;
        }

        /** */
        @Override
        public boolean canImport(JComponent comp,
                                 DataFlavor[] transferFlavors) {
            return true;
        }

        /** */
        @Override
        @SuppressWarnings("unchecked")
        public boolean importData(JComponent comp, Transferable t) {
            try {
                Class c = t.getTransferDataFlavors()[0].getRepresentationClass();
                List<File> list = null;
                if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    list = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                } else {
                    String data =
                        (String) t.getTransferData(createURIListFlavor());
                    list = textURIListToFileList(data);
                }
                File f = list.get(0);
                if (isJarFile(f) || f.getName().toLowerCase().endsWith(".jar")) {
                    buildTree(f);
                } else {
                    parse(f);
                }
            } catch (UnsupportedFlavorException e) {
                return false;
            } catch (IOException e) {
                return false;
            }

            return true;
        }

        /**
         *
         */
        private final DataFlavor createURIListFlavor() {
            DataFlavor df = null;
            try {
                df = new DataFlavor("text/uri-list;class=java.lang.String");
            } catch (ClassNotFoundException e) {
                // shouldn't happen
            }

            return df;
        }

        /**
         *
         * @param uriList
         */
        private final List<File> textURIListToFileList(String uriList) {
            List<File> list = new ArrayList<File>(1);
            StringTokenizer st = new StringTokenizer(uriList, "\r\n");
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                if (s.startsWith("#")) {
                    // the line is a comment (as per the RFC 2483)
                    continue;
                }
                try {
                    URI uri = new URI(s);
                    File file = new File(uri);
                    if (file.length() != 0) {
                        list.add(file);
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }

            return list;
        }
    }
}
