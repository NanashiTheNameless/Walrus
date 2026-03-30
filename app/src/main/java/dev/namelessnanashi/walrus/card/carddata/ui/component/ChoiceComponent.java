/*
 * Copyright 2018 Daniel Underhay & Matthew Daley.
 *
 * This file is part of Walrus.
 *
 * Walrus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Walrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Walrus.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.namelessnanashi.walrus.card.carddata.ui.component;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import dev.namelessnanashi.walrus.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChoiceComponent extends ContainerComponent {

    private final List<Choice> choices;

    private final LinearLayout viewGroup;
    private final Spinner spinner;

    public ChoiceComponent(final Context context, String title, List<Choice> choices,
            int initialChoice) {
        super(context, title);

        this.choices = choices;

        viewGroup = new LinearLayout(context);
        viewGroup.setOrientation(LinearLayout.VERTICAL);

        spinner = new Spinner(context);
        FrameLayout spinnerContainer = new FrameLayout(context);
        spinnerContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        spinnerContainer.setBackgroundResource(R.drawable.drawable_choice_spinner_background);
        viewGroup.addView(spinnerContainer);

        spinner.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        spinner.setPadding(0, 0, 0, 0);
        spinner.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, android.R.color.transparent)));
        spinner.setPrompt(title);
        spinnerContainer.addView(spinner);

        ImageView dropDownIndicator = new ImageView(context);
        FrameLayout.LayoutParams indicatorLayoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.CENTER_VERTICAL);
        int indicatorMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12,
                spinner.getResources().getDisplayMetrics());
        indicatorLayoutParams.setMargins(0, 0, indicatorMargin, 0);
        dropDownIndicator.setLayoutParams(indicatorLayoutParams);
        dropDownIndicator.setImageDrawable(ContextCompat.getDrawable(context,
                R.drawable.ic_arrow_drop_down_white_24px));
        dropDownIndicator.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.primaryTextColor)));
        dropDownIndicator.setClickable(false);
        dropDownIndicator.setFocusable(false);
        spinnerContainer.addView(dropDownIndicator);

        viewGroup.addView(MultiComponent.createSpacer(context));

        final ViewGroup choiceViewGroup = new FrameLayout(context);
        viewGroup.addView(choiceViewGroup);

        spinner.setPopupBackgroundDrawable(new ColorDrawable(
                ContextCompat.getColor(context, R.color.primaryDarkColor)));

        List<String> choiceNames = new ArrayList<>();
        for (Choice choice : choices) {
            choiceNames.add(choice.name);
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(context,
                R.layout.layout_multiline_spinner_item, choiceNames) {
            @Override
            public View getDropDownView(int position, @Nullable View convertView,
                    @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                applyChoiceColor(position, view);
                return view;
            }

            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                applyChoiceColor(position, view);
                return view;
            }

            private void applyChoiceColor(int position, View view) {
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(
                            ChoiceComponent.this.choices.get(position).color);
                }
            }
        };
        spinnerAdapter.setDropDownViewResource(R.layout.layout_multiline_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                choiceViewGroup.removeAllViews();

                View choiceView = getChoiceComponent().getView();
                if (choiceView != null) {
                    choiceViewGroup.addView(choiceView);
                    showKeyboardForFirstTextEditor(choiceView);
                }

                if (onComponentChangeCallback != null) {
                    onComponentChangeCallback.onComponentChange(ChoiceComponent.this);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        spinner.setSelection(initialChoice);
    }

    @Nullable
    @Override
    protected View getInnerView() {
        return viewGroup;
    }

    @Override
    public List<Component> getChildren() {
        List<Component> children = new ArrayList<>();

        for (Choice choice : choices) {
            children.add(choice.component);
        }

        return children;
    }

    @Override
    public List<Component> getVisibleChildren() {
        return Collections.singletonList(getChoiceComponent());
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        super.restoreInstanceState(savedInstanceState);

        spinner.setSelection(savedInstanceState.getInt("choice"));
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        super.saveInstanceState(outState);

        outState.putInt("choice", getChoicePosition());
    }

    public int getChoicePosition() {
        return spinner.getSelectedItemPosition();
    }

    public Component getChoiceComponent() {
        return choices.get(getChoicePosition()).component;
    }

    private void showKeyboardForFirstTextEditor(@NonNull View rootView) {
        rootView.post(() -> {
            View textEditor = findFirstTextEditor(rootView);
            if (textEditor == null || !textEditor.isShown()) {
                return;
            }

            textEditor.requestFocus();

            InputMethodManager inputMethodManager =
                    (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(textEditor, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    @Nullable
    private View findFirstTextEditor(@Nullable View view) {
        if (view == null || view.getVisibility() != View.VISIBLE || !view.isEnabled()) {
            return null;
        }

        if (view.onCheckIsTextEditor()) {
            return view;
        }

        if (!(view instanceof ViewGroup)) {
            return null;
        }

        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View textEditor = findFirstTextEditor(viewGroup.getChildAt(i));
            if (textEditor != null) {
                return textEditor;
            }
        }

        return null;
    }

    public static class Choice {

        private final String name;
        private final int color;
        private final Component component;

        public Choice(String name, int color, Component component) {
            this.name = name;
            this.color = color;
            this.component = component;
        }
    }
}
