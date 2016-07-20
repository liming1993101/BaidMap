package com.lm.lmmapdemo.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.lm.lmmapdemo.R;
import com.lm.lmmapdemo.ui.widget.TitlePopup;
import com.lm.lmmapdemo.utils.ActionItem;

public class MainActivity extends BaseActivity implements View.OnClickListener,TitlePopup.OnItemOnClickListener {

    private MapView mMapView;
    private MyLocationConfiguration.LocationMode mLocatinMode;
    private Button mModeBt;
    private Button mModeBt1;
    private BaiduMap mBaiduMap;

    private boolean isFirstLoc = true;
    private TitlePopup titlePopup;
    private BitmapDescriptor mCurrentMarker;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    @Override
    public void getLocation(BDLocation location) {

        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(location.getRadius())
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(100).latitude(location.getLatitude())
                .longitude(location.getLongitude()).build();
        mBaiduMap.setMyLocationData(locData);
        if (isFirstLoc) {
            isFirstLoc = false;
            LatLng ll = new LatLng(location.getLatitude(),
                    location.getLongitude());
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
            mBaiduMap.animateMapStatus(u);
        }
    }

    private void initView() {
        mMapView= (MapView) findViewById(R.id.map_view1);
        mModeBt= (Button) findViewById(R.id.mode_map);
        mModeBt1= (Button) findViewById(R.id.mode_map1);
        mModeBt.setOnClickListener(this);
        mModeBt1.setOnClickListener(this);

    }
    private void initData()
    {
        mLocatinMode= MyLocationConfiguration.LocationMode.NORMAL;
        mBaiduMap=mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);//开启定位图层；
        opGps();
        titlePopup = new TitlePopup(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        titlePopup.addAction(new ActionItem(this,"正常"));
        titlePopup.addAction(new ActionItem(this, "卫星"));
        titlePopup.addAction(new ActionItem(this, "交通"));
        titlePopup.addAction(new ActionItem(this, "热力"));
        titlePopup.setItemOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.mode_map:
                 switch (mLocatinMode)
               {
                     case NORMAL:
                         mModeBt.setText("罗盘");
                         mLocatinMode= MyLocationConfiguration.LocationMode.COMPASS;
                         mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(mLocatinMode, true, mCurrentMarker));
                     break;
                   case COMPASS:
                         mModeBt.setText("跟谁");
                         mLocatinMode= MyLocationConfiguration.LocationMode.FOLLOWING;
                         mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(mLocatinMode,true,mCurrentMarker));
                       break;
                   case FOLLOWING:
                       mModeBt.setText("正常");
                       mLocatinMode= MyLocationConfiguration.LocationMode.NORMAL;
                       mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(mLocatinMode,true,mCurrentMarker));
                       break;
                }
                break;
            case R.id.mode_map1:
                titlePopup.show(view);
        }
    }

    @Override
    public void onItemClick(ActionItem item, int position) {

        switch (position) {
            case 0:
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                mBaiduMap.setBaiduHeatMapEnabled(false);
                mBaiduMap.setTrafficEnabled(false);
                break;
            case 1:
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                mBaiduMap.setBaiduHeatMapEnabled(false);
                mBaiduMap.setTrafficEnabled(false);
                break;
            case 2:
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                mBaiduMap.setBaiduHeatMapEnabled(false);
                mBaiduMap.setTrafficEnabled(true);
                break;
            case 3:
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                mBaiduMap.setTrafficEnabled(false);
                mBaiduMap.setBaiduHeatMapEnabled(true);
                break;
        }
    }

    /**
     * 定位SDK监听函数
     */

    @Override
    protected void onPause() {
        // MapView的生命周期与Activity同步，当activity挂起时需调用MapView.onPause()
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        // MapView的生命周期与Activity同步，当activity恢复时需调用MapView.onResume()
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
        super.onDestroy();
    }
}
