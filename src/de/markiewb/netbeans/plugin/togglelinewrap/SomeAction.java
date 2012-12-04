package de.markiewb.netbeans.plugin.togglelinewrap;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.*;
import org.netbeans.api.editor.*;
import org.netbeans.api.editor.mimelookup.*;
import org.netbeans.api.editor.settings.*;
import org.netbeans.modules.editor.*;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.*;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

@ActionID(
    category = "Editor",
id = "de.markiewb.netbeans.plugin.togglelinewrap.SomeAction")
@ActionRegistration(
    displayName = "#CTL_SomeAction")
@ActionReference(path = "Menu/File", position = 100)
@Messages({
    "CTL_SomeAction=Toggle line wrap",
    "mode.none=none",
    "mode.words=words",
    "mode.chars=everywhere"
})
public final class SomeAction implements ActionListener {

    private static final String key = SimpleValueNames.TEXT_LINE_WRAP;
    private static final JLabel LABEL = new JLabel("XXXX");
    private static final Map<String, String> modes = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;

        {
            put("none", Bundle.mode_none());
            put("words", Bundle.mode_words());
            put("chars", Bundle.mode_chars());
        }
    };

    public SomeAction() {



        LABEL.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final JTextComponent comp = EditorRegistry.focusedComponent();

                if (comp == null) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }

                final JList l = new JList();
                DefaultListModel model = new DefaultListModel();

                for (String k : modes.keySet()) {
                    model.addElement(k);
                }

                l.setModel(model);
//                l.setSelectedValue(comp.getDocument().getProperty(BaseDocument.READ_LINE_SEPARATOR_PROP), true);
                l.setCellRenderer(new DefaultListCellRenderer() {
                    @Override
                    @SuppressWarnings("element-type-mismatch")
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        return super.getListCellRendererComponent(list, modes.get(value), index, isSelected, cellHasFocus);
                    }
                });
                l.setBorder(new LineBorder(Color.GRAY, 1));

                Point labelStart = LABEL.getLocationOnScreen();
                int x = Math.min(labelStart.x, labelStart.x + LABEL.getSize().width - l.getPreferredSize().width);
                int y = labelStart.y - l.getPreferredSize().height;

                final Popup popup = PopupFactory.getSharedInstance().getPopup(LABEL, l, x, y);
                final AWTEventListener multicastListener = new AWTEventListener() {
                    @Override
                    public void eventDispatched(AWTEvent event) {
                        if (event instanceof MouseEvent && ((MouseEvent) event).getClickCount() > 0) {
                            popup.hide();
                            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
                        }
                    }
                };

                Toolkit.getDefaultToolkit().addAWTEventListener(multicastListener, AWTEvent.MOUSE_EVENT_MASK);

                l.addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
//                        comp.getDocument().putProperty(BaseDocument.READ_LINE_SEPARATOR_PROP, l.getSelectedValue());
                        JTextComponent lastFocusedComponent = EditorRegistry.lastFocusedComponent();
                        JTextComponent jtc = lastFocusedComponent;

                        String mimeType = NbEditorUtilities.getMimeType(jtc);
                        Preferences prefs = MimeLookup.getLookup(mimeType).lookup(Preferences.class);
                        setWrapMode(l.getSelectedValue().toString(), prefs, jtc);


                        popup.hide();
                        Toolkit.getDefaultToolkit().removeAWTEventListener(multicastListener);
                    }
                });

                popup.show();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public @Override
            void run() {
                JTextComponent lastFocusedComponent = EditorRegistry.lastFocusedComponent();
                JTextComponent jtc = lastFocusedComponent;

                String mimeType = NbEditorUtilities.getMimeType(jtc);
                Preferences prefs = MimeLookup.getLookup(mimeType).lookup(Preferences.class);
                List<String> options = new ArrayList<String>(modes.keySet());
                String defaultMode = options.get(0);
                //cycle to next available mode
                String previous = prefs.get(key, defaultMode);
                String next = options.get((options.indexOf(previous) + 1) % options.size());
                setWrapMode(next, prefs, jtc);
            }
        });

    }

    private void setWrapMode(String newMode, Preferences prefs, JTextComponent jtc) {
        Logger.getLogger(SomeAction.class.getName()).log(Level.FINE, "set line wrap mode ''{0}'' for {1}", new Object[]{newMode, NbEditorUtilities.getMimeType(jtc)});
        // save new mode to preferences
        prefs.put(key, newMode);
        try {
            prefs.flush();
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }

        // set new mode for open editor
        jtc.getDocument().putProperty(key, newMode); //NOI18N
        jtc.invalidate();

        // update ui
        LABEL.setText(modes.get(newMode));
    }

    static Component panelWithSeparator(JLabel cell) {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(3, 3); // Y-unimportant -> gridlayout will stretch it
            }
        };
        separator.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(separator, BorderLayout.WEST);
        panel.add(cell);
        return panel;
    }

    @ServiceProvider(service = StatusLineElementProvider.class)
    public static final class StatusLineElementProviderImpl implements StatusLineElementProvider {

        @Override
        public Component getStatusLineElement() {
            return panelWithSeparator(LABEL);
        }
    }
}
