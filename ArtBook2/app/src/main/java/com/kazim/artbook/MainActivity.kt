package com.kazim.artbook

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.kazim.artbook.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var artList : ArrayList<Art>
    private lateinit var artAdapter : ArtAdapter
    private lateinit var binding:ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view =binding.root
        setContentView(view)
        artList = ArrayList<Art>()
        artAdapter = ArtAdapter(artList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = artAdapter

        try {

            val database = this.openOrCreateDatabase("Arts", Context.MODE_PRIVATE,null)

            val cursor = database.rawQuery("SELECT * FROM arts",null)
            val artNameIx = cursor.getColumnIndex("artName")
            val idIx = cursor.getColumnIndex("id")
            val imageIx =cursor.getColumnIndex("image")

            while (cursor.moveToNext()) {
                val name = cursor.getString(artNameIx)
                val id = cursor.getInt(idIx)
                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                val art = Art(name,id,bitmap)
                artList.add(art)
            }

            artAdapter.notifyDataSetChanged()

            cursor.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater =menuInflater
        menuInflater.inflate(R.menu.art_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId ==R.id.itemid_art){
            val intent =Intent(this@MainActivity,ArtActivity::class.java)
            intent.putExtra("info","new")
            startActivity(intent)

        }
        return super.onOptionsItemSelected(item)
    }
}