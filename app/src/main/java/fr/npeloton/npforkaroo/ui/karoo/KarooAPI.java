package fr.npeloton.npforkaroo.ui.karoo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class KarooAPI {
    public static String RF_TOKEN ="refreshtoken";
    public static String TOKEN ="token";
    public static String PREF_EXPIRES_AT ="time";
    public static String API_GET_TOKEN_URL = "https://dashboard.hammerhead.io/v1/auth/token";
    public static String API_PUSH_GPX =   "https://dashboard.hammerhead.io/ihub/api/v1/routes/import/file";
    public static String API_REFRESH_TOKEN_URL =  "https://dashboard.hammerhead.io/v1/auth/token";

    public static String API_PUSH_URL ="https://dashboard.hammerhead.io/ihub/api/v1/routes/import/url";

    public static void printdebug(String title, String message){
        if(message == null){
            Log.d(title, "NULL");
        } else {
            Log.d(title, message);
        }
    }

    public static String getToken(Context context){
         String token =  PreferenceManager.getDefaultSharedPreferences(context).getString(TOKEN,null);
        printdebug("getToken ",token);
        return token;
    }
    public static long getLastTokenRequestTime(Context context){
        long time =  PreferenceManager.getDefaultSharedPreferences(context).getLong(PREF_EXPIRES_AT,0);
        printdebug("getLastTokenRequestTime  ",String.valueOf(time));

        return time;
    }
    public static String getRefreshToken(Context context){
        String refreshToken =  PreferenceManager.getDefaultSharedPreferences(context).getString(RF_TOKEN,null);
        printdebug("getRefreshToken ",refreshToken);

        return refreshToken;
    }
    public static void setToken(Context context, String token){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(TOKEN,token).apply();
        printdebug("setToken ",String.valueOf(token));

    }
    public static void setLastTokenRequestTime(Context context, long time){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong(PREF_EXPIRES_AT,time).apply();
        printdebug("setLastTokenRequestTime ",String.valueOf(time));


    }
    public static void setRefreshToken(Context context, String refreshtoken){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(RF_TOKEN,refreshtoken).apply();
        printdebug("setRefreshToken ",refreshtoken);

    }


    public static void toastInResponse(Context context,String message){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() {
                Toast.makeText(context
                        , message, Toast.LENGTH_LONG).show();
            }
        });
    }


    public static boolean pushUrl(Context context, String gpx){

        printdebug("pushUrl", gpx);
        RequestBody requestBody = new FormBody.Builder()
                .add("url", gpx)
                .add("addToDb", "true")
                .build();


        printdebug("Authorisation ","Bearer " + getToken(context));
        Request request = new Request.Builder().
                header("Authorization", "Bearer " + getToken(context))
                .url(API_PUSH_URL)
                .post(requestBody)
                .build();


        OkHttpClient client  = new OkHttpClient();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String mMessage = e.getMessage().toString();
                Log.w("failure Response", mMessage);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String mMessage = response.body().string();
                try {
                    JSONObject res = new JSONObject(mMessage);
                    String rideName = (String) res.get("name");

                    printdebug("Ride uploaded",rideName);
                    toastInResponse(context, "Ride " +rideName+"uploaded");

                } catch (JSONException e) {
                    Log.w("GPXUpload JSon resp", mMessage);
                    toastInResponse(context, "Ride not uploaded "+mMessage);

                }


                Log.w("GPXUpload", mMessage);

            }
        });



        return true;
    }


    public static boolean isTokenNull(Context context){
        if( getToken(context) == null ){
            return true;
        }
        return false;
    }

    public static boolean isTokenExpired(Context context){
        long currenttime = System.currentTimeMillis();


        long expires_at = getLastTokenRequestTime(context);
        //  String access_token = prefs.getString(PREF_ACCESS_TOKEN, null);
       // String refresh_token = getRefreshToken(context);


        if (expires_at < currenttime) {
            printdebug("expired : e<c", expires_at + "<" + currenttime);
            return true;

        }else {
            printdebug("not expired : e>c", expires_at + ">" + currenttime);
            return false;

        }

    }


    public static void fullProcessToken(Context context){
        if(getToken(context)==null){
            requestToken(context);
        } else if(isTokenExpired(context)){
            refreshToken(context);
        }

    }



    public static boolean requestToken(Context context){


        

            String username =PreferenceManager.getDefaultSharedPreferences(context).getString("karoo_email",null);;
            String password = PreferenceManager.getDefaultSharedPreferences(context).getString("karoo_password",null);;

        if ((password == null)||(username==null) ){
            return false;
        }

          RequestBody requestBody = new FormBody.Builder()
                  .add("grant_type", "password")
                  .add("username", username)
                  .add("password", password)
                .build();

            /**RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("grant_type", "password")
                    .addFormDataPart("username", username)
                    .addFormDataPart("password", password)
                    .build(); **/


            Request request = new Request.Builder()
                    .url(API_GET_TOKEN_URL)
                    .post(requestBody)
                    .build();


                OkHttpClient client  = new OkHttpClient();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    String mMessage = e.getMessage().toString();
                    Log.w("failure Response", mMessage);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    String mMessage = response.body().string();
                    try {
                        JSONObject res = new JSONObject(mMessage);
                        setRefreshToken(context, (String) res.get("refresh_token"));
                        setToken(context, (String) res.get("access_token"));

                        long expires_in = ((Integer)res.get("expires_in")).longValue() ;
                        printdebug("expires_in",String.valueOf(expires_in));
                        long expires_at = System.currentTimeMillis() + expires_in*1000;
                        //printdebug("expires_at",String.valueOf(expires_at));
                        setLastTokenRequestTime(context, expires_at);
                       // Log.w("refresh_token ", getRefreshToken(context));
                       // Log.w("access_token ", getToken(context));
                        //Log.w("expires_at ", (String.valueOf(getLastTokenRequestTime(context))));

                    } catch (JSONException e) {
                        Log.w("access JSon resp", mMessage);
                    }


                    Log.w("SUCCESS getToken", mMessage);

                }
            });



        return true;
    }

    /**
     * Appelle l'API karoo pour récuperer un refreshtoken
     * @param context
     * @return
     */
    public static boolean refreshToken(Context context) {
        boolean isOk = true;

        //  checking to see if the short-lived access token has expired
        long currenttime = System.currentTimeMillis();


        long expires_at = getLastTokenRequestTime(context);
        //  String access_token = prefs.getString(PREF_ACCESS_TOKEN, null);
        String refresh_token = getRefreshToken(context);

        printdebug("refresh_token",refresh_token);

        RequestBody requestBody = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refresh_token)

                .build();
      /**
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("grant_type", "refresh_token")
                    .addFormDataPart("refresh_token", refresh_token)
                    .build(); **/


            Request request = new Request.Builder()
                    .url(API_REFRESH_TOKEN_URL)
                    .post(requestBody)
                    .build();


            OkHttpClient client  = new OkHttpClient();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    String mMessage = e.getMessage().toString();
                    Log.w("failure Response", mMessage);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    String mMessage = response.body().string();
                    try {
                        JSONObject res = new JSONObject(mMessage);
                        setRefreshToken(context, (String) res.get("refresh_token"));
                        setToken(context, (String) res.get("access_token"));
                        long expires_in = ((Integer)res.get("expires_in")).longValue() ;

                        long expires_at = currenttime + expires_in;

                        setLastTokenRequestTime(context, expires_at);
                        Log.w("refresh_token ", getRefreshToken(context));
                        Log.w("access_token ", getToken(context));
                        Log.w("expires_at ", (String.valueOf(getLastTokenRequestTime(context))));

                    } catch (JSONException e) {
                        Log.w("refreshtoken JSon resp", mMessage);
                    }


                    Log.w("SUCCESS RefreshToken", mMessage);

                }
            });




        return isOk;
    }
}

