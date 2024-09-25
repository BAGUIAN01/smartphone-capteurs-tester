package com.example.myhousecontrol;
import com.google.gson.annotations.SerializedName;
import java.util.List;
public class WeatherResponse {
    @SerializedName("weather")
    private List<Weather> weatherList;

    @SerializedName("main")
    private Main main;

    public List<Weather> getWeatherList() {
        return weatherList;
    }

    public Main getMain() {
        return main;
    }

    public static class Weather {
        @SerializedName("main")
        private String main;

        @SerializedName("description")
        private String description;

        public String getMain() {
            return main;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class Main {
        @SerializedName("temp")
        private float temp;

        public float getTemp() {
            return temp;
        }
    }
}
