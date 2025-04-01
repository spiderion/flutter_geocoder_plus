package com.aloisdeniel.geocoder;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class GeocoderPlugin implements FlutterPlugin, MethodCallHandler {

    private MethodChannel channel;
    private Geocoder geocoder;
    private Context context;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        context = binding.getApplicationContext();
        geocoder = new Geocoder(context);
        channel = new MethodChannel(binding.getBinaryMessenger(), "github.com/aloisdeniel/geocoder");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;
        geocoder = null;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("findAddressesFromQuery")) {
            String address = call.argument("address");
            findAddressesFromQuery(address, result);
        } else if (call.method.equals("findAddressesFromCoordinates")) {
            double latitude = ((Number) call.argument("latitude")).doubleValue();
            double longitude = ((Number) call.argument("longitude")).doubleValue();
            findAddressesFromCoordinates(latitude, longitude, result);
        } else {
            result.notImplemented();
        }
    }

    private void findAddressesFromQuery(final String address, final Result result) {
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(address, 20);
                postResult(result, createAddressMapList(addresses));
            } catch (IOException e) {
                postError(result, "io_error", e.getMessage());
            }
        }).start();
    }

    private void findAddressesFromCoordinates(final double latitude, final double longitude, final Result result) {
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 20);
                postResult(result, createAddressMapList(addresses));
            } catch (IOException e) {
                postError(result, "io_error", e.getMessage());
            }
        }).start();
    }

    private void postResult(Result result, Object value) {
        new Handler(Looper.getMainLooper()).post(() -> result.success(value));
    }

    private void postError(Result result, String code, String message) {
        new Handler(Looper.getMainLooper()).post(() -> result.error(code, message, null));
    }

    private Map<String, Object> createCoordinatesMap(Address address) {
        if (address == null) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("latitude", address.getLatitude());
        result.put("longitude", address.getLongitude());
        return result;
    }

    private Map<String, Object> createAddressMap(Address address) {
        if (address == null) return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(address.getAddressLine(i));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("coordinates", createCoordinatesMap(address));
        result.put("featureName", address.getFeatureName());
        result.put("countryName", address.getCountryName());
        result.put("countryCode", address.getCountryCode());
        result.put("locality", address.getLocality());
        result.put("subLocality", address.getSubLocality());
        result.put("thoroughfare", address.getThoroughfare());
        result.put("subThoroughfare", address.getSubThoroughfare());
        result.put("adminArea", address.getAdminArea());
        result.put("subAdminArea", address.getSubAdminArea());
        result.put("addressLine", sb.toString());
        result.put("postalCode", address.getPostalCode());

        return result;
    }

    private List<Map<String, Object>> createAddressMapList(List<Address> addresses) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (addresses == null) return result;

        for (Address address : addresses) {
            result.add(createAddressMap(address));
        }

        return result;
    }
}
