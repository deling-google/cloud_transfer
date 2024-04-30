package com.google.hello_hotspot;

import android.content.ContentResolver;
import android.net.Uri;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FileServer extends Thread {
  static class Pair<T1, T2> {
    public final T1 first;
    public final T2 second;

    public Pair(T1 first, T2 second) {
      this.first = first;
      this.second = second;
    }
  }

  /** HTTP response status line and headers, excluding body. */
  static class Response {
    private int statusCode;
    private final List<Pair<String, String>> headers = new ArrayList<>();

    private String statusText(int code) {
      switch (code) {
        case 200:
          return "OK";
        case 400:
          return "Not Found";
        default:
          return "";
      }
    }

    public void setStatusCode(int code) {
      this.statusCode = code;
    }

    public void addHeader(String name, String value) {
      headers.add(new Pair<>(name, value));
    }

    public byte[] getBytes() {
      String newline = "\r\n";
      StringBuilder sb = new StringBuilder();
      sb.append(
          String.format(Locale.getDefault(), "HTTP/1.0 %d %s", statusCode, statusText(statusCode)));
      sb.append(newline);
      for (Pair<String, String> header : headers) {
        sb.append(header.first);
        sb.append(": ");
        sb.append(header.second);
        sb.append(newline);
      }
      sb.append(newline);

      return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
  }

  private final int port;
  private final ContentResolver contentResolver;
  private List<Uri> filesServed;

  public FileServer(int port, ContentResolver contentResolver) {
    this.port = port;
    this.contentResolver = contentResolver;
  }

  public void serveFiles(List<Uri> files) {
    filesServed = files;
  }

  private byte[] getFileContent(Uri uri) {
    System.out.println(uri);

    if (uri == null) {
      return null;
    }

    byte[] bytes = null;
    try {
      InputStream inputStream = contentResolver.openInputStream(uri);
      bytes = inputStream.readAllBytes();
      inputStream.close();
    } catch (IOException e) {
      System.out.println(Arrays.toString(e.getStackTrace()));
    }
    System.out.println(bytes.length);
    return bytes;
  }

  private String getFileType(Uri uri) {
    return uri == null ? null : contentResolver.getType(uri);
  }

  @Override
  public void run() {
    try {
      ServerSocket serverSocket = new ServerSocket(port, 100, null);
      while (true) {
        Socket socket = serverSocket.accept();
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

        String path = null;
        String request;
        do {
          request = inputStream.readLine();
          String prefix = "GET /";
          if (request.startsWith(prefix)) {
            path = request.substring(prefix.length());
            int space = path.indexOf(' ');
            if (space != -1) {
              // "GET /path HTTP/1.1"
              path = path.substring(0, space);
            }
          }
        } while (request.length() != 0);

        if (path != null) {
          // GET method
          int index;
          try {
            index=Integer.parseInt(path);
          } catch (NumberFormatException e) {
            index = -1;
          }

          System.out.println(path);
          System.out.println(index);

          Uri uri = (index >= 0 && index < filesServed.size()) ? filesServed.get(index) : null;
          byte[] body = getFileContent(uri);
          String type = getFileType(uri);

          System.out.println(type);
          Response response = new Response();

          if (body != null && type != null) {
            response.setStatusCode(200);
            response.addHeader("Content-Length", Integer.toString(body.length));
            response.addHeader("Content-Type", type);
            response.addHeader("Connection", "Close");
          } else {
            response.setStatusCode(404);
            response.addHeader("Connection", "Close");
          }

          try {
            outputStream.write(response.getBytes());
            if (body != null) {
              outputStream.write(body);
            }

            outputStream.flush();
            outputStream.close();
            socket.close();
          } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
          }
        } else {
          outputStream.close();
          socket.close();
        }
      }
    } catch (Exception e) {
      System.out.println(Arrays.toString(e.getStackTrace()));
    }
  }
}
