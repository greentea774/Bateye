package jp.ac.shibaura_it.minet.bateye;

/**
 * Created by UCHIYAMA on 2015/12/10.
 */
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;


public class BlueToothCom extends Thread{

    MainActivity ma = new MainActivity();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBtDevice;
    private BluetoothSocket mBtSocket;
    private DataInputStream DIS;
    String period = null;
    boolean flag =true;
    private boolean logCheck = false;

    Handler handler ;
    private final UUID MY_UUID = UUID.fromString( "00001101-0000-1000-8000-00805F9B34FB");

    public BlueToothCom( Handler UIHandler, BluetoothAdapter btAdapter){
        this.handler = UIHandler;
        this.mBluetoothAdapter = btAdapter;

        //↓BlueToothのMacアドレスを指定。モノによって変える！//
        mBtDevice = mBluetoothAdapter.getRemoteDevice("00:06:66:73:E6:5F");

        try{
            mBtSocket =mBtDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        }catch(IOException e){
            e.printStackTrace();
        }
    }


    public void run(){
        try{
            mBtSocket.connect();
            DIS = new DataInputStream(mBtSocket.getInputStream());
            try{
                if( mBtDevice == null){
                    Log.i("BT","接続なし");
                    return;
                }
                while(true) {
                    @SuppressWarnings("deprecation")
                    String readLine = DIS.readLine();
                    //Log.i("BT:Sensor:", readLine + "cm");

                    //handlerへデータを送信
                    Message msg = Message.obtain();
                    msg.obj = readLine;
                    handler.sendMessageDelayed(msg,10);
                }
            }catch(IOException e){
            }
        }catch(IOException connectException){
            try {
                //connectに失敗したら閉じる//
                mBtSocket.close();
                Log.i("BT", "TIME OUT or Can not find a BlueTooth Device");

                //通信できませんでしたメッセージを送る//
                Message msg = Message.obtain();
                msg.obj = "404,404,404";
                handler.sendMessage(msg);

            }catch(IOException closeException) {}
        }
    }
    public void setLogCheck(){
        if (this.getLogCheck() == false){
            this.logCheck = true;
        }else{
            this.logCheck = false;
        }
    }

    public boolean getLogCheck(){
        return this.logCheck;
    }

    public void chngeFlag(boolean flag){
        this.flag =flag;
    }

    public void BTClose() {
        // TODO 自動生成されたメソッド・スタブ
        try {
            mBtSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
