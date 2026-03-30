package dev.namelessnanashi.walrus.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import dev.namelessnanashi.walrus.R;
import dev.namelessnanashi.walrus.WalrusApplication;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.FillExtrusionLayer;
import org.maplibre.android.style.layers.FillLayer;
import org.maplibre.android.style.layers.Layer;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.Property;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Point;

import java.util.List;
import java.util.Locale;

public class OpenStreetMapActivity extends AppCompatActivity {

    private static final String EXTRA_EDITABLE =
            "dev.namelessnanashi.walrus.ui.OpenStreetMapActivity.EXTRA_EDITABLE";
    private static final String EXTRA_LATITUDE =
            "dev.namelessnanashi.walrus.ui.OpenStreetMapActivity.EXTRA_LATITUDE";
    private static final String EXTRA_LONGITUDE =
            "dev.namelessnanashi.walrus.ui.OpenStreetMapActivity.EXTRA_LONGITUDE";
    private static final String STATE_MARKER_LATITUDE = "marker_latitude";
    private static final String STATE_MARKER_LONGITUDE = "marker_longitude";
    private static final String MAP_STYLE_URI = "https://tiles.openfreemap.org/styles/dark";
    private static final String MARKER_SOURCE_ID = "selected-location-source";
    private static final String MARKER_LAYER_ID = "selected-location-layer";
    private static final String MARKER_IMAGE_ID = "selected-location-pin";
    private static final double DEFAULT_ZOOM = 16d;
    private static final double DEFAULT_EMPTY_ZOOM = 3d;
    private static final double DEFAULT_CURRENT_LOCATION_ZOOM = 15d;

    private MapView mapView;
    private TextView mapInstructions;
    private TextView mapCoordinates;
    private Button useMapLocation;

    private boolean editable;
    @Nullable
    private MapLibreMap mapLibreMap;
    @Nullable
    private GeoJsonSource markerSource;
    @Nullable
    private LatLng selectedLocation;

    public static Intent createIntent(Context context, @Nullable Double latitude,
            @Nullable Double longitude, boolean editable) {
        Intent intent = new Intent(context, OpenStreetMapActivity.class);
        intent.putExtra(EXTRA_EDITABLE, editable);
        if (latitude != null) {
            intent.putExtra(EXTRA_LATITUDE, latitude);
        }
        if (longitude != null) {
            intent.putExtra(EXTRA_LONGITUDE, longitude);
        }
        return intent;
    }

    @Nullable
    public static Pair<Double, Double> getLocationResult(@Nullable Intent intent) {
        if (intent == null
                || !intent.hasExtra(EXTRA_LATITUDE)
                || !intent.hasExtra(EXTRA_LONGITUDE)) {
            return null;
        }

        return new Pair<>(intent.getDoubleExtra(EXTRA_LATITUDE, 0d),
                intent.getDoubleExtra(EXTRA_LONGITUDE, 0d));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapLibre.getInstance(this);

        setContentView(R.layout.activity_open_street_map);
        setTitle(R.string.openstreetmap_activity_name);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        editable = getIntent().getBooleanExtra(EXTRA_EDITABLE, false);

        mapView = findViewById(R.id.map);
        mapInstructions = findViewById(R.id.mapInstructions);
        mapCoordinates = findViewById(R.id.mapCoordinates);
        useMapLocation = findViewById(R.id.useMapLocation);

        mapInstructions.setVisibility(editable ? View.VISIBLE : View.GONE);
        useMapLocation.setVisibility(editable ? View.VISIBLE : View.GONE);

        selectedLocation = resolveInitialLocation(savedInstanceState);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapLibreMap map) {
                mapLibreMap = map;
                configureUi(map);
                map.setStyle(new Style.Builder().fromUri(MAP_STYLE_URI),
                        new Style.OnStyleLoaded() {
                            @Override
                            public void onStyleLoaded(Style style) {
                                enhanceMapContrast(style);
                                initializeMarkerLayer(style);
                                centerMap(selectedLocation);
                                updateSelectedLocationState();
                                bindMapInteractions();
                            }
                        });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
        if (selectedLocation == null) {
            return;
        }

        outState.putDouble(STATE_MARKER_LATITUDE, selectedLocation.getLatitude());
        outState.putDouble(STATE_MARKER_LONGITUDE, selectedLocation.getLongitude());
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onUseLocationClick(View view) {
        if (selectedLocation == null) {
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_LATITUDE, selectedLocation.getLatitude());
        intent.putExtra(EXTRA_LONGITUDE, selectedLocation.getLongitude());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void configureUi(MapLibreMap map) {
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);
        map.getUiSettings().setLogoEnabled(false);
        map.getUiSettings().setAttributionEnabled(false);
    }

    @Nullable
    private LatLng resolveInitialLocation(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_MARKER_LATITUDE)
                && savedInstanceState.containsKey(STATE_MARKER_LONGITUDE)) {
            return new LatLng(savedInstanceState.getDouble(STATE_MARKER_LATITUDE),
                    savedInstanceState.getDouble(STATE_MARKER_LONGITUDE));
        }

        if (getIntent().hasExtra(EXTRA_LATITUDE) && getIntent().hasExtra(EXTRA_LONGITUDE)) {
            return new LatLng(getIntent().getDoubleExtra(EXTRA_LATITUDE, 0d),
                    getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0d));
        }

        return null;
    }

    private void initializeMarkerLayer(Style style) {
        style.addImage(MARKER_IMAGE_ID, drawableToBitmap(R.drawable.drawable_map_pin_modern));

        markerSource = new GeoJsonSource(MARKER_SOURCE_ID,
                FeatureCollection.fromFeatures(new Feature[]{}));
        style.addSource(markerSource);

        SymbolLayer markerLayer = new SymbolLayer(MARKER_LAYER_ID, MARKER_SOURCE_ID)
                .withProperties(
                        PropertyFactory.iconImage(MARKER_IMAGE_ID),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM)
                );
        style.addLayer(markerLayer);

        updateMarkerSource();
    }

    private void enhanceMapContrast(Style style) {
        for (Layer layer : style.getLayers()) {
            String layerId = layer.getId();
            if (layerId == null) {
                continue;
            }

            String normalizedLayerId = layerId.toLowerCase(Locale.US);

            if (layer instanceof SymbolLayer) {
                enhanceSymbolLayer((SymbolLayer) layer, normalizedLayerId);
            }

            if (normalizedLayerId.contains("road")
                    || normalizedLayerId.contains("street")
                    || normalizedLayerId.contains("highway")
                    || normalizedLayerId.contains("path")
                    || normalizedLayerId.contains("bridge")
                    || normalizedLayerId.contains("tunnel")) {
                enhanceRoadLayer(layer);
            }

            if (normalizedLayerId.contains("rail")
                    || normalizedLayerId.contains("train")
                    || normalizedLayerId.contains("subway")
                    || normalizedLayerId.contains("tram")) {
                enhanceRailLayer(layer);
            }

            if (normalizedLayerId.contains("water")
                    || normalizedLayerId.contains("river")
                    || normalizedLayerId.contains("stream")
                    || normalizedLayerId.contains("lake")
                    || normalizedLayerId.contains("canal")) {
                enhanceWaterLayer(layer);
            }

            if (!normalizedLayerId.contains("building")) {
                continue;
            }

            if (layer instanceof FillExtrusionLayer) {
                ((FillExtrusionLayer) layer).setProperties(
                        PropertyFactory.fillExtrusionColor(Color.parseColor("#5F6978")),
                        PropertyFactory.fillExtrusionOpacity(0.96f),
                        PropertyFactory.fillExtrusionVerticalGradient(true)
                );
            } else if (layer instanceof FillLayer) {
                ((FillLayer) layer).setProperties(
                        PropertyFactory.fillColor(Color.parseColor("#586273")),
                        PropertyFactory.fillOutlineColor(Color.parseColor("#B2BCC9")),
                        PropertyFactory.fillOpacity(0.9f)
                );
            } else if (layer instanceof LineLayer) {
                ((LineLayer) layer).setProperties(
                        PropertyFactory.lineColor(Color.parseColor("#C3CDD9")),
                        PropertyFactory.lineOpacity(0.82f),
                        PropertyFactory.lineWidth(1.2f)
                );
            }
        }
    }

    private void enhanceRoadLayer(Layer layer) {
        if (!(layer instanceof LineLayer)) {
            return;
        }

        ((LineLayer) layer).setProperties(
                PropertyFactory.lineColor(Color.parseColor("#D7DEE8")),
                PropertyFactory.lineOpacity(0.88f),
                PropertyFactory.lineWidth(1.35f)
        );
    }

    private void enhanceRailLayer(Layer layer) {
        if (layer instanceof LineLayer) {
            ((LineLayer) layer).setProperties(
                    PropertyFactory.lineColor(Color.parseColor("#C9D1DA")),
                    PropertyFactory.lineOpacity(0.84f),
                    PropertyFactory.lineWidth(1.25f)
            );
        } else if (layer instanceof SymbolLayer) {
            ((SymbolLayer) layer).setProperties(
                    PropertyFactory.textColor(Color.parseColor("#E2E8EF")),
                    PropertyFactory.textOpacity(0.98f),
                    PropertyFactory.textHaloColor(Color.parseColor("#11161D")),
                    PropertyFactory.textHaloWidth(1.0f),
                    PropertyFactory.iconColor(Color.parseColor("#D8E0E8")),
                    PropertyFactory.iconOpacity(0.96f),
                    PropertyFactory.iconHaloColor(Color.parseColor("#11161D")),
                    PropertyFactory.iconHaloWidth(0.6f)
            );
        }
    }

    private void enhanceWaterLayer(Layer layer) {
        if (layer instanceof FillLayer) {
            ((FillLayer) layer).setProperties(
                    PropertyFactory.fillColor(Color.parseColor("#466987")),
                    PropertyFactory.fillOutlineColor(Color.parseColor("#8AB2D3")),
                    PropertyFactory.fillOpacity(0.92f)
            );
        } else if (layer instanceof LineLayer) {
            ((LineLayer) layer).setProperties(
                    PropertyFactory.lineColor(Color.parseColor("#8FBEDF")),
                    PropertyFactory.lineOpacity(0.9f),
                    PropertyFactory.lineWidth(1.2f)
            );
        } else if (layer instanceof SymbolLayer) {
            ((SymbolLayer) layer).setProperties(
                    PropertyFactory.textColor(Color.parseColor("#CAE4F7")),
                    PropertyFactory.textOpacity(0.98f),
                    PropertyFactory.textHaloColor(Color.parseColor("#12202B")),
                    PropertyFactory.textHaloWidth(1.0f),
                    PropertyFactory.iconColor(Color.parseColor("#B8D7EF")),
                    PropertyFactory.iconOpacity(0.96f),
                    PropertyFactory.iconHaloColor(Color.parseColor("#12202B")),
                    PropertyFactory.iconHaloWidth(0.5f)
            );
        }
    }

    private void enhanceSymbolLayer(SymbolLayer layer, String normalizedLayerId) {
        if (normalizedLayerId.contains("poi")
                || normalizedLayerId.contains("place")
                || normalizedLayerId.contains("label")
                || normalizedLayerId.contains("name")
                || normalizedLayerId.contains("symbol")
                || normalizedLayerId.contains("icon")
                || normalizedLayerId.contains("direction")
                || normalizedLayerId.contains("transport")
                || normalizedLayerId.contains("station")) {
            layer.setProperties(
                    PropertyFactory.textColor(Color.parseColor("#F0F4F8")),
                    PropertyFactory.textOpacity(0.99f),
                    PropertyFactory.textHaloColor(Color.parseColor("#11161D")),
                    PropertyFactory.textHaloWidth(1.05f),
                    PropertyFactory.iconColor(Color.parseColor("#E1E7EE")),
                    PropertyFactory.iconOpacity(0.97f),
                    PropertyFactory.iconHaloColor(Color.parseColor("#11161D")),
                    PropertyFactory.iconHaloWidth(0.55f)
            );
        }
    }

    private void bindMapInteractions() {
        if (mapLibreMap == null || !editable) {
            return;
        }

        mapLibreMap.addOnMapClickListener(new MapLibreMap.OnMapClickListener() {
            @Override
            public boolean onMapClick(LatLng point) {
                if (isExistingMarkerTap(point)) {
                    return true;
                }

                selectedLocation = point;
                updateMarkerSource();
                updateSelectedLocationState();
                return true;
            }
        });
    }

    private boolean isExistingMarkerTap(LatLng point) {
        if (mapLibreMap == null || selectedLocation == null) {
            return false;
        }

        PointF screenPoint = mapLibreMap.getProjection().toScreenLocation(point);
        List<Feature> features = mapLibreMap.queryRenderedFeatures(screenPoint, MARKER_LAYER_ID);
        return !features.isEmpty();
    }

    private void centerMap(@Nullable LatLng point) {
        LatLng mapCenter = point;
        double zoom = DEFAULT_ZOOM;

        if (mapCenter == null) {
            Location currentBestLocation = WalrusApplication.getCurrentBestLocation();
            if (currentBestLocation != null) {
                mapCenter = new LatLng(currentBestLocation.getLatitude(),
                        currentBestLocation.getLongitude());
                zoom = DEFAULT_CURRENT_LOCATION_ZOOM;
            } else {
                mapCenter = new LatLng(0d, 0d);
                zoom = DEFAULT_EMPTY_ZOOM;
            }
        }

        if (mapLibreMap == null) {
            return;
        }

        mapLibreMap.setCameraPosition(new CameraPosition.Builder()
                .target(mapCenter)
                .zoom(zoom)
                .build());
    }

    private void updateMarkerSource() {
        if (markerSource == null) {
            return;
        }

        if (selectedLocation == null) {
            markerSource.setGeoJson(FeatureCollection.fromFeatures(new Feature[]{}));
            return;
        }

        markerSource.setGeoJson(Feature.fromGeometry(Point.fromLngLat(
                selectedLocation.getLongitude(), selectedLocation.getLatitude())));
    }

    private void updateSelectedLocationState() {
        if (selectedLocation == null) {
            mapCoordinates.setText(R.string.no_location_selected);
            useMapLocation.setEnabled(false);
            return;
        }

        mapCoordinates.setText(getString(R.string.location_coordinates,
                selectedLocation.getLatitude(), selectedLocation.getLongitude()));
        useMapLocation.setEnabled(true);
    }

    private Bitmap drawableToBitmap(int drawableId) {
        Drawable drawable = AppCompatResources.getDrawable(this, drawableId);
        if (drawable == null) {
            throw new IllegalStateException("Missing map marker drawable");
        }

        int width = Math.max(drawable.getIntrinsicWidth(), 1);
        int height = Math.max(drawable.getIntrinsicHeight(), 1);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
