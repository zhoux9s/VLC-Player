package com.zx.vlc;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.anoah.vlcplayer.R;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;

/**
 * Created by ZhouXiang on 2017/10/24.
 */
public class VlcActivity extends AppCompatActivity implements View.OnClickListener, View.OnLayoutChangeListener {
    private final String TAG = "VlcActivity";
    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;

    //region view
    private SurfaceView svVideo = null;
    private ImageView ivMusicBg = null;
    private FrameLayout flOverlay = null;
    private TextView tvTitle = null;
    private ImageView ivLoading = null;
    private ImageView ivPause = null;
    private ImageView ivPlay = null;
    private LinearLayout llProgress = null;
    private SeekBar sbProgress = null;
    private TextView tvTime = null;
    private TextView tvLength = null;
    //endregion

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null && getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vlc);
        init();
        setListeners();
        handleIntent();
    }

    private void init() {
        svVideo = (SurfaceView) findViewById(R.id.svVideo);
        ivMusicBg = (ImageView) findViewById(R.id.ivMusicBg);
        flOverlay = (FrameLayout) findViewById(R.id.flOverlay);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        ivLoading = (ImageView) findViewById(R.id.ivLoading);
        ivPause = (ImageView) findViewById(R.id.ivPause);
        ivPlay = (ImageView) findViewById(R.id.ivPlay);
        llProgress = (LinearLayout) findViewById(R.id.llProgress);
        sbProgress = (SeekBar) findViewById(R.id.sbProgress);
        tvTime = (TextView) findViewById(R.id.tvTime);
        tvLength = (TextView) findViewById(R.id.tvLength);
        sbProgress.setMax(Integer.MAX_VALUE);

        mLibVLC = new LibVLC(this);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        if (!mMediaPlayer.getVLCVout().areViewsAttached()) {
            mMediaPlayer.getVLCVout().setVideoView(svVideo);
            mMediaPlayer.getVLCVout().attachViews();
        }
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent == null || TextUtils.isEmpty(intent.getStringExtra("url"))) {
            new AlertDialog.Builder(this)
                    .setMessage("视频地址不能为空")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            String urlStr = intent.getStringExtra("url");
            if (urlStr.startsWith("/")) {
                urlStr = "file://" + urlStr;
            }
            if (isMusic(urlStr)) {
                ivMusicBg.setImageResource(R.drawable.music_bg);
            }
            mMediaPlayer.setMedia(new Media(mLibVLC, Uri.parse(urlStr)));
            mMediaPlayer.play();

            //标题
            if (intent.hasExtra("title")) {
                tvTitle.setVisibility(View.VISIBLE);
                tvTitle.setText(intent.getStringExtra("title").trim());
            } else {
                String[] strings = urlStr.split("/");
                String name = strings[strings.length - 1].trim();
                if (TextUtils.isEmpty(name)) {
                    tvTitle.setVisibility(View.GONE);
                } else {
                    tvTitle.setVisibility(View.VISIBLE);
                    tvTitle.setText(name);
                }
            }
        }
    }

    private void setListeners() {
        mMediaPlayer.setEventListener(new MediaListener(this));
        flOverlay.setOnClickListener(this);
        ivPause.setOnClickListener(this);
        ivPlay.setOnClickListener(this);
        sbProgress.setOnSeekBarChangeListener(new SeekBarListener(this));
        svVideo.addOnLayoutChangeListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mMediaPlayer.getVLCVout().areViewsAttached()) {
            mMediaPlayer.getVLCVout().setVideoView(svVideo);
            mMediaPlayer.getVLCVout().attachViews();
        }
        if (mMediaPlayer.getPlayerState() == Media.State.Paused) {
            handleOverlay();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMediaPlayer.pause();
        mMediaPlayer.getVLCVout().detachViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            svVideo.removeOnLayoutChangeListener(this);
            mMediaPlayer.release();
            mLibVLC.release();
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //是否为音乐类型
    private boolean isMusic(String urlStr) {
        return urlStr.endsWith(".aac") || urlStr.endsWith(".AAC") || urlStr.endsWith(".ac3") || urlStr.endsWith(".AC3")
                || urlStr.endsWith(".aiff") || urlStr.endsWith(".AIFF") || urlStr.endsWith(".amr") || urlStr.endsWith(".AMR")
                || urlStr.endsWith(".m4a") || urlStr.endsWith(".M4A") || urlStr.endsWith(".mp2") || urlStr.endsWith(".MP2")
                || urlStr.endsWith(".mp3") || urlStr.endsWith(".MP3") || urlStr.endsWith(".ogg") || urlStr.endsWith(".OGG")
                || urlStr.endsWith(".ra") || urlStr.endsWith(".RA") || urlStr.endsWith(".au") || urlStr.endsWith(".AU")
                || urlStr.endsWith(".wav") || urlStr.endsWith(".WAV") || urlStr.endsWith(".wma") || urlStr.endsWith(".WMA")
                || urlStr.endsWith(".mka") || urlStr.endsWith(".MKA") || urlStr.endsWith(".flac") || urlStr.endsWith(".FLAC")
                || urlStr.endsWith(".wav") || urlStr.endsWith(".WAV");
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        mMediaPlayer.getVLCVout().setWindowSize(right - left, bottom - top);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(flOverlay)) {
            handleOverlay();
        } else if (v.equals(ivPause)) {
            mLastCtrlTime = SystemClock.elapsedRealtime();
            ivPause.setVisibility(View.INVISIBLE);
            ivPlay.setVisibility(View.VISIBLE);
            mMediaPlayer.pause();
        } else if (v.equals(ivPlay)) {
            mLastCtrlTime = SystemClock.elapsedRealtime();
            ivPlay.setVisibility(View.INVISIBLE);
            llProgress.setVisibility(View.GONE);
            tvTitle.setVisibility(View.GONE);
            mMediaPlayer.play();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("退出播放吗？")
                .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setNegativeButton("再看看", null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        handleOverlay();
                    }
                })
                .create()
                .show();
        mMediaPlayer.pause();
    }

    //隐藏所有控制组件
    private void dismissAllCtrlView() {
        try {
            ivPause.setVisibility(View.INVISIBLE);
            ivPlay.setVisibility(View.INVISIBLE);
            llProgress.setVisibility(View.GONE);
            tvTitle.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //上次操作时间（屏幕、暂停、播放、进度条）
    private long mLastCtrlTime = 0;

    //显示隐藏遮盖层
    private void handleOverlay() {
        if (mMediaPlayer.getPlayerState() == Media.State.Paused) {
            if (ivPlay.getVisibility() == View.VISIBLE) {
                ivPlay.setVisibility(View.INVISIBLE);
            } else {
                ivPlay.setVisibility(View.VISIBLE);
            }
        } else {
            if (ivPause.getVisibility() == View.VISIBLE) {
                ivPause.setVisibility(View.INVISIBLE);
            } else {
                ivPause.setVisibility(View.VISIBLE);
            }
        }
        if (llProgress.getVisibility() == View.VISIBLE) {
            llProgress.setVisibility(View.GONE);
        } else {
            llProgress.setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(tvTitle.getText())) {
            if (tvTitle.getVisibility() == View.VISIBLE) {
                tvTitle.setVisibility(View.GONE);
            } else {
                tvTitle.setVisibility(View.VISIBLE);
            }
        }

        //延时后隐藏view
        mLastCtrlTime = SystemClock.elapsedRealtime();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (SystemClock.elapsedRealtime() - mLastCtrlTime < 1000 * 3) {
                    SystemClock.sleep(1000);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissAllCtrlView();
                    }
                });
            }
        }).start();
    }

    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
        private WeakReference<VlcActivity> mWeakActivity = null;

        public SeekBarListener(VlcActivity activity) {
            mWeakActivity = new WeakReference<>(activity);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && mWeakActivity.get() != null) {
                long curM = (long) (mWeakActivity.get().mMediaPlayer.getLength() *
                        ((float) progress) / ((float) seekBar.getMax()));
                mWeakActivity.get().tvTime.setText(TimeConvertUtil.millisToString(curM));
                mWeakActivity.get().mLastCtrlTime = SystemClock.elapsedRealtime();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mWeakActivity.get() != null) {
                long curM = (long) (mWeakActivity.get().mMediaPlayer.getLength() *
                        ((float) seekBar.getProgress()) / ((float) seekBar.getMax()));
                mWeakActivity.get().tvTime.setText(TimeConvertUtil.millisToString(curM));
                mWeakActivity.get().mLastCtrlTime = SystemClock.elapsedRealtime();
                mWeakActivity.get().mMediaPlayer.setTime(curM);
                mWeakActivity.get().mMediaPlayer.play();
            }
        }
    }

    private class MediaListener implements MediaPlayer.EventListener {
        private WeakReference<VlcActivity> mWeakActivity = null;
        private boolean mIsLoading = false;//加载中？

        public MediaListener(VlcActivity activity) {
            mWeakActivity = new WeakReference<>(activity);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            if (mWeakActivity.get() != null) {
                VlcActivity activity = mWeakActivity.get();
                switch (event.type) {
                    case MediaPlayer.Event.MediaChanged:
                    case MediaPlayer.Event.Opening:
                        break;
                    case MediaPlayer.Event.Buffering:
                        startLoadingAnim();
                        break;
                    case MediaPlayer.Event.Playing:
                        activity.tvLength.setText(
                                TimeConvertUtil.millisToString(activity.mMediaPlayer.getLength()));
                        stopLoadingAnim();
                        break;
                    case MediaPlayer.Event.Paused:
                        stopLoadingAnim();
                        break;
                    case MediaPlayer.Event.Stopped:
                        stopLoadingAnim();
                        activity.finish();
                        break;
                    case MediaPlayer.Event.EndReached:
                        stopLoadingAnim();
                        break;
                    case MediaPlayer.Event.EncounteredError:
                        stopLoadingAnim();
                        Toast.makeText(activity, "无法播放该视频！", Toast.LENGTH_SHORT).show();
                        break;
                    case MediaPlayer.Event.TimeChanged:
                        activity.tvTime.setText(
                                TimeConvertUtil.millisToString(activity.mMediaPlayer.getTime()));
                        activity.sbProgress.setProgress((int) ((activity.mMediaPlayer.getTime() *
                                activity.sbProgress.getMax()) / activity.mMediaPlayer.getLength()));
                    case MediaPlayer.Event.PositionChanged:
                        stopLoadingAnim();
                        break;
                    case MediaPlayer.Event.Vout:
                        break;
                }
            }
        }

        //开始加载动画
        private void startLoadingAnim() {
            VlcActivity activity = mWeakActivity.get();
            if (activity == null) return;
            if (mIsLoading) {
                //Do nothing.
            } else {
                mIsLoading = true;
                AnimationSet animationSet = new AnimationSet(true);
                RotateAnimation rotateAnimation =
                        new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setDuration(1500);
                rotateAnimation.setInterpolator(new DecelerateInterpolator());
                rotateAnimation.setRepeatCount(RotateAnimation.INFINITE);
                animationSet.addAnimation(rotateAnimation);
                activity.ivLoading.setVisibility(View.VISIBLE);
                activity.ivLoading.startAnimation(animationSet);

                activity.flOverlay.setEnabled(false);//加载过程禁用其他操作
                activity.dismissAllCtrlView();
            }
        }

        //停止加载动画
        private void stopLoadingAnim() {
            VlcActivity activity = mWeakActivity.get();
            if (activity == null) return;

            if (mIsLoading) {
                mIsLoading = false;
                activity.ivLoading.setVisibility(View.INVISIBLE);
                activity.ivLoading.clearAnimation();

                activity.flOverlay.setEnabled(true);
            } else {
                //Do nothing.
            }
        }
    }
}
