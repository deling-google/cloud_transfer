package com.google.hello_hotspot;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// For debugging upload, connect to the hotspot from a PC/Mac and use cURL. E.g.
// curl -v --data-binary "@puppy.jpeg" 192.168.18.23:50000/0 -H "Content-Type: image/jpeg"

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
    return bytes;
  }

  private String getFileType(Uri uri) {
    return uri == null ? null : contentResolver.getType(uri);
  }

  enum Method {
    Unknown,
    Get,
    Post
  };

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
        Method method = Method.Unknown;
        String postType = null;
        int postLength = -1;

        do {
          final String prefixGet = "GET /";
          final String prefixPost = "POST /";
          final String prefixContentLength = "Content-Length:";
          final String prefixContentType = "Content-Type:";

          request = inputStream.readLine();

          if (request.startsWith(prefixGet) || request.startsWith(prefixPost)) {
            if (request.startsWith(prefixGet)) {
              method = Method.Get;
              path = request.substring(prefixGet.length());
            } else {
              method = Method.Post;
              path = request.substring(prefixPost.length());
            }

            int space = path.indexOf(' ');
            if (space != -1) {
              // "GET|POST /path HTTP/1.1"
              path = path.substring(0, space);
            }
          } else if (request.startsWith(prefixContentType)) {
            postType = request.substring(prefixContentType.length()).trim();
          } else if (request.startsWith(prefixContentLength)) {
            try {
              postLength = Integer.parseInt(request.substring(prefixContentLength.length()).trim());
            } catch (NumberFormatException e) {
              postLength = -1;
            }
          }
        } while (request.length() != 0);

        if (method == Method.Get) {
          int index;
          try {
            index = Integer.parseInt(path);
          } catch (NumberFormatException e) {
            index = -1;
          }

          Uri uri = (index >= 0 && index < filesServed.size()) ? filesServed.get(index) : null;
          byte[] body = getFileContent(uri);
          String type = getFileType(uri);

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
        } else if (method == Method.Post) {
          Response response = new Response();

          try {
            byte[] content = new byte[postLength];
            inputStream.readFully(content);

            // Write the POST content to a file
            String fileName = UUID.randomUUID().toString().toUpperCase();
            Uri imagesUri =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, postType);
            Uri uri = contentResolver.insert(imagesUri, values);
            OutputStream stream = contentResolver.openOutputStream(uri);
            stream.write(content);
            response.setStatusCode(200);
            stream.close();
          } catch (NullPointerException|IOException e) {
            response.setStatusCode(500);
          }

          outputStream.write(response.getBytes());
          outputStream.close();
          socket.close();

          // TODO: add an event to notify the main activity that a file has been uploaded
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
