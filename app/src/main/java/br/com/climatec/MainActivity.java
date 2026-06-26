package br.com.climatec;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import br.com.climatec.api.APIService;
import br.com.climatec.api.HttpResponse;
import br.com.climatec.api.RetrofitClient;
import br.com.climatec.models.WeatherCurrent;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvCity;
    private TextView tvTemperature;
    private TextView tvHumidity;
    private TextView tvWindSpeed;
    private Button btnRefresh;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private final String CURRENT_PARAMS = "temperature_2m,relative_humidity_2m,wind_speed_10m";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Vinculando as Views
        tvCity = findViewById(R.id.tvCity);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWindSpeed = findViewById(R.id.tvWindSpeed);
        btnRefresh = findViewById(R.id.btnRefresh);

        // Inicializando o cliente de localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Ao abrir o app, tenta pegar a localização
        checkLocationPermissionAndFetch();

        // Botão de atualizar também pega a localização novamente
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationPermissionAndFetch();
            }
        });
    }

    // 1. Verifica Permissões
    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Solicita a permissão se ainda não foi concedida
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permissão já concedida, busca a localização
            getLastKnownLocation();
        }
    }

    // 2. Trata a resposta do usuário (Permitiu ou Negou?)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastKnownLocation();
            } else {
                Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show();
                tvCity.setText("Localização indisponível");
            }
        }
    }

    // 3. Pega as Coordenadas do GPS
    private void getLastKnownLocation() {
        // Verifica novamente a permissão por exigência do Android Studio
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        tvCity.setText("Obtendo localização...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    // Descobre o nome da cidade
                    getCityName(latitude, longitude);

                    // Busca o clima com as coordenadas dinâmicas
                    fetchWeatherData(latitude, longitude);
                } else {
                    tvCity.setText("Ligue o GPS do aparelho.");
                }
            }
        });
    }

    // 4. Converte Latitude/Longitude em Nome da Cidade (Geocoder)
    private void getCityName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String cityName = addresses.get(0).getSubAdminArea(); // Pega o nome da cidade
                if (cityName == null) {
                    cityName = addresses.get(0).getLocality();
                }
                tvCity.setText(cityName);
            } else {
                tvCity.setText("Cidade não encontrada");
            }
        } catch (IOException e) {
            e.printStackTrace();
            tvCity.setText("Erro ao buscar cidade");
        }
    }

    // 5. Busca os dados na API Open-Meteo
    private void fetchWeatherData(double latitude, double longitude) {
        APIService apiService = RetrofitClient.getApiService();
        Call<HttpResponse> call = apiService.getCurrentWeather(latitude, longitude, CURRENT_PARAMS);

        call.enqueue(new Callback<HttpResponse>() {
            @Override
            public void onResponse(Call<HttpResponse> call, Response<HttpResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherCurrent currentWeather = response.body().getCurrent();

                    tvTemperature.setText(String.format("%.1f ºC", currentWeather.getTemperature()*10));
                    tvHumidity.setText(String.format("%2d %%", currentWeather.getRelativeHumidity()));
                    tvWindSpeed.setText(String.format("%.1f km/h", currentWeather.getWindSpeed()));
                } else {
                    Toast.makeText(MainActivity.this, "Erro ao obter clima.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<HttpResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Falha na conexão: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}