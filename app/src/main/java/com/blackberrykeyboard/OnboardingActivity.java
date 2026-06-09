package com.blackberrykeyboard;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.blackberrykeyboard.prefs.KeyboardPreferences;
import com.blackberrykeyboard.util.KeyboardSetupHelper;

public class OnboardingActivity extends AppCompatActivity {

    private int step = 1;
    private TextView stepIndicator;
    private TextView stepDescription;
    private TextView statusText;
    private Button actionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        stepIndicator = findViewById(R.id.step_indicator);
        stepDescription = findViewById(R.id.step_description);
        statusText = findViewById(R.id.status_text);
        actionButton = findViewById(R.id.action_button);
        Button skipButton = findViewById(R.id.skip_button);

        actionButton.setOnClickListener(v -> onActionClicked());
        skipButton.setOnClickListener(v -> finishOnboarding());

        updateStepUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStepUi();
        autoAdvanceIfReady();
    }

    private void onActionClicked() {
        if (step == 1) {
            startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            return;
        }
        if (step == 2) {
            KeyboardSetupHelper.showInputMethodPicker(this);
            return;
        }
        finishOnboarding();
    }

    private void autoAdvanceIfReady() {
        if (step == 1 && KeyboardSetupHelper.isEnabled(this)) {
            step = 2;
        }
        if (step == 2 && KeyboardSetupHelper.isDefault(this)) {
            step = 3;
        }
        updateStepUi();
    }

    private void updateStepUi() {
        boolean enabled = KeyboardSetupHelper.isEnabled(this);
        boolean isDefault = KeyboardSetupHelper.isDefault(this);

        switch (step) {
            case 1:
                stepIndicator.setText(R.string.onboarding_step1);
                stepDescription.setText(R.string.onboarding_step1_desc);
                actionButton.setText(R.string.onboarding_btn_enable);
                statusText.setText(enabled
                        ? R.string.onboarding_status_done
                        : R.string.onboarding_status_pending);
                break;
            case 2:
                stepIndicator.setText(R.string.onboarding_step2);
                stepDescription.setText(R.string.onboarding_step2_desc);
                actionButton.setText(R.string.onboarding_btn_default);
                statusText.setText(isDefault
                        ? R.string.onboarding_status_done
                        : R.string.onboarding_status_pending);
                break;
            default:
                step = 3;
                stepIndicator.setText(R.string.onboarding_step3);
                stepDescription.setText(R.string.onboarding_step3_desc);
                actionButton.setText(R.string.onboarding_btn_finish);
                statusText.setText(R.string.status_ready);
                break;
        }
    }

    private void finishOnboarding() {
        KeyboardPreferences.get(this).setOnboardingDone(true);
        startActivity(new Intent(this, SettingsActivity.class));
        finish();
    }
}
