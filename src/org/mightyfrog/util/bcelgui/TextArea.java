package org.mightyfrog.util.bcelgui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 *
 *
 */
class TextArea extends JTextArea implements PopupMenuListener {
    private final JPopupMenu POPUP = new JPopupMenu();
    private final JMenuItem COPY_MI =
        new JMenuItem(getActionMap().get("copy-to-clipboard"));

    /**
     *
     */
    public TextArea() {
        COPY_MI.setText(I18N.get("popup.copy"));
        POPUP.add(COPY_MI);
        POPUP.addPopupMenuListener(this);
        addMouseListener(new MouseAdapter() {
                /** */
                @Override
                public void mousePressed(MouseEvent evt) {
                    handlePopup(evt);
                }
                
                /** */
                @Override
                public void mouseReleased(MouseEvent evt) {
                    handlePopup(evt);
                }
            });
    }

    /** */
    @Override
    public void popupMenuCanceled(PopupMenuEvent evt) {
        // no-op
    }

    /** */
    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent evt) {
        // no-op
    }

    /** */
    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent evt) {
        boolean b = getCaret().getMark() != getCaret().getDot();
        COPY_MI.setEnabled(b);
    }

    //
    //
    //

    /**
     *
     * @param evt
     */
    private void handlePopup(MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            POPUP.show(this, evt.getX(), evt.getY());
        }
    }
}
