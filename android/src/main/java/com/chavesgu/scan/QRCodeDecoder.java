package com.chavesgu.scan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class QRCodeDecoder {
    private static byte[] yuvs;
    public static int MAX_PICTURE_PIXEL = 256;

    public static final List<BarcodeFormat> allFormats = new ArrayList<BarcodeFormat>() {{
        add(BarcodeFormat.AZTEC);
        add(BarcodeFormat.CODABAR);
        add(BarcodeFormat.CODE_39);
        add(BarcodeFormat.CODE_93);
        add(BarcodeFormat.CODE_128);
        add(BarcodeFormat.DATA_MATRIX);
        add(BarcodeFormat.EAN_8);
        add(BarcodeFormat.EAN_13);
        add(BarcodeFormat.ITF);
        add(BarcodeFormat.MAXICODE);
        add(BarcodeFormat.PDF_417);
        add(BarcodeFormat.QR_CODE);
        add(BarcodeFormat.RSS_14);
        add(BarcodeFormat.RSS_EXPANDED);
        add(BarcodeFormat.UPC_A);
        add(BarcodeFormat.UPC_E);
        add(BarcodeFormat.UPC_EAN_EXTENSION);
    }};

    public static final Map<DecodeHintType, Object> HINTS = new EnumMap<DecodeHintType, Object>(DecodeHintType.class) {{
        put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        put(DecodeHintType.POSSIBLE_FORMATS, allFormats);
        put(DecodeHintType.CHARACTER_SET, "utf-8");
    }};

    public static String decodeQRCode(String path) {
        return syncDecodeQRCode(path);
    }

    public static String decodeQRCode(Bitmap bitmap) {
        return syncDecodeQRCode(bitmap);
    }

    public static String syncDecodeQRCode(String path) {
        Bitmap bitmap = pathToBitMap(path, MAX_PICTURE_PIXEL, MAX_PICTURE_PIXEL);
        return decodeBitmap(bitmap);
    }

    public static String syncDecodeQRCode(Bitmap bitmap) {
        return decodeBitmap(bitmap);
    }

    private static String decodeBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        byte[] mData = getYUV420sp(width, height, bitmap);
        Result result = decodeImage(mData, width, height);
        return result != null ? result.getText() : null;
    }

    private static Result decodeImage(byte[] data, int width, int height) {
        try {
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
            BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
            return new QRCodeReader().decode(bitmap, HINTS);
        } catch (NotFoundException e) {
            try {
                PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                return new QRCodeReader().decode(bitmap, HINTS);
            } catch (NotFoundException | ChecksumException | FormatException ignored) {
            }
        } catch (FormatException | ChecksumException ignored) {
        }
        return null;
    }

    private static Bitmap pathToBitMap(String imgPath, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imgPath, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static byte[] getYUV420sp(int inputWidth, int inputHeight, Bitmap scaled) {
        int[] argb = new int[inputWidth * inputHeight];
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        int requiredWidth = inputWidth % 2 == 0 ? inputWidth : inputWidth + 1;
        int requiredHeight = inputHeight % 2 == 0 ? inputHeight : inputHeight + 1;
        int byteLength = requiredWidth * requiredHeight * 3 / 2;

        if (yuvs == null || yuvs.length < byteLength) {
            yuvs = new byte[byteLength];
        } else {
            Arrays.fill(yuvs, (byte) 0);
        }

        encodeYUV420SP(yuvs, argb, inputWidth, inputHeight);
        scaled.recycle();
        return yuvs;
    }

    private static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int R, G, B;
        int argbIndex = 0;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                R = (argb[argbIndex] >> 16) & 0xff;
                G = (argb[argbIndex] >> 8) & 0xff;
                B = argb[argbIndex] & 0xff;
                argbIndex++;

                int Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                int U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                int V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                Y = Math.max(0, Math.min(Y, 255));
                U = Math.max(0, Math.min(U, 255));
                V = Math.max(0, Math.min(V, 255));

                yuv420sp[yIndex++] = (byte) Y;
                if ((j % 2 == 0) && (i % 2 == 0)) {
                    yuv420sp[uvIndex++] = (byte) V;
                    yuv420sp[uvIndex++] = (byte) U;
                }
            }
        }
    }
}