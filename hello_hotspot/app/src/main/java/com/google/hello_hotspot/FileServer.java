package com.google.hello_hotspot;

import android.annotation.SuppressLint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class FileServer extends Thread {
  private final int port;

  public FileServer(int port) {
    this.port = port;
  }

  @Override
  public void run() {
    try {
      InetAddress address = InetAddress.getByName("localhost");
      System.out.println(address.toString());
      ServerSocket serverSocket = new ServerSocket(port, 100, address);
      while (true) {
        Socket socket = serverSocket.accept();
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

        String request;
        do {
          request = inputStream.readLine();
          System.out.println(request);
        } while (request.length() != 0);

        String newline = "\r\n";
        String status = "HTTP/1.0 200 OK";
        String body = "Here's your file!";
        int length = body.getBytes(StandardCharsets.UTF_8).length;
        @SuppressLint("DefaultLocale") String headers =
            String.format(
                "Content-Length: %d"
                    + newline
                    + "Content-type: text/plain; charset=utf-8"
                    + newline
                    + "Connection: Close",
                length);
        byte[] bytes =
            (status + newline + headers + newline + newline + body + newline + newline)
                .getBytes(StandardCharsets.UTF_8);

        try {
          outputStream.write(bytes);
          outputStream.flush();
          outputStream.close();
          socket.close();
        } catch (Exception e) {
          System.out.println(Arrays.toString(e.getStackTrace()));
        }
      }
    } catch (Exception e) {
      System.out.println(Arrays.toString(e.getStackTrace()));
    }
  }
}
