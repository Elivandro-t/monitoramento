package com.example.projetoAP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Ping {
    public static void main(String[] args) throws IOException {
        String ip = "192.168.88.254";
        String cmd = null;
        while (true) {
            Process p = Runtime.getRuntime().exec("ping -c 1 " + ip); // Para Linux/macOS
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                if (line.contains("ttl=") || line.contains("TTL=")) {
                   cmd = line;
                }
            }
            try {
                Thread.sleep(1000); // Aguarda 1 segundo entre pings
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("ping " + cmd);

        }

    }
}
