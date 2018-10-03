package jp.ac.shibaura_it.minet.bateye;

/**
 * Created by UCHIYAMA on 2015/12/21.
 */
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;


public class LayerService1 extends Service {
    private WindowManager mWindowManager;
    private FrameLayout mOverlapView;

    public void onCreate() {
        super.onCreate();

        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        mOverlapView = new FrameLayout(getApplicationContext());
        ((FrameLayout)mOverlapView).addView(layoutInflater.inflate(R.layout.overlay1, null));
        WindowManager.LayoutParams mOverlapViewParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,       // アプリケーションのTOPに配置
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |  // フォーカスを当てない(下の画面の操作がd系なくなるため)
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |        // OverlapするViewを全画面表示
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // モーダル以外のタッチを背後のウィンドウへ送信
                PixelFormat.TRANSLUCENT);  // viewを透明にする

        mWindowManager.addView(mOverlapView, mOverlapViewParams);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // サービスが破棄されるときには重ね合わせしていたViewを削除する
        mWindowManager.removeView(mOverlapView);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
}