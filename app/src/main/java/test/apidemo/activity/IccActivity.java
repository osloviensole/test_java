package test.apidemo.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.ctk.sdk.PosApiHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Created by Administrator on 2021/03/17.
 */


public class IccActivity extends Activity implements View.OnClickListener {

    private final String TAG = "IccActivity";
    private boolean isIccChecked = false;
    private boolean isPsam1Checked =false;
    private boolean isPsam2Checked =false;
    private RadioGroup cardTypeRadioGroup = null;
    private RadioButton radioButtonPsam = null;
    private Button TestButton = null;

    private byte ATR[] = new byte[40];
    private byte vcc_mode = 1;
    private int ret;

    private WorkHandler mWorkHandler;
    private HandlerThread mWorkThread;

    TextView tv_msg = null;
    PosApiHelper posApiHelper = PosApiHelper.getInstance();

    private static byte CARD_SLOT_ICC = 0;
    private static byte CARD_SLOT_PSAM1 = 1;
    private static byte CARD_SLOT_PSAM2 = 2;

    private class WorkHandler extends Handler {
        public static final int MSG_WORK_PSAM_ACTION = 1 << 0;

        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case MSG_WORK_PSAM_ACTION:
                    startTestIcc(CARD_SLOT_PSAM1);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //无title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);

        setContentView(R.layout.activity_icc);

        cardTypeRadioGroup = (RadioGroup) this.findViewById(R.id.rg_card_type);
        cardTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId) {
                    case R.id.RadioButton_psam1:
                        tv_msg.setText("Psam Checked");
                        break;

                }
            }
        });

        tv_msg = (TextView) this.findViewById(R.id.tv_msg);

        TestButton = (Button) findViewById(R.id.button_SingleTest);
        TestButton.setOnClickListener(IccActivity.this);

        radioButtonPsam = (RadioButton) findViewById(R.id.RadioButton_psam1);
        radioButtonPsam.setChecked(true);

        //star a thread for psam action
        mWorkThread = new HandlerThread("sdk_psam_thread");
        mWorkThread.start();
        mWorkHandler = new WorkHandler(mWorkThread.getLooper());


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWorkHandler.removeCallbacksAndMessages(null);
        mWorkThread.quitSafely();

    }


    String strInfo = "";
    void startTestIcc(byte slot) {
        ret = 1;
        byte dataIn[] = new byte[512];

        ret = posApiHelper.IccOpen(slot, vcc_mode, ATR);
        if (ret != 0) {
            runOnUiThread(new Runnable() {
                public void run() {
                    tv_msg.setText("Open Failed");
                }
            });
            Log.e(TAG, "IccOpen failed!");
            return;
        }

        String atrString = "";
        for (int i = 0; i < ATR.length; i++) {
            atrString += Integer.toHexString(Integer.valueOf(String.valueOf(ATR[i]))).replaceAll("f", "");
        }
        Log.d(TAG, "atrString = " + atrString);

        byte cmd[] = new byte[4];
        short lc = 0;
        short le = 0;

        cmd[0] = 0x00;            //0-3 cmd
        cmd[1] = (byte) 0x84;
        cmd[2] = 0x00;
        cmd[3] = 0x00;
        lc = 0x00;
        le = 0x08;
        String sendmsg = "";
        dataIn = sendmsg.getBytes();
        Log.e("liuhao Icc  " ,"PSAM *******");


        ApduSend mApduSend = new ApduSend(cmd, lc, dataIn, le);
        ApduResp mApduResp = null;
        byte[] resp = new byte[516];

        ret = posApiHelper.IccCommand(slot, mApduSend.getBytes(), resp);
        if (0 == ret) {
            mApduResp = new ApduResp(resp);
            strInfo = ByteUtil.bytearrayToHexString(mApduResp.DataOut, mApduResp.LenOut) + "SWA:"
                    + ByteUtil.byteToHexString(mApduResp.SWA) + " SWB:" + ByteUtil.byteToHexString(mApduResp.SWB);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_msg.setText(strInfo);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_msg.setText("Command Failed");
                }
            });
            Log.e(TAG, "Icc_Command failed!");
        }

       posApiHelper.IccClose(slot);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    //Converting a string of hex character to bytes
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2){
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }



    public void onClick(View v) {

        tv_msg.setText("");

        switch (v.getId()){
            case R.id.button_SingleTest:
                mWorkHandler.sendEmptyMessage(WorkHandler.MSG_WORK_PSAM_ACTION);
                break;
        }
    }




}
