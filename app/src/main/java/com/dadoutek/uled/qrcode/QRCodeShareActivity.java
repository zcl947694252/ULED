package com.dadoutek.uled.qrcode;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import com.dadoutek.uled.R;
import com.dadoutek.uled.base.TelinkBaseActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;


public final class QRCodeShareActivity extends TelinkBaseActivity {

    private ImageView qr_image;
    private Handler mGeneratorHandler;
    QRCodeGenerator mQrCodeGenerator;
    private final static int Request_Code_Scan = 1;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_place_share);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        qr_image =  this.findViewById(R.id.qr_image);
        TextView title =  this.findViewById(R.id.txt_header_title);
        title.setText("Share");
        findViewById(R.id.act_share_other).setOnClickListener(v -> startActivityForResult(new Intent(QRCodeShareActivity.this, QRCodeScanActivity.class), Request_Code_Scan));

       /* mGeneratorHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == QRCodeGenerator.QRCode_Generator_Success) {
                    if (mQrCodeGenerator.getResult() != null)
                        qr_image.setImageBitmap(mQrCodeGenerator.getResult());
                } else {
                    showToast("qr code data error!");
                }
            }
        };
        mQrCodeGenerator = new QRCodeGenerator(mGeneratorHandler);
        mQrCodeGenerator.execute();*/
        /**
         * 生成不带logo的二维码图片
         */
        QRCodeDataOperator dataProvider = new QRCodeDataOperator();
        String src = dataProvider.provideStr();
        Bitmap mBitmap = CodeUtils.createImage(src, 400, 400, null);
        qr_image.setImageBitmap(mBitmap);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

   /* @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Request_Code_Scan && resultCode == RESULT_OK) {
            finish();
        }
    }*/
}
