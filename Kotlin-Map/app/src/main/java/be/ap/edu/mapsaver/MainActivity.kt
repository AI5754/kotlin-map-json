package be.ap.edu.mapsaver

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.beust.klaxon.*
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration

import org.osmdroid.util.GeoPoint
import org.osmdroid.views.*

import java.util.*
import org.osmdroid.views.overlay.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : Activity() {

    companion object {

        private val REQUEST_CODE = 10
    }

    var mMapView: MapView? = null
    var mMyLocationOverlay: ItemizedOverlay<OverlayItem>? = null
    var searchField: EditText? = null
    var searchButton: Button? = null
    var clearButton: Button? = null
    val urlSearch = "https://nominatim.openstreetmap.org/search?q="

    var i: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        i = Intent(this, Main2Activity::class.java)

        // Problem with SQLite db, solution :
        // https://stackoverflow.com/questions/40100080/osmdroid-maps-not-loading-on-my-device
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        val tileCache = File(osmConfig.osmdroidBasePath, "tile")
        osmConfig.osmdroidTileCache = tileCache

        setContentView(R.layout.activity_main)
        mMapView = findViewById(R.id.mapview) as MapView

        searchField = findViewById(R.id.search_txtview)
        searchButton = findViewById(R.id.search_button)
        searchButton!!.setOnClickListener {
            val url = URL(urlSearch + URLEncoder.encode(searchField?.text.toString(), "UTF-8") + "&format=json")
            it.hideKeyboard()

            MyAsyncTask().execute(url)
        }

        clearButton = findViewById(R.id.clear_button)
        clearButton!!.setOnClickListener {
            mMapView!!.overlays.clear()
            // Redraw map
            mMapView!!.invalidate()
        }

        if (hasPermissions()) {
            initMap()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (hasPermissions()) {
                initMap()
            } else {
                finish()
            }
        }
    }

    fun addPoint(lat: Double, lon: Double){
        mMapView!!.setTileSource(TileSourceFactory.MAPNIK)

        run {
            // Create a static ItemizedOverlay showing a some Markers on some cities
            val items = ArrayList<OverlayItem>()
            items.add(OverlayItem("Meistraat", "Campus Meistraat",
                    GeoPoint(51.2162764, 4.41160291036386)))
            items.add(OverlayItem("Lange Nieuwstraat", "Campus Lange Nieuwstraat",
                    GeoPoint(51.2196911, 4.4092625)))


            items.add(OverlayItem("Current", "Campus Meistraat",
                    GeoPoint(lat, lon)))

            // OnTapListener for the Markers, shows a simple Toast
            this.mMyLocationOverlay = ItemizedIconOverlay(items,
                    object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                        override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                            Toast.makeText(
                                    applicationContext, "Item '" + item.title + "' (index=" + index
                                    + ") got single tapped up", Toast.LENGTH_LONG).show()
                            return true // We 'handled' this event.
                        }

                        override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                            Toast.makeText(
                                    applicationContext, "Item '" + item.title + "' (index=" + index
                                    + ") got long pressed", Toast.LENGTH_LONG).show()
                            return true
                        }
                    }, applicationContext)

            this.mMapView!!.overlays.add(this.mMyLocationOverlay)
        }

        // MiniMap
        run {
            val miniMapOverlay = MinimapOverlay(this, mMapView!!.tileRequestCompleteHandler)
            this.mMapView!!.overlays.add(miniMapOverlay)
        }

        val mapController = mMapView!!.controller
        mapController.setZoom(20.0)
        // Default = Ellermanstraat 33
    }

    fun initMap() {




        mMapView!!.setTileSource(TileSourceFactory.MAPNIK)

        run {

            val items = ArrayList<OverlayItem>()

            val jsonfile: String = applicationContext.assets.open("velostation.json").bufferedReader().use {it.readText()}

            var jsonObject = JSONArray(jsonfile)

            for (i in 0 until jsonObject.length()) {
                val item = jsonObject.getJSONObject(i)

                items.add(OverlayItem(item.getString("naam"), item.getString("aantal_loc"),
                        GeoPoint(item.getDouble("point_lat"), item.getDouble("point_lng"))))
            }

            // OnTapListener for the Markers, shows a simple Toast
            this.mMyLocationOverlay = ItemizedIconOverlay(items,
                    object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                       // i:Intent

                        override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                            i?.putExtra("text1", item.title)
                            i?.putExtra("text2", item.snippet)

                            //i.putExtra("text2", text2.getText().toString())

                            startActivity(i)

//                            Toast.makeText(
//                                    applicationContext, "Item '" + item.title + "' (index=" + index
//                                    + ") got single tapped up", Toast.LENGTH_LONG).show()
                            return true // We 'handled' this event.
                        }

                        override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                            Toast.makeText(
                                    applicationContext, "Item '" + item.title + "' (index=" + index
                                    + ") got long pressed", Toast.LENGTH_LONG).show()
                            return true
                        }
                    }, applicationContext)
            this.mMapView!!.overlays.add(this.mMyLocationOverlay)
        }

        // MiniMap
        run {
            val miniMapOverlay = MinimapOverlay(this, mMapView!!.tileRequestCompleteHandler)
            this.mMapView!!.overlays.add(miniMapOverlay)
        }

        val mapController = mMapView!!.controller
        mapController.setZoom(20.0)
        // Default = Ellermanstraat 33
        mapController.setCenter(GeoPoint(51.23020595, 4.41655480828479))
    }

    fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onPause() {
        super.onPause()
        mMapView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        mMapView!!.onResume()
    }

    // AsyncTask inner class
    inner class MyAsyncTask : AsyncTask<URL, Int, String>() {

        private var result: String = ""
        private val parser: Parser = Parser.default()

        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg params: URL?): String {

            val connect = params[0]?.openConnection() as HttpURLConnection
            connect.readTimeout = 8000
            connect.connectTimeout = 8000
            connect.requestMethod = "GET"
            connect.connect();

            val responseCode: Int = connect.responseCode;
            if (responseCode == 200) {
                result = streamToString(connect.inputStream)
            }

            return result
        }

        // vararg : variable number of arguments
        // * : spread operator, unpacks an array into the list of values from the array
        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            // Parse result as Array of JSON objects
            val jsonString = StringBuilder(result!!)
            val parser: Parser = Parser.default()
            val array = parser.parse(jsonString) as JsonArray<JsonObject>

            if (array.size > 0) {
                // Use low-level API
                val obj = array[0]
                val mapController = mMapView!!.controller
                mapController.setCenter(GeoPoint(obj.string("lat")!!.toDouble(), obj.string("lon")!!.toDouble()))
                Log.d("objtologhere", obj.toString())
                //addPoint(obj.string("lat")!!.toDouble(), obj.string("lon")!!.toDouble())
            }
        }
    }

    fun streamToString(inputStream: InputStream): String {

        val bufferReader = BufferedReader(InputStreamReader(inputStream))
        var line: String
        var result = ""

        try {
            do {
                line = bufferReader.readLine()
                if (line != null) {
                    result += line
                }
            } while (line != "")
            inputStream.close()
        } catch (ex: Exception) {

        }
        return result
    }
}