package com.zx.vlc;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.anoah.vlcplayer.R;


/**
 * Created by ZhouXiang on 2017/10/26.
 */

public class DevActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "DevActivity";
    private final int REQUEST_CODE_STORAGE_PERMISSION = 0;//请求存储权限
    private final int REQUEST_CODE_PICK_FILE = 1;//读取本地文件

    //    private final String DEFAULT_URL = "http://download.blender.org/peach/big8buckbunny_movies/BigBuckBunny_640x360.m4v";
    private final String DEFAULT_URL = "http://gslb.miaopai.com/stream/C3TeHjIh2wW-GfVMN0xcFaMFLKUgKAtY.mp4";
//    private final String DEFAULT_URL = "https://www.quirksmode.org/html5/videos/big_buck_bunny.webm";

    //region view
    private EditText etOnline = null;
    private Button btnPlayOnline = null;
    private Button btnPlayLocal = null;
    //endregion

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        LeakCanary.install(getApplication());
        setContentView(R.layout.activity_dev);
        init();
        setListener();
    }

    private void init() {
        etOnline = (EditText) findViewById(R.id.etOnline);
        btnPlayOnline = (Button) findViewById(R.id.btnPlayOnline);
        btnPlayLocal = (Button) findViewById(R.id.btnPlayLocal);
        etOnline.setText(DEFAULT_URL);
    }

    private void setListener() {
        btnPlayOnline.setOnClickListener(this);
        btnPlayLocal.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        if (v.equals(btnPlayOnline)) {
            if (checkInput()) {
                Intent intent = new Intent();
                intent.setClass(this, VlcActivity.class);
                intent.putExtra("url", etOnline.getText().toString());
//                intent.putExtra("title", "标题-test");
                startActivity(intent);
            }
        } else if (v.equals(btnPlayLocal)) {
            if (checkStoragePermission()) {
                pickLocalVideo();
            } else {
                showReqStoragePermissionDialog();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FILE) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent();
                intent.setClass(this, VlcActivity.class);
                intent.putExtra("url", FileUtils.getPath(getApplicationContext(), data.getData()));
                startActivity(intent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickLocalVideo();
                } else {
                    showStoragePermissionDeniedDialog();
                }
                break;
        }
    }

    //检查输入
    private boolean checkInput() {
        if (etOnline.length() <= 0) {
            Toast.makeText(this, "请输入地址", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(etOnline.getText().toString().trim())) {
            Toast.makeText(this, "请输入正确地址", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    //读取本地资源
    private void pickLocalVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    //检查存储权限
    private boolean checkStoragePermission() {
        return PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    //请求权限弹窗
    private void showReqStoragePermissionDialog() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setMessage("播放本地资源需要您的授权，否则将无法播放本地视频音频资源。请在接下来的提示中，进行授权。")
                    .setTitle("提示！")
                    .setPositiveButton("知道了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(DevActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    REQUEST_CODE_STORAGE_PERMISSION);
                        }
                    })
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(DevActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_STORAGE_PERMISSION);
        }
    }

    //显示拒绝存储权限提示
    private void showStoragePermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setMessage("由于您为授权读取本地文件，无法使用该功能！")
                .setTitle("提示！")
                .setPositiveButton("确定", null)
                .create()
                .show();
    }
}
