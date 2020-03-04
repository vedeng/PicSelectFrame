package com.crop.cropview;

import android.graphics.RectF;
import android.net.Uri;
import com.crop.cropview.callback.LoadCallback;
import io.reactivex.Completable;

public class LoadRequest {

  private float initialFrameScale;
  private RectF initialFrameRect;
  private boolean useThumbnail;
  private FreeCropImageView cropImageView;
  private Uri sourceUri;

  public LoadRequest(FreeCropImageView cropImageView, Uri sourceUri) {
    this.cropImageView = cropImageView;
    this.sourceUri = sourceUri;
  }

  public LoadRequest initialFrameScale(float initialFrameScale) {
    this.initialFrameScale = initialFrameScale;
    return this;
  }

  public LoadRequest initialFrameRect(RectF initialFrameRect) {
    this.initialFrameRect = initialFrameRect;
    return this;
  }

  public LoadRequest useThumbnail(boolean useThumbnail) {
    this.useThumbnail = useThumbnail;
    return this;
  }

  public void execute(LoadCallback callback) {
    if (initialFrameRect == null) {
      cropImageView.setInitialFrameScale(initialFrameScale);
    }
    cropImageView.loadAsync(sourceUri, useThumbnail, initialFrameRect, callback);
  }

  public Completable executeAsCompletable() {
    if (initialFrameRect == null) {
      cropImageView.setInitialFrameScale(initialFrameScale);
    }
    return cropImageView.loadAsCompletable(sourceUri, useThumbnail, initialFrameRect);
  }
}
