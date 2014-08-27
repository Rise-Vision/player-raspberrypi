package com.risevision.risecache.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpHeader implements HttpConstants {
    public static final String METHOD_GET = "GET";
    public static final String METHOD_HEAD = "HEAD";
    public static final String METHOD_POST = "POST";

    private static final String RANGE = "Range";

    public String method;
    public String file;
    public String version;

    public Map<String, String> headers = new LinkedHashMap<String, String>();

    public int getContentLength() {
      String len = headers.get(HEADER_CONTENT_LENGTH);

      try {
        return len == null ? -1 : Integer.parseInt(len);
      } catch (NumberFormatException nfe) {
        return -1;
      }
    }

    public List<int[]> getRanges() {
      // Range: bytes=0-99,500-1499,4000-
      if (headers.containsKey(RANGE)) {
        String rangeStr = headers.get(RANGE);

        if (rangeStr.startsWith("bytes=")) {
          rangeStr = rangeStr.substring("bytes=".length());

          String[] strs = rangeStr.split(",");

          List<int[]> result = new ArrayList<int[]>();

          for (String str : strs) {
            int index = str.indexOf('-');

            try {
              if (index == 0) {
                result.add(new int[] {0, Integer.parseInt(str.substring(1))});
              } else if (index == str.length() - 1) {
                result.add(new int[] {Integer.parseInt(str.substring(0, index)), -1});
              } else if (index != -1) {
                result.add(new int[] {
                    Integer.parseInt(str.substring(0, index)),
                    Integer.parseInt(str.substring(index + 1))});
              }
            } catch (NumberFormatException nfe) {

            }
          }

          return result;
        }
      }

      return null;
    }

    @Override
    public String toString() {
      return "[" + method + " " + file + " " + version + "]";
    }

    void parseGetParams() {
      // Check for a GET request that contains getter parameters.
      if (file != null && file.indexOf('?') != -1) {
        @SuppressWarnings("unused")
        String params = file.substring(file.indexOf('?') + 1);

        file = file.substring(0, file.indexOf('?'));
      }
    }
  }
