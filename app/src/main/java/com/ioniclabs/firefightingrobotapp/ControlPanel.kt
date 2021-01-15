package com.ioniclabs.firefightingrobotapp

import android.content.Context
import android.os.*
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.niqdev.mjpeg.DisplayMode
import com.github.niqdev.mjpeg.Mjpeg
import com.github.niqdev.mjpeg.MjpegInputStream
import com.github.niqdev.mjpeg.MjpegView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jackandphantom.joystickview.JoyStickView
import kotlinx.android.synthetic.main.activity_control_panel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.Socket
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class ControlPanel : AppCompatActivity() {
    var active = false
    var address_of_controller = "192.168.43.74"
    var port_of_controller = 1337
    var data_to_transmit = ""

    lateinit var mjpegView:MjpegView
    lateinit var writer: Socket
    lateinit var connect:Socket
    lateinit var transmit_data:JSONObject

    lateinit var obstacle_status:TextView
    lateinit var temperature_text:TextView
    lateinit var humidity_text:TextView
    lateinit var fire_extenguisher_status:TextView
    lateinit var joyStickView: JoyStickView
    companion object{
        var instruction_id = 0
        var prev_instruction = 0
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_panel)
        supportActionBar?.hide()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        // mjpegView = findViewById(R.id.VIEW_NAME)

        active = true
        transmit_data = JSONObject()
        transmit_data.put("l",1)
        transmit_data.put("w",0)
        transmit_data.put("x",0)
        transmit_data.put("y",0)
        transmit_data.put("s",0)
        humidity_text = findViewById(R.id.humidity_text_view)
        temperature_text = findViewById(R.id.temperature_text_view)
        fire_extenguisher_status = findViewById(R.id.fire_extenguisher_status_view)
        obstacle_status = findViewById(R.id.near_obstacle_distance_view)
        joyStickView = findViewById(R.id.joyStickView)
        CoroutineScope(IO).launch{
            connect = client_connect(address_of_controller, port_of_controller)
            if(connect.isConnected){
                client_receive_data(connect, temperature_text, humidity_text, fire_extenguisher_status, obstacle_status)
            }
        }
        var progress = 50
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                progress = p1
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                vibrate()
                transmit_data.put("s", progress)
                CoroutineScope(IO).launch {
                    client_write(connect, transmit_data.toString()+"\n")
                }
            }

        })
        var shall_transmit = false
        joyStickView.setOnMoveListener { angle, strength ->
            Log.d("MOVE","angle $angle, strength $strength")
            if(angle > 0 && angle < 22.5 && strength > 95){
                Log.d("DIR","Extreme right")
                instruction_id = 1
                shall_transmit = true
            }else if(angle > 345.5 && angle < 360 && strength > 95){
            Log.d("DIR","Extreme right")
                instruction_id = 1
                shall_transmit = true
            }else if(angle <337.5 && angle > 292.5 && strength > 95){
                Log.d("DIR","back right")
                instruction_id = 2
                shall_transmit = true
            }else if(angle <292.5 && angle > 247.5 && strength > 95){
                Log.d("DIR","Extreme back")
                instruction_id = 3
                shall_transmit = true
            }else if(angle <247.5 && angle > 202.5 && strength > 95){
                Log.d("DIR","Back left")
                instruction_id = 4
                shall_transmit = true
            }else if(angle <202.5 && angle > 157.5 && strength > 95){
                Log.d("DIR","Extreme left")
                instruction_id = 5
                shall_transmit = true
            }else if(angle <157.5 && angle > 112.5 && strength > 95){
                Log.d("DIR","Front left")
                instruction_id = 6
                shall_transmit = true
            }else if(angle <112.5 && angle > 67.5 && strength > 95){
                Log.d("DIR","Extreme Front")
                instruction_id = 7
                shall_transmit = true
            }else if(angle <67.5 && angle > 22.5 && strength > 95){
                Log.d("DIR","Right front")
                instruction_id = 8
                shall_transmit = true
            }
            if(strength < 95){
                Log.d("DIR","Stop")
                instruction_id = 0
                shall_transmit = true
            }
            if(instruction_id != prev_instruction){
                prev_instruction = instruction_id
                transmit_data.put("x", instruction_id)
                vibrate()
                CoroutineScope(IO).launch {
                    Log.d("TAG,","Transmit now")
                    client_write(connect, transmit_data.toString()+"\n")
                }
            }

        }

        lamp_switch.setOnCheckedChangeListener{
            vibrate()
            if(it){
                transmit_data.put("l",1)
                CoroutineScope(IO).launch {
                   client_write(connect, transmit_data.toString()+"\n")
                }
            }else{
                transmit_data.put("l",0)
                CoroutineScope(IO).launch {
                     client_write(connect, transmit_data.toString()+"\n")
                 }
            }

        }

        water_switch.setOnCheckedChangeListener{
            vibrate()
            if(it){
                transmit_data.put("w",1)
                CoroutineScope(IO).launch {
                    client_write(connect, transmit_data.toString()+"\n")
                }
            }else{
                transmit_data.put("w",0)
                CoroutineScope(IO).launch {
                    client_write(connect, transmit_data.toString()+"\n")
                }
            }
        }

        camera_switch.setOnCheckedChangeListener{
            vibrate()
            if(it){
                Toast.makeText(this,"THis shuold not run",Toast.LENGTH_SHORT)
            }else{
                camera_switch.setChecked(true)
                vibrate()
            }

        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun loadIpCam() {
        Mjpeg.newInstance()
            .open("http://192.168.43.106:81/stream", 5)
            .subscribe(
                { inputStream: MjpegInputStream? ->
                    mjpegView.setSource(inputStream)
                    mjpegView.setDisplayMode(DisplayMode.FULLSCREEN)
                    mjpegView.flipHorizontal(false)
                    mjpegView.flipVertical(false)
                    mjpegView.setRotate(90F)
                    mjpegView.showFps(true)
                }
            ) { throwable: Throwable? ->
                Log.e(javaClass.simpleName, "mjpeg error", throwable)
                Toast.makeText(this, "Error", Toast.LENGTH_LONG).show()
            }
    }

    fun vibrate(){
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(60)
        }
    }
    override fun onResume() {
        super.onResume()
        loadIpCam()
    }

    override fun onPause(){
        if(mjpegView.isStreaming){
            mjpegView.clearStream()
            super.onPause()
        }

    }

    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            finishAffinity()
            finish()
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Press back button twice to exit", Toast.LENGTH_SHORT).show()

        Handler().postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }

    private suspend fun client_connect(address:String, port:Int): Socket {
        val connection = Socket(address,port)
        return connection
    }

    private suspend fun client_receive_data(connector:Socket, temperature_text:TextView, humidity_text:TextView, fire_extenguisher_status:TextView, obstacle_status:TextView){
        var reader = Scanner(connector.getInputStream())
        while(active){
                if(reader.hasNextLine()){
                    val data = reader.nextLine()
                    Log.d("ESP8266",  data)
                    val gson = Gson()
                    try {
                        val temp_data: Map<String, Any> = Gson().fromJson(
                            data,
                            object : TypeToken<HashMap<String?, Any?>?>() {}.type
                        )
                        withContext(Dispatchers.Main) {
                            temperature_text.text = temp_data.get("tem").toString() + " C"
                            humidity_text.text = temp_data.get("hum").toString() + " %"
                            var dd = temp_data["fir"].toString()
                            if(dd == "1.0"){
                                fire_extenguisher_status.text = "YES"
                            }else{
                                fire_extenguisher_status.text = "NO"
                            }
                            obstacle_status.text = temp_data.get("obs").toString() + " CM"
                        }

                    }catch (e:Exception){
                        Log.d("TAG",e.toString())
                    }

                }

        }
        reader.close()

    }


    private suspend fun client_write(connector: Socket, data:String){
            val writer = connector.getOutputStream()
        Log.d("CHECK", data.toString())
        Log.d("CHECKK",data.toByteArray().toString())
            val byteArray = data.toByteArray()
            writer.write(byteArray)
    }

    /*
    private suspend fun client(address:String, port:Int){
        val connection = Socket(address, port)
        var writer = connection.getOutputStream()
        var reader = Scanner(connection.getInputStream())
        while(active){
            val data = reader.nextLine()
            Log.d("ESP8266",  data)
        }
        reader.close()
        writer.close()
        connection.close()
    }*/
}