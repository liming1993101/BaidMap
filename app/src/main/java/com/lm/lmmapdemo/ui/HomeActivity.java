package com.lm.lmmapdemo.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.lm.lmmapdemo.R;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener{

    private Button mBtMap;
    private Button mBtNavigation;
    private Button mBtNearBy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initView();
    }

    private void initView() {
        mBtMap= (Button) findViewById(R.id.mode_map1);
        mBtNavigation= (Button) findViewById(R.id.mode_map2);
        mBtNearBy= (Button) findViewById(R.id.mode_map3);
        mBtNearBy.setOnClickListener(this);
        mBtMap.setOnClickListener(this);
        mBtNavigation.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.mode_map1:
                Log.v("事件地图","没执行");
                startActivity(new Intent(HomeActivity.this,MainActivity.class));

                break;
            case R.id.mode_map2:
               startActivity(new Intent(this,NavigationActivity.class));
                break;
        }

    }
}
