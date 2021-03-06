/*
 * Copyright (C) 2010-2014 The MPDroid Project
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

package com.namelessdev.mpdroid;

import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.helpers.CoverManager;

import org.a0z.mpd.MPD;
import org.a0z.mpd.exception.MPDServerException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.SearchRecentSuggestions;
import android.text.format.Formatter;

public class SettingsFragment extends PreferenceFragment {

    private PreferenceScreen informationScreen;

    private EditTextPreference cacheUsage1;

    private EditTextPreference cacheUsage2;

    private Handler handler;

    private EditTextPreference version;

    private EditTextPreference artists;

    private EditTextPreference albums;

    private EditTextPreference songs;

    private CheckBoxPreference localCoverCheckbox;

    private Preference coverFilename;

    private Preference musicPath;

    private CheckBoxPreference localCoverCache;

    private CheckBoxPreference albumArtLibrary;

    private CheckBoxPreference phonePause;

    private CheckBoxPreference phoneStateChange;

    private boolean preferencesBinded;


    public SettingsFragment() {
        preferencesBinded = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        handler = new Handler();

        informationScreen = (PreferenceScreen) findPreference("informationScreen");

        if (!getResources().getBoolean(R.bool.isTablet)) {
            final PreferenceScreen interfaceCategory = (PreferenceScreen) findPreference(
                    "nowPlayingScreen");
            interfaceCategory.removePreference(findPreference("tabletUI"));
        }

        version = (EditTextPreference) findPreference("version");
        artists = (EditTextPreference) findPreference("artists");
        albums = (EditTextPreference) findPreference("albums");
        songs = (EditTextPreference) findPreference("songs");

        localCoverCheckbox = (CheckBoxPreference) findPreference(
                "enableLocalCover");
        musicPath = (Preference) findPreference("musicPath");
        coverFilename = (Preference) findPreference("coverFileName");
        if (localCoverCheckbox.isChecked()) {
            musicPath.setEnabled(true);
            coverFilename.setEnabled(true);
        } else {
            musicPath.setEnabled(false);
            coverFilename.setEnabled(false);
        }

        cacheUsage1 = (EditTextPreference) findPreference("cacheUsage1");
        cacheUsage2 = (EditTextPreference) findPreference("cacheUsage2");

        // Album art library listing requires cover art cache
        localCoverCache = (CheckBoxPreference) findPreference(
                "enableLocalCoverCache");
        albumArtLibrary = (CheckBoxPreference) findPreference(
                "enableAlbumArtLibrary");
        albumArtLibrary.setEnabled(localCoverCache.isChecked());

        // Enable/Disable playback resume when call ends only if playback pause
        // is enabled when call starts
        phonePause = (CheckBoxPreference) findPreference("pauseOnPhoneStateChange");
        phoneStateChange = (CheckBoxPreference) findPreference("playOnPhoneStateChange");
        phoneStateChange.setEnabled(phonePause.isChecked());

        preferencesBinded = true;
        refreshDynamicFields();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        refreshDynamicFields();
    }

    public void refreshDynamicFields() {
        if (getActivity() == null || !preferencesBinded) {
            return;
        }
        final MPDApplication app = (MPDApplication) getActivity().getApplication();
        long size = new CachedCover(app).getCacheUsage();
        final String usage = Formatter.formatFileSize(app, size);
        cacheUsage1.setSummary(usage);
        cacheUsage2.setSummary(usage);
        onConnectionStateChanged(app, app.oMPDAsyncHelper.oMPD.isConnected());
    }

    public void onConnectionStateChanged(final MPDApplication app, boolean connected) {
        informationScreen.setEnabled(connected);
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    final String versionText = app.oMPDAsyncHelper.oMPD
                            .getMpdVersion();
                    final String artistsText = ""
                            + app.oMPDAsyncHelper.oMPD.getStatistics()
                            .getArtists();
                    final String albumsText = ""
                            + app.oMPDAsyncHelper.oMPD.getStatistics()
                            .getAlbums();
                    final String songsText = ""
                            + app.oMPDAsyncHelper.oMPD.getStatistics()
                            .getSongs();
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            version.setSummary(versionText);
                            artists.setSummary(artistsText);
                            albums.setSummary(albumsText);
                            songs.setSummary(songsText);
                        }
                    });
                } catch (MPDServerException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        // Is it the connectionscreen which is called?
        if (preference.getKey() == null) {
            return false;
        }

        final MPDApplication app = (MPDApplication) getActivity().getApplication();

        if (preference.getKey().equals("refreshMPDDatabase")) {
            try {
                MPD oMPD = app.oMPDAsyncHelper.oMPD;
                oMPD.refreshDatabase();
            } catch (MPDServerException e) {
            }
            return true;
        } else if (preference.getKey().equals("clearLocalCoverCache")) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.clearLocalCoverCache)
                    .setMessage(R.string.clearLocalCoverCachePrompt)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Todo : The covermanager must already have been
                            // initialized, get rid of the getInstance arguments
                            CoverManager.getInstance(app, null).clear();
                            cacheUsage1.setSummary("0.00B");
                            cacheUsage2.setSummary("0.00B");
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .show();
            return true;

        } else if (preference.getKey().equals("enableLocalCover")) {
            if (localCoverCheckbox.isChecked()) {
                musicPath.setEnabled(true);
                coverFilename.setEnabled(true);
            } else {
                musicPath.setEnabled(false);
                coverFilename.setEnabled(false);
            }
            return true;
        } else if (preference.getKey().equals("enableLocalCoverCache")) {
            // album art library listing requires cover art cache
            if (localCoverCache.isChecked()) {
                albumArtLibrary.setEnabled(true);
            } else {
                albumArtLibrary.setEnabled(false);
                albumArtLibrary.setChecked(false);
            }
            return true;

        } else if (preference.getKey().equals("pauseOnPhoneStateChange")) {
            // Enable/Disable playback resume when call ends only if playback
            // pause is enabled when call starts
            CheckBoxPreference phonePause = (CheckBoxPreference) findPreference(
                    "pauseOnPhoneStateChange");
            CheckBoxPreference phoneStateChange = (CheckBoxPreference) findPreference(
                    "playOnPhoneStateChange");
            phoneStateChange.setEnabled(phonePause.isChecked());
        } else if (preference.getKey().equals("clearSearchHistory")) {
            final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
                    SearchRecentProvider.AUTHORITY, SearchRecentProvider.MODE);
            suggestions.clearHistory();
            preference.setEnabled(false);
        }

        return false;

    }

}
