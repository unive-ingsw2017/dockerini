package it.unive.dockerini.openbikes.activities;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.coreutils.BuildConfig;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

public class InfoActivity extends AppCompatActivity {

    public String credits(Context ctx) {
        ApplicationInfo ai = ctx.getApplicationInfo();
        StringBuffer buf = new StringBuffer();
        buf.append("\tVERSION.RELEASE {").append(Build.VERSION.RELEASE).append("}");
        buf.append("\n\tVERSION.INCREMENTAL {").append(Build.VERSION.INCREMENTAL).append("}");
        buf.append("\n\tVERSION.SDK {").append(Build.VERSION.SDK_INT).append("}");
        buf.append("\n\tBOARD {").append(Build.BOARD).append("}");
        buf.append("\n\tBRAND {").append(Build.BRAND).append("}");
        buf.append("\n\tDEVICE {").append(Build.DEVICE).append("}");
        buf.append("\n\tFINGERPRINT {").append(Build.FINGERPRINT).append("}");
        buf.append("\n\tHOST {").append(Build.HOST).append("}");
        buf.append("\n\tID {").append(Build.ID).append("}");
        return "Applicazione creata da: \n" +
                getString(R.string.credits_authors)+"\n\n" +
                "--- APP ---\n" +
                ctx.getString(ai.labelRes) + " " + BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE+"\n"+
                "--- ANDROID ---\n"+
                buf;
    }

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_info);
        TextView tv_1 = (TextView) findViewById(R.id.textView_1);
        tv_1.setText(credits(this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
