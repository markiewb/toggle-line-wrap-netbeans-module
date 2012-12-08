/**
 * Copyright 2012 markiewb
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package de.markiewb.netbeans.plugin.togglelinewrap;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.*;
import javax.swing.*;
import javax.swing.text.*;
import org.netbeans.api.editor.*;
import org.netbeans.api.editor.mimelookup.*;
import org.netbeans.api.editor.settings.*;
import org.netbeans.modules.editor.*;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.*;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

@ActionID(
    category = "Editor",
id = "de.markiewb.netbeans.plugin.togglelinewrap.SwitchLineWrapModeAction")
@ActionRegistration(
    displayName = "#CTL_SwitchLineWrapModeAction")
@Messages({
    "CTL_SwitchLineWrapModeAction=Toggle line wrap",
    "mode.none=Wrap: off",
    "mode.words=Wrap: words",
    "mode.chars=Wrap: chars"
})
/**
 * Cycle the line wrap mode for the current editor. The setting is persisted per mimetype.
 *
 * http://code.google.com/p/toggle-line-wrap-netbeans-module/
 *
 * @author markiewb
 * @author jlahoda@netbeans.org (statusbar registration taken from https://hg.kenai.com/hg/pelmel~nb69)
 */
public final class SwitchLineWrapModeAction implements ActionListener {

    private static final String key = SimpleValueNames.TEXT_LINE_WRAP;
    public static final String defaultEmptyLabel = "           ";
    private static final JLabel LABEL = new JLabel(defaultEmptyLabel);
    private static final Map<String, String> modes = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;

        {
            put("none", Bundle.mode_none());
            put("words", Bundle.mode_words());
            put("chars", Bundle.mode_chars());
        }
    };
    private boolean supportsSaving = true;

    @Override
    public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public @Override
            void run() {
                setNextWrapMode();
            }
        });

    }

    private void setNextWrapMode() {
        JTextComponent jtc = EditorRegistry.lastFocusedComponent();
        if (null == jtc) {
            return;
        }

        final String mimeType = NbEditorUtilities.getMimeType(jtc);
        final Lookup mime = MimeLookup.getLookup(mimeType);
        final Preferences prefs = mime.lookup(Preferences.class);
        final List<String> options = new ArrayList<String>(modes.keySet());
        String defaultMode = options.get(0);
        //cycle to next available mode
        String previous = prefs.get(key, defaultMode);

        String next = options.get((options.indexOf(previous) + 1) % options.size());
        saveWrapMode(next, prefs, jtc);
    }

    public void updateLabel() {
        JTextComponent jtc = EditorRegistry.lastFocusedComponent();
        if (null==jtc){
            LABEL.setText(defaultEmptyLabel);
            LABEL.setToolTipText(null);
            return;
        }
        String mimeType = NbEditorUtilities.getMimeType(jtc);
        Preferences prefs = MimeLookup.getLookup(mimeType).lookup(Preferences.class);
        List<String> options = new ArrayList<String>(modes.keySet());
        String defaultMode = options.get(0);
        //cycle to next available mode
        String previous = prefs.get(key, defaultMode);
        LABEL.setText(modes.get(previous));
        LABEL.setToolTipText(mimeType);
    }

    private void saveWrapMode(String newMode, Preferences prefs, JTextComponent jtc) {
        Logger.getLogger(SwitchLineWrapModeAction.class.getName()).log(Level.FINE, "set line wrap mode ''{0}'' for {1}", new Object[]{newMode, NbEditorUtilities.getMimeType(jtc)});
        // save new mode to preferences
        if (supportsSaving) {
            prefs.put(key, newMode);
            try {
                prefs.flush();
            } catch (BackingStoreException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        // set new mode for open editor
        jtc.getDocument().putProperty(key, newMode); //NOI18N
        jtc.invalidate();

        // update ui
        updateLabel();
    }

    @ServiceProvider(service = StatusLineElementProvider.class)
    public static final class StatusLineElementProviderImpl implements StatusLineElementProvider {

        @Override
        public Component getStatusLineElement() {
            Component panel = panelWithSeparator(LABEL);

            LABEL.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    new SwitchLineWrapModeAction().actionPerformed(null);
                }
            });
            return panel;
        }

        Component panelWithSeparator(JLabel cell) {
            JSeparator separator = new JSeparator(SwingConstants.VERTICAL) {
                private static final long serialVersionUID = 1L;

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
    }
}
