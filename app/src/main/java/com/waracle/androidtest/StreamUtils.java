package com.waracle.androidtest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Riad on 20/05/2015.
 */
public class StreamUtils {
    private static final String TAG = StreamUtils.class.getSimpleName();

    // Can you see what's wrong with this???
    public static byte[] readInputStream(InputStream stream) throws IOException {
        // Read in stream of bytes. We need to have a ByteArrayOutputStream to write the inputStream. I return a byte array
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        // Removed as redundant code
        /*ArrayList<Byte> data = new ArrayList<>();
        while (true) {
            int result = stream.read();
            if (result == -1) {
                break;
            }
            data.add((byte) result);
        }*/
        int next = stream.read();
        // Removed as redundant code
        // Convert ArrayList<Byte> to byte[]
        //byte[] bytes = new byte[data.size()];
        /*for (int i = 0; i < bytes.length; i++) {
            bytes[i] = data.get(i);
        }*/
        while (next > -1) {
            buffer.write(next);
            next = stream.read();
        }
        buffer.flush();
        // Return the raw byte array.
        return buffer.toByteArray();
    }

    // removed as redundant code
    /*public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }*/
}
