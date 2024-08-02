package v4lpt.vpt.i018.vpc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private EditText editTextMenge;
    private EditText editTextProzent;
    private EditText editTextGramm;
    private Button buttonCalculate;
    private Button buttonClear;
    private Button buttonMengeEinheit;
    private boolean isLiter = false;
    private Button infoButton;
    private View backgroundView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        editTextMenge = findViewById(R.id.editTextMenge);
        editTextProzent = findViewById(R.id.editTextProzent);
        editTextGramm = findViewById(R.id.editTextGramm);
        buttonCalculate = findViewById(R.id.buttonCalculate);
        buttonClear = findViewById(R.id.buttonClear);
        backgroundView = findViewById(R.id.backgroundView);

        buttonMengeEinheit = findViewById(R.id.buttonMengeEinheit);

        // Set click listeners
        buttonCalculate.setOnClickListener(v -> berechneWert());
        buttonClear.setOnClickListener(v -> resetFields());
        buttonMengeEinheit.setOnClickListener(v -> toggleMengeEinheit());
        infoButton = findViewById(R.id.infoButton);
        infoButton.setOnClickListener(v -> openInfoFragment());
        // Set keyboard listeners
        setKeyboardListener(editTextMenge);
        setKeyboardListener(editTextProzent);
        setKeyboardListener(editTextGramm);

        resetFields();

        editTextMenge.requestFocus();
        editTextMenge.post(() -> editTextMenge.setSelection(editTextMenge.getText().length()));
    }
    private void openInfoFragment() {
        InfoFragment infoFragment = new InfoFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, infoFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void closeInfoFragment() {
        getSupportFragmentManager().popBackStack();

    }
    private void setKeyboardListener(EditText editText) {
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) || actionId == EditorInfo.IME_ACTION_DONE){
                if (exactlyTwoFieldsFilled()) {
                    berechneWert();
                    return true;
                }
            }
            return false;
        });
    }

    private boolean exactlyTwoFieldsFilled() {
        int filledFields = 0;
        if (!editTextMenge.getText().toString().isEmpty()) filledFields++;
        if (!editTextProzent.getText().toString().isEmpty()) filledFields++;
        if (!editTextGramm.getText().toString().isEmpty()) filledFields++;
        return filledFields == 2;
    }

    private void toggleMengeEinheit() {
        isLiter = !isLiter;
        buttonMengeEinheit.setText(isLiter ? "ℓ" : "mℓ");
        String currentValue = editTextMenge.getText().toString();
        if (!currentValue.isEmpty()) {
            try {
                double value = Double.parseDouble(currentValue);
                if (isLiter) {
                    value /= 1000; // Convert from mL to L
                    editTextMenge.setText(formatLiter(value));
                } else {
                    value *= 1000; // Convert from L to mL
                    editTextMenge.setText(formatMilliliter(value));
                }
            } catch (NumberFormatException e) {
                // Ignore if the current value is not a valid number
            }
        }
    }

    private void berechneWert() {
        String mengeString = editTextMenge.getText().toString();
        String prozentString = editTextProzent.getText().toString();
        String grammString = editTextGramm.getText().toString();

        int filledFields = 0;
        if (!mengeString.isEmpty()) filledFields++;
        if (!prozentString.isEmpty()) filledFields++;
        if (!grammString.isEmpty()) filledFields++;

        try {
            double menge = mengeString.isEmpty() ? 0 : Double.parseDouble(mengeString);
            double prozent = prozentString.isEmpty() ? 0 : Double.parseDouble(prozentString);
            double gramm = grammString.isEmpty() ? 0 : Double.parseDouble(grammString);

            if (!isLiter) {
                menge /= 1000; // Convert mL to L for calculation
            }

            if (filledFields == 2) {
                // Calculate the third value
                if (mengeString.isEmpty()) {
                    menge = gramm / (prozent * 0.789 * 10);
                    editTextMenge.setText(isLiter ? formatLiter(menge) : formatMilliliter(menge * 1000));
                    flashEditText(editTextMenge);
                } else if (prozentString.isEmpty()) {
                    prozent = gramm / (menge * 0.789 * 10);
                    editTextProzent.setText(formatProzent(prozent));
                    flashEditText(editTextProzent);
                } else {
                    gramm = menge * prozent * 0.789 * 10;
                    editTextGramm.setText(formatGramm(gramm));
                    flashEditText(editTextGramm);
                }
            } else if (filledFields == 3) {
                // Overwrite one value based on focus
                if (editTextMenge.hasFocus()) {
                    gramm = menge * prozent * 0.789 * 10;
                    editTextGramm.setText(formatGramm(gramm));
                    flashEditText(editTextGramm);
                } else if (editTextProzent.hasFocus() || editTextGramm.hasFocus()) {
                    menge = gramm / (prozent * 0.789 * 10);
                    editTextMenge.setText(isLiter ? formatLiter(menge) : formatMilliliter(menge * 1000));
                    flashEditText(editTextMenge);
                }
            } else {
                Toast.makeText(this, R.string.notenoughfields, Toast.LENGTH_SHORT).show();
            }
            if (prozent > 100 || gramm > 100 || (isLiter && menge > 10) || (!isLiter && menge > 10000)) {
                flashBackground();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.bad_input, Toast.LENGTH_SHORT).show();
        }
    }
    private void flashEditText(EditText editText) {
        // Store the original background
        final Drawable originalBackground = editText.getBackground();

        // Create a green color filter
        PorterDuffColorFilter greenFilter = new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.green), PorterDuff.Mode.SRC_ATOP);
        // Apply the green filter
        editText.getBackground().setColorFilter(greenFilter);
        // Animate back to original
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(700); // 1 second total animation
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                int alpha = (int) (255 * value);
                editText.getBackground().setColorFilter(new PorterDuffColorFilter(
                        Color.argb(alpha, 0, 102, 102), PorterDuff.Mode.SRC_ATOP));
                editText.invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Ensure we remove the filter at the end
                editText.getBackground().clearColorFilter();
            }
        });

        // Start with a delay
        editText.postDelayed(animator::start, 300);
    }

    private void resetFields() {
        editTextMenge.setText("");
        editTextProzent.setText("");
        editTextGramm.setText("");
    }

    private String formatMilliliter(double value) {
        return String.format("%.0f", value);
    }

    private String formatLiter(double value) {
        DecimalFormat df = new DecimalFormat("0.####");
        return df.format(value);
    }

    private String formatProzent(double value) {
        DecimalFormat df = new DecimalFormat("0.0");
        return df.format(value);
    }

    private String formatGramm(double value) {
        DecimalFormat df = new DecimalFormat("0.0");
        return df.format(value);
    }
    private void flashBackground() {
        int colorFrom = ContextCompat.getColor(this, R.color.vptback);
        int colorTo = Color.RED;
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(500); // 500ms for red flash
        colorAnimation.addUpdateListener(animator -> backgroundView.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.start();

        // Animation to fade back to white
        ValueAnimator fadeAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorTo, colorFrom);
        fadeAnimation.setDuration(500); // 500ms to fade back
        fadeAnimation.setStartDelay(500); // Start after the red flash
        fadeAnimation.addUpdateListener(animator -> backgroundView.setBackgroundColor((int) animator.getAnimatedValue()));
        fadeAnimation.start();
    }
}