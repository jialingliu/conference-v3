package cmu.cconfs.parseUtils.helper;

import android.support.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import cmu.cconfs.model.parseModel.FutureWeather;

/**
 * @author jialingliu
 */
public class JSONFutureWeatherParser {
    private JSONObject getJsonFutureWeather(@NonNull String data,
                                            String durationInString) {
        System.out.println("data: ");
        System.out.println(data);
        long durationInS = Long.parseLong(durationInString);
        // response.json
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(data);
        JsonArray weathers = jsonElement.getAsJsonArray();
        long lowerTmp = Long.MAX_VALUE;
        JSONObject prev = null;
        try {
            for (int i = 0; i < weathers.size(); i++) {
                // do not use method jsonElement.getAsString()
                // it will throw UnsupportedOperationException
                JSONObject jsonObject = new JSONObject(weathers.get(i).toString());
                long epochDateTime = jsonObject.getLong("EpochDateTime");
                if ((i == 0 && epochDateTime > durationInS)
                        || i == weathers.size() - 1 && epochDateTime < durationInS){
                    return jsonObject;
                }
                if (durationInS > lowerTmp && durationInS < epochDateTime) {
                    if (durationInS - lowerTmp < epochDateTime - durationInS) {
                        return prev;
                    } else {
                        return jsonObject;
                    }
                } else if (durationInS == epochDateTime) {
                    return jsonObject;
                } else {
                    lowerTmp = epochDateTime;
                    prev = jsonObject;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();

        }
        return null;

    }
    public FutureWeather getFutureWeather(@NonNull String data,
                                          String durationInString) {
        // TODO: 3/24/17 parse futureWeather from response
        JSONObject jsonObject = getJsonFutureWeather(data, durationInString);
        FutureWeather futureWeather = new FutureWeather();
        if (jsonObject == null) {
            return futureWeather;
        }
        try {
            futureWeather.setDateTime(jsonObject.getString("DateTime"));
            futureWeather.setEpochDateTime(jsonObject.getLong("EpochDateTime"));
//            int weatherIcon = jsonObject.getInt("WeatherIcon");
//            StringBuilder sb = new StringBuilder();
//            if (weatherIcon < 10) {
//                sb.append(0).append(weatherIcon);
//            } else {
//                sb.append(weatherIcon);
//            }
//            futureWeather.setWeatherIcon(sb.toString());
            futureWeather.setWeatherIcon(Integer.toString(jsonObject.getInt("WeatherIcon")));
            futureWeather.setIconPhrase(jsonObject.getString("IconPhrase"));
            futureWeather.setDayLight(jsonObject.getBoolean("IsDaylight"));

            futureWeather.temperature.setTemp(jsonObject.getJSONObject("Temperature").getDouble("Value"));
            futureWeather.temperature.setRealFeelTemp(jsonObject.getJSONObject("RealFeelTemperature").getDouble("Value"));
            futureWeather.temperature.setUnit(jsonObject.getJSONObject("Temperature").getString("Unit"));

            futureWeather.wind.setSpeed(jsonObject.getJSONObject("Wind").getJSONObject("Speed").getDouble("Value"));
            futureWeather.wind.setUnit(jsonObject.getJSONObject("Wind").getJSONObject("Speed").getString("Unit"));
            futureWeather.wind.setDegree(jsonObject.getJSONObject("Wind").getJSONObject("Direction").getDouble("Degrees"));
            futureWeather.wind.setLocalized(jsonObject.getJSONObject("Wind").getJSONObject("Direction").getString("Localized"));

            futureWeather.probability.setPrecipitationProbability(jsonObject.getDouble("PrecipitationProbability"));
            futureWeather.probability.setRainProbability(jsonObject.getDouble("RainProbability"));
            futureWeather.probability.setSnowProbability(jsonObject.getDouble("SnowProbability"));
            futureWeather.probability.setIceProbability(jsonObject.getDouble("IceProbability"));

            futureWeather.rain.setVal(jsonObject.getJSONObject("Rain").getInt("Value"));
            futureWeather.rain.setUnit(jsonObject.getJSONObject("Rain").getString("Unit"));

            futureWeather.snow.setVal(jsonObject.getJSONObject("Snow").getInt("Value"));
            futureWeather.snow.setUnit(jsonObject.getJSONObject("Snow").getString("Unit"));

            futureWeather.ice.setVal(jsonObject.getJSONObject("Ice").getInt("Value"));
            futureWeather.ice.setUnit(jsonObject.getJSONObject("Ice").getString("Unit"));

            futureWeather.setCloudCover(jsonObject.getInt("CloudCover"));
            String link = jsonObject.getString("Link");
//            futureWeather.setLink("<a href=\"" + link + "\">" + link + "</a>");
            futureWeather.setLink(link);
            String mobileLink = jsonObject.getString("MobileLink");
//            futureWeather.setMobileLink("<a href=\"" + mobileLink + "\">Click for details</a>");
            futureWeather.setMobileLink(mobileLink);
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("field does not exist");
        }
        return futureWeather;
    }
}
