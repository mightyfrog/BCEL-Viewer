package org.mightyfrog.util.bcelgui;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Shigehiro Soejima
 */
public class BcelGui extends JFrame {
    private final JSplitPane SPLIT_PANE =
        new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    private final JarTree TREE = new JarTree();

    //
    private final JTabbedPane TABBED_PANE = new JTabbedPane();
    private final TextArea CI_TA = new TextArea(); // class info
    private final TextArea BC_TA = new TextArea(); // bytecode
    private final TextArea CP_TA = new TextArea(); // constant pool

    //
    private JMenu fileMenu = null;
    private JMenuItem openJarMI = null;
    private JMenuItem openClassMI = null;
    private JMenuItem exitMI = null;

    //
    private JMenu helpMenu = null;
    private JMenuItem wikiMI = null;
    private JMenuItem aboutMI = null;

    //
    private JFileChooser fileChooser = null;

    /**
     *
     */
    public BcelGui() {
        super(I18N.get("frame.title"));

        JOptionPane.setRootFrame(this);
        setIconImage(new ImageIcon(BcelGui.class.getResource("icon.png")).getImage());

        setJMenuBar(createMenuBar());

        TABBED_PANE.addTab(I18N.get("tab.0"), new JScrollPane(BC_TA));
        TABBED_PANE.addTab(I18N.get("tab.1"), new JScrollPane(CP_TA));
        TABBED_PANE.addTab(I18N.get("tab.2"), new JScrollPane(CI_TA));
        SPLIT_PANE.setDividerLocation(300);
        SPLIT_PANE.setLeftComponent(new JScrollPane(TREE));
        SPLIT_PANE.setRightComponent(TABBED_PANE);
        add(SPLIT_PANE);

        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(3);
        setVisible(true);
    }

    /**
     *
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            // ClassNotFoundException, InstantiationException
            // IllegalAccessException, UnsupportedLookAndFeelException
        }

        EventQueue.invokeLater(new Runnable() {
                /** */
                @Override
                public void run() {
                    new BcelGui();
                }
            });
    }

    //
    //
    //

    /**
     *
     * @param text
     */
    void setClassInfoText(String text) {
        CI_TA.setText(text);
        CI_TA.setCaretPosition(0);
    }

    /**
     *
     * @param text
     */
    void setBytecodeText(String text) {
        BC_TA.setText(text);
        BC_TA.setCaretPosition(0);
    }

    /**
     *
     * @param text
     */
    void setConstantPoolText(String text) {
        CP_TA.setText(text);
        CP_TA.setCaretPosition(0);
    }

    //
    //
    //

    /**
     *
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        this.fileMenu = new JMenu(I18N.get("menu.file"));
        this.openJarMI = new JMenuItem(I18N.get("menuitem.open.jar"));
        this.openClassMI = new JMenuItem(I18N.get("menuitem.open.class"));
        this.exitMI = new JMenuItem(I18N.get("menuitem.exit"));
        this.fileMenu.add(this.openJarMI);
        this.fileMenu.add(this.openClassMI);
        this.fileMenu.addSeparator();
        this.fileMenu.add(this.exitMI);

        this.openJarMI.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    openJarFile();
                }
            });

        this.openClassMI.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    openClassFile();
                }
            });

        this.exitMI.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    System.exit(0);
                }
            });
        menuBar.add(fileMenu);
        this.helpMenu = new JMenu(I18N.get("menu.help"));
        this.wikiMI = new JMenuItem(I18N.get("menuitem.wiki"));
        this.aboutMI = new JMenuItem(I18N.get("menuitem.about"));
        this.helpMenu.add(this.wikiMI);
        this.helpMenu.addSeparator();
        this.helpMenu.add(this.aboutMI);

        this.wikiMI.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    try {
                        URI uri = new URI(I18N.get("wiki.url"));
                        Desktop.getDesktop().browse(uri);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        this.aboutMI.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    showAboutDialog();
                }
            });

        menuBar.add(this.helpMenu);

        return menuBar;
    }

    /**
     *
     */
    private void openJarFile() {
        if (this.fileChooser == null) {
            this.fileChooser = new JFileChooser(new File("."));
        }
        this.fileChooser.setFileFilter(new FileFilter() {
                /** */
                @Override
                public boolean accept(File file) {
                    if (file.isDirectory()) {
                        return true;
                    }
                    return file.getName().toLowerCase().endsWith(".jar");
                }

                /** */
                @Override
                public String getDescription() {
                    return "Jar (*.jar)";
                }
            });
        this.fileChooser.showOpenDialog(this);
        File file = this.fileChooser.getSelectedFile();
        if (file != null) {
            TREE.buildTree(file);
        }
    }

    /**
     *
     */
    private void openClassFile() {
        if (this.fileChooser == null) {
            this.fileChooser = new JFileChooser(new File("."));
        }
        this.fileChooser.setFileFilter(new FileFilter() {
                /** */
                @Override
                public boolean accept(File file) {
                    if (file.isDirectory()) {
                        return true;
                    }
                    return file.getName().toLowerCase().endsWith(".class");
                }

                /** */
                @Override
                public String getDescription() {
                    return "Java (*.class)";
                }
            });
        this.fileChooser.showOpenDialog(this);
        File file = this.fileChooser.getSelectedFile();
        if (file != null) {
            try {
                TREE.parse(file);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        }
    }

    /**
     *
     */
    private void showAboutDialog() {
        String version = I18N.get("dialog.1",
                                  "Shigehiro Soejima",
                                  "mightyfrog.gc@gmail.com",
                                  "@TIMESTAMP@");
        JOptionPane.showMessageDialog(this, version);
    }
}
