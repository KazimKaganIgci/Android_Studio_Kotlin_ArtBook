package com.kazim.artbook
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.Image
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.kazim.artbook.databinding.ActivityArtBinding
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.Exception


class ArtActivity : AppCompatActivity() {
    private lateinit var binding:ActivityArtBinding
    private lateinit var activityResultLauncher:ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher:ActivityResultLauncher<String>
    var selectedBitmap:Bitmap ?=null;
    private lateinit var database: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view =binding.root
        setContentView(view)

        database=openOrCreateDatabase("Arts", MODE_PRIVATE,null)

        registerLauncher()

        val intent=intent
        val info =intent.getStringExtra("info")

        if (info.equals("new")){
            binding.ArtNameText.setText("")
            binding.ArtistNameText.setText("")
            binding.yearText.setText(" ")
            binding.button.visibility =View.VISIBLE
        }else{
            binding.button.visibility=View.INVISIBLE
            val selectedId =intent.getIntExtra("id",1)
            val cursor =database.rawQuery("SELECT * FROM arts WHERE id =?", arrayOf(selectedId.toString()))
            val artNameIx= cursor.getColumnIndex("artName")
            val artistNameIx =cursor.getColumnIndex("artistName")
            val yearIx =cursor.getColumnIndex("year")
            val imageIx =cursor.getColumnIndex("image")
            while (cursor.moveToNext()){
                binding.ArtNameText.setText(cursor.getString(artNameIx))
                binding.ArtistNameText.setText(cursor.getString(artistNameIx))
                binding.yearText.setText(cursor.getString(yearIx))
                val byteArray = cursor.getBlob(imageIx)
                val bitmap =BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }
            cursor.close()
        }


        
    }

    fun saveButtonClicked(view: View){
        var artName =binding.ArtNameText.text.toString()
        var artistName =binding.ArtistNameText.text.toString()
        var year = binding.yearText.text.toString()

        if (selectedBitmap !=null){
            val smallerBitmap =makeSmallerBitmap(selectedBitmap!!,300)
            val outputStream =ByteArrayOutputStream()
            smallerBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray =outputStream.toByteArray()
            try {
              //  val database =this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artName VARCHAR,artistName VARCHAR,year VARCHAR,image BLOB)")
                val  sqlString ="INSERT INTO arts(artName,artistName,year,image) VALUES(?,?,?,?)"
                val statement =database.compileStatement(sqlString)// direk bağlanmıyor kontrolü kendimiz sağlıyoruz..
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()
            }catch (e:Exception){
                e.printStackTrace()
            }

        }
        val intent =Intent(this@ArtActivity,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)


    }
    private fun makeSmallerBitmap(image:Bitmap,maxSize:Int):Bitmap{
        var width =image.width
        var height =image.height
        val bitmapRatio:Double =width.toDouble()/height.toDouble()
        if(bitmapRatio>1){
            width =maxSize
            val h =width/bitmapRatio
            height=h.toInt()

        }else{
            height=maxSize
            val w =height*bitmapRatio
            width =w.toInt()

        }



        return Bitmap.createScaledBitmap(image,width,height,true)


    }

    fun selectImage(view:View){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permisson needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permisson",
                    View.OnClickListener {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()


            }else{
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

            }

        }else{
            val galleryIntent =Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(galleryIntent)
        }

    }

    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    val imageData = intentFromResult.data
                    try {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(this@ArtActivity.contentResolver, imageData!!)
                            selectedBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(selectedBitmap)
                        } else {
                            selectedBitmap = MediaStore.Images.Media.getBitmap(this@ArtActivity.contentResolver, imageData)
                            binding.imageView.setImageBitmap(selectedBitmap)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                //permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            } else {
                //permission denied
                Toast.makeText(this@ArtActivity, "Permisson needed!  ", Toast.LENGTH_LONG).show()
            }
        }
    }
}