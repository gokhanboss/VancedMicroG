/*
 * Copyright (C) 2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.mgoogle.android.gms.R;

import org.microg.gms.gcm.GcmPrefs;
import org.microg.gms.gcm.McsService;
import org.microg.gms.gcm.TriggerReceiver;
import org.microg.tools.ui.ResourceSettingsFragment;

public class GcmAdvancedFragment extends ResourceSettingsFragment {

    private static String[] HEARTBEAT_PREFS = new String[]{GcmPrefs.PREF_NETWORK_MOBILE, GcmPrefs.PREF_NETWORK_ROAMING, GcmPrefs.PREF_NETWORK_WIFI, GcmPrefs.PREF_NETWORK_OTHER};

    public GcmAdvancedFragment() {
        preferencesResource = R.xml.preferences_gcm_advanced;
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        for (String pref : HEARTBEAT_PREFS) {
            findPreference(pref).setOnPreferenceChangeListener((preference, newValue) -> {
                getPreferenceManager().getSharedPreferences().edit().putString(preference.getKey(), (String) newValue).apply();
                updateContent();
                if (newValue.equals("-1") && preference.getKey().equals(McsService.activeNetworkPref)) {
                    McsService.stop(getContext());
                } else if (!McsService.isConnected()) {
                    getContext().sendBroadcast(new Intent(TriggerReceiver.FORCE_TRY_RECONNECT, null, getContext(), TriggerReceiver.class));
                }
                return true;
            });
        }
        updateContent();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateContent();
    }

    private void updateContent() {
        GcmPrefs prefs = GcmPrefs.get(getContext());
        for (String pref : HEARTBEAT_PREFS) {
            Preference preference = findPreference(pref);
            int state = prefs.getNetworkValue(pref);
            if (state == 0) {
                int heartbeat = prefs.getHeartbeatMsFor(preference.getKey(), true);
                if (heartbeat == 0) {
                    preference.setSummary(getString(R.string.service_status_enabled_short) + " / " + getString(R.string.gcm_status_pref_auto));
                } else {
                    preference.setSummary(getString(R.string.service_status_enabled_short) + " / " + getString(R.string.gcm_status_pref_auto) + ": " + getHeartbeatString(heartbeat));
                }
            } else if (state == -1) {
                preference.setSummary(getString(R.string.service_status_disabled_short));
            } else {
                preference.setSummary(getString(R.string.service_status_enabled_short) + " / " + getString(R.string.gcm_status_pref_manual) + ": " + getHeartbeatString(state * 60000));
            }
        }
    }

    private String getHeartbeatString(int heartbeatMs) {
        if (heartbeatMs < 120000) {
            return (heartbeatMs / 1000) + " " + getString(R.string.gcm_status_pref_sec);
        }
        return (heartbeatMs / 60000) + " " + getString(R.string.gcm_status_pref_min);
    }
}
