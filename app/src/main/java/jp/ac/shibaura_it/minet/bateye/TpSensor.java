package jp.ac.shibaura_it.minet.bateye;

/**
 * Created by UCHIYAMA on 2015/12/21.
 */
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

public class TpSensor implements SensorEventListener {

    private SensorManager _sensorManager = null;
    private float Data=0;

    public void onCreate(Context c) {
        _sensorManager = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);//センサーマネージャを取得
        SensorStart();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //使用しない。//
    }

    //センサ起動//
    public void SensorStart() {
        List<Sensor> sensorList = _sensorManager.getSensorList(Sensor.TYPE_AMBIENT_TEMPERATURE);//センサーリストを取得
        if (sensorList != null && !sensorList.isEmpty()) {
            Sensor sensor = sensorList.get(0);
            _sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);//リスナー登録
        }
    }

    //センサを止める//
    public void onPause() {
        if( _sensorManager == null ){
            return;
        }
        _sensorManager.unregisterListener(this);
    }

    //センサーの値に変化があった時呼ばれる
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            Data = event.values[SensorManager.SENSOR_TEMPERATURE];
        }
    }

    public float getTp(){
        return Data;
    }


    //シングルトン
    private static TpSensor _instance = new TpSensor();

    public static TpSensor Inst() {
        return _instance;
    }

}