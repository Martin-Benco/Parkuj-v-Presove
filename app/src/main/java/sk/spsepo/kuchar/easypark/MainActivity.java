package sk.spsepo.kuchar.easypark;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.Resources;
import java.io.InputStream;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONException;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.geojson.*;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng presov = new LatLng(49.0038, 21.2396);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(presov, 14));

        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);
        uiSettings.setScrollGesturesEnabled(true);
        uiSettings.setTiltGesturesEnabled(true);
        uiSettings.setRotateGesturesEnabled(true);

        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.map_style));
            if (!success) {
                Toast.makeText(this, "Map style parsing failed.", Toast.LENGTH_SHORT).show();
            }
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }

        try {
            GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.parking, getApplicationContext());

            layer.setOnFeatureClickListener(this::showParkingBottomSheet);

            for (GeoJsonFeature feature : layer.getFeatures()) {
                if (feature.hasGeometry() && feature.getGeometry().getGeometryType().equals("Polygon")) {
                    GeoJsonPolygonStyle polygonStyle = new GeoJsonPolygonStyle();
                    polygonStyle.setStrokeWidth(4);

                    String zone = feature.getProperty("Zone");
                    if (zone != null) {
                        if (zone.equalsIgnoreCase("A")) {
                            // Červená
                            polygonStyle.setStrokeColor(Color.RED);
                            polygonStyle.setFillColor(Color.argb(100, 255, 0, 0));
                        } else if (zone.startsWith("R")) {
                            // Hnedá
                            polygonStyle.setStrokeColor(Color.rgb(102, 51, 0));
                            polygonStyle.setFillColor(Color.argb(100, 102, 51, 0));
                        } else {
                            // Zelená
                            polygonStyle.setStrokeColor(Color.GREEN);
                            polygonStyle.setFillColor(Color.argb(100, 0, 255, 0));
                        }
                    } else {
                        // Sivá pre neznáme zóny
                        polygonStyle.setStrokeColor(Color.GRAY);
                        polygonStyle.setFillColor(Color.argb(100, 128, 128, 128));
                    }

                    feature.setPolygonStyle(polygonStyle);
                }
            }

            layer.addLayerToMap();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Chyba pri načítaní GeoJSON vrstvy", Toast.LENGTH_LONG).show();
        }
    }

    private void showParkingBottomSheet(Feature feature) {
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_parking, null);

        TextView nameText = sheetView.findViewById(R.id.parking_name);
        TextView zoneText = sheetView.findViewById(R.id.parking_zone);
        TextView priceText = sheetView.findViewById(R.id.parking_price);
        Button btnMaps = sheetView.findViewById(R.id.btn_maps);
        Button btnMessage = sheetView.findViewById(R.id.btn_message);

        String name = feature.getProperty("Name");
        String zone = feature.getProperty("Zone");

        nameText.setText(name != null ? name : "Neznámy názov");
        zoneText.setText(zone != null ? "Zóna: " + zone : "Zóna: neznáma");

        try {
            InputStream is = getResources().openRawResource(R.raw.parking_prices);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            JSONObject prices = new JSONObject(json);
            if (zone != null && prices.has(zone)) {
                JSONObject z = prices.getJSONObject(zone);
                String sadzba = z.getString("sadzba");
                String max = z.getString("max");

                priceText.setText("Sadzba: " + sadzba + "\nDenná maximálna sadzba: " + max);
            } else {
                priceText.setText("Cenník pre túto zónu nie je dostupný.");
            }

        } catch (IOException | JSONException e) {
            priceText.setText("Nepodarilo sa načítať cenník.");
            e.printStackTrace();
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }
}
