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

package dev.namelessnanashi.walrus.card.carddata.ui;

import android.app.Dialog;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import dev.namelessnanashi.walrus.R;
import dev.namelessnanashi.walrus.card.carddata.MifareCardData;
import dev.namelessnanashi.walrus.card.carddata.MifareReadStep;
import dev.namelessnanashi.walrus.card.carddata.StaticKeyMifareReadStep;
import dev.namelessnanashi.walrus.databinding.StaticKeyMifareReadStepDialogBinding;
import dev.namelessnanashi.walrus.util.UIUtils;

// TODO XXX: setError on views like component dialogs
public class StaticKeyMifareReadStepDialogFragment extends MifareReadStepDialogFragment {

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        StaticKeyMifareReadStep staticReadStep =
                (StaticKeyMifareReadStep) getArguments().getSerializable("read_step");

        final StaticKeyMifareReadStepDialogViewModel viewModel =
                ViewModelProviders.of(this,
                        new StaticKeyMifareReadStepDialogViewModel.Factory(staticReadStep))
                        .get(StaticKeyMifareReadStepDialogViewModel.class);

        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(staticReadStep != null ? R.string.edit_mifare_static_key_read_step :
                        R.string.add_mifare_static_key_read_step)
                .titleColorRes(R.color.primaryTextColor)
                .contentColorRes(R.color.primaryTextColor)
                .backgroundColorRes(R.color.primaryDarkColor)
                .positiveColorRes(R.color.primaryTextColor)
                .negativeColorRes(R.color.secondaryTextColor)
                .widgetColorRes(R.color.secondaryColor)
                .customView(R.layout.layout_static_key_mifare_read_step_dialog, true)
                .positiveText(staticReadStep != null ? android.R.string.ok : R.string.add)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog,
                            @NonNull DialogAction which) {
                        viewModel.onAddClick();
                    }
                })
                .negativeText(android.R.string.cancel)
                .build();

        StaticKeyMifareReadStepDialogBinding binding = StaticKeyMifareReadStepDialogBinding.bind(
                dialog.getCustomView());

        binding.blocksToRead.setText(viewModel.blocks.getValue());
        binding.key.setText(viewModel.key.getValue());
        MifareReadStep.KeySlotAttempts initialKeySlotAttempts = viewModel.keySlotAttempts.getValue();
        binding.slotA.setChecked(initialKeySlotAttempts != null && initialKeySlotAttempts.hasSlotA());
        binding.slotB.setChecked(initialKeySlotAttempts != null && initialKeySlotAttempts.hasSlotB());

        binding.blocksToRead.addTextChangedListener(new UIUtils.TextChangeWatcher() {
            @Override
            public void onNotIgnoredTextChanged(CharSequence charSequence, int i, int i1,
                    int i2) {
                viewModel.blocks.setValue(charSequence.toString());
            }
        });
        binding.key.addTextChangedListener(new UIUtils.TextChangeWatcher() {
            @Override
            public void onNotIgnoredTextChanged(CharSequence charSequence, int i, int i1,
                    int i2) {
                viewModel.key.setValue(charSequence.toString());
            }
        });
        binding.slotA.setOnCheckedChangeListener((buttonView, isChecked) ->
                viewModel.onSlotCheckedChanged(MifareCardData.KeySlot.A, isChecked));
        binding.slotB.setOnCheckedChangeListener((buttonView, isChecked) ->
                viewModel.onSlotCheckedChanged(MifareCardData.KeySlot.B, isChecked));

        viewModel.getIsValid().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean isValid) {
                dialog.getActionButton(DialogAction.POSITIVE).setEnabled(isValid);
            }
        });

        viewModel.getResult().observe(this, new Observer<MifareReadStep>() {
            @Override
            public void onChanged(@Nullable MifareReadStep readStep) {
                ((OnResultCallback) getParentFragment()).onResult(readStep,
                        getArguments().getInt("callback_id"));
            }
        });

        return dialog;
    }
}
