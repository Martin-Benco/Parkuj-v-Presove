package sk.spsepo.kuchar.easypark;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.geojson.*;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        String spz = prefs.getString("spz", "");
        String phone = prefs.getString("phone", "");
        if (spz.isEmpty() || phone.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng presov = new LatLng(49.0038, 21.2396);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(presov, 14));

        try {
            boolean ok = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            );
            if (!ok) {
                Toast.makeText(this, "Map style parsing failed.", Toast.LENGTH_SHORT).show();
            }
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }

        try {
            GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.parking, this);
            layer.setOnFeatureClickListener(this::showParkingBottomSheet);

            for (GeoJsonFeature f : layer.getFeatures()) {
                if (f.hasGeometry() &&
                        "Polygon".equals(f.getGeometry().getGeometryType())) {

                    GeoJsonPolygonStyle s = new GeoJsonPolygonStyle();
                    s.setStrokeWidth(4);
                    String z = f.getProperty("Zone");
                    if (z != null) {
                        if ("A".equalsIgnoreCase(z)) {
                            s.setStrokeColor(Color.RED);
                            s.setFillColor(Color.argb(100,255,0,0));
                        } else if (z.startsWith("R")) {
                            s.setStrokeColor(Color.rgb(102,51,0));
                            s.setFillColor(Color.argb(100,102,51,0));
                        } else {
                            s.setStrokeColor(Color.GREEN);
                            s.setFillColor(Color.argb(100,0,255,0));
                        }
                    } else {
                        s.setStrokeColor(Color.GRAY);
                        s.setFillColor(Color.argb(100,128,128,128));
                    }
                    f.setPolygonStyle(s);
                }
            }
            layer.addLayerToMap();

        } catch (IOException | JSONException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Chyba pri načítaní parkovacích zón", Toast.LENGTH_LONG).show();
        }
    }

    private void showParkingBottomSheet(Feature feature) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_parking, null);
        BottomSheetDialog dlg = new BottomSheetDialog(this);
        dlg.setContentView(view);

        TextView tvName     = view.findViewById(R.id.parking_name);
        TextView tvZone     = view.findViewById(R.id.parking_zone);
        TextView tvPrice    = view.findViewById(R.id.parking_price);
        Spinner spHours     = view.findViewById(R.id.spinner_hours);
        Button btnSMS       = view.findViewById(R.id.btn_message);
        Button btnNavigate  = view.findViewById(R.id.btn_maps);

        String name    = feature.getProperty("Name");
        String rawZone = feature.getProperty("Zone");
        String zoneKey = rawZone != null ? rawZone : "";

        tvName.setText(name != null ? name : "Neznáme miesto");
        tvZone.setText(zoneKey.isEmpty() ? "Zóna: neznáma" : "Zóna: " + zoneKey);

        btnNavigate.setEnabled(false);
        btnNavigate.setOnClickListener(v -> {
            GeoJsonPolygon poly = (GeoJsonPolygon) feature.getGeometry();
            LatLng dest = poly.getCoordinates().get(0).get(0);
            String uri = "google.navigation:q="
                    + dest.latitude + "," + dest.longitude;
            Intent nav = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            nav.setPackage("com.google.android.apps.maps");
            startActivity(nav);
        });

        try {
            InputStream is = getResources().openRawResource(R.raw.parking_prices);
            byte[] buf = new byte[is.available()];
            is.read(buf); is.close();
            JSONObject prices = new JSONObject(new String(buf, "UTF-8"));

            if (!zoneKey.isEmpty() && prices.has(zoneKey)) {
                JSONObject z    = prices.getJSONObject(zoneKey);
                String sadzba   = z.getString("sadzba");
                String maxStr   = z.getString("max");
                tvPrice.setText("Sadzba: " + sadzba + "\nDenná max: " + maxStr);

                double maxPrice = parsePrice(maxStr);

                List<Double> rates = new ArrayList<>();
                switch (zoneKey) {
                    case "A":
                        rates.add(1.00);
                        rates.add(1.50);
                        rates.add(2.00);
                        break;
                    case "D":
                        rates.add(0.20);
                        rates.add(0.40);
                        rates.add(1.00);
                        break;
                    default:
                        rates.add(parsePrice(sadzba));
                        break;
                }

                double cum = 0;
                int   h   = 0;
                List<Integer> options = new ArrayList<>();
                while (true) {
                    double rate = (h < rates.size() ? rates.get(h) : rates.get(rates.size()-1));
                    if (cum + rate > maxPrice) break;
                    cum += rate;
                    h++;
                    options.add(h);
                }

                if (!options.isEmpty()) {
                    spHours.setVisibility(View.VISIBLE);
                    spHours.setEnabled(true);
                    btnSMS.setEnabled(true);
                    btnNavigate.setEnabled(true);

                    ArrayAdapter<Integer> ad = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, options
                    );
                    ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spHours.setAdapter(ad);

                    btnSMS.setOnClickListener(v -> {
                        int hrs = (int) spHours.getSelectedItem();
                        sendSMS(generateSMS(zoneKey, hrs));
                    });

                } else {
                    spHours.setVisibility(View.GONE);
                    btnSMS.setEnabled(true);
                    btnNavigate.setEnabled(true);
                    btnSMS.setOnClickListener(v ->
                            sendSMS(generateSMS(zoneKey, -1))
                    );
                    tvPrice.append(
                            "\n\nVýber hodín nie je dostupný.\n" +
                                    "Manuálne doplňte počet hodín na konci SMS."
                    );
                }

            } else {
                tvPrice.setText("Cenník nie je dostupný.");
                spHours.setVisibility(View.GONE);
                btnSMS.setEnabled(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            tvPrice.setText("Chyba pri načítaní cenníka.");
        }

        dlg.show();
    }

    private void sendSMS(String sms) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:2100"));
        intent.putExtra("sms_body", sms);
        startActivity(intent);
    }

    private String generateSMS(String zone, int hours) {
        SharedPreferences p = getSharedPreferences("user_data", MODE_PRIVATE);
        String spz = p.getString("spz", "NEZADANA");
        return hours > 0
                ? zone + " " + spz + " " + hours
                : zone + " " + spz;
    }

    private double parsePrice(String txt) {
        try {
            txt = txt.replace(",", ".").replaceAll("[^\\d.]", "");
            return Double.parseDouble(txt);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
