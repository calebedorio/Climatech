package br.com.climatec.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WeatherCurrent {
    @SerializedName("temperature_2m")
    @Expose
    private double temperature;

    @SerializedName("relative_humidity_2m")
    @Expose
    private int relativeHumidity;

    @SerializedName("wind_speed_10m")
    @Expose
    private double windSpeed;

    public double getTemperature() {
        return temperature;
    }

    public int getRelativeHumidity() {
        return relativeHumidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

}
