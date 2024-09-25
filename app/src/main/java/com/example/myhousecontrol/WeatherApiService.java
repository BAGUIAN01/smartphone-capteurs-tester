package com.example.myhousecontrol;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    @GET("weather")
    Call<WeatherResponse> getCurrentWeather(
            @Query("q") String location,
            @Query("units") String units,
            @Query("appid") String apiKey
    );
}
