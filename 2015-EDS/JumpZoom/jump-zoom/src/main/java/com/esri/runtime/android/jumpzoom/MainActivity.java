package com.esri.runtime.android.jumpzoom;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.GeoView;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.NavigationChangedEvent;
import com.esri.arcgisruntime.mapping.view.NavigationChangedListener;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

  private static final String TAG = MainActivity.class.getSimpleName();
  private double mZoomScale = 5000;
  private MapView mMapView;
  private Viewpoint mWorldViewpoint = new Viewpoint(new Point(0, 0, SpatialReferences.getWebMercator()), 200000000);
  private Viewpoint mViewpoint1 = new Viewpoint(new Point(1493528.253391, 6894813.853409, SpatialReferences.getWebMercator()), mZoomScale);
  private Viewpoint mViewpoint2 = new Viewpoint(new Point(1489222.588445, 6893995.144173, SpatialReferences.getWebMercator()), mZoomScale);
  private Viewpoint mViewpoint3 = new Viewpoint(new Point(1196644.068456, 6033554.266798, SpatialReferences.getWebMercator()), mZoomScale);
  private Viewpoint mViewpoint4 = new Viewpoint(new Point(774593.577598, 6610915.197602, SpatialReferences.getWebMercator()), mZoomScale);

  ListenableFuture<Boolean> booleanListenableFuture;

  private LogCenterAndScale navCompletedListener;

  private class LogCenterAndScale implements NavigationChangedListener
  {
    @Override
    public void navigationChanged(NavigationChangedEvent navigationCompletedEvent) {
      if (navigationCompletedEvent != null) {
        GeoView source = navigationCompletedEvent.getSource();
        if (source instanceof MapView) {
          MapView mapView = (MapView) source;
          Point pt = mapView.getVisibleArea().getExtent().getCenter();
          Log.i(TAG, String.format("CenterPoint: X:%.6f, Y:%.6f", pt.getX(), pt.getY()));
          Log.i(TAG, "Current scale: " + mapView.getMapScale());
        }
      }
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    mMapView = (MapView) findViewById(R.id.mapView);
    ArcGISMap map = new ArcGISMap(Basemap.Type.IMAGERY_WITH_LABELS, 0, 0, 1);
    mMapView.setMap(map);

    navCompletedListener = new LogCenterAndScale();
    mMapView.addNavigationChangedListener(navCompletedListener);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mMapView.setViewpointAsync(mWorldViewpoint, 2);
      }
    });

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.setDrawerListener(toggle);
    toggle.syncState();

    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);
  }


  @Override
  public void onBackPressed() {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    Viewpoint selectedViewpoint = null;
    if (id == R.id.nav_world_location1) {
      selectedViewpoint = mViewpoint1;
    } else if (id == R.id.nav_world_location2) {
      selectedViewpoint = mViewpoint2;
    } else if (id == R.id.nav_world_location3) {
      selectedViewpoint = mViewpoint3;
    } else if (id == R.id.nav_world_location4) {
      selectedViewpoint = mViewpoint4;
    }

    // If new target already inside the current extent, then zoom directly to it.
    if (GeometryEngine.intersects(mMapView.getVisibleArea(), selectedViewpoint.getTargetGeometry())) {
      jumpZoom(selectedViewpoint, null);
    } else {
      // If target is outside of current extent, zoom out first to see both extents, then zoom back in.
      Geometry union = GeometryEngine.union(mMapView.getVisibleArea().getExtent().getCenter(), selectedViewpoint
          .getTargetGeometry());
      if ((union != null) && (!union.isEmpty())) {
        jumpZoom(new Viewpoint(union.getExtent()), selectedViewpoint);
      }
    }

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  private void jumpZoom(Viewpoint firstViewpoint, final Viewpoint secondViewpoint) {
    if (firstViewpoint == null) return;

    booleanListenableFuture = mMapView.setViewpointAsync(firstViewpoint, 3);

    if (secondViewpoint == null) return;

    booleanListenableFuture.addDoneListener(new Runnable() {
      @Override
      public void run() {
        try {
          if (booleanListenableFuture.get()) {
            // First navigation is complete, was not interrupted by the user or another navigation.
            mMapView.setViewpointAsync(secondViewpoint, 3);
          }
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }
    });
  }

  @Override
  protected void onPause() {
    super.onPause();
    mMapView.pause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mMapView.resume();
  }
}
