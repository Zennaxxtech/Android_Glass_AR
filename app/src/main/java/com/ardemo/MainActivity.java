package com.ardemo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.ardemo.network.place.Location;
import com.ardemo.utils.PermissionCheck;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private int mnc;
    private int mcc;
    private int phoneType;
    String radioType = "";
    private String carrierName;
    private String macAddress;
    private int wifiSignalSt;
    private final static ArrayList<Integer> channelsFrequency = new ArrayList<Integer>(
            Arrays.asList(0, 2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447,
                    2452, 2457, 2462, 2467, 2472, 2484));
    private int channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initDataForNetworkLocation();

        findViewById(R.id.network).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callApi();
            }
        });

        findViewById(R.id.gps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionCheck.initialPermissionCheckAll(getApplicationContext(), MainActivity.this);
            }
        });
    }

    public void initDataForNetworkLocation() {
        TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String networkOperator = tel.getNetworkOperator();

        if (!TextUtils.isEmpty(networkOperator)) {
            mcc = Integer.parseInt(networkOperator.substring(0, 3));
            mnc = Integer.parseInt(networkOperator.substring(3));
            phoneType = tel.getNetworkType();
            carrierName = tel.getNetworkOperatorName();
        }

        switch (phoneType) {
            case (TelephonyManager.NETWORK_TYPE_CDMA):
                // your code
                radioType = "cdma";
                break;
            case (TelephonyManager.NETWORK_TYPE_GSM):
                // your code
                radioType = "gsm";
                break;
            case (TelephonyManager.NETWORK_TYPE_LTE):
                radioType = "lte";
                // your code
            case (TelephonyManager.NETWORK_TYPE_HSPA):
                radioType = "wcdma";
                // your code
                break;
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        macAddress = wInfo.getMacAddress();
        wifiSignalSt = WifiManager.calculateSignalLevel(wInfo.getRssi(), 5);
        channel = getChannelFromFrequency(wInfo.getFrequency());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 10: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Intent intent = new Intent(MainActivity.this, ArCameraViewPlacesActivity.class);
                    startActivity(intent);
                    finish();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public static int getChannelFromFrequency(int frequency) {
        return channelsFrequency.indexOf(Integer.valueOf(frequency));
    }

    private void callApi() {
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        String url = "https://www.googleapis.com/geolocation/v1/geolocate?key=AIzaSyB6ykj37QV02hT4DJEEo2U1oB7FKvwgMmk";
        asyncHttpClient.addHeader("Content-Type", "application/json");

        RequestParams requestParams = new RequestParams();

        requestParams.put("homeMobileCountryCode", mcc);
        requestParams.put("homeMobileNetworkCode", mnc);
        requestParams.put("radioType", radioType);
        requestParams.put("carrier", carrierName);
        requestParams.put("considerIp", "true");

        JSONObject jsonParams = new JSONObject();

        try {
            jsonParams.put("macAddress", macAddress);
            jsonParams.put("signalStrength", wifiSignalSt);
            jsonParams.put("age", "0");
            jsonParams.put("channel", channel);
            jsonParams.put("signalToNoiseRatio", "0");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        requestParams.put("cellTowers", "" + getCellInfo(MainActivity.this));
        requestParams.put("wifiAccessPoints", "" + jsonParams.toString());


        asyncHttpClient.post(url, requestParams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d("callApi", "onSuccess" + new String(responseBody));
                String res = new String(responseBody);

                JSONObject mainObject = null;
                try {
                    mainObject = new JSONObject(res);
                    JSONObject location = mainObject.getJSONObject("location");
                    double lat = location.getDouble("lat");
                    double lng = location.getDouble("lng");

                    Intent intent = new Intent(MainActivity.this, ArCameraViewPlacesActivity.class);
                    intent.putExtra("lat", lat);
                    intent.putExtra("lng", lng);
                    startActivity(intent);
                    finish();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("callApi", "onFailure" + error.getMessage());
            }
        });
    }

    public static JSONArray getCellInfo(Context ctx) {
        TelephonyManager tel = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

        JSONArray cellList = new JSONArray();

        int phoneTypeInt = tel.getPhoneType();
        String phoneType = null;
        phoneType = phoneTypeInt == TelephonyManager.PHONE_TYPE_GSM ? "gsm" : phoneType;
        phoneType = phoneTypeInt == TelephonyManager.PHONE_TYPE_CDMA ? "cdma" : phoneType;

        //from Android M up must use getAllCellInfo

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            List<NeighboringCellInfo> neighCells = tel.getNeighboringCellInfo();
            for (int i = 0; i < neighCells.size(); i++) {
                try {
                    JSONObject cellObj = new JSONObject();
                    NeighboringCellInfo thisCell = neighCells.get(i);
                    cellObj.put("cellId", thisCell.getCid());
                    cellObj.put("locationAreaCode", thisCell.getLac());
                    cellObj.put("mobileCountryCode", tel.getNetworkCountryIso());
                    cellObj.put("mobileNetworkCode", tel.getNetworkOperator());
                    cellObj.put("rssi", thisCell.getRssi());
                    cellList.put(cellObj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            List<CellInfo> infos = tel.getAllCellInfo();
            for (int i = 0; i < infos.size(); ++i) {
                try {
                    JSONObject cellObj = new JSONObject();
                    CellInfo info = infos.get(i);
                    if (info instanceof CellInfoGsm) {
                        CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                        CellIdentityGsm identityGsm = ((CellInfoGsm) info).getCellIdentity();
                        cellObj.put("cellId", identityGsm.getCid());
                        cellObj.put("locationAreaCode", identityGsm.getLac());
                        cellObj.put("mobileCountryCode", ((CellInfoGsm) info).getCellIdentity().getMcc());
                        cellObj.put("mobileNetworkCode", ((CellInfoGsm) info).getCellIdentity().getMnc());
                        cellObj.put("age", "0");
                        cellObj.put("signalStrength", gsm.getDbm());
                        cellObj.put("timingAdvance", gsm.getTimingAdvance());
                        cellList.put(cellObj);
                    } else if (info instanceof CellInfoWcdma) {
                        CellSignalStrengthWcdma lte = ((CellInfoWcdma) info).getCellSignalStrength();
                        CellIdentityWcdma identityLte = ((CellInfoWcdma) info).getCellIdentity();
                        cellObj.put("cellId", identityLte.getCid());
                        cellObj.put("locationAreaCode", identityLte.getLac());
                        cellObj.put("mobileCountryCode", identityLte.getMcc());
                        cellObj.put("mobileNetworkCode", identityLte.getMnc());
                        cellList.put(cellObj);
                    } else if (info instanceof CellInfoLte) {
                        CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                        CellIdentityLte identityLte = ((CellInfoLte) info).getCellIdentity();
                        cellObj.put("cellId", identityLte.getCi());
                        cellObj.put("locationAreaCode", identityLte.getEarfcn());
                        cellObj.put("mobileCountryCode", identityLte.getMcc());
                        cellObj.put("mobileNetworkCode", identityLte.getMnc());
                        cellList.put(cellObj);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return cellList;
    }
}
