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

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;

import dev.namelessnanashi.walrus.R;
import dev.namelessnanashi.walrus.WalrusApplication;
import dev.namelessnanashi.walrus.card.Card;
import dev.namelessnanashi.walrus.card.DatabaseHelper;
import dev.namelessnanashi.walrus.card.OrmLiteBaseAppCompatActivity;
import dev.namelessnanashi.walrus.card.QueryUtils;
import dev.namelessnanashi.walrus.device.ui.BulkReadCardsActivity;
import dev.namelessnanashi.walrus.device.ui.DevicesActivity;
import dev.namelessnanashi.walrus.ui.SettingsActivity;

import java.util.List;

import io.github.yavski.fabspeeddial.FabSpeedDial;
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class WalletActivity extends OrmLiteBaseAppCompatActivity<DatabaseHelper> {

    private static final int LOCATION_REQUEST_CODE = 0;

    private RecyclerView recyclerView;
    private final BroadcastReceiver walletUpdateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    };

    private SearchView sv;

    private boolean isFirstRun = true;
    private SharedPreferences mSharedPreferences;

    public WalletActivity() {
        super(DatabaseHelper.class);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.App_WithTransitions);
        super.onCreate(savedInstanceState);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        isFirstRun = mSharedPreferences.getBoolean("isFirstRun", true);

        if(isFirstRun){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.update_notification_title);
            builder.setMessage(R.string.update_notification_message);
            builder.setNeutralButton(R.string.update_notification_neutral_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    isFirstRun = false;
                    mSharedPreferences.edit().putBoolean("isFirstRun", isFirstRun).apply();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();

            TextView messageView = dialog.findViewById(android.R.id.message);
            if (messageView != null) {
                Linkify.addLinks(messageView, Linkify.WEB_URLS);
                messageView.setMovementMethod(LinkMovementMethod.getInstance());
                messageView.setLinkTextColor(ContextCompat.getColor(this, R.color.secondaryLightColor));
            }
        }

        setContentView(R.layout.activity_wallet);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        sv = findViewById(R.id.searchView);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (recyclerView.getAdapter() != null) {
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
                return false;
            }
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setVisibility(View.INVISIBLE);
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        int span = (int) (recyclerView.getWidth() / (WalrusCardView.getMaxSize(
                                getResources().getDisplayMetrics()).first * 0.8));

                        recyclerView.setLayoutManager(span > 1
                                ? new GridLayoutManager(WalletActivity.this, span)
                                : new LinearLayoutManager(WalletActivity.this));
                        recyclerView.setHasFixedSize(true);
                        recyclerView.setAdapter(new CardAdapter());
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });

        FabSpeedDial fabSpeedDial = findViewById(R.id.floatingActionButton);
        fabSpeedDial.setMenuListener(new SimpleMenuListenerAdapter() {
            @Override
            public boolean onMenuItemSelected(MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.add_new_card) {
                    CardActivity.startActivity(WalletActivity.this,
                            CardActivity.Mode.EDIT, null, null);
                    return true;
                }
                if (itemId == R.id.bulk_read_cards) {
                    CardActivity.startActivity(WalletActivity.this,
                            CardActivity.Mode.EDIT_BULK_READ_CARD_TEMPLATE, null, null);
                    return true;
                }

                return false;
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(walletUpdateBroadcastReceiver,
                new IntentFilter(QueryUtils.ACTION_WALLET_UPDATE));

        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            gotLocationPermissions();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.location_rationale),
                    LOCATION_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(LOCATION_REQUEST_CODE)
    private void gotLocationPermissions() {
        WalrusApplication.startLocationUpdates();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_wallet, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.search) {
            sv.setVisibility(View.VISIBLE);
            sv.requestFocus();
            return true;
        }
        if (itemId == R.id.bulk_read_cards) {
            startActivity(new Intent(this, BulkReadCardsActivity.class));
            return true;
        }
        if (itemId == R.id.devices) {
            startActivity(new Intent(this, DevicesActivity.class));
            return true;
        }
        if (itemId == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (sv.getVisibility() != View.GONE) {
            sv.setIconified(true);
            sv.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(walletUpdateBroadcastReceiver);
    }

    private class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout frameLayout = new FrameLayout(parent.getContext());
            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            View walrusCardView = new WalrusCardView(parent.getContext());
            walrusCardView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_HORIZONTAL));

            frameLayout.addView(walrusCardView);

            return new ViewHolder(frameLayout);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Card card;
            String filter = sv.getQuery().toString();
            if (!filter.isEmpty()) {
                List<Card> cards = QueryUtils.filterCards(getHelper().getCardDao(), filter);
                card = cards.get(position);
            } else {
                card = QueryUtils.getNthRow(getHelper().getCardDao(), position);
            }

            ((WalrusCardView) ((FrameLayout) holder.itemView).getChildAt(0)).setCard(card);
            holder.id = card.id;
            ViewCompat.setTransitionName(holder.itemView, "card-" + card.id);
        }

        @Override
        public int getItemCount() {
            String filter = sv.getQuery().toString();
            if (!filter.isEmpty()) {
                List<Card> cards = QueryUtils.filterCards(getHelper().getCardDao(), filter);
                return cards.size();
            } else {
                return (int) getHelper().getCardDao().countOf();
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            private int id;

            ViewHolder(View itemView) {
                super(itemView);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CardActivity.startActivity(WalletActivity.this, CardActivity.Mode.VIEW,
                                getHelper().getCardDao().queryForId(id),
                                ((FrameLayout) v).getChildAt(0));
                    }
                });
            }
        }
    }
}
