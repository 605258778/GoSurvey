package com.zeng.gosurvey

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.res.Configuration
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.LocationDisplay
import com.esri.arcgisruntime.mapping.view.MapView

import com.zeng.gosurvey.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val locationDisplay: LocationDisplay by lazy { mapView.locationDisplay }

    private val activityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val mapView: MapView by lazy {
        activityMainBinding.mapView
    }

    private val drawerLayout: DrawerLayout by lazy {
        activityMainBinding.drawerLayout
    }

    private val drawerList: ListView by lazy {
        activityMainBinding.drawerList
    }

    private val spinner: Spinner by lazy {
        activityMainBinding.spinner
    }

    private lateinit var mNavigationDrawerItemTitles: Array<String>

    private val mDrawerToggle: ActionBarDrawerToggle by lazy { setupDrawer() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityMainBinding.root)

        // authentication with an API key or named user is required to access basemaps and other
        // location services
        ArcGISRuntimeEnvironment.setApiKey("AAPK01d1046b08764d429a373677eb86dff4tnMmFO2UU49gAva12uy674XGZOuv3e9wpmUvdFY9BuwAPGXkwLCR1qaSxDR55J-r")

        // inflate navigation drawer with all basemap types in a human readable format
        mNavigationDrawerItemTitles =
            BasemapStyle.values().map { it.name.replace("_", " ") }
                .toTypedArray()

        addDrawerItems()
        drawerLayout.addDrawerListener(mDrawerToggle)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            // set opening basemap title to ARCGIS IMAGERY
            title = mNavigationDrawerItemTitles[0]
        }

        // create a map with the imagery Basemap and set it to the map
        mapView.map = ArcGISMap(BasemapStyle.ARCGIS_IMAGERY)
        mapView.setViewpoint(Viewpoint(47.6047, -122.3334, 10000000.0))

        locationDisplay.addDataSourceStatusChangedListener {
            // if LocationDisplay isn't started or has an error
            if (!it.isStarted && it.error != null) {
                // check permissions to see if failure may be due to lack of permissions
                requestPermissions(it)
            }
        }
        // populate the list for the location display options for the spinner's adapter
        val list = arrayListOf(
            ItemData("停止", R.drawable.locationdisplaydisabled),
            ItemData("打开", R.drawable.locationdisplayon),
            ItemData("中心", R.drawable.locationdisplayrecenter),
            ItemData("导航", R.drawable.locationdisplaynavigation),
            ItemData("罗盘", R.drawable.locationdisplayheading)
        )

        spinner.apply {
            adapter = SpinnerAdapter(this@MainActivity, R.id.locationTextView, list)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    when (position) {
                        0 ->  // stop location display
                            if (locationDisplay.isStarted) locationDisplay.stop()
                        1 ->  // start location display
                            if (!locationDisplay.isStarted) locationDisplay.startAsync()
                        2 -> {
                            // re-center MapView on location
                            locationDisplay.autoPanMode = LocationDisplay.AutoPanMode.RECENTER
                            if (!locationDisplay.isStarted) locationDisplay.startAsync()
                        }
                        3 -> {
                            // start navigation mode
                            locationDisplay.autoPanMode = LocationDisplay.AutoPanMode.NAVIGATION
                            if (!locationDisplay.isStarted) locationDisplay.startAsync()
                        }
                        4 -> {
                            // start compass navigation mode
                            locationDisplay.autoPanMode = LocationDisplay.AutoPanMode.COMPASS_NAVIGATION
                            if (!locationDisplay.isStarted) locationDisplay.startAsync()
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        // move the spinner above the attribution bar
        mapView.addAttributionViewLayoutChangeListener { view, _, _, _, _, _, oldTop, _, oldBottom ->
            spinner.y -= view.height - (oldBottom - oldTop)
        }

    }

    private fun addButtonItem(){
//        var fangda: ImageButton = findViewById(R.id.fangda)
//        var suoxiao: ImageButton = findViewById(R.id.suoxiao)
//        var dingwei: ImageButton = findViewById(R.id.dingwei)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // if request is cancelled, the result arrays are empty
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // location permission was granted; this would have been triggered in response to failing to start the
            // LocationDisplay, so try starting this again
            locationDisplay.startAsync()
        } else {
            // if permission was denied, show toast to inform user what was chosen
            // if LocationDisplay is started again, request permission UI will be shown again,
            // option should be shown to allow never showing the UX again
            // alternative would be to disable functionality so request is not shown again
            Toast.makeText(
                this@MainActivity,
                resources.getString(R.string.location_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
            // update UI to reflect that the location display did not actually start
            spinner.setSelection(0, true)
        }
    }

    /**
     * Add navigation drawer items
     */
    private fun addDrawerItems() {
        ArrayAdapter(this, android.R.layout.simple_list_item_1, mNavigationDrawerItemTitles).apply {
            drawerList.adapter = this
            drawerList.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ -> selectBasemap(position) }
        }
    }

    /**
     * Set up the navigation drawer
     */
    private fun setupDrawer() =
        object :
            ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {

            override fun isDrawerIndicatorEnabled() = true

            /** Called when a drawer has settled in a completely open state.  */
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                supportActionBar?.title = title
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state.  */
            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }
        }

    /**
     * Select the Basemap item based on position in the navigation drawer
     *
     * @param position order int in navigation drawer
     */
    private fun selectBasemap(position: Int) {
        // update selected item and title, then close the drawer
        drawerList.setItemChecked(position, true)
        drawerLayout.closeDrawer(drawerList)

        // get basemap title by position
        val baseMapTitle = mNavigationDrawerItemTitles[position]
        supportActionBar?.title = baseMapTitle

        // create a new Basemap(BasemapStyle.THE_ENUM_SELECTED)
        mapView.map.basemap =
            Basemap(BasemapStyle.valueOf(baseMapTitle.replace(" ", "_")))
    }

    /**
     * Request fine and coarse location permissions for API level 23+.
     */
    private fun requestPermissions(dataSourceStatusChangedEvent: LocationDisplay.DataSourceStatusChangedEvent) {
        val requestCode = 2
        val reqPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        // fine location permission
        val permissionCheckFineLocation =
            ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[0]) ==
                    PackageManager.PERMISSION_GRANTED
        // coarse location permission
        val permissionCheckCoarseLocation =
            ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[1]) ==
                    PackageManager.PERMISSION_GRANTED
        if (!(permissionCheckFineLocation && permissionCheckCoarseLocation)) { // if permissions are not already granted, request permission from the user
            ActivityCompat.requestPermissions(this@MainActivity, reqPermissions, requestCode)
        } else {
            // report other unknown failure types to the user - for example, location services may not
            // be enabled on the device.
            val message = String.format(
                "Error in DataSourceStatusChangedListener: %s", dataSourceStatusChangedEvent
                    .source.locationDataSource.error.message
            )
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            // update UI to reflect that the location display did not actually start
            spinner.setSelection(0, true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Activate the navigation drawer toggle
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onResume() {
        super.onResume()
        mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        mapView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.dispose()
    }
}
