package com.lm.lmmapdemo.ui;

import android.app.Dialog;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.route.BikingRouteLine;
import com.baidu.mapapi.search.route.BikingRoutePlanOption;
import com.baidu.mapapi.search.route.BikingRouteResult;
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
import com.lm.lmmapdemo.R;
import com.lm.lmmapdemo.adapter.RouteLineAdapter;
import com.lm.lmmapdemo.utils.BikingRouteOverlay;
import com.lm.lmmapdemo.utils.DrivingRouteOverlay;
import com.lm.lmmapdemo.utils.OverlayManager;
import com.lm.lmmapdemo.utils.TransitRouteOverlay;
import com.lm.lmmapdemo.utils.WalkingRouteOverlay;

import java.util.List;

public class NavigationActivity extends BaseActivity implements View.OnClickListener,BaiduMap.OnMapClickListener,OnGetRoutePlanResultListener,OnGetGeoCoderResultListener {

    private EditText mNowLocation;//目前位置显示
    private EditText mGoLocation;//要去的位置
    private Button pre, next,czbt;// 导航上个节点及下个节点
    private Button mWalk, mDrive, mTransport, mCycling;//导航方式
    private LinearLayout mNodeLayout;//节点布局
    private CardView mCardView;
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    RoutePlanSearch mSearch = null;
    private String nowLocation = null;
    private String mNowCity;
    int nodeIndex = -1;
    OverlayManager routeOverlay = null;
    private RouteLine routeLine;
    // 搜索模块，也可去掉地图模块独立使用
    GeoCoder mGeoSearch = null;
    TransitRouteResult nowResult = null;
    DrivingRouteResult nowResultd = null;
    private TextView popupText = null; // 泡泡view

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation2);
        initView();
        initListener();
    }

    private void initView() {
        mNowLocation = (EditText) findViewById(R.id.now_location_tv);
        mGoLocation = (EditText) findViewById(R.id.go_location_tv);
        pre = (Button) findViewById(R.id.pre);
        next = (Button) findViewById(R.id.next);
        mWalk = (Button) findViewById(R.id.walk);
        czbt= (Button) findViewById(R.id.czbt);
        czbt.setOnClickListener(this);
        mDrive = (Button) findViewById(R.id.drive);
        mCycling = (Button) findViewById(R.id.cycling);
        mTransport = (Button) findViewById(R.id.public_transport);
        mNodeLayout = (LinearLayout) findViewById(R.id.node_layout);
        mCardView = (CardView) findViewById(R.id.navigation_cardview);
        mMapView = (MapView) findViewById(R.id.mapview_navigation);
        mWalk.setOnClickListener(this);
        mDrive.setOnClickListener(this);
        mCycling.setOnClickListener(this);
        mTransport.setOnClickListener(this);
        mBaiduMap = mMapView.getMap();//百度地图初始化
        mBaiduMap.setMyLocationEnabled(true);
        mBaiduMap.setOnMapClickListener(this);//初始化地图点击事件
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(this);
        mGeoSearch = GeoCoder.newInstance();
        mGeoSearch.setOnGetGeoCodeResultListener(this);
        opGps();//获取经纬度

    }

    private void initListener() {
        pre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (routeLine == null || routeLine.getAllStep() == null) {
                    return;
                }
                if (nodeIndex == -1)
                    return;
                nodeIndex--;
                nodeLine();
            }

        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (routeLine == null || routeLine.getAllStep() == null) {
                    return;
                }
                if (nodeIndex < (routeLine.getAllStep().size() - 1)) {
                    nodeIndex++;
                } else {
                    return;
                }
                nodeLine();
            }
        });
    }

    public void nodeLine() {

        LatLng nodeLocation = null;
        String nodeTitle = null;
        Object step = routeLine.getAllStep().get(nodeIndex);
        if (step instanceof DrivingRouteLine.DrivingStep) {
            nodeLocation = ((DrivingRouteLine.DrivingStep) step).getEntrance().getLocation();
            nodeTitle = ((DrivingRouteLine.DrivingStep) step).getInstructions();
        } else if (step instanceof WalkingRouteLine.WalkingStep) {
            nodeLocation = ((WalkingRouteLine.WalkingStep) step).getEntrance().getLocation();
            nodeTitle = ((WalkingRouteLine.WalkingStep) step).getInstructions();
        } else if (step instanceof TransitRouteLine.TransitStep) {
            nodeLocation = ((TransitRouteLine.TransitStep) step).getEntrance().getLocation();
            nodeTitle = ((TransitRouteLine.TransitStep) step).getInstructions();
        } else if (step instanceof BikingRouteLine.BikingStep) {
            nodeLocation = ((BikingRouteLine.BikingStep) step).getEntrance().getLocation();
            nodeTitle = ((BikingRouteLine.BikingStep) step).getInstructions();
        }

        if (nodeLocation == null || nodeTitle == null) {
            return;
        }
        // 移动节点至中心
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(nodeLocation));
        // show popup
        popupText = new TextView(NavigationActivity.this);
        popupText.setBackgroundResource(R.mipmap.popup);
        popupText.setTextColor(0xFF000000);
        popupText.setText(nodeTitle);
        mBaiduMap.showInfoWindow(new InfoWindow(popupText, nodeLocation, 0));
    }

    /*
     *获取经纬度
     */
    @Override
    public void getLocation(BDLocation location) {

        mNowCity = location.getCity();
        LatLng ptCenter = new LatLng(location.getLatitude(), location.getLongitude());
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(location.getRadius())
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(100).latitude(location.getLatitude())
                .longitude(location.getLongitude()).build();
        mBaiduMap.setMyLocationData(locData);
        mGeoSearch.reverseGeoCode(new ReverseGeoCodeOption()
                .location(ptCenter));

    }

    /*
     *系统点击事件
     */
    @Override
    public void onClick(View view) {
        String goLocation = mGoLocation.getText().toString();
        if (goLocation.equals("")) {
            Toast.makeText(NavigationActivity.this, "你没有输入你要去的地方哟！", Toast.LENGTH_SHORT);
            return;
        }

        routeLine = null;
        mBaiduMap.clear();

        PlanNode stNode = PlanNode.withCityNameAndPlaceName(mNowCity, nowLocation);
        PlanNode enNode = PlanNode.withCityNameAndPlaceName(mNowCity, goLocation);
        switch (view.getId()) {
            case R.id.drive:
                mSearch.drivingSearch((new DrivingRoutePlanOption())
                        .from(stNode).currentCity(mNowCity).to(enNode));
                mCardView.setVisibility(View.GONE);
                opView();
                break;
            case R.id.walk:
                mSearch.walkingSearch(new WalkingRoutePlanOption().from(stNode).to(enNode));
                opView();
                break;
            case R.id.cycling:
                mSearch.bikingSearch(new BikingRoutePlanOption().from(stNode).to(enNode));
                opView();
                break;
            case R.id.public_transport:
                mSearch.transitSearch(new TransitRoutePlanOption().from(stNode).city(mNowCity).to(enNode));
                opView();
                break;
            case R.id.czbt:
                closeView();
                break;
            default:
                break;
        }


    }

   void opView()
    {
       mNodeLayout.setVisibility(View.VISIBLE);
        mCardView.setVisibility(View.GONE);
    }

   void closeView()
   {
       mNodeLayout.setVisibility(View.GONE);
       mCardView.setVisibility(View.VISIBLE);
   }

    /*
     *以下两个方法为百度地图点击事件
     */
    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public boolean onMapPoiClick(MapPoi mapPoi) {
        return false;
    }

    /*
     * 下面四个方法为百度搜索 导航方式搜索回调结果
     */
    @Override
    public void onGetWalkingRouteResult(WalkingRouteResult result) {

        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(NavigationActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            pre.setVisibility(View.VISIBLE);
            next.setVisibility(View.VISIBLE);
            routeLine = result.getRouteLines().get(0);
            WalkingRouteOverlay overlay = new MyWalkingRouteOverlay(mBaiduMap);
            mBaiduMap.setOnMarkerClickListener(overlay);
            routeOverlay = overlay;
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
        }
        //步行
    }

    @Override
    public void onGetTransitRouteResult(TransitRouteResult result) {
        //公共交通
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(NavigationActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            pre.setVisibility(View.VISIBLE);
            next.setVisibility(View.VISIBLE);


            if (result.getRouteLines().size() > 1) {
                nowResult = result;

                MyTransitDlg myTransitDlg = new MyTransitDlg(NavigationActivity.this,
                        result.getRouteLines(),
                        RouteLineAdapter.Type.TRANSIT_ROUTE);
                myTransitDlg.setOnItemInDlgClickLinster(new OnItemInDlgClickListener() {
                    public void onItemClick(int position) {
                        routeLine = nowResult.getRouteLines().get(position);
                        TransitRouteOverlay overlay = new TransitRouteOverlay(mBaiduMap);
                        mBaiduMap.setOnMarkerClickListener(overlay);
                        routeOverlay = overlay;
                        overlay.setData(nowResult.getRouteLines().get(position));
                        overlay.addToMap();
                        overlay.zoomToSpan();
                    }

                });
                myTransitDlg.show();


            } else if (result.getRouteLines().size() == 1) {
                // 直接显示
                routeLine = result.getRouteLines().get(0);
                TransitRouteOverlay overlay = new TransitRouteOverlay(mBaiduMap);
                mBaiduMap.setOnMarkerClickListener(overlay);
                routeOverlay = overlay;
                overlay.setData(result.getRouteLines().get(0));
                overlay.addToMap();
                overlay.zoomToSpan();

            } else {
                Log.d("transitresult", "结果数<0");
                return;
            }


        }
    }

    @Override
    public void onGetDrivingRouteResult(DrivingRouteResult result) {
        //自驾
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(NavigationActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;


            if (result.getRouteLines().size() > 1 ) {
                nowResultd = result;

                MyTransitDlg myTransitDlg = new MyTransitDlg(NavigationActivity.this,
                        result.getRouteLines(),
                        RouteLineAdapter.Type.DRIVING_ROUTE);
                myTransitDlg.setOnItemInDlgClickLinster(new OnItemInDlgClickListener() {
                    public void onItemClick(int position) {
                        routeLine = nowResultd.getRouteLines().get(position);
                        DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mBaiduMap);
                        mBaiduMap.setOnMarkerClickListener(overlay);
                        routeOverlay = overlay;
                        overlay.setData(nowResultd.getRouteLines().get(position));
                        overlay.addToMap();
                        overlay.zoomToSpan();
                    }

                });
                myTransitDlg.show();

            } else if ( result.getRouteLines().size() == 1 ) {
                routeLine = result.getRouteLines().get(0);
                DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mBaiduMap);
                routeOverlay = overlay;
                mBaiduMap.setOnMarkerClickListener(overlay);
                overlay.setData(result.getRouteLines().get(0));
                overlay.addToMap();
                overlay.zoomToSpan();
                pre.setVisibility(View.VISIBLE);
                next.setVisibility(View.VISIBLE);
            }

        }
    }

    @Override
    public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {
        //骑行
        if (bikingRouteResult == null || bikingRouteResult.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(NavigationActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (bikingRouteResult.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (bikingRouteResult.error == SearchResult.ERRORNO.NO_ERROR) {
            nodeIndex = -1;
            pre.setVisibility(View.VISIBLE);
            next.setVisibility(View.VISIBLE);
            routeLine = bikingRouteResult.getRouteLines().get(0);
            BikingRouteOverlay overlay = new MyBikingRouteOverlay(mBaiduMap);
            routeOverlay = overlay;
            mBaiduMap.setOnMarkerClickListener(overlay);
            overlay.setData(bikingRouteResult.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
        }
    }

    /*
     *经纬度与地址转换
     */
    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
        if (reverseGeoCodeResult == null || reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(NavigationActivity.this, "抱歉，未能找到结果", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        nowLocation = reverseGeoCodeResult.getAddress();
        mNowLocation.setText("当前位置：" + reverseGeoCodeResult.getAddress());
    }

    interface OnItemInDlgClickListener {
        public void onItemClick(int position);
    }

    class MyTransitDlg extends Dialog {

        private List<? extends RouteLine> mtransitRouteLines;
        private ListView transitRouteList;
        private RouteLineAdapter mTransitAdapter;

        OnItemInDlgClickListener onItemInDlgClickListener;

        public MyTransitDlg(Context context, int theme) {
            super(context, theme);
        }

        public MyTransitDlg(Context context, List<? extends RouteLine> transitRouteLines, RouteLineAdapter.Type
                type) {
            this(context, 0);
            mtransitRouteLines = transitRouteLines;
            mTransitAdapter = new RouteLineAdapter(context, mtransitRouteLines, type);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_transit_dialog);

            transitRouteList = (ListView) findViewById(R.id.transitList);
            transitRouteList.setAdapter(mTransitAdapter);

            transitRouteList.setOnItemClickListener( new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    onItemInDlgClickListener.onItemClick( position);
                    pre.setVisibility(View.VISIBLE);
                    next.setVisibility(View.VISIBLE);
                    dismiss();

                }
            });
        }

        public void setOnItemInDlgClickLinster( OnItemInDlgClickListener itemListener) {
            onItemInDlgClickListener = itemListener;
        }

    }

    public class MyDrivingRouteOverlay extends DrivingRouteOverlay {

        public MyDrivingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            if (false) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.location);
            }
            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            if (false) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.location_log);
            }
            return null;
        }
    }


    // 定制RouteOverly
    public class MyTransitRouteOverlay extends TransitRouteOverlay {

        public MyTransitRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            if (true) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.location);
            }
            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            if (true) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.location);
            }
            return null;
        }
    }

    public class MyBikingRouteOverlay extends BikingRouteOverlay {

        public MyBikingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            if (true) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.location);
            }
            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            if (true) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.location);
            }
            return null;
        }
    }


    public class MyWalkingRouteOverlay extends WalkingRouteOverlay {

        public MyWalkingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            if (true) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.location);
            }
            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            if (true) {
                return BitmapDescriptorFactory.fromResource(R.mipmap.location);
            }
            return null;
        }
    }

}

