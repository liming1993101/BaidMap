package com.lm.lmmapdemo.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.DrivingRouteOverlay;
import com.baidu.mapapi.overlayutil.OverlayManager;
import com.baidu.mapapi.overlayutil.TransitRouteOverlay;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.lm.lmmapdemo.R;

import java.util.ArrayList;
import java.util.List;

public class NavigationActivity extends BaseActivity implements
        OnGetSuggestionResultListener,BaiduMap.OnMapClickListener,
        OnGetRoutePlanResultListener ,TextWatcher,View.OnClickListener{
    private Button mDrive,mWalk,mPublic,mNavigationBt;
    private AutoCompleteTextView mOrigin;
    private AutoCompleteTextView mEndPoint;
    private boolean isFirstLoc = true;
    private ArrayAdapter<String> sugAdapter = null;
    private String city;// 当前城市名
    // private String street;// 当前街道名
    // private String district;// 当前县
    private LocationClient mLocClient;
    private MyLocationListenner myListener;
    // 浏览路线节点相关
    private Button mBtnPre = null;// 上一个节点
    private Button mBtnNext = null;// 下一个节点
    private Button bt_plan;
    private int nodeIndex = -1;// 节点索引,供浏览节点时使用
    @SuppressWarnings("rawtypes")
    private RouteLine route = null;
    @SuppressWarnings("unused")
    private OverlayManager routeOverlay = null;
    private boolean useDefaultIcon = false;
    private TextView popupText = null;// 泡泡view
    AutoCompleteTextView start;
    AutoCompleteTextView end;
    private PopupWindow popupWindow;
    SuggestionSearch suggestionSearch;
    private LatLng latLng;// 当前经纬度信息
    public static int distance;

    // 地图相关，使用继承MapView的MyRouteMapView目的是重写touch事件实现泡泡处理
    // 如果不处理touch事件，则无需继承，直接使用MapView即可
    private MapView mMapView = null; // 地图View
    private BaiduMap mBaidumap = null;
    // 搜索相关
    private RoutePlanSearch mSearch = null; // 搜索模块，也可去掉地图模块独立使用

    public static ArrayList<String> arrayList;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        suggestionSearch = SuggestionSearch.newInstance();
        suggestionSearch.setOnGetSuggestionResultListener(this);
        initView();
        initData();
    }


    private void initView() {
        mNavigationBt= (Button) findViewById(R.id.navigation_bt);
        mBtnPre= (Button) findViewById(R.id.pre);
        mBtnNext= (Button) findViewById(R.id.next);
        bt_plan= (Button) findViewById(R.id.plan);

    }

    private void initData() {
        mLocClient=new LocationClient(this);
        myListener=new MyLocationListenner();
        mLocClient.registerLocationListener(myListener);
        mMapView= (MapView) findViewById(R.id.map);
        mBaidumap=mMapView.getMap();
        LocationClientOption option=new LocationClientOption();
        option.setOpenGps(true);
        option.setCoorType("bd0911");
        option.setScanSpan(1000);
        option.setIsNeedAddress(true);
        mLocClient.setLocOption(option);
        mLocClient.start();
        mBtnPre.setVisibility(View.INVISIBLE);
        mBtnNext.setVisibility(View.INVISIBLE);
        bt_plan.setVisibility(View.INVISIBLE);
        mBaidumap.setOnMapClickListener(this);
        mSearch=RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(this);




    }
    private void showPopupWindow()
    {
        View contentView= LayoutInflater.from(this).inflate(R.layout.pop_window_1,null);
        mDrive= (Button) contentView.findViewById(R.id.drive);
        mWalk= (Button) contentView.findViewById(R.id.walk);
        mPublic= (Button) contentView.findViewById(R.id.public_transportation);
        mOrigin= (AutoCompleteTextView) contentView.findViewById(R.id.origin);
        mEndPoint= (AutoCompleteTextView) contentView.findViewById(R.id.end_point);

        sugAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line);
        end.setAdapter(sugAdapter);
        start.setAdapter(sugAdapter);
        arrayList=new ArrayList<String >();
        end.addTextChangedListener(this);
        start.addTextChangedListener(this);
        popupWindow = new PopupWindow(contentView,
                200, ViewGroup.LayoutParams.MATCH_PARENT, true);
        // 设置动画效果
        popupWindow.setAnimationStyle(R.style.AnimationFade);
        // 点击其他地方消失
        contentView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                if (popupWindow != null && popupWindow.isShowing()) {
                    popupWindow.dismiss();
                    popupWindow = null;
                }
                return false;
            }
        });
    }

    @Override
    public void onClick(View view) {
        // 重置浏览节点的路线数据
        route = null;
        mBtnPre.setVisibility(View.INVISIBLE);
        mBtnNext.setVisibility(View.INVISIBLE);
        mBaidumap.clear();

        PlanNode stNode = null;
        if (start.getText().toString().equals("我的位置")) {
            stNode = PlanNode.withLocation(latLng);
        } else {
            stNode = PlanNode.withCityNameAndPlaceName(city, start.getText()
                    .toString().trim());
        }
        // 设置起终点信息，对于tranist search 来说，城市名无意义
        PlanNode enNode = null;

        enNode = PlanNode.withCityNameAndPlaceName(city, end.getText()
                .toString().trim());
      switch (view.getId())
      {
          case R.id.drive:
              mSearch.drivingSearch((new DrivingRoutePlanOption()).from(stNode).to(enNode));
              break;
          case R.id.public_transportation:
              mSearch.transitSearch((new TransitRoutePlanOption()).from(stNode)
                      .city(city).to(enNode));
              break;
          case R.id.walk:
              mSearch.walkingSearch((new WalkingRoutePlanOption()).from(stNode)
                      .to(enNode));
              break;
          case R.id.navigation_bt:
              getPopupWindow();
              // 这里是位置显示方式,在屏幕的左侧
              popupWindow.showAtLocation(view, Gravity.LEFT, 0, 0);
              break;
          case R.id.pre:
              if (nodeIndex == -1 && view.getId() == R.id.pre) {
                  break;
              }
              if (nodeIndex > 0) {
                  nodeIndex--;
                  node();
              }
              break;
          case R.id.next:
              if (nodeIndex < route.getAllStep().size() - 1) {
                  nodeIndex++;
                  node();
              }
              break;
      }
    }
    private void node()
    {
        LatLng nodeLocation = null;
        String nodeTitle = null;

        Object step = route.getAllStep().get(nodeIndex);
        if (step instanceof DrivingRouteLine.DrivingStep) {
            nodeLocation = ((DrivingRouteLine.DrivingStep) step).getEntrace()
                    .getLocation();// 节点经纬度
            nodeTitle = ((DrivingRouteLine.DrivingStep) step).getInstructions();// 节点行驶路线
        } else if (step instanceof WalkingRouteLine.WalkingStep) {
            nodeLocation = ((WalkingRouteLine.WalkingStep) step).getEntrace()
                    .getLocation();
            nodeTitle = ((WalkingRouteLine.WalkingStep) step).getInstructions();
        } else if (step instanceof TransitRouteLine.TransitStep) {
            nodeLocation = ((TransitRouteLine.TransitStep) step).getEntrace()
                    .getLocation();
            nodeTitle = ((TransitRouteLine.TransitStep) step).getInstructions();
        }

        if (nodeLocation == null || nodeTitle == null) {
            return;
        }

        // 移动节点至中心
        mBaidumap.setMapStatus(MapStatusUpdateFactory.newLatLng(nodeLocation));
        // show popup
        popupText = new TextView(NavigationActivity.this);
        popupText.setBackgroundResource(R.mipmap.popup);
        popupText.setTextColor(0xFF000000);
        popupText.setText(nodeTitle);
        mBaidumap.showInfoWindow(new InfoWindow(popupText, nodeLocation, 0));

    }
    @Override
    public void onGetSuggestionResult(SuggestionResult res) {

        if (res == null || res.getAllSuggestions() == null) {
            return;
        }
        sugAdapter.clear();

        for (SuggestionResult.SuggestionInfo info : res.getAllSuggestions()) {
            if (info.key != null) {
                sugAdapter.add(info.key);

            }
        }
        sugAdapter.notifyDataSetChanged();

    }


    @Override
    public void onMapClick(LatLng latLng) {
        mBaidumap.hideInfoWindow();
    }

    @Override
    public boolean onMapPoiClick(MapPoi mapPoi) {
        return false;
    }

    private class MyWalkingRouteOverlay extends WalkingRouteOverlay {

        public MyWalkingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.icon_st);
            }
            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.icon_en);
            }
            return null;
        }
    }
    @Override
    public void onGetWalkingRouteResult(WalkingRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(NavigationActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                    .show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            result.getSuggestAddrInfo();
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            mBtnPre.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);
            bt_plan.setVisibility(View.VISIBLE);
            route = result.getRouteLines().get(0);
            WalkingRouteOverlay overlay = new MyWalkingRouteOverlay(mBaidumap);
            mBaidumap.setOnMarkerClickListener(overlay);
            routeOverlay = overlay;
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
        }
    }

    @Override
    public void onGetTransitRouteResult(TransitRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(NavigationActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                    .show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            result.getSuggestAddrInfo();
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            mBtnPre.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);
            route = result.getRouteLines().get(0);
            TransitRouteOverlay overlay = new MyTransitRouteOverlay(mBaidumap);
            mBaidumap.setOnMarkerClickListener(overlay);
            routeOverlay = overlay;
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
        }
    }
    private class MyTransitRouteOverlay extends TransitRouteOverlay {

        public MyTransitRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.icon_st);
            }
            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.icon_en);
            }
            return null;
        }
    }

    @Override
    public void onGetDrivingRouteResult(DrivingRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(NavigationActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                    .show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息

            List<CityInfo> list = result.getSuggestAddrInfo()
                    .getSuggestEndCity();

            Log.i("info", list.toString());
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            mBtnPre.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);
            bt_plan.setVisibility(View.VISIBLE);
            route = result.getRouteLines().get(0);
            DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mBaidumap);
            routeOverlay = overlay;
            mBaidumap.setOnMarkerClickListener(overlay);
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
        }
    }


    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence cs, int i, int i1, int i2) {

        if (cs.length() <= 0) {
            return;
        }
        String cityname = city;
        /**
         * 使用建议搜索服务获取建议列表，结果在onSuggestionResult()中更新
         */
        suggestionSearch.requestSuggestion((new SuggestionSearchOption())
                .keyword(cs.toString()).city(cityname));
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    public void getPopupWindow() {
        if (null != popupWindow) {
            popupWindow.dismiss();
            return;
        } else {
            showPopupWindow();
        }
    }


    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null)
                return;
            city = location.getCity();// 合肥市
            // Toast.makeText(getApplicationContext(), city, 0).show();
            // street = location.getStreet();// 文曲路
            // district = location.getDistrict();// 肥西县
            // longitude = location.getLongitude();
            // latitude = location.getLatitude();
            // adress = location.getAddrStr()；
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaidumap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                latLng = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(latLng);
                mBaidumap.animateMapStatus(u);
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {

        }
    }
private class MyDrivingRouteOverlay extends DrivingRouteOverlay {

    public MyDrivingRouteOverlay(BaiduMap baiduMap) {
        super(baiduMap);
    }

    @Override
    public BitmapDescriptor getStartMarker() {
        if (useDefaultIcon) {
            return BitmapDescriptorFactory.fromResource(R.mipmap.icon_st);
        }
        return null;
    }

    @Override
    public BitmapDescriptor getTerminalMarker() {
        if (useDefaultIcon) {
            return BitmapDescriptorFactory.fromResource(R.mipmap.icon_en);
        }
        return null;
    }
}
}
