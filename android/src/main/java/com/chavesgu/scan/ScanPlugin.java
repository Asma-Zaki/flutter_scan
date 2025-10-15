package com.chavesgu.scan;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import static android.content.Context.VIBRATOR_SERVICE;

/** ScanPlugin */
public class ScanPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  private MethodChannel channel;
  private Activity activity;
  private FlutterPluginBinding flutterPluginBinding;
  private Result _result;
  private QrCodeAsyncTask task;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    this.flutterPluginBinding = flutterPluginBinding;
  }

  private void configChannel(ActivityPluginBinding binding) {
    activity = binding.getActivity();
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "chavesgu/scan");
    channel.setMethodCallHandler(this);
    flutterPluginBinding.getPlatformViewRegistry()
            .registerViewFactory("chavesgu/scan_view", new ScanViewFactory(
                    flutterPluginBinding.getBinaryMessenger(),
                    flutterPluginBinding.getApplicationContext(),
                    activity,
                    binding
            ));
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    configChannel(binding);
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    configChannel(binding);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {}

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    this.flutterPluginBinding = null;
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
    if (channel != null) {
      channel.setMethodCallHandler(null);
    }
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    _result = result;
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + Build.VERSION.RELEASE);
    } else if (call.method.equals("parse")) {
      String path = (String) call.arguments;
      task = new QrCodeAsyncTask(this, path);
      task.execute(path);
    } else {
      result.notImplemented();
    }
  }

  static class QrCodeAsyncTask extends AsyncTask<String, Integer, String> {
    private final WeakReference<ScanPlugin> mWeakReference;
    private final String path;

    public QrCodeAsyncTask(ScanPlugin plugin, String path) {
      mWeakReference = new WeakReference<>(plugin);
      this.path = path;
    }

    @Override
    protected String doInBackground(String... strings) {
      // ZXing-only decoding (no context needed)
      return QRCodeDecoder.decodeQRCode(path);
    }

    @Override
    protected void onPostExecute(String s) {
      super.onPostExecute(s);
      ScanPlugin plugin = mWeakReference.get();
      if (plugin != null && plugin._result != null) {
        plugin._result.success(s);
        plugin.task = null;

        if (s != null) {
          Vibrator myVib = (Vibrator) plugin.flutterPluginBinding.getApplicationContext().getSystemService(VIBRATOR_SERVICE);
          if (myVib != null) {
            if (Build.VERSION.SDK_INT >= 26) {
              myVib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
              myVib.vibrate(50);
            }
          }
        }
      }
    }
  }
}