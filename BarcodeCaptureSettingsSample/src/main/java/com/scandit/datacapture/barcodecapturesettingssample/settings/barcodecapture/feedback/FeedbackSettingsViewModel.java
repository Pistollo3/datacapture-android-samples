/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scandit.datacapture.barcodecapturesettingssample.settings.barcodecapture.feedback;

import androidx.lifecycle.ViewModel;
import com.scandit.datacapture.barcodecapturesettingssample.models.SettingsManager;

@SuppressWarnings("WeakerAccess")
public class FeedbackSettingsViewModel extends ViewModel {

    private final SettingsManager settingsManager = SettingsManager.getCurrentSettings();

    void setSoundEnabled(boolean enabled) {
        settingsManager.setSoundEnabled(enabled);
    }

    boolean isSoundEnabled() {
        return settingsManager.isSoundEnabled();
    }

    void setVibrationEnabled(boolean enabled) {
        settingsManager.setVibrationEnabled(enabled);
    }

    boolean isVibrationEnabled() {
        return settingsManager.isVibrationEnabled();
    }
}
