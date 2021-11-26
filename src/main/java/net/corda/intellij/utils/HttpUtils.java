package net.corda.intellij.utils;

import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import net.corda.intellij.models.CloseableHttpEntity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class HttpUtils {

    private HttpUtils() {
    }

    public static void waitUntilApiIsReady(String host, int port, Runnable next) {
        new Thread(() -> {
            while (!isReachable(host, port)) {
                sleep(1);
            }
            next.run();
        }).start();
    }

    public static <T> T get(String path, Class<T> clazz) {
        try {
            try (CloseableHttpClient httpClient = HttpClients.createMinimal();
                 CloseableHttpResponse response = httpClient.execute(new HttpGet(path));
                 CloseableHttpEntity closeableEntity = new CloseableHttpEntity(response.getEntity())) {
                String string = EntityUtils.toString(closeableEntity.getEntity());
                return new Gson().fromJson(string, clazz);
            }
        } catch (IOException e) {
            throw new RuntimeException("Something went wrong", e);
        }
    }

    public static boolean isReachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 50_000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void sleep(int second) {
        try {
            Thread.sleep(second * 1_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
