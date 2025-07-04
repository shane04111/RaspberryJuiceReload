package com.shane.RaspberryJuiceReload;

import com.shane.RaspberryJuiceReload.commands.base.CommandProcessor;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;

/**
 * 處理與客戶端的遠程會話
 */
public class RemoteSession {
    private final Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private final ArrayDeque<String> inQueue = new ArrayDeque<>();
    private final ArrayDeque<String> outQueue = new ArrayDeque<>();
    public boolean running = true;
    public boolean pendingRemoval = false;
    public RaspberryJuiceReload plugin;
    public CommandProcessor commandProcessor;

    /**
     * 創建一個新的遠程會話
     *
     * @param plugin 插件實例
     * @param socket 客戶端連接的套接字
     * @throws IOException 如果I/O錯誤發生
     */
    public RemoteSession(RaspberryJuiceReload plugin, Socket socket) throws IOException {
        this.socket = socket;
        this.plugin = plugin;
        init();
    }

    /**
     * 初始化會話
     *
     * @throws IOException 如果I/O錯誤發生
     */
    public void init() throws IOException {
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setTrafficClass(0x10);
        this.commandProcessor = new CommandProcessor(plugin);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        startThreads();
        RaspberryJuiceReload.logger.info("Opened connection to {}.", socket.getRemoteSocketAddress());
    }

    /**
     * 啟動I/O線程
     */
    protected void startThreads() {
        Thread inThread = new Thread(new InputThread());
        inThread.start();
        Thread outThread = new Thread(new OutputThread());
        outThread.start();
    }

    /**
     * 獲取套接字
     *
     * @return 客戶端連接的套接字
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * 從服務器主線程調用，處理命令隊列
     */
    public void tick() {
        commandProcessor.tick();

        int processedCount = 0;
        String message;
        while ((message = inQueue.poll()) != null) {
            handleLine(message);
            processedCount++;
            int maxCommandsPerTick = 9000;
            if (processedCount >= maxCommandsPerTick) {
                RaspberryJuiceReload.logger.warn("Over {} commands were queued - deferring {} to next tick", maxCommandsPerTick, inQueue.size());
                break;
            }
        }

        if (!running && inQueue.isEmpty()) {
            pendingRemoval = true;
        }
    }

    /**
     * 處理從客戶端接收的命令行
     *
     * @param line 命令行
     */
    protected void handleLine(String line) {
        String methodName = line.substring(0, line.indexOf("("));
        String[] args = line.substring(line.indexOf("(") + 1, line.length() - 1).split(",");
        handleCommand(methodName, args);
    }

    /**
     * 處理命令及其參數
     *
     * @param c    命令名稱
     * @param args 命令參數
     */
    protected void handleCommand(String c, String[] args) {
        send(commandProcessor.processCommand(c, args));
    }

    /**
     * 向客戶端發送消息
     *
     * @param msg 消息
     */
    private void send(Object msg) {
        try {
            outQueue.add(msg.toString() + "\n");
        } catch (Exception e) {
            RaspberryJuiceReload.logger.error("Failed to add message to output queue", e);
        }
    }

    /**
     * 關閉會話
     */
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (Exception e) {
            RaspberryJuiceReload.logger.warn("Failed to close socket");
        }
    }

    /**
     * 踢出客戶端
     *
     * @param reason 原因
     */
    public void kick(String reason) {
        try {
            out.write(reason + "\n");
            out.flush();
        } catch (Exception e) {
            RaspberryJuiceReload.logger.warn("Failed to kick", e);
        }
        close();
    }

    /**
     * 輸入線程，處理從客戶端接收的命令
     */
    private class InputThread implements Runnable {
        @Override
        public void run() {
            try {
                String newLine;
                while (running && (newLine = in.readLine()) != null) {
                    // 將命令加入隊列
                    inQueue.add(newLine);
                }
            } catch (Exception e) {
                if (running) {
                    RaspberryJuiceReload.logger.warn("Error reading from socket", e);
                    running = false;
                }
            }
        }
    }

    /**
     * 輸出線程，處理發送到客戶端的消息
     */
    private class OutputThread implements Runnable {
        @Override
        public void run() {
            try {
                while (running) {
                    String line;
                    while ((line = outQueue.poll()) != null) {
                        out.write(line);
                        out.flush();
                    }
                    Thread.sleep(1);
                }
            } catch (Exception e) {
                if (running) {
                    RaspberryJuiceReload.logger.warn("Error writing to socket", e);
                }
            }
        }
    }
}
