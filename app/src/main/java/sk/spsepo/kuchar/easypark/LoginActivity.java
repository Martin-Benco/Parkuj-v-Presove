package sk.spsepo.kuchar.easypark;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText inputSpz, inputPhone;
    private Button btnLogin;
    private static final String DATA_FILE = "users.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputSpz = findViewById(R.id.input_spz);
        inputPhone = findViewById(R.id.input_phone);
        btnLogin = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> {
            String spz = inputSpz.getText().toString().trim().toUpperCase();
            String phone = inputPhone.getText().toString().trim();

            if (!isValidSpz(spz) || !isValidPhone(phone)) {
                Toast.makeText(
                        LoginActivity.this,
                        "Nezadal si správne údaje, skús to znovu.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("spz", spz);
            editor.putString("phone", phone);
            editor.apply();

            updateDataFile(spz + "," + phone);

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private boolean isValidSpz(String spz) {
        return spz.matches("[A-Z]{2}\\d{3}[A-Z]{2}");
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("\\+421\\d{9}") || phone.matches("0\\d{9}");
    }

    private void updateDataFile(String entry) {
        try {
            File dir = getExternalFilesDir(null);
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, DATA_FILE);

            Map<String, Integer> counts = new HashMap<>();

            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length < 3) continue;
                    String key = parts[0] + "," + parts[1];
                    int cnt = Integer.parseInt(parts[2]);
                    counts.put(key, cnt);
                }
                reader.close();
            }

            int newCount = counts.getOrDefault(entry, 0) + 1;
            counts.put(entry, newCount);

            FileOutputStream fos = new FileOutputStream(file, false);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                writer.write(e.getKey() + "," + e.getValue());
                writer.newLine();
            }
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
