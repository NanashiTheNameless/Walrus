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

package dev.namelessnanashi.walrus.card.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.UiThread;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import dev.namelessnanashi.walrus.R;
import dev.namelessnanashi.walrus.WalrusApplication;
import dev.namelessnanashi.walrus.card.Card;
import dev.namelessnanashi.walrus.card.DatabaseHelper;
import dev.namelessnanashi.walrus.card.OrmLiteBaseAppCompatActivity;
import dev.namelessnanashi.walrus.card.QueryUtils;
import dev.namelessnanashi.walrus.card.carddata.CardData;
import dev.namelessnanashi.walrus.card.carddata.HIDCardData;
import dev.namelessnanashi.walrus.card.carddata.MifareCardData;
import dev.namelessnanashi.walrus.card.carddata.ui.PickCardDataClassDialogFragment;
import dev.namelessnanashi.walrus.card.carddata.ui.component.ComponentDialogFragment;
import dev.namelessnanashi.walrus.card.carddata.ui.component.ComponentSourceAndSink;
import dev.namelessnanashi.walrus.device.BulkReadCardsService;
import dev.namelessnanashi.walrus.device.CardDevice;
import dev.namelessnanashi.walrus.device.CardDeviceManager;
import dev.namelessnanashi.walrus.device.CardDeviceOperation;
import dev.namelessnanashi.walrus.device.ReadCardDataOperation;
import dev.namelessnanashi.walrus.device.WriteOrEmulateCardDataOperation;
import dev.namelessnanashi.walrus.device.ui.CardDeviceAdapter;
import dev.namelessnanashi.walrus.device.ui.PickCardDataTargetDialogFragment;
import dev.namelessnanashi.walrus.device.ui.ReadCardDataOperationFragment;
import dev.namelessnanashi.walrus.device.ui.WriteOrEmulateCardDataOperationFragment;
import dev.namelessnanashi.walrus.ui.OpenStreetMapActivity;
import dev.namelessnanashi.walrus.util.AppFontManager;
import dev.namelessnanashi.walrus.util.UIUtils;

import org.apache.commons.lang3.ArrayUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CardActivity extends OrmLiteBaseAppCompatActivity<DatabaseHelper>
        implements DeleteCardConfirmDialogFragment.OnDeleteCardConfirmCallback,
        PickCardDataTargetDialogFragment.OnCardDataTargetClickCallback,
        PickCardDataClassDialogFragment.OnCardDataClassClickCallback,
        ReadCardDataOperationFragment.OnResultCallback, ComponentDialogFragment.OnEditedCallback,
        CardDevice.OnOperationCreatedCallback {

    private static final String EXTRA_MODE =
            "dev.namelessnanashi.walrus.card.ui.CardActivity.EXTRA_MODE";
    private static final String EXTRA_CARD =
            "dev.namelessnanashi.walrus.card.ui.CardActivity.EXTRA_CARD";

    private static final String PICK_CARD_DEVICE_DIALOG_FRAGMENT_TAG = "pick_card_device_dialog";
    private static final String PICK_CARD_DATA_CLASS_DIALOG_FRAGMENT_TAG =
            "pick_card_data_class_dialog";
    private static final int PICK_LOCATION_REQUEST_CODE = 100;
    private static final double MIN_LATITUDE = -90d;
    private static final double MAX_LATITUDE = 90d;
    private static final double MIN_LONGITUDE = -180d;
    private static final double MAX_LONGITUDE = 180d;

    private final UIUtils.TextChangeWatcher notesEditorDirtier = new TextChangeDirtier();
    private final UIUtils.TextChangeWatcher walrusCardViewNameDirtier = new TextChangeDirtier();
    private final UIUtils.TextChangeWatcher locationLatEditorWatcher =
            new LocationEditorChangeWatcher();
    private final UIUtils.TextChangeWatcher locationLngEditorWatcher =
            new LocationEditorChangeWatcher();

    private Mode mode;
    private Card card;
    private boolean firstResume = true;
    private boolean dirty;
    private WalrusCardView walrusCardView;
    private TextView notes;
    private EditText notesEditor;
    private View locationDisplay;
    private View locationMap;
    private View locationEditor;
    private TextView locationCoordinates;
    private EditText locationLatEditor;
    private EditText locationLngEditor;
    private View openEditedLocationMap;

    public CardActivity() {
        super(DatabaseHelper.class);
    }

    public static void startActivity(Activity activity, Mode mode, Card card, View transitionView) {
        Intent intent = new Intent(activity, CardActivity.class);

        intent.putExtra(EXTRA_MODE, mode);
        intent.putExtra(EXTRA_CARD, card);

        if (transitionView != null) {
            List<Pair<View, String>> sharedElements = new ArrayList<>();

            View view = activity.findViewById(android.R.id.statusBarBackground);
            if (view != null) {
                sharedElements.add(new Pair<>(view, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME));
            }
            view = activity.findViewById(android.R.id.navigationBarBackground);
            if (view != null) {
                sharedElements.add(new Pair<>(view,
                        Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME));
            }
            view = activity.findViewById(R.id.toolbar);
            if (view != null) {
                sharedElements.add(new Pair<>(view, "toolbar"));
            }

            sharedElements.add(new Pair<>(transitionView, "card"));

            // noinspection unchecked, SuspiciousToArrayCall
            ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(activity,
                    (Pair<View, String>[]) sharedElements.toArray(new Pair[sharedElements.size()]));

            activity.startActivity(intent, activityOptions.toBundle());
        } else {
            activity.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_card);

        Intent intent = getIntent();

        mode = (Mode) intent.getSerializableExtra(EXTRA_MODE);

        if (savedInstanceState == null) {
            card = (Card) intent.getSerializableExtra(EXTRA_CARD);

            if (card == null) {
                card = new Card();
            } else if (card.id == 0) {
                dirty = true;
            } else {
                Card persistedCard = getHelper().getCardDao().queryForId(card.id);
                if (persistedCard != null) {
                    card = persistedCard;
                }
            }
        } else {
            card = (Card) savedInstanceState.getSerializable("card");
            dirty = savedInstanceState.getBoolean("dirty");
        }

        switch (mode) {
            case VIEW:
                setTitle(R.string.view_card);
                break;

            case EDIT:
                setTitle(intent.getSerializableExtra(EXTRA_CARD) == null ? R.string.new_card :
                        R.string.edit_card);
                break;

            case EDIT_BULK_READ_CARD_TEMPLATE:
                setTitle(R.string.set_template);
                break;
        }

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (mode != Mode.VIEW) {
                actionBar.setHomeAsUpIndicator(
                        ContextCompat.getDrawable(this, R.drawable.ic_close_white_24px));
            }
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        walrusCardView = findViewById(R.id.card);
        notes = findViewById(R.id.notes);
        notesEditor = findViewById(R.id.notesEditor);
        locationDisplay = findViewById(R.id.locationDisplay);
        locationMap = findViewById(R.id.locationMap);
        locationEditor = findViewById(R.id.locationEditor);
        locationCoordinates = findViewById(R.id.locationCoordinates);
        locationLatEditor = findViewById(R.id.locationLatEditor);
        locationLngEditor = findViewById(R.id.locationLngEditor);
        openEditedLocationMap = findViewById(R.id.openEditedLocationMap);

        walrusCardView.setCard(card);
        walrusCardView.setEditable(mode != Mode.VIEW);

        switch (mode) {
            case VIEW:
                findViewById(R.id.editButtons).setVisibility(View.GONE);
                findViewById(R.id.notesEditor).setVisibility(View.GONE);
                locationEditor.setVisibility(View.GONE);
                break;

            case EDIT:
                findViewById(R.id.viewButtons).setVisibility(View.GONE);
                findViewById(R.id.notes).setVisibility(View.GONE);
                locationDisplay.setVisibility(View.GONE);
                locationEditor.setVisibility(View.VISIBLE);
                break;

            case EDIT_BULK_READ_CARD_TEMPLATE:
                findViewById(R.id.viewButtons).setVisibility(View.GONE);
                findViewById(R.id.editButtons).setVisibility(View.GONE);
                findViewById(R.id.notes).setVisibility(View.GONE);
                locationDisplay.setVisibility(View.GONE);
                locationEditor.setVisibility(View.VISIBLE);
                break;
        }

        updateUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int id = 0;
        switch (mode) {
            case VIEW:
                id = R.menu.menu_view_card;
                break;

            case EDIT:
                id = R.menu.menu_edit_card;
                break;

            case EDIT_BULK_READ_CARD_TEMPLATE:
                id = R.menu.menu_bulk_read_card;
                break;
        }

        getMenuInflater().inflate(id, menu);
        return true;
    }

    private void updateUI() {
        notesEditorDirtier.ignoreNext();
        walrusCardViewNameDirtier.ignoreNext();
        locationLatEditorWatcher.ignoreNext();
        locationLngEditorWatcher.ignoreNext();

        walrusCardView.setCard(card);

        findViewById(R.id.viewData).setEnabled(card.cardData != null);

        ((TextView) findViewById(R.id.dateAcquired)).setText(card.cardDataAcquired != null
                ? card.cardDataAcquired.toString() : getString(R.string.unknown));

        TextView locationUnknown = findViewById(R.id.locationUnknown);
        if (card.cardLocationLat != null && card.cardLocationLng != null) {
            locationMap.setVisibility(View.VISIBLE);
            locationCoordinates.setText(getString(R.string.location_coordinates,
                    card.cardLocationLat, card.cardLocationLng));
            locationUnknown.setVisibility(View.GONE);
        } else {
            locationMap.setVisibility(View.GONE);
            locationUnknown.setVisibility(View.VISIBLE);
        }

        notes.setText(card.notes);
        notesEditor.setText(card.notes);
        locationLatEditor.setText(card.cardLocationLat != null
                ? Double.toString(card.cardLocationLat) : "");
        locationLngEditor.setText(card.cardLocationLng != null
                ? Double.toString(card.cardLocationLng) : "");
        updateEditedLocationButtonState();
    }

    public void onOpenLocationMapClick(View view) {
        Pair<Double, Double> locationCoordinates = getLocationToOpen(false);
        if (mode == Mode.VIEW
                && (locationCoordinates.first == null || locationCoordinates.second == null)) {
            return;
        }

        Intent mapIntent = OpenStreetMapActivity.createIntent(this, locationCoordinates.first,
                locationCoordinates.second, mode != Mode.VIEW);
        if (mode == Mode.VIEW) {
            startActivity(mapIntent);
        } else {
            startActivityForResult(mapIntent, PICK_LOCATION_REQUEST_CODE);
        }
    }

    private Pair<Double, Double> getLocationToOpen(boolean showErrors) {
        if (mode == Mode.VIEW) {
            return new Pair<>(card.cardLocationLat, card.cardLocationLng);
        }

        Pair<Double, Double> editedLocation = parseEditedLocation(showErrors);
        if (editedLocation != null
                && editedLocation.first != null
                && editedLocation.second != null) {
            return editedLocation;
        }

        return new Pair<>(card.cardLocationLat, card.cardLocationLng);
    }

    private Pair<Double, Double> parseEditedLocation(boolean showErrors) {
        String latitudeText = locationLatEditor.getText().toString().trim();
        String longitudeText = locationLngEditor.getText().toString().trim();

        if (showErrors) {
            locationLatEditor.setError(null);
            locationLngEditor.setError(null);
        }

        if (latitudeText.isEmpty() && longitudeText.isEmpty()) {
            return new Pair<>(null, null);
        }

        if (latitudeText.isEmpty() || longitudeText.isEmpty()) {
            if (showErrors) {
                if (latitudeText.isEmpty()) {
                    locationLatEditor.setError(getString(R.string.enter_both_coordinates));
                }
                if (longitudeText.isEmpty()) {
                    locationLngEditor.setError(getString(R.string.enter_both_coordinates));
                }
            }
            return null;
        }

        Double latitude = parseCoordinate(latitudeText, MIN_LATITUDE, MAX_LATITUDE,
                locationLatEditor, R.string.invalid_latitude, showErrors);
        Double longitude = parseCoordinate(longitudeText, MIN_LONGITUDE, MAX_LONGITUDE,
                locationLngEditor, R.string.invalid_longitude, showErrors);

        if (latitude == null || longitude == null) {
            return null;
        }

        return new Pair<>(latitude, longitude);
    }

    private Double parseCoordinate(String value, double minValue, double maxValue,
            EditText editText, int errorStringId, boolean showErrors) {
        try {
            double coordinate = Double.parseDouble(value);
            if (coordinate < minValue || coordinate > maxValue) {
                if (showErrors) {
                    editText.setError(getString(errorStringId));
                }
                return null;
            }

            return coordinate;
        } catch (NumberFormatException e) {
            if (showErrors) {
                editText.setError(getString(errorStringId));
            }
            return null;
        }
    }

    private void updateEditedLocationButtonState() {
        if (mode == Mode.VIEW) {
            return;
        }

        openEditedLocationMap.setEnabled(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("card", card);
        outState.putBoolean("dirty", dirty);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != PICK_LOCATION_REQUEST_CODE || resultCode != RESULT_OK) {
            return;
        }

        Pair<Double, Double> pickedLocation = OpenStreetMapActivity.getLocationResult(data);
        if (pickedLocation == null) {
            return;
        }

        locationLatEditorWatcher.ignoreNext();
        locationLngEditorWatcher.ignoreNext();
        locationLatEditor.setError(null);
        locationLngEditor.setError(null);
        locationLatEditor.setText(Double.toString(pickedLocation.first));
        locationLngEditor.setText(Double.toString(pickedLocation.second));
        dirty = true;
        updateEditedLocationButtonState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (firstResume) {
            notesEditor.addTextChangedListener(notesEditorDirtier);
            walrusCardView.editableNameView.addTextChangedListener(walrusCardViewNameDirtier);
            locationLatEditor.addTextChangedListener(locationLatEditorWatcher);
            locationLngEditor.addTextChangedListener(locationLngEditorWatcher);

            firstResume = false;
        }

        if (mode == Mode.VIEW) {
            Card updatedCard = getHelper().getCardDao().queryForId(card.id);
            if (updatedCard != null) {
                card = updatedCard;
                updateUI();
            }
        }
    }

    private void save() {
        card.name = walrusCardView.editableNameView.getText().toString();

        // Do not save a Card if the Name field is blank
        if (card.name.isEmpty()) {
            Toast.makeText(this, R.string.card_name_required, Toast.LENGTH_LONG).show();
            return;
        }

        // TODO: get acquire date (allow change)

        card.notes = notesEditor.getText().toString();
        Pair<Double, Double> editedLocation = parseEditedLocation(true);
        if (editedLocation == null) {
            return;
        }
        card.cardLocationLat = editedLocation.first;
        card.cardLocationLng = editedLocation.second;

        getHelper().getCardDao().createOrUpdate(card);
        LocalBroadcastManager.getInstance(this).sendBroadcast(
                new Intent(QueryUtils.ACTION_WALLET_UPDATE));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.editCard) {
            CardActivity.startActivity(this, Mode.EDIT, card, walrusCardView);
            return true;
        }
        if (itemId == R.id.duplicateCard) {
            Card duplicatedCard = Card.copyOf(card);
            duplicatedCard.name = getString(R.string.copy_of, duplicatedCard.name);
            CardActivity.startActivity(this, Mode.EDIT, duplicatedCard, null);
            return true;
        }
        if (itemId == R.id.deleteCard) {
            DeleteCardConfirmDialogFragment.create(0).show(getSupportFragmentManager(),
                    "delete_card_confirm_dialog");
            return true;
        }
        if (itemId == R.id.save) {
            save();
            supportFinishAfterTransition();
            return true;
        }
        if (itemId == R.id.start) {
            startReadCardDataSetup();
            return true;
        }
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDeleteCardConfirm(int callbackId) {
        getHelper().getCardDao().delete(card);
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(
                QueryUtils.ACTION_WALLET_UPDATE));

        finish();
    }

    public void onViewCardDataClick(View view) {
        if (card.cardData == null) {
            return;
        }

        CardData.Metadata cardDataMetadata = card.cardData.getClass().getAnnotation(
                CardData.Metadata.class);

        Class<? extends DialogFragment> viewDialogFragmentClass =
                cardDataMetadata.viewDialogFragmentClass();
        if (viewDialogFragmentClass == DialogFragment.class) {
            Toast.makeText(CardActivity.this, R.string.no_view_card_dialog,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        DialogFragment viewDialogFragment = instantiateDialogFragment(viewDialogFragmentClass);

        Bundle args = new Bundle();
        args.putString("title", getString(R.string.view_card_data_title, cardDataMetadata.name()));
        args.putSerializable("source_and_sink", card.cardData);
        args.putBoolean("editable", false);
        viewDialogFragment.setArguments(args);

        viewDialogFragment.show(getSupportFragmentManager(), "card_data_view_dialog");
    }

    public void onReadCardDataClick(View view) {
        startReadCardDataSetup();
    }

    private void startReadCardDataSetup() {
        PickCardDataTargetDialogFragment.create(null, CardDeviceAdapter.CardDataFilterMode.READABLE,
                mode != Mode.EDIT_BULK_READ_CARD_TEMPLATE, 0).show(
                getSupportFragmentManager(), PICK_CARD_DEVICE_DIALOG_FRAGMENT_TAG);
    }

    public void onWriteCardDataClick(View view) {
        startWriteOrEmulateCardSetup(true);
    }

    public void onEmulateCardDataClick(View view) {
        startWriteOrEmulateCardSetup(false);
    }

    private void startWriteOrEmulateCardSetup(boolean write) {
        if (card.cardData == null) {
            Toast.makeText(this, R.string.no_card_data, Toast.LENGTH_LONG).show();
            return;
        }

        if (CardDeviceManager.INSTANCE.getCardDevices().isEmpty()) {
            Toast.makeText(this, R.string.no_card_devices, Toast.LENGTH_LONG).show();
            return;
        }

        final List<CardDevice> cardDevices = new ArrayList<>();
        for (CardDevice cardDevice : CardDeviceManager.INSTANCE.getCardDevices().values()) {
            CardDevice.Metadata metadata = cardDevice.getClass().getAnnotation(
                    CardDevice.Metadata.class);
            if (ArrayUtils.contains(write ? metadata.supportsWrite() : metadata.supportsEmulate(),
                    card.cardData.getClass())) {
                cardDevices.add(cardDevice);
            }
        }

        if (cardDevices.isEmpty()) {
            Toast.makeText(this,
                    write ? R.string.no_device_can_write : R.string.no_device_can_emulate,
                    Toast.LENGTH_LONG).show();
            return;
        }

        PickCardDataTargetDialogFragment.create(
                card.cardData.getClass(),
                write ? CardDeviceAdapter.CardDataFilterMode.WRITABLE :
                        CardDeviceAdapter.CardDataFilterMode.EMULATABLE,
                false,
                write ? 1 : 2)
                .show(getSupportFragmentManager(), PICK_CARD_DEVICE_DIALOG_FRAGMENT_TAG);
    }

    private void dismissPickCardSourceDialogFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment pickCardDeviceDialogFragment = fragmentManager.findFragmentByTag(
                PICK_CARD_DEVICE_DIALOG_FRAGMENT_TAG);
        if (pickCardDeviceDialogFragment != null) {
            fragmentManager.beginTransaction()
                    .remove(pickCardDeviceDialogFragment)
                    .commit();
        }
    }

    @Override
    public void onManualEntryClick(int callbackId) {
        dismissPickCardSourceDialogFragment();

        Set<Class<? extends CardData>> cardDataClasses = new HashSet<>();
        for (Class<? extends CardData> cardDataClass : CardData.getCardDataClasses()) {
            if (cardDataClass.getAnnotation(CardData.Metadata.class).editDialogFragmentClass()
                    != DialogFragment.class) {
                cardDataClasses.add(cardDataClass);
            }
        }

        PickCardDataClassDialogFragment.create(cardDataClasses, -1).show(
                getSupportFragmentManager(), PICK_CARD_DATA_CLASS_DIALOG_FRAGMENT_TAG);
    }

    @Override
    public void onCardDeviceClick(final CardDevice cardDevice, int callbackId) {
        dismissPickCardSourceDialogFragment();

        switch (callbackId) {
            case 0:
                PickCardDataClassDialogFragment.create(
                        new HashSet<>(Arrays.asList(
                                cardDevice.getClass().getAnnotation(CardDevice.Metadata.class)
                                        .supportsRead())),
                        cardDevice.getId()).show(getSupportFragmentManager(),
                        PICK_CARD_DATA_CLASS_DIALOG_FRAGMENT_TAG);
                break;

            case 1:
            case 2:
                cardDevice.createWriteOrEmulateDataOperation(this, card.cardData, callbackId == 1,
                        callbackId);
                break;
        }
    }

    @Override
    public void onCardDataClassClick(final Class<? extends CardData> cardDataClass,
            int callbackId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment pickCardDataSourceDialogFragment = fragmentManager.findFragmentByTag(
                PICK_CARD_DATA_CLASS_DIALOG_FRAGMENT_TAG);
        if (pickCardDataSourceDialogFragment != null) {
            fragmentManager.beginTransaction()
                    .remove(pickCardDataSourceDialogFragment)
                    .commit();
        }

        if (callbackId == -1) {
            Class<? extends DialogFragment> editDialogFragmentClass =
                    cardDataClass.getAnnotation(CardData.Metadata.class).editDialogFragmentClass();
            DialogFragment editDialogFragment = instantiateDialogFragment(editDialogFragmentClass);

            boolean clean = card.cardData == null || card.cardData.getClass() != cardDataClass;

            CardData cardData;
            if (!clean) {
                try {
                    cardData = (CardData) card.cardData.clone();
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                cardData = instantiateCardData(cardDataClass);
            }

            CardData.Metadata cardDataMetadata = cardData.getClass().getAnnotation(
                    CardData.Metadata.class);

            Bundle args = new Bundle();
            args.putString("title", getString(R.string.edit_card_data_title,
                    cardDataMetadata.name()));
            args.putSerializable("source_and_sink", cardData);
            args.putBoolean("clean", clean);
            args.putBoolean("editable", true);
            args.putInt("callback_id", 0);
            editDialogFragment.setArguments(args);

            editDialogFragment.show(fragmentManager, "card_data_edit_dialog");
        } else {
            CardDevice cardDevice = CardDeviceManager.INSTANCE.getCardDevices().get(callbackId);
            if (cardDevice == null) {
                return;
            }

            cardDevice.createReadCardDataOperation(this, cardDataClass, 0);
        }
    }

    @Override
    @UiThread
    public void onOperationCreated(CardDeviceOperation operation, int callbackId) {
        switch (callbackId) {
            case 0: {
                ReadCardDataOperation readCardDataOperation = (ReadCardDataOperation) operation;

                if (mode != Mode.EDIT_BULK_READ_CARD_TEMPLATE) {
                    getSupportFragmentManager().beginTransaction()
                            .add(ReadCardDataOperationFragment.create(readCardDataOperation, 0),
                                    "card_data_io")
                            .commit();
                } else {
                    BulkReadCardsService.startService(this, readCardDataOperation, card);
                    supportFinishAfterTransition();
                }
                break;
            }

            case 1:
            case 2:
                getSupportFragmentManager().beginTransaction()
                        .add(WriteOrEmulateCardDataOperationFragment.create(
                                (WriteOrEmulateCardDataOperation) operation, 0), "card_data_io")
                        .commit();
                break;
        }
    }

    @Override
    public void onResult(CardData cardData, int callbackId) {
        if (cardData.equals(card.cardData)) {
            return;
        }

        card.setCardData(cardData, WalrusApplication.getCurrentBestLocation());
        dirty = true;
        updateUI();
    }

    @Override
    public void onEdited(ComponentSourceAndSink componentSourceAndSink, int callbackId) {
        onResult((CardData) componentSourceAndSink, callbackId);
    }

    private DialogFragment instantiateDialogFragment(
            Class<? extends DialogFragment> dialogFragmentClass) {
        if (dialogFragmentClass == ComponentDialogFragment.class) {
            return new ComponentDialogFragment();
        }

        try {
            return dialogFragmentClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private CardData instantiateCardData(Class<? extends CardData> cardDataClass) {
        if (cardDataClass == HIDCardData.class) {
            return new HIDCardData();
        }
        if (cardDataClass == MifareCardData.class) {
            return new MifareCardData();
        }

        try {
            return cardDataClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        if (mode != Mode.VIEW && dirty) {
            AlertDialog dialog = new AlertDialog.Builder(this).setMessage(mode == Mode.EDIT
                    ? R.string.discard_card_changes : R.string.discard_bulk_read_changes)
                    .setCancelable(true)
                    .setPositiveButton(R.string.discard_button,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    finish();
                                    dialog.dismiss();
                                }
                            })
                    .setNegativeButton(R.string.back_button,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    dialog.dismiss();
                                }
                            })
                    .show();
            AppFontManager.applyToDialog(dialog);
        } else {
            supportFinishAfterTransition();
        }
    }

    public enum Mode {
        VIEW,
        EDIT,
        EDIT_BULK_READ_CARD_TEMPLATE
    }

    private class TextChangeDirtier extends UIUtils.TextChangeWatcher {

        TextChangeDirtier() {
            ignoreNext();
        }

        @Override
        public void onNotIgnoredTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            dirty = true;
        }
    }

    private class LocationEditorChangeWatcher extends UIUtils.TextChangeWatcher {

        @Override
        public void onNotIgnoredTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            dirty = true;
            locationLatEditor.setError(null);
            locationLngEditor.setError(null);
            updateEditedLocationButtonState();
        }
    }
}
