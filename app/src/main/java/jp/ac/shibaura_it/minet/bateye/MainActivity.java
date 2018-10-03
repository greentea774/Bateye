package jp.ac.shibaura_it.minet.bateye;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Handler handler;
    String Inbox = null; //Handlerのメッセージ受け
    public static int handlerFlag = 1; //Handlerの制御,ループ解除用

    String[] splitText ; //センサーから送られてくるデータはカンマ区切りなので、それを分けて格納する変数
    double InFSum = 0,InFAve = 0,InF = 0,UltR = 0,UltL = 0,tempUlt=0,Differential = 0; //センサー値を格納する変数,赤外線、超音波R,超音波L
    double UltRData[] = new double[10];
    double UltLData[] = new double[10];
    double DUltData[] = new double[10];
    double ObstacleLevelR=0,ObstacleLevelL=0,StepLevelZ=0,WalkLevelZ=0;
    int finalObstacleLevel=0,finalEnvironmentalLevel = 0;

    double Time_max=0,Time_min=0,WalkSpeed=0,WalkSpeedFinal=0;
    private double Threshold_max1=14.24493,Threshold_max2=11.98916, Thresold_min1=10.37416,Thresold_min2=7.445972; //閾値の設定
    double Thresold_state = 1; //閾値のステート．1は最大値待ち．2は最小値待ち
    double WalkFind = 0,tempWalkFind = 0, NoWalk = 0; //閾値を超えた回数
    double ax = 0,ay = 0,az = 0,ar = 0,tempar=0;

    public static double Time = 0;
    public static double LoopCount = 1;

    static int TenCount = 0,DTenCount = 0;

    private BluetoothAdapter mBluetoothAdapter;
    BlueToothCom BlueToothCom;
    private static final int REQUEST_ENABLE_BT = 0xffff;
    private Vibrator vib;

    int alertflag = 0;

    TextView WalkState,WalkLevel,StepState,StepLevel,ObstacleState,ObstacleLevel,EnvironmentalLevel,BTState,Thresold_Time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //BlueToothの初期設定
        vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter==null){
            Log.i("App", "Device does not support Bluetooth");
        }else{
            if(!mBluetoothAdapter.isEnabled()){
                Intent intent =new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                Log.i("App","BlueTooth is enable in this device.");
                startActivityForResult(intent ,REQUEST_ENABLE_BT);
            }
        }

        //ボタン設定
        final Button start = (Button) findViewById(R.id.start);
        final Button stop = (Button) findViewById(R.id.stop);

        // センサー初期化//
        AcSensor.Inst().onCreate(this);
        //TpSensor.Inst().onCreate(this);
        stop.setEnabled(false); //stopボタンを押せないように沈黙させる//初期状態

        //TextViewのID関連付け//
        WalkState = (TextView) findViewById(R.id.WalkState);
        WalkLevel = (TextView) findViewById(R.id.WalkLevel);
        StepState = (TextView) findViewById(R.id.StepState);
        StepLevel = (TextView) findViewById(R.id.StepLevel);
        ObstacleState = (TextView) findViewById(R.id.ObstacleState);
        ObstacleLevel = (TextView) findViewById(R.id.ObstacleLevel);
        EnvironmentalLevel = (TextView) findViewById(R.id.EnvironmentalLevel);
        BTState = (TextView) findViewById(R.id.BTState);
        Thresold_Time = (TextView) findViewById(R.id.Thresold_Time);

        //Handlerでループを回す.メインループ//
        handler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                Inbox = (String) msg.obj;
                BTState.setText("通信状態：接続");
                splitText = Inbox.split(",");//『,』ごとに値を区切って値をString splitTextに格納する
                InF = Double.parseDouble(splitText[0]); //赤外線センサー値
                UltR = Double.parseDouble(splitText[1]); //超音波センサー値R
                UltL = Double.parseDouble(splitText[2]); //超音波センサー値L

                //通信状態を画面に反映
                BTState.setText("通信状態：接続");

                LoopCount++;
                //ループ回数 / 10ミリ秒 = 経過時間//
                Time = LoopCount / 100;

/////////////////////////////////////段差検知////////////////////////////////////////////////////////////////////////////////////////////

                if(LoopCount <= 300 && LoopCount >=1 ){
                    if(LoopCount >=1 && LoopCount <= 100){
                        Thresold_Time.setText("    3");
                    }else if(LoopCount >=101 && LoopCount <= 200){
                        Thresold_Time.setText("    2");
                    }else if(LoopCount >=201 && LoopCount <= 300){
                        Thresold_Time.setText("    1");
                    }
                    //閾値設定
                    InFSum += InF;
                }else if(LoopCount == 301){
                    InFAve = InFSum / 300; //閾値
                    Thresold_Time.setText(" 完了");
                }else if(LoopCount >= 302){
                    StepLevel.setText(""+StepLevelZ);
                    //段差検知開始
                    if(InF > InFAve + 15){
                        StepState.setText("アリ（下り）");
                        StepLevelZ = 100;
                    }else{
                        StepState.setText("ナシ");
                        StepLevelZ = 0;
                    }

                }else{
                }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////障害物検知/////////////////////////////////////////////////////////////////////////////////////////


                UltRData[TenCount] = UltR;
                UltLData[TenCount] = UltL;

                //温度取得
               // Tp = (int)-TpSensor.Inst().getTp();
                //Log.i("App","温度："+Tp);


                //昇順ソートR
                for (int i=0; i<TenCount-1; i++) {
                    for (int j=i+1; j<TenCount; j++) {
                        if (UltRData[i] > UltRData[j]) {
                            double tmp = UltRData[i];
                            UltRData[i] = UltRData[j];
                            UltRData[j] = tmp;
                        }
                    }
                }

                //昇順ソートL
                for (int i=0; i<TenCount-1; i++) {
                    for (int j=i+1; j<TenCount; j++) {
                        if (UltLData[i] > UltLData[j]) {
                            double tmp = UltLData[i];
                            UltLData[i] = UltLData[j];
                            UltLData[j] = tmp;
                        }
                    }
                }

                TenCount++;

         if(TenCount == 10) {
             //25cm以下の場合は障害物ナシの判定フィルター//
             //両方検知していない
             if (UltRData[5] <= 30 && UltLData[5] <= 30) {
                 ObstacleState.setText("ナシ");
                 tempUlt = 0;
                 //finalObstacleLevel = 0;
                 //ObstacleLevel.setText("" + finalObstacleLevel);
                 //右側のみ検知
             }else if (UltLData[5] <= 30 && UltRData[5] >30) {
                 ObstacleState.setText(UltRData[5] + "cm前（右）");
                 tempUlt = UltRData[5];
                //ObstacleLevelR = -0.264 * UltRData[5] + 106.6;
                 //finalObstacleLevel = (int) ObstacleLevelR;
                 //ObstacleLevel.setText("" + finalObstacleLevel);
                 //左側のみ検知
             }else if (UltRData[5] <= 30 && UltLData[5] >30) {
                 ObstacleState.setText(UltLData[5] + "cm前（左）");
                 tempUlt = UltLData[5];
                 //ObstacleLevelL = -0.264 * UltLData[5] + 106.6;
                // finalObstacleLevel = (int) ObstacleLevelL;
                 //ObstacleLevel.setText("" + finalObstacleLevel);
                 //両方検知
             }else if (UltRData[5] > 30 && UltLData[5] >30 ) {
                 //正面
                 if((UltRData[5]-5 <= UltLData[5] && UltLData[5] <= UltRData[5]+5) || (UltLData[5]-5 <= UltRData[5] && UltRData[5] <= UltLData[5]+5)){
                     ObstacleState.setText((UltLData[5]+UltRData[5])/2 + "cm前（正面）");
                     tempUlt = (UltLData[5]+UltRData[5])/2;
                     //ObstacleLevelL = -0.264 * (UltLData[5]+UltRData[5])/2 + 106.6;
                    // finalObstacleLevel = (int) ObstacleLevelL;
                    // ObstacleLevel.setText("" + finalObstacleLevel);
                     //右が近い
                 }else if(UltRData[5] < UltLData[5]){
                     ObstacleState.setText(UltRData[5] + "cm前（右）");
                     tempUlt = UltRData[5];
                    // ObstacleLevelR = -0.264 * UltRData[5] + 106.6;
                    // finalObstacleLevel = (int) ObstacleLevelR;
                    // ObstacleLevel.setText("" + finalObstacleLevel);
                     //左が近い
                 }else if(UltLData[5] < UltRData[5]){
                     ObstacleState.setText(UltLData[5] + "cm前（左）");
                     tempUlt = UltLData[5];
                     //ObstacleLevelL = -0.264 * UltLData[5] + 106.6;
                    // finalObstacleLevel = (int) ObstacleLevelL;
                    // ObstacleLevel.setText("" + finalObstacleLevel);
                 }
             }

             //１秒あたりの変化量（平均変化率）
             //最初の10回（0.1秒）は数値が溜まっていないので、変化率を計算しない。
             if(DTenCount < 5){
                 DUltData[DTenCount] = tempUlt;
                 DTenCount++;
             }else if(DTenCount == 5){
                 DUltData[0] = DUltData[1];
                 DUltData[1] = DUltData[2];
                 DUltData[2] = DUltData[3];
                 DUltData[3] = DUltData[4];
                 DUltData[4] = DUltData[5];
                 DUltData[5] = tempUlt;
                 Differential = -1 * ((DUltData[1]-DUltData[0])+(DUltData[2]-DUltData[1])+(DUltData[3]-DUltData[2])+(DUltData[4]-DUltData[3])
                 +(DUltData[5]-DUltData[4]));

                 finalObstacleLevel = (int)Differential;
                 if(finalObstacleLevel < 0){
                     finalObstacleLevel = 0;
                 }
                 ObstacleLevel.setText("" + finalObstacleLevel);
             }

             TenCount = 0;
         }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////歩行検知///////////////////////////////////////////////////////////////////////////////////////////

                //加速度の取得//
                ax = -AcSensor.Inst().getX();
                ay = AcSensor.Inst().getY();
                az = AcSensor.Inst().getZ();
                ar = Math.sqrt(ax * ax + ay * ay + az * az);

               // WalkState.setText("Find:"+WalkFind);

                //最大値待ち状態//
                if(Thresold_state == 1) {
                    //歩行速度を歩幅から測定
                    Time_max = Time;
                    WalkSpeed = Time_max - Time_min;
                    if(ar >= Threshold_max2 && ar <= Threshold_max1){
                        if(ar - tempar <= 0) {
                            WalkFind++;
                            WalkSpeedFinal = WalkSpeed;
                            Thresold_state = 2;
                        }
                        tempar = ar;
                    }
                }else if(Thresold_state == 2) {
                    //歩行速度を歩幅から測定
                    Time_min = Time;
                    WalkSpeed = Time_min - Time_max;
                    if (ar >= Thresold_min2 && ar <= Thresold_min1) {
                        if (ar - tempar >= 0) {
                            WalkFind++;
                            //WalkSpeedFinal = WalkSpeed;
                            Thresold_state = 1;
                        }
                        tempar = ar;
                    }
                }

                //さらに閾値を超えた回数が120中40を超えたら歩きスマホ確定//
                if (WalkFind >= 10 && WalkSpeedFinal < 2) {
                    WalkState.setText("歩行中");
                    // startService(new Intent(self, TimerIntentService.class));//ダイアログ通知

                    //歩行速度
                    if(WalkSpeedFinal >= 0.35){
                        WalkState.setText("歩行中");
                        WalkLevelZ = 1; WalkLevel.setText(""+WalkLevelZ);
                    }else if(WalkSpeedFinal < 0.35 && WalkSpeedFinal >=0.22){
                        WalkState.setText("歩行中");
                        WalkLevelZ = 1; WalkLevel.setText(""+WalkLevelZ);
                    }else if(WalkSpeedFinal < 0.22){
                        WalkState.setText("歩行中");
                        WalkLevelZ = 1; WalkLevel.setText(""+WalkLevelZ);
                    }
                }

                if(WalkSpeedFinal >= 2 || WalkSpeed >= 2){
                    WalkState.setText("停止");
                    WalkLevelZ = 0; WalkLevel.setText(""+WalkLevelZ);
                    WalkFind = 0;
                }

                tempWalkFind = WalkFind;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////通信失敗処理///////////////////////////////////////////////////////////////////////////////////////

                if(Inbox == "404,404,404"){
                    Toast.makeText(MainActivity.this, "通信できませんでした", Toast.LENGTH_SHORT).show();
                    BTState.setText("通信状態：未接続");
                    handlerFlag = 1;
                    start.setEnabled(true); //startボタン復活
                }else{
                }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////環境危険度/////////////////////////////////////////////////////////////////////////////////////////

                //歩行していれば注意喚起トリガオン
                if(WalkLevelZ == 1) {
                    finalEnvironmentalLevel = (int) WalkLevelZ + (int) StepLevelZ + (int) finalObstacleLevel;
                    EnvironmentalLevel.setText("" + finalEnvironmentalLevel);
                }else{
                    finalEnvironmentalLevel = 0;
                }


                //注意喚起
                //初期設定
                if(alertflag == 0){
                    startService(new Intent(MainActivity.this, LayerService1.class));
                    alertflag = 1;
                }

                //レベル1（青）
                if(finalEnvironmentalLevel >= 0 && finalEnvironmentalLevel <= 40 && (alertflag == 2 || alertflag == 3)){
                    if(alertflag == 2){
                        stopService(new Intent(MainActivity.this, LayerService2.class));
                    }
                    if(alertflag == 3){
                        stopService(new Intent(MainActivity.this, LayerService3.class));
                    }
                    startService(new Intent(MainActivity.this, LayerService1.class));
                    alertflag = 1;
                    //レベル2（黄色）
                }else if(finalEnvironmentalLevel > 40 && finalEnvironmentalLevel <= 99 && (alertflag == 1 || alertflag == 3)){
                    if(alertflag == 1){
                        stopService(new Intent(MainActivity.this, LayerService1.class));
                    }
                    if(alertflag == 3){
                        stopService(new Intent(MainActivity.this, LayerService3.class));
                    }
                    startService(new Intent(MainActivity.this, LayerService2.class));
                    alertflag = 2;
                    //レベル3（赤）
                }else if(finalEnvironmentalLevel >= 100 && (alertflag == 1 || alertflag == 2)){
                if(alertflag == 1){
                    stopService(new Intent(MainActivity.this, LayerService1.class));
                }
                if(alertflag == 2){
                    stopService(new Intent(MainActivity.this, LayerService2.class));
                }
                startService(new Intent(MainActivity.this, LayerService3.class));
                    ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(300);
                alertflag = 3;
            }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                //ループ抜け処理//
                if (handlerFlag == 0) {
                } else {
                    //メッセージは送信されず，ループ解除//
                    LoopCount = 1;
                    Time = 0;
                    handlerFlag = 1;
                }
            }
        };//Handler

        //startボタン処理//
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                BlueToothCom = new BlueToothCom(handler, mBluetoothAdapter);
                BlueToothCom.start();
                AcSensor.Inst().SensorStart();// 開始時にセンサーを動かし始める
                handlerFlag = 0; //ループメッセージ送信許可//
                start.setEnabled(false); //startボタンを押せないように沈黙させる
                stop.setEnabled(true); //stopボタンをザオリクさせる
                Toast.makeText(MainActivity.this, "接続を開始しました", Toast.LENGTH_SHORT).show();
            }
        });
        //stopボタン処理
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                BlueToothCom.BTClose();
                AcSensor.Inst().onPause();// 中断時にセンサーを止める
                handlerFlag = 1;//ループ解除判定//
                stop.setEnabled(false); //stopボタンを押せないように沈黙させる
                start.setEnabled(true); //startボタンをザオリクさせる
                Toast.makeText(MainActivity.this, "接続切りました", Toast.LENGTH_SHORT).show();
                if(alertflag == 1){
                    stopService(new Intent(MainActivity.this, LayerService1.class));
                }else if(alertflag == 2){
                    stopService(new Intent(MainActivity.this, LayerService2.class));
                }else if(alertflag == 3){
                    stopService(new Intent(MainActivity.this, LayerService3.class));
                }
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
