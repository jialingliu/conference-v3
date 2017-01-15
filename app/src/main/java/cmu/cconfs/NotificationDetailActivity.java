package cmu.cconfs;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;

import org.w3c.dom.Text;

import java.util.HashMap;

import cmu.cconfs.model.parseModel.Profile;
import cmu.cconfs.parseUtils.helper.CloudCodeUtils;
import cmu.cconfs.service.FCMRegistrationService;
import cmu.cconfs.utils.PreferencesManager;

public class NotificationDetailActivity extends AppCompatActivity {
    private final static String TAG = NotificationActivity.class.getSimpleName();
    public final static String EXTRA_NOTI_DATA = "data";

    private TextView mDetailTypeTv;
    private TextView mTitleTv;
    private TextView mDetailTv;
    private Button mAcceptBtn;
    private Button mRejectBtn;
    private Button mReplyBtn;

    private AppointmentActivity.NotificationPayload mAppointmentNotificationPayload;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_result);

        mDetailTypeTv = (TextView) findViewById(R.id.detail_type_label);

        mTitleTv = (TextView) findViewById(R.id.noti_title_tv);
        mDetailTv = (TextView) findViewById(R.id.noti_detail_tv);
        mAcceptBtn = (Button) findViewById(R.id.acc_btn);
        mRejectBtn = (Button) findViewById(R.id.rej_btn);

        mAcceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: save an appointment for both users

                // notify sender about the acceptance
                CloudCodeUtils.sendNotification("Appointment with " + ParseUser.getCurrentUser().getString(Profile.FULL_NAME_KEY),
                        "Status: accepted",
                        mAppointmentNotificationPayload.getSenderUsername(),
                        CloudCodeUtils.APPOINTMENT_ACCEPT_MSG_TYPE);
                finish();
            }
        });
        mRejectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // notify sender about the rejection
                CloudCodeUtils.sendNotification("Appointment with " + ParseUser.getCurrentUser().getString(Profile.FULL_NAME_KEY),
                        "Status: rejected",
                        mAppointmentNotificationPayload.getSenderUsername(),
                        CloudCodeUtils.APPOINTMENT_REJECT_MSG_TYPE);
                finish();

            }
        });

        HashMap<String, String> data = (HashMap<String, String>) getIntent().getSerializableExtra(EXTRA_NOTI_DATA);
        Log.d(TAG, "Get intent extra: " + data);

        String msgType = data.get("type");
        switch (msgType) {
            case CloudCodeUtils.APPOINTMENT_REQUEST_MSG_TYPE:
                mAppointmentNotificationPayload = AppointmentActivity.NotificationPayload.fromJsonStr(data.get("body"));
                mDetailTypeTv.setText("Appointment");
                mTitleTv.setText(mAppointmentNotificationPayload.getSubject());
                mDetailTv.setText(mAppointmentNotificationPayload.toString());
                break;
            case CloudCodeUtils.NORMAL_MESSAGE_MSG_TYPE:
                mDetailTypeTv.setText("Message");
                mAcceptBtn.setVisibility(View.GONE);
                mRejectBtn.setVisibility(View.GONE);
                break;
        }
    }

}
