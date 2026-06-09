package com.blackberrykeyboard;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.blackberrykeyboard.nativeengine.NativeEngine;
import com.blackberrykeyboard.prefs.KeyboardPreferences;
import com.blackberrykeyboard.prefs.KeyboardTheme;
import com.blackberrykeyboard.util.KeyboardSetupHelper;

public class SettingsActivity extends AppCompatActivity {

    private NativeEngine engine;
    private KeyboardPreferences prefs;
    private TextView statusCard;
    private TextView dictionaryCount;
    private TextView keyScaleLabel;
    private SeekBar keyScaleSeek;
    private Spinner themeSpinner;
    private Spinner languageSpinner;
    private Switch switchSound;
    private Switch switchLearn;
    private Switch switchTwoType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = KeyboardPreferences.get(this);
        if (!prefs.isOnboardingDone() && !KeyboardSetupHelper.isReady(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_settings);
        engine = new NativeEngine(this, getFilesDir().getAbsolutePath());
        engine.setLearningEnabled(prefs.isLearnEnabled());
        engine.setLanguage(prefs.resolveLanguageCode());

        bindViews();
        loadPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        refreshDictionaryCount();
    }

    @Override
    protected void onDestroy() {
        if (engine != null) {
            engine.saveUserDictionary();
            engine.destroy();
            engine = null;
        }
        super.onDestroy();
    }

    private void bindViews() {
        statusCard = findViewById(R.id.status_card);
        dictionaryCount = findViewById(R.id.dictionary_count);
        keyScaleLabel = findViewById(R.id.key_scale_label);
        keyScaleSeek = findViewById(R.id.key_scale_seek);
        themeSpinner = findViewById(R.id.theme_spinner);
        languageSpinner = findViewById(R.id.language_spinner);
        switchSound = findViewById(R.id.switch_sound);
        switchLearn = findViewById(R.id.switch_learn);
        switchTwoType = findViewById(R.id.switch_two_type);

        findViewById(R.id.btn_enable_keyboard).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));

        findViewById(R.id.btn_set_default).setOnClickListener(v ->
                KeyboardSetupHelper.showInputMethodPicker(this));

        findViewById(R.id.btn_clear_dictionary).setOnClickListener(v -> confirmClearDictionary());

        EditText demoInput = findViewById(R.id.demo_input);
        demoInput.requestFocus();
        demoInput.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(demoInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void loadPreferences() {
        String[] themeNames = new String[KeyboardTheme.ALL.length];
        for (int i = 0; i < KeyboardTheme.ALL.length; i++) {
            themeNames[i] = KeyboardTheme.ALL[i].displayName;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, themeNames);
        themeSpinner.setAdapter(adapter);

        KeyboardTheme current = prefs.getTheme();
        for (int i = 0; i < KeyboardTheme.ALL.length; i++) {
            if (KeyboardTheme.ALL[i].id.equals(current.id)) {
                themeSpinner.setSelection(i);
                break;
            }
        }

        themeSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                prefs.setTheme(KeyboardTheme.ALL[position]);
            }
        });

        switchSound.setChecked(prefs.isSoundEnabled());
        switchSound.setOnCheckedChangeListener((v, checked) -> prefs.setSoundEnabled(checked));

        switchLearn.setChecked(prefs.isLearnEnabled());
        switchLearn.setOnCheckedChangeListener((v, checked) -> {
            prefs.setLearnEnabled(checked);
            if (engine != null) {
                engine.setLearningEnabled(checked);
            }
        });

        switchTwoType.setChecked(prefs.isTwoTypeMode());
        switchTwoType.setOnCheckedChangeListener((v, checked) -> {
            prefs.setTwoTypeMode(checked);
            if (engine != null) {
                engine.setTwoTypeMode(checked);
            }
        });

        final String[] langCodes = {"auto", "es", "en"};
        String[] langNames = {
                getString(R.string.lang_auto),
                getString(R.string.lang_es),
                getString(R.string.lang_en)
        };
        languageSpinner.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, langNames));
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(prefs.getLanguage())) {
                languageSpinner.setSelection(i);
                break;
            }
        }
        languageSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                prefs.setLanguage(langCodes[position]);
                if (engine != null) {
                    engine.setLanguage(prefs.resolveLanguageCode());
                }
            }
        });

        float scale = prefs.getKeyScale();
        keyScaleSeek.setProgress(Math.round((scale - 1.0f) * 100f));
        updateKeyScaleLabel(scale);
        keyScaleSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = 1.0f + progress / 100f;
                prefs.setKeyScale(value);
                updateKeyScaleLabel(value);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    private void updateKeyScaleLabel(float scale) {
        int percent = Math.round(scale * 100f);
        keyScaleLabel.setText(getString(R.string.label_key_size) + " (" + percent + "%)");
    }

    private void updateStatus() {
        if (KeyboardSetupHelper.isReady(this)) {
            statusCard.setText(R.string.status_ready);
        } else if (KeyboardSetupHelper.isEnabled(this)) {
            statusCard.setText(R.string.status_enabled_not_default);
        } else {
            statusCard.setText(R.string.status_not_enabled);
        }
    }

    private void refreshDictionaryCount() {
        if (engine != null) {
            dictionaryCount.setText(getString(
                    R.string.label_dictionary_count, engine.getLearnedWordCount()));
        }
    }

    private void confirmClearDictionary() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.clear_dict_confirm)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (engine != null) {
                        engine.clearUserDictionary();
                        refreshDictionaryCount();
                        Toast.makeText(this, R.string.clear_dict_done, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
