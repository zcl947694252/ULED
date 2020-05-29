package com.dadoutek.uled.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dadoutek.uled.R;

public class SpeedDialog extends AlertDialog implements View.OnClickListener {
    private OnSpeedListener mListener;
    private int speed;
    private ImageView cancelBtn;
    private Button okBtn;
    private SeekBar speedBar;
    private ImageView addSpeenBtn;
    private ImageView lessSpeedBtn;
    private TextView speedText;
    long downTime = 0;//Button被按下时的时间
    long thisTime = 0;//while每次循环时的时间
    boolean onBtnTouch = false;//Button是否被按下
    int tvValue = 0;//TextView中的值

    public SpeedDialog(Context context, int speed, int style, OnSpeedListener mListener) {
        super(context, style);
        this.mListener = mListener;
        this.speed = speed;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_speed_gradient);

        cancelBtn = (ImageView) findViewById(R.id.delete_cancel);
        okBtn = (Button) findViewById(R.id.okBtn);
        speedBar = (SeekBar) findViewById(R.id.sbSpeed);
        speedText = (TextView) findViewById(R.id.speed_num);

        speedBar.setProgress(speed);
        if (speed >= 100)
            speed = 100;
        if (speed==0)
            speed =1;

        speedText.setText(speed + "%");

        addSpeenBtn = findViewById(R.id.speed_add);
        lessSpeedBtn = findViewById(R.id.speed_less);

        cancelBtn.setOnClickListener(this);
        okBtn.setOnClickListener(this);

        addSpeenBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis();
                    onBtnTouch = true;
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            while (onBtnTouch) {
                                thisTime = System.currentTimeMillis();
                                if (thisTime - downTime >= 500) {
                                    tvValue++;
                                    Message msg = handler_add.obtainMessage();
                                    msg.arg1 = tvValue;
                                    handler_add.sendMessage(msg);
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    };
                    t.start();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    onBtnTouch = false;
                    if (thisTime - downTime < 500) {
                        tvValue++;
                        Message msg = handler_add.obtainMessage();
                        msg.arg1 = tvValue;
                        handler_add.sendMessage(msg);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    onBtnTouch = false;
                }
                return true;
            }
        });

        lessSpeedBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis();
                    onBtnTouch = true;
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            while (onBtnTouch) {
                                thisTime = System.currentTimeMillis();
                                if (thisTime - downTime >= 500) {
                                    tvValue++;
                                    Message msg = handler.obtainMessage();
                                    msg.arg1 = tvValue;
                                    handler.sendMessage(msg);
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    };
                    t.start();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    onBtnTouch = false;
                    if (thisTime - downTime < 500) {
                        tvValue++;
                        Message msg = handler.obtainMessage();
                        msg.arg1 = tvValue;
                        handler.sendMessage(msg);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    onBtnTouch = false;
                }
                return true;
            }

        });


        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speedText.setText(progress + 1 +  "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                this.onValueChange(seekBar, seekBar.getProgress(), true);
            }

            private void onValueChange(SeekBar seekBar, int progress, boolean b) {
                speed = progress;
                speedText.setText(speed + 1 +  "%");
                if (speed >= 100) {
                    addSpeenBtn.setEnabled(false);
                    lessSpeedBtn.setEnabled(true);
                } else if (speed <= 0) {
                    addSpeenBtn.setEnabled(true);
                    lessSpeedBtn.setEnabled(false);
                } else {
                    addSpeenBtn.setEnabled(true);
                    lessSpeedBtn.setEnabled(true);
                }
            }
        });
    }


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.e("TAG_MSG", String.valueOf(msg));
            if (speedBar.getProgress() < 100)
                speedBar.setProgress(speedBar.getProgress() - 1);
            if (speedBar.getProgress() < 1) {
                onBtnTouch = false;
            } else if (speedBar.getProgress() == 1) {
                onBtnTouch = false;
                speedText.setText(speedBar.getProgress() + 1 + "%");
                speed = speedBar.getProgress();
            } else {
                lessSpeedBtn.setEnabled(true);
                speedText.setText(speedBar.getProgress() + 1 +"%");
                speed = speedBar.getProgress();
            }

            if (speedBar.getProgress() < 100) {
                addSpeenBtn.setEnabled(true);
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handler_add = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (speedBar.getProgress() < 100) {
                int i = speedBar.getProgress() + 1;
                speedBar.setProgress(i);
            }
            if (speedBar.getProgress() > 100) {
                addSpeenBtn.setEnabled(false);
                onBtnTouch = false;
            } else if (speedBar.getProgress() == 100) {
                addSpeenBtn.setEnabled(false);
                onBtnTouch = false;
                speedText.setText(speedBar.getProgress() + 1 +  "%");
                speed = speedBar.getProgress();
            } else {
                addSpeenBtn.setEnabled(true);
                speedText.setText(speedBar.getProgress() + 1 +  "%");
                speed = speedBar.getProgress();
            }

            if (speedBar.getProgress() > 0) {
                lessSpeedBtn.setEnabled(true);
            }
        }
    };

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.okBtn) {
            mListener.speedFinished(speed);
            dismiss();
        }

        if (v.getId() == R.id.delete_cancel) {
            dismiss();
        }
    }


    public interface OnSpeedListener {
        void speedFinished(int password);
    }
}
