package com.aloisdeniel.geocoder;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeocoderPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {

    private MethodChannel channel;
    private Geocoder geocoder;
    private Context context;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        this.context = flutterPluginBinding.getApplicationContext();
        this.geocoder = new Geocoder(context);
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "github.com/aloisdeniel/geocoder");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        // If activity reference is needed in the future.
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    }

    @Override
    public void onDetachedFromActivity() {
    }

    @Override
    public void onMethodCall(MethodCall call, Result rawResult) {
        Result result = new MethodResultWrapper(rawResult);

        if (call.method.equals("findAddressesFromQuery")) {
            String address = (String) call.argument("address");
            findAddressesFromQuery(address, result);
        } else if (call.method.equals("findAddressesFromCoordinates")) {
            float latitude = ((Number) call.argument("latitude")).floatValue();
            float longitude = ((Number) call.argument("longitude")).floatValue();
            findAddressesFromCoordinates(latitude, longitude, result);
        } else {
            result.notImplemented();
        }
    }

    private class MethodResultWrapper implements Result {
        private Result methodResult;
        private Handler handler;

        MethodResultWrapper(Result result) {
            methodResult = result;
            handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void success(final Object result) {
            handler.post(() -> methodResult.success(result));
        }

        @Override
        public void error(final String errorCode, final String errorMessage, final Object errorDetails) {
            handler.post(() -> methodResult.error(errorCode, errorMessage, errorDetails));
        }

        @Override
        public void notImplemented() {
            handler.post(() -> methodResult.notImplemented());
        }
    }

    private void findAddressesFromQuery(final String address, final Result result) {
        new AsyncTask<Void, Void, List<Address>>() {
            @Override
            protected List<Address> doInBackground(Void... params) {
                try {
                    if (!geocoder.isPresent()) return new ArrayList<>();
                    return geocoder.getFromLocationName(address, 20);
                } catch (IOException ex) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Address> addresses) {
                if (addresses != null) {
                    if (addresses.isEmpty())
                        result.error("not_available", "Empty", null);
                    else
                        result.success(createAddressMapList(addresses));
                } else result.error("failed", "Failed", null);
            }
        }.execute();
    }

    private void findAddressesFromCoordinates(final float latitude, final float longitude, final Result result) {
        new AsyncTask<Void, Void, List<Address>>() {
            @Override
            protected List<Address> doInBackground(Void... params) {
                try {
                    if (!geocoder.isPresent()) return new ArrayList<>();
                    return geocoder.getFromLocation(latitude, longitude, 20);
                } catch (IOException ex) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Address> addresses) {
                if (addresses != null) {
                    if (addresses.isEmpty())
                        result.error("not_available", "Empty", null);
                    else
                        result.success(createAddressMapList(addresses));
                } else result.error("failed", "Failed", null);
            }
        }.execute();
    }

    private List<Map<String, Object>> createAddressMapList(List<Address> addresses) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Address address : addresses) {
            Map<String, Object> map = new HashMap<>();
            map.put("latitude", address.getLatitude());
            map.put("longitude", address.getLongitude());
            map.put("addressLine", address.getAddressLine(0));
            result.add(map);
        }
        return result;
    }
}