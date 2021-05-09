package com.am.steammarketanalyzer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import static com.am.steammarketanalyzer.App.CHANNEL_ID;

public class MyService extends Service{
    private NotificationManagerCompat notificationManager;
    private final Handler handler = new Handler();
    private RequestQueue queue;

    private int id = 2;
    public int getId() {
        return id;
    }
    public void setId(int newId) {
        this.id = newId;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationManager = NotificationManagerCompat.from(this);

        String input = intent.getStringExtra(getString(R.string.input_extra));

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        startForeground(1, createNotification(getString(R.string.content_title), getString(R.string.content_text)));

        String[][] items = extractInput(input);
        queue = Volley.newRequestQueue(this);

        for (String[] item : items) {
            createRunnables(item);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private String[][] extractInput(String input) {
        String[] line = input.split("\n");
        String[][] items = new String[line.length][line[0].split("]").length];

        for (int i = 0; i < line.length; i++) {
            String[] parts = line[i].split("]");

            for (int j = 0; j < parts.length; j++) {
                items[i][j] = parts[j].substring(1);
            }
        }

        return items;
    }

    private void createRunnables(String[] item) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                check(item[0], item[1], item[2], item[3], item[4], item[6]);
                handler.postDelayed(this, TimeUnit.SECONDS.toMillis(Long.parseLong(item[5])));
            }
        };
        handler.post(runnable);
    }

    private void check(String name, String id, String sellRange, String buyRange, String differenceRange, String currency) {
        String url = String.format("https://steamcommunity.com/market/itemordershistogram?language=english&item_nameid=%s&currency=%s", id, currency);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            String symbol = response.getString("price_prefix");
                            if (symbol.equals("")) {
                                symbol = response.getString("price_suffix");
                            }

                            float sell = cutSummary(response.getString("sell_order_summary"), symbol);
                            float buy = cutSummary(response.getString("buy_order_summary"), symbol);
                            float difference = sell - buy;

                            String[] sellString = sellRange.split("-");
                            String[] buyString = buyRange.split("-");
                            String[] differenceString = differenceRange.split("-");

                            float[] sellFloat = new float[sellString.length];
                            float[] buyFloat = new float[buyString.length];
                            float[] differenceFloat = new float[differenceString.length];

                            for (int i = 0; i < sellFloat.length; i++) {
                                sellFloat[i] = Float.parseFloat(sellString[i]);
                                buyFloat[i] = Float.parseFloat(buyString[i]);
                                differenceFloat[i] = Float.parseFloat(differenceString[i]);
                            }

                            if (sell >= sellFloat[0] && sell <= sellFloat[1]) {
                                showNotification(name, String.format("Sell: %.2f%s", sell, symbol));
                            }
                            if (buy >= buyFloat[0] && buy <= buyFloat[1]) {
                                showNotification(name, String.format("Buy: %.2f%s", buy, symbol));
                            }
                            if (difference >= differenceFloat[0] && difference <= differenceFloat[1]) {
                                showNotification(name, String.format("Difference: %.2f%s", difference, symbol));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        queue.add(request);
    }

    private void showNotification(String title, String content) {
        notificationManager.notify(getId(), createNotification(title, content));
        setId(getId() + 1);
    }

    private float cutSummary(String summary, String symbol)
    {
        String[] initialParts = summary.split(">");
        String[] endParts = initialParts[3].split("<");
        return Float.parseFloat(endParts[0].replace(symbol, "").replace(",", "."));
    }

    private Notification createNotification(String title, String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_android)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }
}
