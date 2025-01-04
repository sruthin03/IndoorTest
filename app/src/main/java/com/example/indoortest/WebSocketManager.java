package com.example.indoortest;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketManager {
    private WebSocket webSocket;
    private final Handler uiHandler;
    private final TextView textView;


    public WebSocketManager(TextView textView) {
        this.textView = textView;
        this.uiHandler = new Handler(Looper.getMainLooper());
    }


    public void connectWebSocket(String serverUrl) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url(serverUrl).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d("WebSocket", "Connection established");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {

                Log.d("WebSocket", text);

                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (textView != null) {
                            String currentText = textView.getText().toString();
                            String updatedText = text + "\n" + currentText;
                            textView.setText(updatedText);
                        }
                    }
                });
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {

                webSocket.close(1000, "Closing connection");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e("WebSocket", "Connection failed", t);
            }
        });


    }


    public void sendMessage(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        }
    }


    public void closeWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing connection");
        }
    }
}
