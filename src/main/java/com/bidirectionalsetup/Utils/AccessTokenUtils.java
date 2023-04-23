package com.bidirectionalsetup.Utils;

import com.bidirectionalsetup.Config.Config;
import com.zerodhatech.kiteconnect.KiteConnect;

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.User;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//import okhttp3.;

import java.io.*;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Scanner;

public class AccessTokenUtils {
    private final String baseUrl;
    private final String loginUrl;
    private final String twofaUrl;
    private final String instrumentsUrl;
    private final TOTPUtils totpUtils;
    private final OkHttpClient client;
    private final KiteConnect kiteSdk;
    private final Scanner scanner;

    public AccessTokenUtils(KiteConnect kiteSdk){
        this.baseUrl = "https://kite.zerodha.com";
        this.loginUrl = "https://kite.zerodha.com/api/login";
        this.twofaUrl = "https://kite.zerodha.com/api/twofa";
        this.instrumentsUrl = "https://api.kite.trade/instruments";
        this.totpUtils = new TOTPUtils(Config.totpSecret);
        this.client = new OkHttpClient().newBuilder().build();
        this.kiteSdk = kiteSdk;
        this.scanner = new Scanner(System.in);
    }

    /*public void getAccessToken() throws IOException {

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("user_id", Config.userId);
        requestBody.put("password", Config.password);

        String urlParameters = getEncodedData(requestBody);
        System.out.println(urlParameters);

        String contentType = "application/x-www-form-urlencoded";
        String responseString = makePostRequest(urlParameters, loginUrl, contentType);
        System.out.println(responseString);

        // Parse the JSON response into a JSONNode and Derive Request Id
        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseNode = mapper.readTree(responseString);
        String requestId = String.valueOf(responseNode.get("data").get("request_id"));

        System.out.println("Request Id : "+requestId);

        Map<String, String> totpRequestBody = new HashMap<>();
        totpRequestBody.put("user_id", Config.userId);
        totpRequestBody.put("request_id", requestId);
        totpRequestBody.put("twofa_value", totpUtils.getCurrentTOTP());
        totpRequestBody.put("twofa_type", "totp");

        String totpUrlParameters = getEncodedData(totpRequestBody);
        System.out.println(totpUrlParameters);

        String totpResponseString = makePostRequest(totpUrlParameters, twofaUrl, contentType);
        System.out.println(totpResponseString);

        String callbackUrl = kiteSdk.getLoginURL();
        try{
            //MediaType mediaType = MediaType.parse("text/plain");
            //RequestBody body = RequestBody.create(urlParameters, mediaType);
            Request callbackRequest = new Request.Builder()
                    .url(callbackUrl)
                    .get()
                    .build();
            Response callbackResponse = this.client.newCall(callbackRequest).execute();
            System.out.println(callbackResponse.body().string());
        }catch(Exception e){
            System.out.println(e.toString());
        }
    }*/

    public String getAccessToken() throws IOException, InterruptedException, KiteException {
        String loginUrl = kiteSdk.getLoginURL();
        System.out.println("Get Token Link : " + loginUrl);
        Boolean printTotp = true;
        while(printTotp) {
            Thread.sleep(2000);
            System.out.println("TOTP : " + totpUtils.getCurrentTOTP());
            System.out.println("Refresh TOTP : ");
            String getTotpAgain = scanner.nextLine();
            if(getTotpAgain.equals("n")  || getTotpAgain.equals("N")) {
                printTotp = false;
            }
        }

        System.out.println("Enter Request Token : ");
        String requestToken = scanner.nextLine();
        User user = kiteSdk.generateSession(requestToken, Config.apiSecret);
        return user.accessToken;
    }

    private static String getEncodedData(Map<String, String> postData) throws UnsupportedEncodingException {
        StringBuilder encodedData = new StringBuilder();
        for (Map.Entry<String, String> entry : postData.entrySet()) {
            if (encodedData.length() != 0) {
                encodedData.append("&");
            }
            encodedData.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            encodedData.append("=");
            encodedData.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return encodedData.toString();
    }

    private String makePostRequest(String urlParameters, String url, String contentType) throws IOException {
        MediaType mediaType = MediaType.parse(contentType);
        RequestBody body = RequestBody.create(urlParameters, mediaType);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = this.client.newCall(request).execute();
        System.out.print("Response Headers:" + response.headers());
        return response.body().string();
    }
}
