package com.shane.RaspberryJuiceReload;

import java.io.*;
import java.net.*;

public class ServerListenerThread implements Runnable {

    public ServerSocket serverSocket;
    public SocketAddress bindAddress;
    public boolean running = true;
    private final RaspberryJuiceReload plugin;

    public ServerListenerThread(RaspberryJuiceReload plugin, SocketAddress bindAddress) throws IOException {
        this.plugin = plugin;
        this.bindAddress = bindAddress;
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(bindAddress);
    }

    @Override
    public void run() {
        while (running) {
            try {
                Socket newConnection = serverSocket.accept();
                if (!running) return;
                plugin.handleConnection(new RemoteSession(plugin, newConnection));
            } catch (Exception e) {
                // 如果服務器線程仍在運行，則引發錯誤
                if (running) {
                    RaspberryJuiceReload.logger.warn("Error creating new connection", e);
                }
            }
        }
        try {
            serverSocket.close();
        } catch (Exception e) {
            RaspberryJuiceReload.logger.warn("Error closing server socket", e);
        }
    }
}
