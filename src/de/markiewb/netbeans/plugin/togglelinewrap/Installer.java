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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall implements PropertyChangeListener{

    @Override
    public void restored() {
         EditorRegistry.addPropertyChangeListener(this);
    }

    @Override
    public void uninstalled() {
        EditorRegistry.removePropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        new SwitchLineWrapModeAction().updateLabel();
    }
    
    
}
