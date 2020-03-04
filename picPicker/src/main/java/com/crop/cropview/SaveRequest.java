package com.crop.cropview;

import android.graphics.Bitmap;
import android.net.Uri;
import com.crop.cropview.callback.SaveCallback;
import io.reactivex.Single;

public class SaveRequest {

  private FreeCropImageView cropImageView;
  private Bitmap image;
  private Bitmap.CompressFormat compressFormat;
  private int compressQuality = -1;

  public SaveRequest(FreeCropImageView cropImageView, Bitmap image) {
    this.cropImageView = cropImageView;
    this.image = image;
  }

  public SaveRequest compressFormat(Bitmap.CompressFormat compressFormat) {
    this.compressFormat = compressFormat;
    return this;
  }

  public SaveRequest compressQuality(int compressQuality) {
    this.compressQuality = compressQuality;
    return this;
  }

  private void build() {
    if (compressFormat != null) {
      cropImageView.setCompressFormat(compressFormat);
    }
    if (compressQuality >= 0) {
      cropImageView.setCompressQuality(compressQuality);
    }
  }

  public void execute(Uri saveUri, SaveCallback callback) {
    build();
    cropImageView.saveAsync(saveUri, image, callback);
  }

  public Single<Uri> executeAsSingle(Uri saveUri) {
    build();
    return cropImageView.saveAsSingle(image, saveUri);
  }
}
